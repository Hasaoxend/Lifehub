import { useState, useEffect, useCallback } from 'react';
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
import { generateTOTP } from '../utils/totp';
import { decrypt } from '../utils/encryption';
import { 
  getCorrectedTimestamp, 
  getCorrectedRemainingSeconds, 
  getCorrectedProgress
} from '../utils/timeSync';


interface TotpAccount {
  documentId?: string;
  secretKey: string;
  accountName: string;
  issuer?: string;
  digits?: number;
  period?: number;
  algorithm?: string;
  userOwnerId: string;
  lastModified?: Date;
}

interface TotpWithCode extends TotpAccount {
  currentCode: string;
  remainingSeconds: number;
  progress: number;
}

export function useTOTP() {
  const { user, encryptionKey } = useAuth();
  const [totpAccounts, setTotpAccounts] = useState<TotpAccount[]>([]);
  const [totpWithCodes, setTotpWithCodes] = useState<TotpWithCode[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch TOTP accounts from Firestore
  // IMPORTANT: Only fetch when we have the encryption key
  useEffect(() => {
    if (!user) {
      setTotpAccounts([]);
      setLoading(false);
      return;
    }

    // Don't try to load accounts until we have the encryption key
    // Without it, we can't decrypt the secrets and will get base32 errors
    if (!encryptionKey) {
      setTotpAccounts([]);
      setLoading(true); // Still loading until key is available
      return;
    }
    
    console.log('[TOTP] Starting listener, user:', user.uid, 'hasKey:', !!encryptionKey);

    const totpRef = collection(db, 'users', user.uid, 'totp_accounts');
    // Removed orderBy to avoid Firestore index requirement

    const unsubscribe = onSnapshot(
      totpRef,
      async (snapshot) => {
        console.log('[TOTP] Snapshot received, docs:', snapshot.size);
        const list: TotpAccount[] = [];
        
        for (const docSnap of snapshot.docs) {
          const data = docSnap.data();
          let secretKey = data.secretKey || '';
          
          // Decrypt secret key if we have the encryption key
          if (encryptionKey && data.secretKey) {
            try {
              secretKey = await decrypt(data.secretKey, encryptionKey);
              console.log('[TOTP] Decrypted successfully:', data.accountName);
            } catch (err) {
              console.error('[TOTP] Decryption failed for', data.accountName, '- keeping raw value');
              // Keep original value - might be unencrypted legacy data
              secretKey = data.secretKey;
            }
          }
          
          // IMPORTANT: documentId must come AFTER ...data to prevent being overwritten
          list.push({
            ...data,
            documentId: docSnap.id, // Must be after spread to ensure correct value
            secretKey,
            lastModified: data.lastModified?.toDate()
          } as TotpAccount);
        }
        
        setTotpAccounts(list);
        setLoading(false);
        setError(null);
      },
      (err) => {

        console.error('Error fetching TOTP accounts:', err);
        setError(err.message);
        setLoading(false);
      }
    );

    return () => unsubscribe();
  }, [user, encryptionKey]);


  // Generate codes and update remaining time
  const updateCodes = useCallback(async () => {
    const now = getCorrectedTimestamp(); // Use corrected time
    const updated: TotpWithCode[] = await Promise.all(
      totpAccounts.map(async (account) => {
        const period = account.period || 30;
        const digits = account.digits || 6;
        
        try {
          const code = await generateTOTP(account.secretKey, digits, period, now);
          return {
            ...account,
            currentCode: code,
            remainingSeconds: getCorrectedRemainingSeconds(period),
            progress: getCorrectedProgress(period)
          };
        } catch (err) {
          console.error('Error generating TOTP for', account.accountName, err);
          return {
            ...account,
            currentCode: '------',
            remainingSeconds: 0,
            progress: 0
          };
        }
      })
    );
    setTotpWithCodes(updated);
  }, [totpAccounts]);


  // Update codes every second
  useEffect(() => {
    updateCodes();
    const interval = setInterval(updateCodes, 1000);
    return () => clearInterval(interval);
  }, [updateCodes]);

  const addTotpAccount = async (account: Omit<TotpAccount, 'documentId' | 'userOwnerId' | 'lastModified'>) => {
    if (!user) throw new Error('Not authenticated');
    if (!encryptionKey) throw new Error('Encryption not initialized');
    
    // Encrypt the secret key before saving
    const { encrypt } = await import('../utils/encryption');
    const encryptedSecret = await encrypt(account.secretKey, encryptionKey);
    
    const totpRef = collection(db, 'users', user.uid, 'totp_accounts');
    await addDoc(totpRef, {
      ...account,
      secretKey: encryptedSecret,
      userOwnerId: user.uid,
      lastModified: new Date()
    });
  };

  const updateTotpAccount = async (documentId: string, updates: Partial<TotpAccount>) => {
    if (!user) throw new Error('Not authenticated');
    
    const totpDocRef = doc(db, 'users', user.uid, 'totp_accounts', documentId);
    await updateDoc(totpDocRef, {
      ...updates,
      lastModified: new Date()
    });
  };

  const deleteTotpAccount = async (documentId: string) => {
    if (!user) throw new Error('Not authenticated');
    if (!documentId) throw new Error('Document ID is required');
    
    const totpDocRef = doc(db, 'users', user.uid, 'totp_accounts', documentId);
    await deleteDoc(totpDocRef);
  };

  return {
    totpAccounts: totpWithCodes,
    loading,
    error,
    addTotpAccount,
    updateTotpAccount,
    deleteTotpAccount
  };
}
