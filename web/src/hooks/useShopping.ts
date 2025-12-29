import { useState, useEffect } from 'react';
import { 
  collection, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  doc,
  writeBatch
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { useAuth } from './useAuth';
import { safeToDate } from '../utils/dateUtils';
import { encrypt, decrypt } from '../utils/encryption';

export interface ShoppingItem {
  documentId?: string;
  name: string;
  completed: boolean;
  lastModified?: Date;
  userOwnerId?: string;
}

export function useShopping() {
  const { user, encryptionKey, isUnlocked } = useAuth();
  const [items, setItems] = useState<ShoppingItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      setItems([]);
      setLoading(false);
      return;
    }

    if (!encryptionKey) {
      console.log('[Shopping] Waiting for encryption key...');
      setLoading(true);
      return;
    }

    console.log('[Shopping] Starting listener for user:', user.uid);

    // Get all tasks and filter client-side for type=1 (shopping)
    const tasksRef = collection(db, 'users', user.uid, 'tasks');

    const unsubscribe = onSnapshot(
      tasksRef,
      async (snapshot) => {
        try {
          console.log('[Shopping] Snapshot event: size =', snapshot.size);
          const itemsList: ShoppingItem[] = [];
          
          for (const docSnap of snapshot.docs) {
            const data = docSnap.data();
            
            // Handle field name compatibility for taskType
            const taskType = data.taskType !== undefined ? Number(data.taskType) : (data.type !== undefined ? Number(data.type) : 0);
            
            // Only include shopping items (type 1)
            if (taskType !== 1) continue;
            
            // Handle field name compatibility: Extension uses 'title', Web uses 'name'
            const rawName = data.title || data.name || '';
            
            // Decrypt item name
            let decryptedName = rawName;
            if (encryptionKey && rawName && rawName.length > 20) {
              try {
                decryptedName = await decrypt(rawName, encryptionKey);
              } catch (e) {
                // Silently fall back to raw
              }
            }
            
            itemsList.push({
              documentId: docSnap.id,
              name: decryptedName,
              completed: data.completed || false,
              lastModified: safeToDate(data.lastModified),
              userOwnerId: data.userOwnerId || ''
            });
          }
          
          // Sort: uncompleted first, then completed
          itemsList.sort((a, b) => {
            if (a.completed !== b.completed) {
              return a.completed ? 1 : -1;
            }
            return 0;
          });
          
          console.log('[Shopping] Successfully mapped', itemsList.length, 'items');
          setItems(itemsList);
          setLoading(false);
          setError(null);
        } catch (err: any) {
          console.error('[Shopping] Mapping error:', err);
          setError('Lỗi xử lý dữ liệu: ' + err.message);
          setLoading(false);
        }
      },
      (err) => {
        console.error('[Shopping] Firestore error:', err);
        setError('Lỗi kết nối: ' + err.message);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [user, encryptionKey]);

  const addItem = async (name: string) => {
    if (!user) throw new Error('Not authenticated');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    // Encrypt item name
    const encryptedName = name ? await encrypt(name, encryptionKey) : '';
    
    const tasksRef = collection(db, 'users', user.uid, 'tasks');
    const docRef = await addDoc(tasksRef, {
      title: encryptedName, // Use 'title' for Extension compatibility
      name: encryptedName,  // Also save as 'name' for legacy Web compatibility
      completed: false,
      taskType: 1,
      type: 1, // Also save as 'type' for Extension compatibility
      projectId: null,
      userOwnerId: user.uid,
      lastModified: new Date()
    });
    return docRef.id;
  };

  const updateItem = async (documentId: string, updates: Partial<ShoppingItem>) => {
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
    
    const itemRef = doc(db, 'users', user.uid, 'tasks', documentId);
    await updateDoc(itemRef, dataToUpdate);
  };

  const toggleItemCompletion = async (documentId: string, currentStatus: boolean) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const itemRef = doc(db, 'users', user.uid, 'tasks', documentId);
    await updateDoc(itemRef, { 
      completed: !currentStatus,
      lastModified: new Date()
    });
  };

  const deleteItem = async (documentId: string) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const itemRef = doc(db, 'users', user.uid, 'tasks', documentId);
    await deleteDoc(itemRef);
  };

  const clearCompleted = async () => {
    if (!user) throw new Error('Not authenticated');
    
    const completedItems = items.filter(item => item.completed);
    if (completedItems.length === 0) return;
    
    const batch = writeBatch(db);
    for (const item of completedItems) {
      if (item.documentId) {
        const itemRef = doc(db, 'users', user.uid, 'tasks', item.documentId);
        batch.delete(itemRef);
      }
    }
    await batch.commit();
  };

  const totalItems = items.length;
  const completedCount = items.filter(i => i.completed).length;
  const pendingCount = totalItems - completedCount;

  return {
    items,
    loading,
    error,
    addItem,
    updateItem,
    toggleItemCompletion,
    deleteItem,
    clearCompleted,
    totalItems,
    completedCount,
    pendingCount,
    isUnlocked
  };
}
