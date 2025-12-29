/**
 * Time synchronization utility
 * Compares local time with Firestore document timestamps
 */

let timeOffset = 0;
let isSynced = false;

/**
 * Get the corrected timestamp accounting for any offset
 */
export function getCorrectedTimestamp(): number {
  return Date.now() + timeOffset;
}

/**
 * Get remaining seconds until next TOTP refresh (with time correction)
 */
export function getCorrectedRemainingSeconds(period: number = 30): number {
  const now = Math.floor(getCorrectedTimestamp() / 1000);
  return period - (now % period);
}

/**
 * Get progress percentage (0-100) for countdown bar (with time correction)
 */
export function getCorrectedProgress(period: number = 30): number {
  const remaining = getCorrectedRemainingSeconds(period);
  return (remaining / period) * 100;
}

/**
 * Calculate time offset from a Firestore document's lastModified timestamp
 * Call this when you fetch documents that have server-set timestamps
 */
export function calculateOffsetFromDocument(
  serverTimestamp: Date,
  localTimeWhenFetched: number
): void {
  if (isSynced) return;
  
  const serverTime = serverTimestamp.getTime();
  // Estimate: server timestamp was set some time before we received it
  // Assume average latency of ~500ms for Firestore round trip
  const estimatedOffset = serverTime - localTimeWhenFetched + 500;
  
  // Only apply if offset is reasonable (less than 60 seconds)
  if (Math.abs(estimatedOffset) < 60000) {
    timeOffset = estimatedOffset;
    isSynced = true;
    console.log('[TimeSync] Offset from Firestore doc:', Math.round(timeOffset), 'ms');
  }
}

/**
 * Sync time - placeholder that does nothing
 * Real sync happens via calculateOffsetFromDocument
 */
export function syncTime(): Promise<number> {
  return Promise.resolve(timeOffset);
}

/**
 * Sync time using Firebase - placeholder for compatibility
 */
export async function syncTimeWithFirebase(): Promise<number> {
  // No-op - we use calculateOffsetFromDocument instead
  return timeOffset;
}

/**
 * Get current time offset
 */
export function getTimeOffset(): number {
  return timeOffset;
}

/**
 * Check if time has been synced
 */
export function isTimeSynced(): boolean {
  return isSynced;
}

/**
 * Manually set time offset (for testing or manual correction)
 * Use this if you know your local clock is off by a specific amount
 * 
 * Example: If your clock is 3 seconds behind server, call setTimeOffset(3000)
 */
export function setTimeOffset(offset: number): void {
  timeOffset = offset;
  isSynced = true;
  console.log('[TimeSync] Manual offset set:', offset, 'ms');
}
