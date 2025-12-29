import { useState, useEffect } from 'react';
import { 
  collection, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  doc
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { useAuth } from './useAuth';
import { safeToDate } from '../utils/dateUtils';
import { encrypt, decrypt } from '../utils/encryption';

export interface NoteEntry {
  documentId?: string;
  title: string;
  content: string;
  lastModified?: Date;
  userOwnerId?: string;
  reminderTime?: Date | null;
}

export function useNotes() {
  const { user, encryptionKey, isUnlocked } = useAuth();
  const [notes, setNotes] = useState<NoteEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      setNotes([]);
      setLoading(false);
      return;
    }

    if (!encryptionKey) {
      setLoading(true);
      return;
    }

    const notesRef = collection(db, 'users', user.uid, 'notes');

    const unsubscribe = onSnapshot(
      notesRef,
      async (snapshot) => {
        try {
          const notesList: NoteEntry[] = [];
          
          for (const docSnap of snapshot.docs) {
            const data = docSnap.data();
            
            // Decrypt title and content
            let decryptedTitle = data.title || '';
            let decryptedContent = data.content || '';

            if (encryptionKey && data.title && data.title.length > 20) {
              try {
                decryptedTitle = await decrypt(data.title, encryptionKey);
              } catch (e: any) {
                // Silently fall back to raw if decryption fails (likely legacy data)
              }
            }
            
            if (encryptionKey && data.content && data.content.length > 20) {
              try {
                decryptedContent = await decrypt(data.content, encryptionKey);
              } catch (e: any) {
                // Silently fall back to raw
              }
            }
            
            notesList.push({
              documentId: docSnap.id,
              title: decryptedTitle,
              content: decryptedContent,
              lastModified: safeToDate(data.lastModified),
              userOwnerId: data.userOwnerId || '',
              reminderTime: safeToDate(data.reminderTime) || null
            });
          }
          
          // Sort client-side by lastModified desc
          notesList.sort((a, b) => {
            const timeA = a.lastModified?.getTime() || 0;
            const timeB = b.lastModified?.getTime() || 0;
            return timeB - timeA;
          });
          
          setNotes(notesList);
          setLoading(false);
          setError(null);
        } catch (err: any) {
          console.error('[Notes] Mapping error:', err);
          setError('Lỗi xử lý dữ liệu: ' + err.message);
          setLoading(false);
        }
      },
      (err) => {
        console.error('[Notes] Firestore error:', err);
        setError('Lỗi kết nối: ' + err.message);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [user, encryptionKey]);

  const addNote = async (note: Omit<NoteEntry, 'documentId' | 'userOwnerId' | 'lastModified'>) => {
    if (!user) throw new Error('Not authenticated');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    console.log('[Notes] Adding note, encrypting...');
    // Encrypt title and content before saving
    const encryptedTitle = note.title ? await encrypt(note.title, encryptionKey) : '';
    const encryptedContent = note.content ? await encrypt(note.content, encryptionKey) : '';
    
    const notesRef = collection(db, 'users', user.uid, 'notes');
    const docRef = await addDoc(notesRef, {
      title: encryptedTitle,
      content: encryptedContent,
      reminderTime: note.reminderTime || null,
      userOwnerId: user.uid,
      lastModified: new Date()
    });
    console.log('[Notes] Note added with ID:', docRef.id);
    return docRef.id;
  };

  const updateNote = async (documentId: string, updates: Partial<NoteEntry>) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    console.log('[Notes] Updating note:', documentId);
    const dataToUpdate: any = { lastModified: new Date() };
    
    // Encrypt fields if they are being updated
    if (updates.title !== undefined) {
      dataToUpdate.title = updates.title ? await encrypt(updates.title, encryptionKey) : '';
    }
    if (updates.content !== undefined) {
      dataToUpdate.content = updates.content ? await encrypt(updates.content, encryptionKey) : '';
    }
    if (updates.reminderTime !== undefined) {
      dataToUpdate.reminderTime = updates.reminderTime;
    }
    
    const noteRef = doc(db, 'users', user.uid, 'notes', documentId);
    await updateDoc(noteRef, dataToUpdate);
  };

  const deleteNote = async (documentId: string) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const noteRef = doc(db, 'users', user.uid, 'notes', documentId);
    await deleteDoc(noteRef);
  };

  return {
    notes,
    loading,
    error,
    addNote,
    updateNote,
    deleteNote,
    isUnlocked
  };
}
