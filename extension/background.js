// LifeHub Background Service Worker
// Handles extension lifecycle and message passing

import { initializeApp } from './popup/libs/firebase-app.js';
import { 
  getFirestore, 
  collection, 
  addDoc,
  updateDoc,
  doc,
  query,
  where,
  getDocs
} from './popup/libs/firebase-firestore.js';
import { getAuth } from './popup/libs/firebase-auth.js';
import * as Encryption from './popup/libs/encryption-helper.js';
import firebaseConfig from './popup/libs/firebase-config.js';

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const db = getFirestore(app);
const auth = getAuth(app);

// Listen for installation
chrome.runtime.onInstalled.addListener((details) => {
  console.log('LifeHub Extension installed:', details.reason);
  
  // Set default options
  chrome.storage.local.set({
    autoFillEnabled: true,
    showIconOnFields: true
  });
});

// Listen for messages
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.action === 'openPopup') {
    // Cannot programmatically open popup, but can notify user
    console.log('Open popup requested from:', sender.tab?.url);
    
    // Show notification as fallback
    chrome.action.setBadgeText({ text: '!' });
    chrome.action.setBadgeBackgroundColor({ color: '#6366f1' });
    
    setTimeout(() => {
      chrome.action.setBadgeText({ text: '' });
    }, 3000);
  }
  
  if (message.action === 'getSettings') {
    chrome.storage.local.get(['autoFillEnabled', 'showIconOnFields'], (result) => {
      sendResponse(result);
    });
    return true; // Keep channel open for async response
  }
  
  if (message.action === 'saveSettings') {
    chrome.storage.local.set(message.settings);
    sendResponse({ success: true });
  }
  
  // Check if account already exists for content script UI
  if (message.action === 'checkAccountExists') {
    const { website, username } = message.data;
    chrome.storage.local.get(['lifehub_accounts'], (result) => {
      const accounts = result.lifehub_accounts || [];
      const exists = accounts.some(acc => {
        try {
          // Robust domain matching
          const accHost = new URL(acc.website.includes('://') ? acc.website : 'https://' + acc.website).hostname;
          const currentHost = website;
          return (accHost.includes(currentHost) || currentHost.includes(accHost)) && 
                 (acc.username === username || acc.email === username);
        } catch (e) {
          // If URL parsing fails, fallback to simple string match
          return acc.website && acc.website.includes(website) && 
                 (acc.username === username || acc.email === username);
        }
      });
      sendResponse({ exists });
    });
    return true;
  }
  
  // Get accounts for autofill dropdown - requires authentication
  if (message.action === 'getAccounts') {
    chrome.storage.session.get(['encryptionKey'], (sessionResult) => {
      // Check if user is authenticated (has encryption key in session)
      if (!sessionResult.encryptionKey) {
        sendResponse({ 
          accounts: [], 
          requiresAuth: true,
          message: 'Vui lòng mở extension và đăng nhập để sử dụng tính năng này.'
        });
        return;
      }
      
      chrome.storage.local.get(['lifehub_accounts'], (result) => {
        const accounts = result.lifehub_accounts || [];
        sendResponse({ accounts, requiresAuth: false });
      });
    });
    return true;
  }
  
  // Fill credentials - get account by ID (requires authentication)
  if (message.action === 'fillCredentials') {
    chrome.storage.session.get(['encryptionKey'], (sessionResult) => {
      if (!sessionResult.encryptionKey) {
        sendResponse({ error: 'Not authenticated', requiresAuth: true });
        return;
      }
      
      chrome.storage.local.get(['lifehub_accounts'], async (result) => {
        const accounts = result.lifehub_accounts || [];
        const account = accounts.find(a => a.id === message.accountId);
        
        if (account) {
          sendResponse({
            username: account.username || account.email || '',
            password: account.password || ''
          });
        } else {
          sendResponse({ error: 'Account not found' });
        }
      });
    });
    return true;
  }
  
  // Save new credentials from form submission (requires authentication for immediate sync)
  if (message.action === 'saveCredentials') {
    const { website, username, password } = message.data;
    
    chrome.storage.session.get(['encryptionKey'], async (sessionResult) => {
      const keyBase64 = sessionResult.encryptionKey;
      
      chrome.storage.local.get(['lifehub_userId', 'lifehub_accounts', 'lifehub_pending_accounts'], async (localResult) => {
        let userId = localResult.lifehub_userId || auth.currentUser?.uid;
        const accounts = localResult.lifehub_accounts || [];
        let syncError = '';
        
        // Find existing account for matching Website (Domain) + Username
        const existingAccount = accounts.find(acc => {
          let accHost = '';
          try {
            const urlStr = acc.website.includes('://') ? acc.website : 'https://' + acc.website;
            accHost = new URL(urlStr).hostname.replace('www.', '');
          } catch(e) {
            accHost = (acc.website || '').replace('www.', '');
          }
          const currentHost = website.replace('www.', '');
          
          return (accHost.includes(currentHost) || currentHost.includes(accHost)) && 
                 (acc.username === username || acc.email === username);
        });

        const isUpdate = !!existingAccount;
        console.log('SaveCredentials: isUpdate=', isUpdate, 'hasKey=', !!keyBase64, 'userId=', userId);

        // I. If authenticated, save to Firestore directly
        if (keyBase64 && userId) {
          try {
            console.log('Importing encryption key in background...');
            const keyBuffer = Encryption.base64ToArray(keyBase64);
            const encryptionKey = await crypto.subtle.importKey(
              'raw', keyBuffer,
              { name: 'AES-GCM' },
              false, ['encrypt', 'decrypt']
            );

            console.log('Syncing save to Firestore immediately...');
            const encryptedPassword = await Encryption.encrypt(password, encryptionKey);
            
            if (isUpdate) {
              // Update existing in Firestore
              const docRef = doc(db, 'users', userId, 'accounts', existingAccount.id);
              await updateDoc(docRef, {
                password: encryptedPassword,
                lastModified: new Date()
              });
              console.log('Updated existing account in Firestore');
            } else {
              // Add new to Firestore
              const serviceName = website.replace('www.', '').split('.')[0];
              const accountsRef = collection(db, 'users', userId, 'accounts');
              await addDoc(accountsRef, {
                serviceName: serviceName.charAt(0).toUpperCase() + serviceName.slice(1),
                username: username,
                password: encryptedPassword,
                websiteUrl: 'https://' + website,
                userOwnerId: userId,
                customFields: {},
                notes: 'Tự động lưu từ trình duyệt',
                lastModified: new Date()
              });
              console.log('Saved new account to Firestore');
            }
            
            sendResponse({ 
              success: true, 
              message: isUpdate ? 'Mật khẩu đã được cập nhật lên Cloud!' : 'Tài khoản đã được lưu lên Cloud!',
              syncing: false
            });
            return;
          } catch (error) {
            console.error('Immediate Firestore sync failed:', error);
            // Fallback to local pending save with actual error info
            syncError = error.message || 'Lỗi Firestore/Mã hóa';
          }
        } else {
          syncError = !keyBase64 ? 'Thiếu khóa mã hóa' : 'Thiếu ID người dùng';
          console.warn('Cannot sync to Cloud:', syncError);
        }

        // II. Fallback: Save to local pending (if not authenticated or Firestore failed)
        const pendingAccounts = localResult.lifehub_pending_accounts || [];
        const alreadyPending = pendingAccounts.some(acc => acc.website === website && acc.username === username);
        
        if (!alreadyPending) {
          pendingAccounts.push({
            website, username, password,
            timestamp: Date.now(),
            isUpdate
          });
        }
        
        // Update local cache too
        if (isUpdate) {
          const idx = accounts.findIndex(a => a.id === existingAccount.id);
          if (idx !== -1) accounts[idx].password = password;
        } else {
          accounts.push({
            id: 'local_' + Date.now(),
            name: website.replace('www.', '').split('.')[0],
            website, username, password,
            lastModified: new Date().toISOString()
          });
        }
        
        chrome.storage.local.set({ 
          lifehub_pending_accounts: pendingAccounts,
          lifehub_accounts: accounts 
        }, () => {
          const statusMsg = keyBase64 
            ? `Đã lưu cục bộ (${syncError}). Sẽ tự động đồng bộ sau.` 
            : 'Đã lưu tạm! Mở extension để đồng bộ lên Cloud.';
            
          sendResponse({ 
            success: true, 
            requiresAuth: !keyBase64,
            message: statusMsg,
            syncing: !keyBase64 
          });
        });
      });
    });
    return true;
  }
});

// Handle tab updates - detect when user navigates to a new page
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.status === 'complete' && tab.url) {
    // Could notify content script here if needed
  }
});

// Keep service worker alive (Manifest V3)
self.addEventListener('activate', (event) => {
  console.log('LifeHub Service Worker activated');
});
