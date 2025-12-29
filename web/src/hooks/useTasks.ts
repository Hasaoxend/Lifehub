import { useState, useEffect } from 'react';
import { 
  collection, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  doc,
  query,
  where
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { useAuth } from './useAuth';
import { safeToDate } from '../utils/dateUtils';
import { encrypt, decrypt } from '../utils/encryption';

export interface TaskEntry {
  documentId?: string;
  name: string;
  completed: boolean;
  taskType: number; // 0 = Task, 1 = Shopping
  projectId?: string | null;
  lastModified?: Date;
  userOwnerId?: string;
  reminderTime?: Date | null;
}

export interface ProjectEntry {
  documentId?: string;
  name: string;
  lastModified?: Date;
  userOwnerId?: string;
}

export function useTasks() {
  const { user, encryptionKey, isUnlocked } = useAuth();
  const [tasks, setTasks] = useState<TaskEntry[]>([]);
  const [projects, setProjects] = useState<ProjectEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch tasks (taskType = 0)
  useEffect(() => {
    if (!user) {
      setTasks([]);
      setLoading(false);
      return;
    }

    if (!encryptionKey) {
      console.log('[Tasks] Waiting for encryption key...');
      setLoading(true);
      return;
    }

    console.log('[Tasks] Starting listener for user:', user.uid);

    const tasksRef = collection(db, 'users', user.uid, 'tasks');
    // Get all tasks, we'll filter client-side for compatibility
    const unsubscribe = onSnapshot(
      tasksRef,
      async (snapshot) => {
        try {
          console.log('[Tasks] Snapshot event: size =', snapshot.size);
          const tasksList: TaskEntry[] = [];
          
          for (const docSnap of snapshot.docs) {
            const data = docSnap.data();
            
            // Handle field name compatibility: Extension uses 'title', Web uses 'name'
            const rawName = data.title || data.name || '';
            
            // Decrypt name (title)
            let decryptedName = data.title || data.name || '';
            if (encryptionKey && decryptedName && decryptedName.length > 20) {
              try {
                decryptedName = await decrypt(decryptedName, encryptionKey);
              } catch (e: any) {
                // Silently fall back to raw
              }
            }
            // Handle field name compatibility: Extension uses 'type', Web uses 'taskType'
            const taskType = data.taskType !== undefined ? Number(data.taskType) : (data.type !== undefined ? Number(data.type) : 0);
            
            // Only include regular tasks (type 0)
            if (taskType === 0) {
              tasksList.push({
                documentId: docSnap.id,
                name: decryptedName,
                completed: data.completed || false,
                taskType: 0,
                projectId: data.projectId || null,
                lastModified: safeToDate(data.lastModified),
                userOwnerId: data.userOwnerId || '',
                reminderTime: safeToDate(data.reminderTime) || null
              });
            }
          }
          
          // Sort: incomplete first, then by lastModified desc
          tasksList.sort((a, b) => {
            if (a.completed !== b.completed) return a.completed ? 1 : -1;
            const timeA = a.lastModified?.getTime() || 0;
            const timeB = b.lastModified?.getTime() || 0;
            return timeB - timeA;
          });
          
          console.log('[Tasks] Successfully mapped', tasksList.length, 'tasks');
          setTasks(tasksList);
          setLoading(false);
          setError(null);
        } catch (err: any) {
          console.error('[Tasks] Mapping error:', err);
          setError('Lỗi xử lý dữ liệu: ' + err.message);
          setLoading(false);
        }
      },
      (err) => {
        console.error('[Tasks] Firestore error:', err);
        setError('Lỗi kết nối: ' + err.message);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [user, encryptionKey]);

  // Fetch projects
  useEffect(() => {
    if (!user) {
      setProjects([]);
      return;
    }

    const projectsRef = collection(db, 'users', user.uid, 'projects');

    const unsubscribe = onSnapshot(
      projectsRef,
      (snapshot) => {
        try {
          console.log('[Projects] Snapshot event: size =', snapshot.size);
          const projectsList: ProjectEntry[] = [];
          
          snapshot.docs.forEach(docSnap => {
            const data = docSnap.data();
            projectsList.push({
              documentId: docSnap.id,
              name: data.name || '',
              lastModified: safeToDate(data.lastModified),
              userOwnerId: data.userOwnerId || ''
            });
          });
          
          setProjects(projectsList);
        } catch (err) {
          console.error('[Projects] Mapping error:', err);
        }
      },
      (err) => {
        console.error('[Projects] Firestore error:', err);
      }
    );

    return () => unsubscribe();
  }, [user]);

  const addTask = async (task: Omit<TaskEntry, 'documentId' | 'userOwnerId' | 'lastModified'>) => {
    if (!user) throw new Error('Not authenticated');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    // Encrypt task name
    const encryptedName = task.name ? await encrypt(task.name, encryptionKey) : '';
    
    const tasksRef = collection(db, 'users', user.uid, 'tasks');
    const docRef = await addDoc(tasksRef, {
      title: encryptedName, // Use 'title' for Extension compatibility
      name: encryptedName,  // Also save as 'name' for legacy Web compatibility
      completed: task.completed || false,
      taskType: 0,
      type: 0, // Also save as 'type' for Extension compatibility
      projectId: task.projectId || null,
      reminderTime: task.reminderTime || null,
      userOwnerId: user.uid,
      lastModified: new Date()
    });
    return docRef.id;
  };

  const updateTask = async (documentId: string, updates: Partial<TaskEntry>) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    const dataToUpdate: any = { lastModified: new Date() };
    
    if (updates.name !== undefined) {
      const encryptedName = updates.name ? await encrypt(updates.name, encryptionKey) : '';
      dataToUpdate.title = encryptedName;
      dataToUpdate.name = encryptedName;
    }
    if (updates.completed !== undefined) {
      dataToUpdate.completed = updates.completed;
    }
    if (updates.projectId !== undefined) {
      dataToUpdate.projectId = updates.projectId;
    }
    if (updates.reminderTime !== undefined) {
      dataToUpdate.reminderTime = updates.reminderTime;
    }
    
    const taskRef = doc(db, 'users', user.uid, 'tasks', documentId);
    await updateDoc(taskRef, dataToUpdate);
  };

  const toggleTaskCompletion = async (documentId: string, currentStatus: boolean) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const taskRef = doc(db, 'users', user.uid, 'tasks', documentId);
    await updateDoc(taskRef, { 
      completed: !currentStatus,
      lastModified: new Date()
    });
  };

  const deleteTask = async (documentId: string) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const taskRef = doc(db, 'users', user.uid, 'tasks', documentId);
    await deleteDoc(taskRef);
  };

  const addProject = async (name: string) => {
    if (!user) throw new Error('Not authenticated');
    
    const projectsRef = collection(db, 'users', user.uid, 'projects');
    const docRef = await addDoc(projectsRef, {
      name,
      userOwnerId: user.uid,
      lastModified: new Date()
    });
    return docRef.id;
  };

  const deleteProject = async (documentId: string) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const projectRef = doc(db, 'users', user.uid, 'projects', documentId);
    await deleteDoc(projectRef);
  };

  const getTasksByProject = (projectId: string | null) => {
    return tasks.filter(t => t.projectId === projectId);
  };

  return {
    tasks,
    projects,
    loading,
    error,
    addTask,
    updateTask,
    toggleTaskCompletion,
    deleteTask,
    addProject,
    deleteProject,
    getTasksByProject,
    isUnlocked
  };
}
