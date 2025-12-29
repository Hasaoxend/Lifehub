import { useState, useEffect } from 'react';
import { 
  collection, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  doc, 
  serverTimestamp,
  Timestamp
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { useAuth } from './useAuth';
import { safeToDate } from '../utils/dateUtils';

export interface CalendarEvent {
  documentId?: string;
  title: string;
  titleType?: string; // 'work', 'meeting', 'event', 'personal'
  description: string;
  startTime: Date;
  endTime: Date;
  location: string;
  color: string;
  userOwnerId: string;
  reminderTime?: Date | null;
  repeatType: 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
  repeatUntil?: Date | null;
  lastModified?: Date;
  createdDate?: Date;
}


export function useCalendar() {
  const { user } = useAuth();
  const [events, setEvents] = useState<CalendarEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      setEvents([]);
      setLoading(false);
      return;
    }

    const eventsRef = collection(db, 'users', user.uid, 'calendar_events');
    
    const unsubscribe = onSnapshot(eventsRef, (snapshot) => {
      try {
        const eventList: CalendarEvent[] = [];
        snapshot.forEach((doc) => {
          const data = doc.data();
          eventList.push({
            documentId: doc.id,
            title: data.title || '',
            titleType: data.titleType || 'work',
            description: data.description || '',
            startTime: safeToDate(data.startTime) || new Date(),
            endTime: safeToDate(data.endTime) || new Date(),
            location: data.location || '',
            color: data.color || '#4285f4',
            userOwnerId: data.userOwnerId || user.uid,
            reminderTime: safeToDate(data.reminderTime),
            repeatType: data.repeatType || 'NONE',
            repeatUntil: safeToDate(data.repeatUntil),
            lastModified: safeToDate(data.lastModified),
            createdDate: safeToDate(data.createdDate)
          });
        });

        eventList.sort((a, b) => a.startTime.getTime() - b.startTime.getTime());
        setEvents(eventList);
        setLoading(false);
        setError(null);
      } catch (err: any) {
        setError(err.message);
        setLoading(false);
      }
    }, (err) => {
      setError(err.message);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [user]);

  const validateEvent = (event: Partial<CalendarEvent>) => {
      if (event.startTime && event.endTime && event.endTime <= event.startTime) {
          throw new Error('Thời gian kết thúc phải sau thời gian bắt đầu.');
      }
      
      const maxFuture = new Date();
      maxFuture.setFullYear(maxFuture.getFullYear() + 5);
      if (event.startTime && event.startTime > maxFuture) {
          throw new Error('Không thể tạo sự kiện quá 5 năm trong tương lai.');
      }
  };

  const addEvent = async (event: Omit<CalendarEvent, 'documentId' | 'userOwnerId' | 'lastModified' | 'createdDate'>) => {
    if (!user) return;
    
    validateEvent(event);

    // Auto-schedule reminder (15 mins before) if not set
    let reminderTime = event.reminderTime;
    if (!reminderTime) {
        reminderTime = new Date(event.startTime.getTime() - 15 * 60 * 1000);
        // Only set if reminder is in the future
        if (reminderTime.getTime() <= Date.now()) {
            reminderTime = null;
        }
    }

    try {
      const eventsRef = collection(db, 'users', user.uid, 'calendar_events');
      await addDoc(eventsRef, {
        ...event,
        reminderTime: reminderTime ? Timestamp.fromDate(reminderTime) : null,
        userOwnerId: user.uid,
        createdDate: serverTimestamp(),
        lastModified: serverTimestamp()
      });
    } catch (err: any) {
      throw err;
    }
  };

  const updateEvent = async (documentId: string, event: Partial<CalendarEvent>) => {
    if (!user) return;
    
    validateEvent(event);

    try {
      const eventRef = doc(db, 'users', user.uid, 'calendar_events', documentId);
      const { documentId: _, lastModified, createdDate, userOwnerId, ...updateData } = event as any;
      
      await updateDoc(eventRef, {
        ...updateData,
        lastModified: serverTimestamp()
      });
    } catch (err: any) {
      throw err;
    }
  };

  const deleteEvent = async (documentId: string) => {
    if (!user) return;
    try {
      const eventRef = doc(db, 'users', user.uid, 'calendar_events', documentId);
      await deleteDoc(eventRef);
    } catch (err: any) {
      throw err;
    }
  };

  return {
    events,
    loading,
    error,
    addEvent,
    updateEvent,
    deleteEvent
  };
}
