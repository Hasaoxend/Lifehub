/**
 * Encryption utilities compatible with Android EncryptionHelper
 * Uses Web Crypto API with AES-256-GCM
 * 
 * ⚠️ IMPORTANT: 
 * - This uses PBKDF2 to derive key from master password
 * - The salt should be stored securely and synced with Android app
 * - For full compatibility, Android app needs to use same key derivation
 */

const ALGORITHM = 'AES-GCM';
const KEY_LENGTH = 256;
const IV_LENGTH = 12; // 96 bits for GCM
const SALT_LENGTH = 16; // 128 bits
const ITERATIONS = 100000; // OWASP recommendation for PBKDF2-HMAC-SHA256

export const VERIFICATION_STRING = "LIFEHUB_VERIFY";

/**
 * Generate a random 32-character recovery code
 */
export function generateRecoveryCode(): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  const values = crypto.getRandomValues(new Uint8Array(24));
  let result = '';
  for (let i = 0; i < 24; i++) {
    result += chars[values[i] % chars.length];
    if ((i + 1) % 4 === 0 && i !== 23) result += '-';
  }
  return result;
}

/**
 * Derive encryption key from passcode or recovery code using PBKDF2
 */
export async function deriveKey(
  secret: string, 
  salt: Uint8Array
): Promise<CryptoKey> {
  const encoder = new TextEncoder();
  const secretBuffer = encoder.encode(secret);
  
  // Import secret as raw key material
  const keyMaterial = await crypto.subtle.importKey(
    'raw',
    secretBuffer,
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );
  
  // Derive AES key using PBKDF2
  return crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: salt as any,
      iterations: ITERATIONS,
      hash: 'SHA-256'
    },
    keyMaterial,
    { name: ALGORITHM, length: KEY_LENGTH },
    false,
    ['encrypt', 'decrypt']
  );
}


/**
 * Generate a random salt
 */
export function generateSalt(): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
}

/**
 * Generate a random IV for AES-GCM
 */
function generateIV(): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(IV_LENGTH));
}

/**
 * Encrypt plaintext using AES-256-GCM
 * Returns: base64(iv + ciphertext)
 */
export async function encrypt(
  plaintext: string, 
  key: CryptoKey
): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(plaintext);
  const iv = generateIV();
  
  const ciphertext = await crypto.subtle.encrypt(
    { name: ALGORITHM, iv: iv },
    key,
    data
  );
  
  // Combine IV + ciphertext
  const combined = new Uint8Array(iv.length + ciphertext.byteLength);
  combined.set(iv, 0);
  combined.set(new Uint8Array(ciphertext), iv.length);
  
  // Return as base64
  return btoa(String.fromCharCode(...combined));
}

/**
 * Decrypt ciphertext using AES-256-GCM
 * Input: base64(iv + ciphertext)
 */
export async function decrypt(
  ciphertext: string, 
  key: CryptoKey
): Promise<string> {
  // Decode base64
  const combined = Uint8Array.from(atob(ciphertext), c => c.charCodeAt(0)) as any;
  
  // Extract IV and ciphertext
  const iv = combined.slice(0, IV_LENGTH);
  const data = combined.slice(IV_LENGTH);
  
  const decrypted = await crypto.subtle.decrypt(
    { name: ALGORITHM, iv: iv as Uint8Array },
    key,
    data as Uint8Array
  );
  
  const decoder = new TextDecoder();
  return decoder.decode(decrypted);
}

/**
 * Utility: Convert Uint8Array to base64 string
 */
export function arrayToBase64(array: Uint8Array): string {
  return btoa(String.fromCharCode(...array));
}

/**
 * Utility: Convert base64 string to Uint8Array
 */
export function base64ToArray(base64: string): Uint8Array {
  return Uint8Array.from(atob(base64), c => c.charCodeAt(0));
}

/**
 * Store encryption salt securely in localStorage
 * ⚠️ In production, consider storing in IndexedDB with additional protection
 */
export function storeSalt(userId: string, salt: Uint8Array): void {
  localStorage.setItem(`lifehub_salt_${userId}`, arrayToBase64(salt));
}

/**
 * Retrieve encryption salt
 */
export function getSalt(userId: string): Uint8Array | null {
  const saltBase64 = localStorage.getItem(`lifehub_salt_${userId}`);
  if (!saltBase64) return null;
  return base64ToArray(saltBase64);
}

/**
 * Check if user has encryption key setup
 */
export function hasEncryptionSetup(userId: string): boolean {
  return localStorage.getItem(`lifehub_salt_${userId}`) !== null;
}

/**
 * Clear encryption data (on logout)
 */
export function clearEncryptionData(userId: string): void {
  localStorage.removeItem(`lifehub_salt_${userId}`);
}
