/**
 * Robustly converts various date formats (Firestore Timestamp, Date, Number, String)
 * into a JavaScript Date object.
 */
export const safeToDate = (val: any): Date | undefined => {
  if (!val) return undefined;
  
  // Firestore Timestamp
  if (typeof val.toDate === 'function') {
    try {
      return val.toDate();
    } catch (e) {
      // Fallback if toDate fails
    }
  }
  
  // Firestore Timestamp-like object (seconds/nanoseconds)
  if (val.seconds !== undefined) {
    return new Date(val.seconds * 1000);
  }

  // Already a Date object
  if (val instanceof Date) {
    return isNaN(val.getTime()) ? undefined : val;
  }
  
  // Number (milliseconds) or String date
  if (typeof val === 'number' || typeof val === 'string') {
    const d = new Date(val);
    return isNaN(d.getTime()) ? undefined : d;
  }
  
  return undefined;
};
