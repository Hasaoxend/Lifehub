import { useState, useEffect } from 'react';
import { 
  collection, 
  onSnapshot, 
  addDoc, 
  updateDoc, 
  deleteDoc, 
  doc,
  query,
  orderBy
} from 'firebase/firestore';
import { db } from '../firebase/config';
import { useAuth } from './useAuth';
import { encrypt, decrypt } from '../utils/encryption';

interface AccountEntry {
  documentId?: string;
  serviceName: string;
  username: string;
  password: string;
  websiteUrl?: string;
  notes?: string;
  customFields?: Record<string, { value: string; type: number }>;
  userOwnerId: string;
  lastModified?: Date;
  isEncryptionError?: boolean;
}

export function useAccounts() {
  const { user, encryptionKey, isUnlocked } = useAuth();
  const [accounts, setAccounts] = useState<AccountEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) {
      setAccounts([]);
      setLoading(false);
      return;
    }

    // Don't load accounts until we have the encryption key
    // Without it, passwords will show as encrypted gibberish
    if (!encryptionKey) {
      setAccounts([]);
      setLoading(true); // Still loading until key is available
      return;
    }

    const accountsRef = collection(db, 'users', user.uid, 'accounts');
    const q = query(accountsRef, orderBy('lastModified', 'desc'));

    const unsubscribe = onSnapshot(
      q,
      async (snapshot) => {
        const accountsList: AccountEntry[] = [];
        
        for (const docSnap of snapshot.docs) {
          const data = docSnap.data();
          let decryptedPassword = data.password || '';
          let isEncryptionError = false;
          
          // Decrypt password if we have the key
          if (encryptionKey && data.password) {
            try {
              decryptedPassword = await decrypt(data.password, encryptionKey);
            } catch (err) {
              console.error('Error decrypting password:', err);
              // If decryption fails, mark as error so UI can handle it
              decryptedPassword = data.password;
              isEncryptionError = true;
            }
          }
          
          accountsList.push({
            documentId: docSnap.id,
            serviceName: data.serviceName || '',
            username: data.username || '',
            password: decryptedPassword,
            websiteUrl: data.websiteUrl || '',
            notes: data.notes || '',
            customFields: data.customFields || undefined,
            userOwnerId: data.userOwnerId || '',
            lastModified: data.lastModified?.toDate(),
            isEncryptionError
          });
        }
        
        setAccounts(accountsList);
        setLoading(false);
        setError(null);
      },
      (err) => {
        console.error('Error fetching accounts:', err);
        setError(err.message);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [user, encryptionKey]);

  const addAccount = async (account: Omit<AccountEntry, 'documentId' | 'userOwnerId' | 'lastModified'>) => {
    if (!user) throw new Error('Not authenticated');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    // Encrypt password before saving
    const encryptedPassword = await encrypt(account.password, encryptionKey);
    
    const accountsRef = collection(db, 'users', user.uid, 'accounts');
    await addDoc(accountsRef, {
      ...account,
      password: encryptedPassword,
      userOwnerId: user.uid,
      lastModified: new Date()
    });
  };

  const updateAccount = async (documentId: string, updates: Partial<AccountEntry>) => {
    if (!user) throw new Error('Not authenticated');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    // Encrypt password if it's being updated
    const dataToUpdate = { ...updates };
    if (updates.password) {
      dataToUpdate.password = await encrypt(updates.password, encryptionKey);
    }
    
    const accountRef = doc(db, 'users', user.uid, 'accounts', documentId);
    await updateDoc(accountRef, {
      ...dataToUpdate,
      lastModified: new Date()
    });
  };

  const deleteAccount = async (documentId: string) => {
    if (!user) throw new Error('Not authenticated');
    
    const accountRef = doc(db, 'users', user.uid, 'accounts', documentId);
    await deleteDoc(accountRef);
  };

  return {
    accounts,
    loading,
    error,
    isUnlocked,
    addAccount,
    updateAccount,
    deleteAccount
  };
}
