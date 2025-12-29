// TypeScript type definitions matching Android data models
// These types mirror the Firestore structure used by the Android app

/**
 * AccountEntry - Password manager entry
 * Firestore path: users/{userId}/accounts/{accountId}
 */
export interface AccountEntry {
  documentId?: string;
  serviceName: string;
  username: string;
  password: string; // Encrypted with AES-256
  websiteUrl?: string;
  notes?: string;
  customFields?: Record<string, CustomField>;
  userOwnerId: string;
  lastModified?: Date;
}

export interface CustomField {
  value: string;
  type: FieldType;
}

export enum FieldType {
  TEXT = 0,
  PASSWORD = 1
}

/**
 * TotpAccount - Authenticator 2FA entry
 * Firestore path: users/{userId}/totp_accounts/{totpAccountId}
 */
export interface TotpAccount {
  documentId?: string;
  secretKey: string;
  accountName: string;
  issuer?: string;
  digits?: number; // Default: 6
  period?: number; // Default: 30 seconds
  algorithm?: string; // Default: SHA1
  userOwnerId: string;
  lastModified?: Date;
}

/**
 * NoteEntry - Notes/Memo
 * Firestore path: users/{userId}/notes/{noteId}
 */
export interface NoteEntry {
  documentId?: string;
  title: string;
  content: string;
  userOwnerId: string;
  reminderTime?: Date;
  lastModified?: Date;
}

/**
 * TaskEntry - Tasks and Shopping lists
 * Firestore path: users/{userId}/tasks/{taskId}
 */
export interface TaskEntry {
  documentId?: string;
  name: string;
  completed: boolean;
  taskType: TaskType;
  projectId?: string; // null = root level
  reminderTime?: Date;
  userOwnerId: string;
  lastModified?: Date;
}

export enum TaskType {
  TASK = 0,
  SHOPPING = 1
}

/**
 * ProjectEntry - Folders for organizing tasks
 * Firestore path: users/{userId}/projects/{projectId}
 */
export interface ProjectEntry {
  documentId?: string;
  name: string;
  color?: string; // Hex color
  projectId?: string; // Parent project (null = root)
  userOwnerId: string;
  createdDate?: Date;
}

/**
 * CalendarEvent - Calendar events
 * Firestore path: users/{userId}/calendar_events/{eventId}
 */
export interface CalendarEvent {
  documentId?: string;
  title: string;
  startTime: Date;
  endTime?: Date;
  location?: string;
  description?: string;
  color?: string; // Hex color
  userOwnerId: string;
  lastModified?: Date;
}

/**
 * User profile and settings
 */
export interface UserProfile {
  uid: string;
  email: string;
  displayName?: string;
  photoURL?: string;
  biometricEnabled?: boolean;
  autofillEnabled?: boolean;
  language?: 'en' | 'vi';
}
