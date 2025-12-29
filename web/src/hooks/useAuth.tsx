import { createContext, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { User } from 'firebase/auth';
import { 
  onAuthStateChanged, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword,
  signOut as firebaseSignOut,
  sendPasswordResetEmail,
  sendEmailVerification,
  ActionCodeSettings
} from 'firebase/auth';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { auth, db } from '../firebase/config';
import { deriveKey, generateSalt, arrayToBase64, base64ToArray, storeSalt, encrypt, decrypt, VERIFICATION_STRING } from '../utils/encryption';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  encryptionKey: CryptoKey | null;
  isUnlocked: boolean;
  needsPasscodeSetup: boolean;
  requiresPasswordChange: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  unlockWithPasscode: (passcode: string) => Promise<CryptoKey>;
  setupEncryption: (passcode: string) => Promise<void>;
  changePasscode: (oldPasscode: string, newPasscode: string) => Promise<void>;
  resetPassword: (email: string, actionCodeSettings?: ActionCodeSettings) => Promise<void>;
  markPasswordAsStrong: () => Promise<void>;
}

const IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Store encryption key in memory (session-based)
let cachedEncryptionKey: CryptoKey | null = null;

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [encryptionKey, setEncryptionKey] = useState<CryptoKey | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      console.log('[Auth] Auth state changed:', user?.email);
      setUser(user);
      
      if (user) {
        // 1. Restore cached key if available
        if (cachedEncryptionKey) {
          setEncryptionKey(cachedEncryptionKey);
        }

        // 2. Check if setup is needed BEFORE we stop loading
        try {
          const userDoc = await getDoc(doc(db, 'users', user.uid));
          if (!userDoc.exists() || !userDoc.data()?.encryptionSalt) {
            setNeedsPasscodeSetup(true);
          } else {
            setNeedsPasscodeSetup(false);
          }
        } catch (err) {
          console.error('[Auth] Error checking setup:', err);
          setNeedsPasscodeSetup(false);
        }
      } else {
        // Clear state on logout
        cachedEncryptionKey = null;
        setEncryptionKey(null);
        setNeedsPasscodeSetup(false);
      }
      
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const [needsPasscodeSetup, setNeedsPasscodeSetup] = useState<boolean>(false);
  const [requiresPasswordChange, setRequiresPasswordChange] = useState<boolean>(false);

  // --- Auto Logout on Inactivity ---
  useEffect(() => {
    let timeoutId: ReturnType<typeof setTimeout>;

    const resetTimer = () => {
      if (!user) return; // Only track if logged in
      
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        console.log('[Auth] Session timed out due to inactivity');
        signOut();
      }, IDLE_TIMEOUT_MS);
    };

    // Events to track activity
    const events = ['mousedown', 'keydown', 'scroll', 'touchstart'];
    
    if (user) {
      // Initialize timer
      resetTimer();
      
      // Add listeners
      events.forEach(event => {
        window.addEventListener(event, resetTimer);
      });
    }

    return () => {
      clearTimeout(timeoutId);
      events.forEach(event => {
        window.removeEventListener(event, resetTimer);
      });
    };
  }, [user]); // Re-run when user logs in/out

  /**
   * Raw sign in (Authentication only)
   */
  const signIn = async (email: string, password: string) => {
    console.log('[Auth] Authenticating with Firebase...');
    await signInWithEmailAndPassword(auth, email, password);
    console.log('[Auth] Firebase authentication successful');
  };

  /**
   * Unlock encryption using passcode or recovery code
   */
  const unlockWithPasscode = async (passcode: string) => {
    if (!user) throw new Error("Must be logged in to unlock");
    
    console.log('[Auth] Unlocking with passcode...');
    const userDoc = await getDoc(doc(db, 'users', user.uid));
    const data = userDoc.data();
    
    if (!data?.encryptionSalt) {
      throw new Error("No encryption setup found");
    }

    const salt = base64ToArray(data.encryptionSalt);
    const key = await deriveKey(passcode, salt);

    // Verify key
    if (data.encryptionVerification) {
      try {
        const decrypted = await decrypt(data.encryptionVerification, key);
        if (decrypted !== VERIFICATION_STRING) {
          throw new Error("Invalid passcode");
        }
      } catch (e) {
        throw new Error("Invalid passcode");
      }
    }

    cachedEncryptionKey = key;
    setEncryptionKey(key);
    sessionStorage.setItem('lifehub_unlocked', 'true');
    
    // Check if password strength needs verification
    if (!data.passwordStrengthVerified) {
      console.log('[Auth] Password strength not verified, requiring password change');
      setRequiresPasswordChange(true);
    }
    
    console.log('[Auth] Vault unlocked successfully');
    return key;
  };

  /**
   * Setup encryption (Passcode + Salt + Verification)
   */
  const setupEncryption = async (passcode: string) => {
    if (!user) throw new Error("Must be logged in to setup");
    
    console.log('[Auth] Setting up encryption...');
    const salt = generateSalt();
    const key = await deriveKey(passcode, salt);
    const verificationString = await encrypt(VERIFICATION_STRING, key);

    await setDoc(doc(db, 'users', user.uid), {
      encryptionSalt: arrayToBase64(salt),
      encryptionVerification: verificationString,
      encryptionVersion: 2,
    }, { merge: true });

    storeSalt(user.uid, salt);
    cachedEncryptionKey = key;
    setEncryptionKey(key);
    setNeedsPasscodeSetup(false);
    sessionStorage.setItem('lifehub_unlocked', 'true');
    console.log('[Auth] Encryption setup complete');
  };

  const signUp = async (email: string, password: string) => {
    console.log('[Auth] Starting sign up...');
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    const uid = userCredential.user.uid;
    
    try {
      await sendEmailVerification(userCredential.user);
    } catch (e) {
      console.error('Email verification error:', e);
    }

    // Save initial user doc
    await setDoc(doc(db, 'users', uid), {
      email: email,
      createdAt: new Date(),
      encryptionVersion: 2
    });
    
    setNeedsPasscodeSetup(true);
    console.log('[Auth] Sign up successful, needs passcode setup');
  };

  /**
   * Change passcode (re-encrypt verification)
   */
  const changePasscode = async (oldPasscode: string, newPasscode: string) => {
    if (!user) throw new Error("Must be logged in");
    
    // 1. Verify old passcode
    const userDoc = await getDoc(doc(db, 'users', user.uid));
    const data = userDoc.data();
    if (!data?.encryptionSalt || !data?.encryptionVerification) {
      throw new Error("No encryption setup found");
    }

    const salt = base64ToArray(data.encryptionSalt);
    const oldKey = await deriveKey(oldPasscode, salt);

    try {
      const decrypted = await decrypt(data.encryptionVerification, oldKey);
      if (decrypted !== VERIFICATION_STRING) {
        throw new Error("Old passcode is incorrect");
      }
    } catch (e) {
      throw new Error("Old passcode is incorrect");
    }

    // 2. Setup with new passcode
    const newSalt = generateSalt();
    const newKey = await deriveKey(newPasscode, newSalt);
    const newVerification = await encrypt(VERIFICATION_STRING, newKey);

    await setDoc(doc(db, 'users', user.uid), {
      encryptionSalt: arrayToBase64(newSalt),
      encryptionVerification: newVerification,
      encryptionVersion: 2
    }, { merge: true });

    cachedEncryptionKey = newKey;
    setEncryptionKey(newKey);
    console.log('[Auth] Passcode changed successfully');
  };

  const signOut = async () => {
    cachedEncryptionKey = null;
    setEncryptionKey(null);
    setNeedsPasscodeSetup(false);
    sessionStorage.setItem('lifehub_unlocked', 'false');
    await firebaseSignOut(auth);
  };

  /**
   * Mark password as verified strong (called after user changes to a strong password)
   */
  const markPasswordAsStrong = async () => {
    if (!user) return;
    await setDoc(doc(db, 'users', user.uid), {
      passwordStrengthVerified: true
    }, { merge: true });
    setRequiresPasswordChange(false);
  };

  const value = {
    user,
    loading,
    encryptionKey,
    isUnlocked: encryptionKey !== null,
    needsPasscodeSetup,
    requiresPasswordChange,
    signIn,
    signUp,
    signOut,
    unlockWithPasscode,
    setupEncryption,
    changePasscode,
    resetPassword: async (email: string, actionCodeSettings?: ActionCodeSettings) => {
      await sendPasswordResetEmail(auth, email, actionCodeSettings);
      if (user) {
        await setDoc(doc(db, 'users', user.uid), {
          passwordStrengthVerified: false
        }, { merge: true });
        setRequiresPasswordChange(true);
      }
    },
    markPasswordAsStrong
  };



  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
