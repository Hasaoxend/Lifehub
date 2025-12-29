/**
 * Encryption utilities for LifeHub Extension
 * Compatible with Android and Web versions
 * Uses Web Crypto API with AES-256-GCM and PBKDF2
 */

const ALGORITHM = 'AES-GCM';
const KEY_LENGTH = 256;
const IV_LENGTH = 12;
const SALT_LENGTH = 16;
const ITERATIONS = 100000;

export const VERIFICATION_STRING = "LIFEHUB_VERIFY";

/**
 * Derive encryption key from passcode or recovery code using PBKDF2
 */
export async function deriveKey(secret, salt) {
    const encoder = new TextEncoder();
    const secretBuffer = encoder.encode(secret);
    
    const keyMaterial = await crypto.subtle.importKey(
        'raw',
        secretBuffer,
        { name: 'PBKDF2' },
        false,
        ['deriveKey']
    );
    
    return crypto.subtle.deriveKey(
        {
            name: 'PBKDF2',
            salt: salt,
            iterations: ITERATIONS,
            hash: 'SHA-256'
        },
        keyMaterial,
        { name: ALGORITHM, length: KEY_LENGTH },
        true,  // extractable = true to allow exporting key for session storage
        ['encrypt', 'decrypt']
    );
}

/**
 * Generate a random salt
 */
export function generateSalt() {
    return crypto.getRandomValues(new Uint8Array(SALT_LENGTH));
}

/**
 * Generate a random IV for AES-GCM
 */
function generateIV() {
    return crypto.getRandomValues(new Uint8Array(IV_LENGTH));
}

/**
 * Encrypt plaintext using AES-256-GCM
 * Returns: base64(iv + ciphertext)
 */
export async function encrypt(plaintext, key) {
    const encoder = new TextEncoder();
    const data = encoder.encode(plaintext);
    const iv = generateIV();
    
    const ciphertext = await crypto.subtle.encrypt(
        { name: ALGORITHM, iv: iv },
        key,
        data
    );
    
    const combined = new Uint8Array(iv.length + ciphertext.byteLength);
    combined.set(iv, 0);
    combined.set(new Uint8Array(ciphertext), iv.length);
    
    return btoa(String.fromCharCode(...combined));
}

/**
 * Decrypt ciphertext using AES-256-GCM
 * Input: base64(iv + ciphertext)
 */
export async function decrypt(ciphertext, key) {
    const combined = Uint8Array.from(atob(ciphertext), c => c.charCodeAt(0));
    
    const iv = combined.slice(0, IV_LENGTH);
    const data = combined.slice(IV_LENGTH);
    
    const decrypted = await crypto.subtle.decrypt(
        { name: ALGORITHM, iv: iv },
        key,
        data
    );
    
    const decoder = new TextDecoder();
    return decoder.decode(decrypted);
}

/**
 * Utility: Convert Uint8Array to base64 string
 */
export function arrayToBase64(array) {
    return btoa(String.fromCharCode(...array));
}

/**
 * Utility: Convert base64 string to Uint8Array
 */
export function base64ToArray(base64) {
    return Uint8Array.from(atob(base64), c => c.charCodeAt(0));
}
