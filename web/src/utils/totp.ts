/**
 * TOTP (Time-based One-Time Password) Generator
 * Compatible with Google Authenticator, Authy, etc.
 * RFC 6238 implementation
 */

const BASE32_CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

/**
 * Decode base32 string to Uint8Array
 */
function base32Decode(input: string): Uint8Array {
  // Remove spaces and convert to uppercase
  const cleanInput = input.replace(/\s/g, '').toUpperCase();
  
  let bits = '';
  for (const char of cleanInput) {
    if (char === '=') continue;
    const index = BASE32_CHARS.indexOf(char);
    if (index === -1) throw new Error('Invalid base32 character');
    bits += index.toString(2).padStart(5, '0');
  }
  
  const bytes = new Uint8Array(Math.floor(bits.length / 8));
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(bits.slice(i * 8, (i + 1) * 8), 2);
  }
  
  return bytes;
}

/**
 * Generate HMAC-SHA1 signature
 */
async function hmacSha1(key: Uint8Array, data: Uint8Array): Promise<Uint8Array> {
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    key,
    { name: 'HMAC', hash: 'SHA-1' },
    false,
    ['sign']
  );
  
  const signature = await crypto.subtle.sign('HMAC', cryptoKey, data);
  return new Uint8Array(signature);
}

/**
 * Convert number to 8-byte big-endian array
 */
function intToBytes(num: number): Uint8Array {
  const bytes = new Uint8Array(8);
  for (let i = 7; i >= 0; i--) {
    bytes[i] = num & 0xff;
    num = Math.floor(num / 256);
  }
  return bytes;
}

/**
 * Generate TOTP code
 * @param secret Base32-encoded secret key
 * @param digits Number of digits (default: 6)
 * @param period Period in seconds (default: 30)
 * @param timestamp Current timestamp in milliseconds (default: now)
 * @returns TOTP code as string
 */
export async function generateTOTP(
  secret: string,
  digits: number = 6,
  period: number = 30,
  timestamp: number = Date.now()
): Promise<string> {
  // Decode secret from base32
  const key = base32Decode(secret);
  
  // Calculate counter (time steps since epoch)
  const counter = Math.floor(timestamp / 1000 / period);
  const counterBytes = intToBytes(counter);
  
  // Generate HMAC-SHA1
  const hmac = await hmacSha1(key, counterBytes);
  
  // Dynamic truncation
  const offset = hmac[hmac.length - 1] & 0x0f;
  const binary = 
    ((hmac[offset] & 0x7f) << 24) |
    ((hmac[offset + 1] & 0xff) << 16) |
    ((hmac[offset + 2] & 0xff) << 8) |
    (hmac[offset + 3] & 0xff);
  
  // Generate OTP
  const otp = binary % Math.pow(10, digits);
  return otp.toString().padStart(digits, '0');
}

/**
 * Get remaining seconds until next TOTP refresh
 */
export function getRemainingSeconds(period: number = 30): number {
  const now = Math.floor(Date.now() / 1000);
  return period - (now % period);
}

/**
 * Get progress percentage (0-100) for countdown bar
 */
export function getProgress(period: number = 30): number {
  const remaining = getRemainingSeconds(period);
  return (remaining / period) * 100;
}

/**
 * Parse otpauth:// URI
 * Format: otpauth://totp/Label?secret=XXX&issuer=YYY&digits=6&period=30
 */
export interface ParsedOtpAuthUri {
  type: 'totp' | 'hotp';
  label: string;
  secret: string;
  issuer?: string;
  digits: number;
  period: number;
  algorithm: string;
}

export function parseOtpAuthUri(uri: string): ParsedOtpAuthUri | null {
  try {
    const url = new URL(uri);
    if (url.protocol !== 'otpauth:') return null;
    
    const type = url.hostname as 'totp' | 'hotp';
    const label = decodeURIComponent(url.pathname.slice(1));
    
    const params = url.searchParams;
    const secret = params.get('secret');
    if (!secret) return null;
    
    return {
      type,
      label,
      secret: secret.toUpperCase(),
      issuer: params.get('issuer') || undefined,
      digits: parseInt(params.get('digits') || '6'),
      period: parseInt(params.get('period') || '30'),
      algorithm: params.get('algorithm') || 'SHA1'
    };
  } catch {
    return null;
  }
}
