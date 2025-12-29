// LifeHub Extension Popup JavaScript
// Uses Firebase for authentication and data sync

console.log('Script started...');

// Global error handler
window.onerror = function(msg, url, lineNo, columnNo, error) {
  console.error('Global Error: ', msg, url, lineNo, columnNo, error);
  document.body.innerHTML += `<div style="color: red; padding: 10px; background: #000; position: fixed; top: 0; left: 0; z-index: 10000; width: 100%;">Lỗi: ${msg}</div>`;
  return false;
};

// Force hide loading after 5 seconds if stuck
setTimeout(() => {
  const loadingEl = document.getElementById('loading');
  if (loadingEl && loadingEl.classList.contains('active')) {
    console.warn('Initialization taking too long, force showing login screen');
    showScreen('login');
  }
}, 5000);

import { initializeApp } from './libs/firebase-app.js';
import { 
  getAuth, 
  signInWithEmailAndPassword, 
  onAuthStateChanged,
  signOut 
} from './libs/firebase-auth.js';
import { 
  getFirestore, 
  collection, 
  query, 
  onSnapshot,
  orderBy,
  doc,
  getDoc,
  setDoc,
  updateDoc,
  addDoc,
  deleteDoc
} from './libs/firebase-firestore.js';
import * as Encryption from './libs/encryption-helper.js';
import firebaseConfig from './libs/firebase-config.js';

// Firebase config
// Firebase instance
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

// State
let accounts = [];
let totpAccounts = [];
let notes = [];
let tasks = [];
let shoppingItems = []; // Still needed for internal logic or just reuse tasks with filter
let projects = [];
let calendarEvents = [];
let searchQuery = '';
let vaultType = 'all'; // 'all', 'accounts', 'totp'
let taskFilter = 'all'; // 'all', 'tasks', 'shopping'
let calendarSelectedDate = new Date();
let currentDomain = '';
let unsubscribeAccounts = null;
let unsubscribeTOTP = null;
let unsubscribeNotes = null;
let unsubscribeTasks = null;
let unsubscribeProjects = null;
let unsubscribeCalendar = null;
let lockTimeout = 0; // minutes, 0 = always

// DOM Elements
const screens = {
  loading: document.getElementById('loading'),
  login: document.getElementById('login-screen'),
  passcode: document.getElementById('passcode-screen'),
  main: document.getElementById('main-screen')
};

// Passcode State
let userEncryptionData = null;
let encryptionKey = null;
let isSetupMode = false;
let currentTheme = localStorage.getItem('theme') || 'dark';
let currentLanguage = localStorage.getItem('language') || 'vi';

// Initialize
document.addEventListener('DOMContentLoaded', init);

async function init() {
  console.log('Initializing LifeHub Extension logic...');
  
  applyTheme();
  applyLanguage();

  // Ensure we can see something even if tab query fails
  const tabQueryTimeout = setTimeout(() => {
    console.warn('Tab query timed out');
    showScreen('login');
  }, 2000);

  // Get current tab domain
  try {
    console.log('Querying current tab...');
    const tabs = await chrome.tabs.query({ active: true, currentWindow: true });
    clearTimeout(tabQueryTimeout);
    
    if (tabs && tabs[0] && tabs[0].url) {
      const url = new URL(tabs[0].url);
      currentDomain = url.hostname;
      console.log('Current domain:', currentDomain);
      
      const domainEl = document.getElementById('site-domain');
      const faviconEl = document.getElementById('site-favicon');
      
      if (domainEl) domainEl.textContent = currentDomain;
      if (faviconEl) {
        faviconEl.src = `https://www.google.com/s2/favicons?domain=${currentDomain}&sz=32`;
      }
    } else {
      console.log('No active tab found or no URL access');
      showScreen('login');
    }
  } catch (e) {
    console.error('Error getting tab:', e);
    clearTimeout(tabQueryTimeout);
    showScreen('login');
  }
  
  // Load settings
  const settings = await chrome.storage.local.get(['lockTimeout', 'language', 'theme']);
  lockTimeout = settings.lockTimeout || 0;
  if (settings.language) currentLanguage = settings.language;
  if (settings.theme) currentTheme = settings.theme;
  
  // Set initial select values
  document.getElementById('lock-timeout-select').value = lockTimeout;

  applyTheme();
  applyLanguage();

  // Check lock status
  const isLocked = await checkLockStatus();
  
  // Check auth state
  try {
    console.log('Setting up auth listener...');
    onAuthStateChanged(auth, async (user) => {
      console.log('Auth state update. User logged in:', !!user);
      if (user) {
        chrome.storage.local.set({ lifehub_userId: user.uid });
        if (!isLocked && encryptionKey) {
          showScreen('main');
          loadData(user.uid);
        } else {
          await checkEncryptionSetup(user);
        }
      } else {
        showScreen('login');
        cleanupListeners();
        encryptionKey = null;
        userEncryptionData = null;
        chrome.storage.session?.remove(['encryptionKey']);
        chrome.storage.local.remove(['lifehub_userId']);
      }
    });
  } catch (error) {
    console.error('Auth listener setup failed:', error);
    showScreen('login');
  }
  
  // Event listeners
  setupEventListeners();
}

function setupEventListeners() {
  // Login form
  document.getElementById('login-form').addEventListener('submit', handleLogin);
  
  // Logout
  document.getElementById('logout-btn').addEventListener('click', handleLogout);
  
  // Header actions
  document.getElementById('refresh-btn').addEventListener('click', () => {
    if (auth.currentUser) loadData(auth.currentUser.uid);
  });
  
  document.getElementById('task-type-select').addEventListener('change', (e) => {
    taskFilter = e.target.value;
    renderTasks();
  });

  document.getElementById('create-task-btn').addEventListener('click', () => {
    window.open('https://lifehub-project-d8c97.web.app/tasks', '_blank');
  });

  document.getElementById('create-event-btn').addEventListener('click', () => {
    window.open('https://lifehub-project-d8c97.web.app/calendar', '_blank');
  });

  document.getElementById('calendar-prev-day').addEventListener('click', () => changeSelectedDate(-1));
  document.getElementById('calendar-next-day').addEventListener('click', () => changeSelectedDate(1));
  document.getElementById('calendar-date-display').addEventListener('click', () => {
    calendarSelectedDate = new Date();
    renderCalendar();
  });

  // Swipe support for calendar
  let touchStartX = 0;
  const calendarTab = document.getElementById('calendar-tab');
  calendarTab.addEventListener('touchstart', (e) => { touchStartX = e.touches[0].clientX; }, {passive: true});
  calendarTab.addEventListener('touchend', (e) => {
    const touchEndX = e.changedTouches[0].clientX;
    const diff = touchStartX - touchEndX;
    if (Math.abs(diff) > 50) { // Threshold for swipe
      changeSelectedDate(diff > 0 ? 1 : -1);
    }
  }, {passive: true});

  document.getElementById('theme-toggle').addEventListener('click', toggleTheme);
  document.getElementById('settings-btn').addEventListener('click', (e) => {
    e.stopPropagation();
    document.getElementById('settings-menu').classList.toggle('hidden');
  });
  
  document.getElementById('lang-toggle-btn').addEventListener('click', (e) => {
    e.stopPropagation();
    toggleLanguage();
  });
  
  // Close settings menu when clicking outside
  document.addEventListener('click', (e) => {
    const menu = document.getElementById('settings-menu');
    if (menu && !menu.contains(e.target) && e.target.id !== 'settings-btn') {
      menu.classList.add('hidden');
    }
  });

  // Tabs
  document.querySelectorAll('.tab').forEach(tab => {
    tab.addEventListener('click', () => switchTab(tab.dataset.tab));
  });
  
  // Vault selector
  document.getElementById('vault-type-select').addEventListener('change', (e) => {
    vaultType = e.target.value;
    renderVault();
  });

  // Lock timeout
  document.getElementById('lock-timeout-select').addEventListener('change', async (e) => {
    lockTimeout = parseInt(e.target.value);
    await chrome.storage.local.set({ lockTimeout });
  });

  // Passcode actions
  document.getElementById('passcode-btn').addEventListener('click', handlePasscodeSubmit);
  document.getElementById('pin-input').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handlePasscodeSubmit();
  });
  document.getElementById('pin-confirm')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') handlePasscodeSubmit();
  });
  
  // Logout from passcode screen
  document.getElementById('passcode-logout-btn')?.addEventListener('click', handleLogout);

  // Search
  document.getElementById('global-search').addEventListener('input', (e) => {
    searchQuery = e.target.value.toLowerCase().trim();
    renderAll();
  });

  // Note creation modal
  setupNoteModal();

  // Note detail modal
  setupNoteDetailModal();
}

function setupNoteDetailModal() {
  const modal = document.getElementById('note-detail-modal');
  const closeBtn = document.getElementById('close-detail-modal');
  const closeBtn2 = document.getElementById('detail-close-btn');
  const copyBtn = document.getElementById('detail-copy-btn');
  const overlay = modal?.querySelector('.modal-overlay');

  const closeModal = () => {
    modal.classList.add('hidden');
  };

  closeBtn?.addEventListener('click', closeModal);
  closeBtn2?.addEventListener('click', closeModal);
  overlay?.addEventListener('click', closeModal);
  
  copyBtn?.addEventListener('click', () => {
    const content = document.getElementById('note-detail-content').textContent;
    copyToClipboard(content);
    showToast('Đã sao chép nội dung!');
  });
}

function showNoteDetail(note) {
  const modal = document.getElementById('note-detail-modal');
  const titleEl = document.getElementById('note-detail-title');
  const contentEl = document.getElementById('note-detail-content');
  const dateEl = document.getElementById('note-detail-date');

  titleEl.textContent = note.title || 'Ghi chú';
  contentEl.textContent = note.content || '';
  dateEl.textContent = note.lastModified ? new Date(note.lastModified.seconds * 1000).toLocaleString() : 'Không rõ';

  modal.classList.remove('hidden');
}

function setupNoteModal() {
  const modal = document.getElementById('note-modal');
  const createBtn = document.getElementById('create-note-btn');
  const closeBtn = document.getElementById('close-note-modal');
  const cancelBtn = document.getElementById('cancel-note-btn');
  const saveBtn = document.getElementById('save-note-btn');
  const overlay = modal?.querySelector('.modal-overlay');

  const openModal = () => {
    modal.classList.remove('hidden');
    document.getElementById('note-title-input').value = '';
    document.getElementById('note-content-input').value = '';
    document.getElementById('note-title-input').focus();
  };

  const closeModal = () => {
    modal.classList.add('hidden');
  };

  createBtn?.addEventListener('click', openModal);
  closeBtn?.addEventListener('click', closeModal);
  cancelBtn?.addEventListener('click', closeModal);
  overlay?.addEventListener('click', closeModal);
  
  saveBtn?.addEventListener('click', async () => {
    const title = document.getElementById('note-title-input').value.trim();
    const content = document.getElementById('note-content-input').value.trim();
    
    if (!content) {
      showToast('Vui lòng nhập nội dung ghi chú!');
      return;
    }
    
    saveBtn.disabled = true;
    saveBtn.textContent = 'Đang lưu...';
    
    try {
      await saveNote(title, content);
      closeModal();
      showToast('Đã lưu ghi chú thành công!');
    } catch (e) {
      console.error('Save note error:', e);
      showToast('Lỗi khi lưu: ' + e.message);
    } finally {
      saveBtn.disabled = false;
      saveBtn.textContent = 'Lưu ghi chú';
    }
  });
}

async function saveNote(title, content) {
  const user = auth.currentUser;
  if (!user) throw new Error('Chưa đăng nhập');
  if (!encryptionKey) throw new Error('Chưa mở khóa');
  
  // Encrypt title and content
  const encryptedTitle = title ? await Encryption.encrypt(title, encryptionKey) : '';
  const encryptedContent = await Encryption.encrypt(content, encryptionKey);
  
  const notesRef = collection(db, 'users', user.uid, 'notes');
  await addDoc(notesRef, {
    title: encryptedTitle,
    content: encryptedContent,
    lastModified: new Date(),
    createdAt: new Date()
  });
}

function renderAll() {
  renderVault();
  renderNotes();
  renderTasks();
  renderCalendar();
  // renderShopping(); // Shopping is now integrated into renderTasks
}

// Core UI functions
function showScreen(screenId) {
  Object.values(screens).forEach(s => s.classList.remove('active'));
  screens[screenId].classList.add('active');
  
  // Hide settings menu when changing screens
  const menu = document.getElementById('settings-menu');
  if (menu) menu.classList.add('hidden');

  if (screenId === 'main') {
    // Ensure main screen elements are updated with current language
    applyLanguage();
  }
}

// Auth handlers
async function handleLogin(e) {
  e.preventDefault();
  
  const email = document.getElementById('email').value;
  const password = document.getElementById('password').value;
  const errorEl = document.getElementById('login-error');
  const btn = document.getElementById('login-btn');
  
  errorEl.classList.add('hidden');
  btn.disabled = true;
  btn.innerHTML = '<span>Đang đăng nhập...</span>';
  
  try {
    await signInWithEmailAndPassword(auth, email, password);
  } catch (error) {
    console.error('Login error:', error);
    errorEl.textContent = getErrorMessage(error.code);
    errorEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.innerHTML = '<span>Đăng nhập</span>';
  }
}

async function handleLogout() {
  try {
    cleanupListeners();
    await signOut(auth);
  } catch (error) {
    console.error('Logout error:', error);
  }
}

// Passcode Logic
async function checkEncryptionSetup(user) {
  showScreen('loading');
  try {
    const userDocRef = doc(db, 'users', user.uid);
    const userDoc = await getDoc(userDocRef);
    
    if (userDoc.exists()) {
      const data = userDoc.data();
      if (data.encryptionSalt && data.encryptionVerification) {
        userEncryptionData = {
          salt: Encryption.base64ToArray(data.encryptionSalt),
          verification: data.encryptionVerification
        };
        showPasscodeScreen(false);
        return;
      }
    }
    // No encryption setup found
    showPasscodeScreen(true);
  } catch (error) {
    console.error('Check encryption setup failed:', error);
    showScreen('login');
  }
}

function showPasscodeScreen(isSetup) {
  isSetupMode = isSetup;
  const title = document.getElementById('passcode-title');
  const subtitle = document.getElementById('passcode-subtitle');
  const btn = document.getElementById('passcode-btn');
  const reEntry = document.getElementById('setup-re-entry');
  const notice = document.getElementById('recovery-notice');
  const errorEl = document.getElementById('passcode-error');
  
  errorEl.classList.add('hidden');
  document.getElementById('pin-input').value = '';
  if (document.getElementById('pin-confirm')) document.getElementById('pin-confirm').value = '';

  if (isSetup) {
    title.textContent = 'Thiết lập mã PIN';
    subtitle.textContent = 'Tạo mã PIN 6 số để bảo vệ dữ liệu của bạn';
    btn.textContent = 'Thiết lập';
    reEntry.classList.remove('hidden');
    notice.classList.remove('hidden');
  } else {
    title.textContent = 'Nhập mã PIN';
    subtitle.textContent = 'Nhập mã PIN 6 số để mở khóa dữ liệu';
    btn.textContent = 'Mở khóa';
    reEntry.classList.add('hidden');
    notice.classList.add('hidden');
  }
  showScreen('passcode');
}

async function handlePasscodeSubmit() {
  const pin = document.getElementById('pin-input').value;
  const errorEl = document.getElementById('passcode-error');
  const btn = document.getElementById('passcode-btn');
  
  if (pin.length !== 6 || !/^\d+$/.test(pin)) {
    errorEl.textContent = 'Mã PIN phải gồm 6 chữ số';
    errorEl.classList.remove('hidden');
    return;
  }

  errorEl.classList.add('hidden');
  btn.disabled = true;
  
  try {
    if (isSetupMode) {
      const confirmPin = document.getElementById('pin-confirm').value;
      if (pin !== confirmPin) {
        throw new Error('Mã PIN không khớp');
      }
      await setupEncryption(pin);
    } else {
      await unlockVault(pin);
    }
    
    showScreen('main');
    await updateUnlockTime();
    loadData(auth.currentUser.uid);
  } catch (error) {
    console.error('Passcode error:', error);
    errorEl.textContent = error.message || 'Mã PIN không chính xác';
    errorEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
  }
}

async function setupEncryption(pin) {
  const salt = Encryption.generateSalt();
  const key = await Encryption.deriveKey(pin, salt);
  const verification = await Encryption.encrypt(Encryption.VERIFICATION_STRING, key);
  
  const userDocRef = doc(db, 'users', auth.currentUser.uid);
  await setDoc(userDocRef, {
    encryptionSalt: Encryption.arrayToBase64(salt),
    encryptionVerification: verification,
    encryptionVersion: 2
  }, { merge: true });
  
  encryptionKey = key;
  userEncryptionData = { salt, verification };
}

async function unlockVault(pin) {
  if (!userEncryptionData) throw new Error('Dữ liệu mã hóa không tồn tại');
  
  console.log('Deriving key from PIN...');
  const key = await Encryption.deriveKey(pin, userEncryptionData.salt);
  try {
    console.log('Decrypting verification string...');
    const decrypted = await Encryption.decrypt(userEncryptionData.verification, key);
    if (decrypted !== Encryption.VERIFICATION_STRING) {
      console.warn('Verification string mismatch');
      throw new Error('Mã PIN không chính xác');
    }
    encryptionKey = key;
    console.log('Vault unlocked successfully');
    
    // Store key in session storage (cleared when browser closes)
    if (chrome.storage.session) {
      const exported = await crypto.subtle.exportKey('raw', key);
      const keyBase64 = Encryption.arrayToBase64(new Uint8Array(exported));
      await chrome.storage.session.set({ encryptionKey: keyBase64 });
    }
  } catch (e) {
    console.error('Decryption failed:', e);
    throw new Error('Mã PIN không chính xác');
  }
}

async function checkLockStatus() {
  const settings = await chrome.storage.local.get(['lockTimeout', 'lastUnlockedTime']);
  lockTimeout = settings.lockTimeout || 0;
  const lastUnlockedTime = settings.lastUnlockedTime || 0;
  
  if (lockTimeout === 0) return true; // Always lock
  
  const now = Date.now();
  const elapsedMinutes = (now - lastUnlockedTime) / (1000 * 60);
  
  if (elapsedMinutes > lockTimeout) return true; // Expired
  
  // Try to retrieve key from session
  if (chrome.storage.session) {
    const session = await chrome.storage.session.get(['encryptionKey']);
    if (session.encryptionKey) {
      console.log('Restoring key from session storage...');
      const keyBuffer = Encryption.base64ToArray(session.encryptionKey);
      encryptionKey = await crypto.subtle.importKey(
        'raw', keyBuffer,
        { name: 'AES-GCM' },
        false, ['encrypt', 'decrypt']
      );
      return false; // Not locked
    }
  }
  
  return true;
}

async function updateUnlockTime() {
  await chrome.storage.local.set({ lastUnlockedTime: Date.now() });
}

// Settings & Theme
function applyTheme() {
  document.documentElement.setAttribute('data-theme', currentTheme);
  localStorage.setItem('theme', currentTheme);
  
  const sun = document.querySelector('.sun-icon');
  const moon = document.querySelector('.moon-icon');
  if (currentTheme === 'light') {
    sun.classList.remove('hidden');
    moon.classList.add('hidden');
  } else {
    sun.classList.add('hidden');
    moon.classList.remove('hidden');
  }
}

function toggleTheme() {
  currentTheme = currentTheme === 'dark' ? 'light' : 'dark';
  applyTheme();
}

const UI_STRINGS = {
  vi: {
    passwords: 'Mật khẩu',
    authenticator: '2FA',
    notes: 'Ghi chú',
    tasks: 'Việc & Mua sắm',
    work_only: 'Công việc',
    shopping_only: 'Mua sắm',
    refresh: 'Làm mới',
    settings: 'Cài đặt',
    logout: 'Đăng xuất',
    no_accounts: 'Không có tài khoản nào cho trang này',
    view_all: 'Xem tất cả',
    no_totp: 'Chưa có tài khoản 2FA nào',
    no_notes: 'Chưa có ghi chú nào',
    no_tasks: 'Không có công việc nào',
    no_shopping: 'Danh sách trống',
    no_events: 'Không có sự kiện nào sắp tới',
    calendar: 'Lịch',
    create_task: 'Tạo công việc',
    create_event: 'Tạo sự kiện',
    forgot_password: 'Quên mật khẩu',
    change_password: 'Đổi mật khẩu',
    change_passcode: 'Đổi Passcode',
    vault: 'Kho lưu trữ',
    all: 'Tất cả',
    lock_timeout: 'Tự động khóa',
    lock_always: 'Luôn hỏi',
    lock_3h: 'Sau 3 giờ',
    lock_6h: 'Sau 6 giờ',
    lock_12h: 'Sau 12 giờ',
    search_placeholder: 'Tìm kiếm tài khoản, ghi chú...',
    copy_success: 'Đã sao chép!'
  },
  en: {
    passwords: 'Passwords',
    authenticator: 'MFA',
    notes: 'Notes',
    tasks: 'Work & Shopping',
    work_only: 'Tasks',
    shopping_only: 'Shopping',
    refresh: 'Refresh',
    settings: 'Settings',
    logout: 'Logout',
    no_accounts: 'No data',
    view_all: 'View All',
    no_totp: 'No 2FA accounts found',
    no_notes: 'No notes found',
    no_tasks: 'No tasks found',
    no_shopping: 'Empty list',
    no_events: 'No upcoming events',
    calendar: 'Calendar',
    create_task: 'Create Task',
    create_event: 'Create Event',
    forgot_password: 'Forgot Password',
    change_password: 'Change Password',
    change_passcode: 'Change Passcode',
    vault: 'Vault',
    all: 'All',
    lock_timeout: 'Auto-lock',
    lock_always: 'Always',
    lock_3h: 'After 3 hours',
    lock_6h: 'After 6 hours',
    lock_12h: 'After 12 hours',
    search_placeholder: 'Search accounts, notes...',
    copy_success: 'Copied!'
  }
};

function applyLanguage() {
  localStorage.setItem('language', currentLanguage);
  const s = UI_STRINGS[currentLanguage];
  const langBtn = document.getElementById('lang-toggle-btn');
  
  if (currentLanguage === 'vi') {
    langBtn.querySelector('.menu-icon').textContent = '🇻🇳';
    langBtn.querySelector('.menu-text').textContent = 'Tiếng Việt';
  } else {
    langBtn.querySelector('.menu-icon').textContent = '🇺🇸';
    langBtn.querySelector('.menu-text').textContent = 'English';
  }

  // Update static UI elements
  document.querySelector('.tab[data-tab="accounts"] span').textContent = s.vault;
  document.querySelector('.tab[data-tab="notes"] span').textContent = s.notes;
  document.querySelector('.tab[data-tab="tasks"] span').textContent = s.tasks;
  document.querySelector('.tab[data-tab="calendar"] span').textContent = s.calendar;
  
  document.getElementById('refresh-btn').title = s.refresh;
  document.getElementById('settings-btn').title = s.settings;
  document.getElementById('logout-btn').title = s.logout;
  
  document.querySelector('#no-accounts p').textContent = s.no_accounts;
  document.querySelector('#no-notes p').textContent = s.no_notes;
  document.querySelector('#no-tasks p').textContent = s.no_tasks;
  document.querySelector('#no-events p').textContent = s.no_events;

  document.getElementById('create-task-btn').title = s.create_task;
  document.getElementById('create-event-btn').title = s.create_event;

  // Update vault selector
  const vaultSelect = document.getElementById('vault-type-select');
  vaultSelect.options[0].text = s.all;
  vaultSelect.options[1].text = currentLanguage === 'vi' ? 'Tài khoản' : 'Accounts';
  vaultSelect.options[2].text = s.authenticator;

  // Update task selector
  const taskSelect = document.getElementById('task-type-select');
  taskSelect.options[0].text = s.all;
  taskSelect.options[1].text = s.work_only;
  taskSelect.options[2].text = s.shopping_only;

  // Update lock timeout labels
  const lockSelect = document.getElementById('lock-timeout-select');
  document.querySelector('.menu-label').textContent = s.lock_timeout;
  lockSelect.options[0].text = s.lock_always;
  lockSelect.options[1].text = s.lock_3h;
  lockSelect.options[2].text = s.lock_6h;
  lockSelect.options[3].text = s.lock_12h;

  // Update links in settings menu
  const links = document.querySelectorAll('.settings-menu a');
  links[0].querySelector('span').textContent = s.forgot_password;
  links[1].querySelector('span').textContent = s.change_password;
  links[2].querySelector('span').textContent = s.change_passcode;

  document.getElementById('global-search').placeholder = s.search_placeholder;
}

function toggleLanguage() {
  currentLanguage = currentLanguage === 'vi' ? 'en' : 'vi';
  applyLanguage();
}

// Initial initialization
applyTheme();
applyLanguage();

function getErrorMessage(code) {
  const messages = {
    'auth/user-not-found': 'Tài khoản không tồn tại',
    'auth/wrong-password': 'Mật khẩu không chính xác',
    'auth/invalid-email': 'Email không hợp lệ',
    'auth/too-many-requests': 'Quá nhiều lần thử. Vui lòng thử lại sau'
  };
  return messages[code] || 'Đăng nhập thất bại';
}

// Sync pending accounts saved from content script to Firestore
async function syncPendingAccounts(userId) {
  try {
    const result = await chrome.storage.local.get(['lifehub_pending_accounts']);
    const pendingAccounts = result.lifehub_pending_accounts || [];
    
    if (pendingAccounts.length === 0) return;
    
    console.log('Syncing', pendingAccounts.length, 'pending accounts to Firestore');
    
    // Process each pending account
    for (const pending of pendingAccounts) {
      try {
        // Encrypt password before saving
        const encryptedPassword = await Encryption.encrypt(pending.password, encryptionKey);
        
        // Add to Firestore
        const accountsRef = collection(db, 'users', userId, 'accounts');
        await addDoc(accountsRef, {
          serviceName: pending.website.replace('www.', '').split('.')[0].charAt(0).toUpperCase() + 
                       pending.website.replace('www.', '').split('.')[0].slice(1),
          username: pending.username,
          password: encryptedPassword,
          websiteUrl: 'https://' + pending.website,
          notes: 'Tự động lưu từ trình duyệt',
          lastModified: new Date()
        });
        
        console.log('Saved pending account:', pending.website);
      } catch (err) {
        console.error('Error saving pending account:', err);
      }
    }
    
    // Clear pending accounts after sync
    await chrome.storage.local.remove('lifehub_pending_accounts');
    console.log('Cleared pending accounts');
    
  } catch (err) {
    console.error('Error syncing pending accounts:', err);
  }
}

// Data loading
function loadData(userId) {
  if (!encryptionKey) {
    console.warn('Cannot load data: encryption key not set');
    return;
  }

  // Load accounts
  const accountsRef = collection(db, 'users', userId, 'accounts');
  const accountsQuery = query(accountsRef); // Removed orderBy to avoid index issues
  
  unsubscribeAccounts = onSnapshot(accountsQuery, async (snapshot) => {
    const rawAccounts = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    // Decrypt passwords and Sort locally
    accounts = await Promise.all(rawAccounts.map(async (acc) => {
      let password = acc.password;
      if (acc.password) {
        try {
          password = await Encryption.decrypt(acc.password, encryptionKey);
        } catch (e) {
          console.error('Failed to decrypt account password:', acc.serviceName, e);
        }
      }
      return { ...acc, password };
    }));
    
    accounts.sort((a, b) => (a.serviceName || '').localeCompare(b.serviceName || ''));
    
    // Sync to chrome.storage.local for autofill dropdown access
    const accountsForAutofill = accounts.map(acc => ({
      id: acc.id,
      name: acc.serviceName || acc.name,
      website: acc.websiteUrl || acc.website || '',
      username: acc.username || '',
      email: acc.email || '',
      password: acc.password || ''
    }));
    chrome.storage.local.set({ lifehub_accounts: accountsForAutofill });
    
    // Check for pending saves from content script
    syncPendingAccounts(userId);
    
    renderVault();
  });
  
  // Load TOTP
  const totpRef = collection(db, 'users', userId, 'totp_accounts');
  const totpQuery = query(totpRef); // Removed orderBy
  
  unsubscribeTOTP = onSnapshot(totpQuery, async (snapshot) => {
    const rawTOTP = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    // Decrypt secrets and Sort locally
    totpAccounts = await Promise.all(rawTOTP.map(async (acc) => {
      let secretKey = acc.secretKey;
      if (acc.secretKey) {
        try {
          secretKey = await Encryption.decrypt(acc.secretKey, encryptionKey);
        } catch (e) {
          console.error('Failed to decrypt TOTP secret:', acc.accountName, e);
        }
      }
      return { ...acc, secretKey };
    }));
    
    totpAccounts.sort((a, b) => (a.accountName || '').localeCompare(b.accountName || ''));

    renderVault();
  });

  // Load Notes
  const notesRef = collection(db, 'users', userId, 'notes');
  unsubscribeNotes = onSnapshot(notesRef, async (snapshot) => {
    console.log('Notes snapshot received:', snapshot.docs.length, 'docs');
    const rawNotes = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    notes = await Promise.all(rawNotes.map(async n => {
      let content = n.content || '';
      let title = n.title || '';
      // Try to decrypt content and title - encrypted content looks like base64
      if (n.content && encryptionKey) {
        try { 
          content = await Encryption.decrypt(n.content, encryptionKey); 
        } catch(e) {
          // Content might not be encrypted, use as-is
          console.log('Notes content decryption failed (might be unencrypted):', e.message);
        }
      }
      if (n.title && encryptionKey) {
        try { 
          title = await Encryption.decrypt(n.title, encryptionKey); 
        } catch(e) {
          // Title might not be encrypted, use as-is
        }
      }
      return { ...n, title, content };
    }));
    notes.sort((a, b) => (b.lastModified?.seconds || 0) - (a.lastModified?.seconds || 0));
    console.log('Processed notes:', notes.length);
    renderNotes();
  });

  // Load Projects
  const projectsRef = collection(db, 'users', userId, 'projects');
  unsubscribeProjects = onSnapshot(projectsRef, (snapshot) => {
    projects = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    console.log('Processed projects:', projects.length);
    renderTasks(); // Re-render tasks to show project names
  });

  // Load Tasks
  const tasksRef = collection(db, 'users', userId, 'tasks');
  unsubscribeTasks = onSnapshot(tasksRef, async (snapshot) => {
    const rawTasks = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    const allItems = await Promise.all(rawTasks.map(async t => {
      let title = t.title || t.name || '';
      try { 
        if (title.length > 20) title = await Encryption.decrypt(title, encryptionKey); 
      } catch(e) {}
      return { ...t, title };
    }));
    
    // Sort: incomplete first, then by lastModified desc
    allItems.sort((a, b) => {
      if (a.completed !== b.completed) return a.completed ? 1 : -1;
      const timeA = a.lastModified?.seconds || 0;
      const timeB = b.lastModified?.seconds || 0;
      return timeB - timeA;
    });

    tasks = allItems;
    renderTasks();
  });

  // Load Calendar
  const calendarRef = collection(db, 'users', userId, 'calendar_events');
  unsubscribeCalendar = onSnapshot(calendarRef, (snapshot) => {
    calendarEvents = snapshot.docs.map(doc => {
      const data = doc.data();
      return {
        id: doc.id,
        ...data,
        startTime: data.startTime?.toDate() || new Date(),
        endTime: data.endTime?.toDate() || new Date()
      };
    });
    
    // Sort by start time
    calendarEvents.sort((a, b) => a.startTime - b.startTime);
    renderCalendar();
  });
}

function getFaviconUrl(domain) {
  if (!domain) return '';
  // Basic validation: must have at least one dot and only standard characters
  if (!domain.includes('.') || /[^\w.-]/.test(domain)) return '';
  return `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
}

function renderVault() {
  if (vaultType === 'all') {
    renderCombinedList();
  } else if (vaultType === 'accounts') {
    renderAccounts();
  } else if (vaultType === 'totp') {
    renderTOTP();
  }
}

async function renderCombinedList() {
  const list = document.getElementById('accounts-list');
  const empty = document.getElementById('no-accounts');
  
  if (accounts.length === 0 && totpAccounts.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }
  
  empty.classList.add('hidden');
  
  // Filter accounts and TOTP by search query
  let filteredAccounts = accounts;
  let filteredTOTP = totpAccounts;

  if (searchQuery) {
    filteredAccounts = accounts.filter(a => 
      (a.serviceName || '').toLowerCase().includes(searchQuery) ||
      (a.username || '').toLowerCase().includes(searchQuery)
    );
    filteredTOTP = totpAccounts.filter(a => 
      (a.issuer || '').toLowerCase().includes(searchQuery) ||
      (a.accountName || '').toLowerCase().includes(searchQuery)
    );
  }

  if (filteredAccounts.length === 0 && filteredTOTP.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }

  // Update codes first to get latest values
  const totpItems = await Promise.all(filteredTOTP.map(async (acc) => {
    const code = await generateTOTP(acc.secretKey, acc.digits || 6, acc.period || 30);
    return { ...acc, code, type: 'totp' };
  }));

  const allItems = [
    ...filteredAccounts.map(a => ({ ...a, type: 'account' })),
    ...totpItems
  ].sort((a, b) => (a.serviceName || a.issuer || a.accountName).localeCompare(b.serviceName || b.issuer || b.accountName));

  list.innerHTML = allItems.map(item => {
    if (item.type === 'account') {
      return `
        <div class="account-item" data-id="${item.id}">
          <div class="account-icon">
            ${getFaviconUrl(item.websiteUrl || item.serviceName) ? 
              `<img src="${getFaviconUrl(item.websiteUrl || item.serviceName)}" onerror="this.style.display='none'">` : 
              ''}
          </div>
          <div class="account-info">
            <div class="account-name">${escapeHtml(item.serviceName)}</div>
            <div class="account-username">${escapeHtml(item.username)}</div>
          </div>
          <div class="account-actions">
            <button class="fill-btn" title="Tự động điền" data-action="fill">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5l7 7-7 7M5 12h14"/>
              </svg>
            </button>
            <button title="Sao chép mật khẩu" data-action="copy">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
              </svg>
            </button>
            <button class="delete-btn" title="Xóa" data-action="delete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>
              </svg>
            </button>
          </div>
        </div>
      `;
    } else {
      const remaining = getRemainingSeconds(item.period || 30);
      const progress = (remaining / (item.period || 30)) * 100;
      return `
        <div class="totp-item" data-id="${item.id}">
          <div class="totp-header">
            <div class="totp-icon">
              ${getFaviconUrl(item.issuer || item.accountName) ? 
                `<img src="${getFaviconUrl(item.issuer || item.accountName)}" onerror="this.style.display='none'">` : 
                ''}
            </div>
            <div>
              <div class="totp-name">${escapeHtml(item.issuer || item.accountName)}</div>
              <div class="totp-account">${escapeHtml(item.accountName)}</div>
            </div>
          </div>
          <div class="totp-code-display">
            <div class="totp-code" data-code="${item.code}" data-secret="${item.secretKey}" data-digits="${item.digits || 6}" data-period="${item.period || 30}">
              <span class="digits">${item.code.slice(0, 3)} ${item.code.slice(3)}</span>
            </div>
            <div class="totp-timer-mini">
              <span>${remaining}s</span>
              <div class="timer-bar-bg"><div class="timer-bar" style="width: ${progress}%"></div></div>
            </div>
            <button class="delete-btn-mini" title="Xóa" data-action="delete">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      `;
    }
  }).join('');

  setupVaultListeners(list);
}

function setupVaultListeners(list) {
  list.querySelectorAll('[data-action="fill"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = e.target.closest('.account-item').dataset.id;
      fillCredentials(accounts.find(a => a.id === id));
    });
  });
  
  list.querySelectorAll('[data-action="copy"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = e.target.closest('.account-item').dataset.id;
      copyPassword(accounts.find(a => a.id === id));
    });
  });

  list.querySelectorAll('[data-action="delete"]').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const itemEl = e.target.closest('[data-id]');
      const id = itemEl.dataset.id;
      const type = itemEl.classList.contains('account-item') ? 'accounts' : 'totp_accounts';
      const name = itemEl.querySelector('.account-name, .totp-name').textContent;
      
      if (confirm(`Bạn có chắc muốn xóa "${name}"?`)) {
        await deleteItemFromFirestore(type, id);
      }
    });
  });

  list.querySelectorAll('.totp-code').forEach(el => {
    el.addEventListener('click', (e) => {
      e.stopPropagation();
      copyToClipboard(el.dataset.code);
      showToast('Đã sao chép mã!');
    });
  });
}

async function deleteItemFromFirestore(collectionName, documentId) {
  if (!auth.currentUser) return;
  try {
    const docRef = doc(db, 'users', auth.currentUser.uid, collectionName, documentId);
    await deleteDoc(docRef);
    showToast('Đã xóa thành công');
  } catch (err) {
    console.error('Delete error:', err);
    showToast('Lỗi khi xóa: ' + err.message);
  }
}

function cleanupListeners() {
  if (unsubscribeAccounts) unsubscribeAccounts();
  if (unsubscribeTOTP) unsubscribeTOTP();
  if (unsubscribeNotes) unsubscribeNotes();
  if (unsubscribeTasks) unsubscribeTasks();
  if (unsubscribeProjects) unsubscribeProjects();
  if (unsubscribeCalendar) unsubscribeCalendar();
}

// Tab switching
function switchTab(tabId) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.querySelector(`.tab[data-tab="${tabId}"]`).classList.add('active');
  
  document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));
  document.getElementById(`${tabId}-tab`).classList.add('active');
}

// Rendering
function renderAccounts() {
  const list = document.getElementById('accounts-list');
  const empty = document.getElementById('no-accounts');
  
  let filtered = accounts;
  if (searchQuery) {
    filtered = accounts.filter(a => 
      (a.serviceName || '').toLowerCase().includes(searchQuery) ||
      (a.username || '').toLowerCase().includes(searchQuery)
    );
  }

  if (filtered.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }
  
  empty.classList.add('hidden');
  list.innerHTML = filtered.map(acc => `
    <div class="account-item" data-id="${acc.id}">
      <div class="account-icon">
        <img src="https://www.google.com/s2/favicons?domain=${acc.websiteUrl || acc.serviceName}&sz=32" 
             onerror="this.style.display='none'">
      </div>
      <div class="account-info">
        <div class="account-name">${escapeHtml(acc.serviceName)}</div>
        <div class="account-username">${escapeHtml(acc.username)}</div>
      </div>
      <div class="account-actions">
        <button class="fill-btn" title="Tự động điền" data-action="fill">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5l7 7-7 7M5 12h14"/>
          </svg>
        </button>
        <button title="Sao chép mật khẩu" data-action="copy">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
          </svg>
        </button>
        <button class="delete-btn" title="Xóa" data-action="delete">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>
          </svg>
        </button>
      </div>
    </div>
  `).join('');
  
  // Add click handlers
  list.querySelectorAll('[data-action="fill"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = e.target.closest('.account-item').dataset.id;
      fillCredentials(filtered.find(a => a.id === id));
    });
  });
  
  list.querySelectorAll('[data-action="copy"]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = e.target.closest('.account-item').dataset.id;
      copyPassword(filtered.find(a => a.id === id));
    });
  });
}

async function renderTOTP() {
  const list = document.getElementById('accounts-list'); // Use same unified list
  const empty = document.getElementById('no-accounts');
  
  let filtered = totpAccounts;
  if (searchQuery) {
    filtered = totpAccounts.filter(a => 
      (a.issuer || '').toLowerCase().includes(searchQuery) ||
      (a.accountName || '').toLowerCase().includes(searchQuery)
    );
  }

  if (filtered.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }
  
  empty.classList.add('hidden');
  
  // Render TOTP items
  const items = await Promise.all(filtered.map(async (acc) => {
    let code = '------';
    try {
      const decryptedSecret = encryptionKey ? await Encryption.decrypt(acc.secretKey, encryptionKey) : acc.secretKey;
      code = await generateTOTP(decryptedSecret, acc.digits || 6, acc.period || 30);
    } catch (e) {
      console.error('TOTP generation error:', e);
    }
    const remaining = getRemainingSeconds(acc.period || 30);
    const progress = (remaining / (acc.period || 30)) * 100;
    
    return `
      <div class="totp-item" data-id="${acc.id}" data-type="totp">
        <div class="totp-header">
          <div class="totp-icon">
            <img src="https://www.google.com/s2/favicons?domain=${acc.issuer || acc.accountName}&sz=32" 
                 onerror="this.style.display='none'">
          </div>
          <div>
            <div class="totp-name">${escapeHtml(acc.issuer || acc.accountName)}</div>
            ${acc.issuer ? `<div class="totp-account">${escapeHtml(acc.accountName)}</div>` : ''}
          </div>
        </div>
        <div class="totp-code-display">
          <div class="totp-code" data-code="${code}" data-secret="${acc.secretKey}" data-digits="${acc.digits || 6}" data-period="${acc.period || 30}">
            <span class="digits">${code.slice(0, 3)} ${code.slice(3)}</span>
          </div>
          <div class="totp-timer-mini">
            <span>${remaining}s</span>
            <div class="timer-bar-bg"><div class="timer-bar" style="width: ${progress}%"></div></div>
          </div>
          <button class="delete-btn-mini" title="Xóa" data-action="delete">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
            </svg>
          </button>
        </div>
      </div>
    `;
  }));
  
  list.innerHTML = items.join('');
  setupVaultListeners(list);
}

function renderNotes() {
  const list = document.getElementById('notes-list');
  const empty = document.getElementById('no-notes');
  
  let filtered = notes;
  if (searchQuery) {
    filtered = notes.filter(n => 
      (n.title || '').toLowerCase().includes(searchQuery) ||
      (n.content || '').toLowerCase().includes(searchQuery)
    );
  }

  if (filtered.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }
  
  empty.classList.add('hidden');
  list.innerHTML = filtered.map(n => `
    <div class="note-item" data-id="${n.id}">
      <div class="note-header">
        <div class="note-title">${escapeHtml(n.title || 'Ghi chú')}</div>
        <div class="note-actions">
          <button class="copy-note-btn" title="Sao chép nội dung" data-action="copy">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
          </button>
          <button class="delete-note-btn" title="Xóa" data-action="delete">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="note-content">${escapeHtml(n.content || '')}</div>
      <div class="note-date">${n.lastModified ? new Date(n.lastModified.seconds * 1000).toLocaleDateString() : ''}</div>
    </div>
  `).join('');

  // Click handler for whole item
  list.querySelectorAll('.note-item').forEach(itemEl => {
    itemEl.addEventListener('click', (e) => {
      const id = itemEl.dataset.id;
      const note = notes.find(n => n.id === id);
      if (note) showNoteDetail(note);
    });
  });

  // Action handlers
  list.querySelectorAll('.copy-note-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
       e.stopPropagation();
       const noteEl = e.target.closest('.note-item');
       const id = noteEl.dataset.id;
       const note = notes.find(n => n.id === id);
       if (note) {
         copyToClipboard(note.content);
         showToast('Đã sao chép nội dung!');
       }
    });
  });

  list.querySelectorAll('.delete-note-btn').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const noteEl = e.target.closest('.note-item');
      const id = noteEl.dataset.id;
      const title = noteEl.querySelector('.note-title').textContent;
      
      if (confirm(`Bạn có chắc muốn xóa ghi chú "${title}"?`)) {
        await deleteItemFromFirestore('notes', id);
      }
    });
  });
}

function renderTasks() {
  const list = document.getElementById('tasks-list');
  const empty = document.getElementById('no-tasks');
  
  let filtered = tasks;
  
  // Filter by type if not 'all'
  if (taskFilter === 'tasks') {
    filtered = tasks.filter(t => t.type !== 1 && t.taskType !== 1);
  } else if (taskFilter === 'shopping') {
    filtered = tasks.filter(t => t.type === 1 || t.taskType === 1);
  }

  if (searchQuery) {
    filtered = filtered.filter(t => 
      (t.title || '').toLowerCase().includes(searchQuery) ||
      (t.description || '').toLowerCase().includes(searchQuery)
    );
  }

  if (filtered.length === 0) {
    list.innerHTML = '';
    empty.classList.remove('hidden');
    return;
  }
  
  empty.classList.add('hidden');
  list.innerHTML = filtered.map(t => {
    const project = projects.find(p => p.id === t.projectId);
    const projectName = project ? project.name : null;
    const isShopping = (t.type === 1 || t.taskType === 1);
    
    return `
      <div class="task-item ${t.completed ? 'completed' : ''}" data-id="${t.id}">
        <div class="task-checkbox">${t.completed ? (isShopping ? '🛒' : '✅') : (isShopping ? '⬜' : '⭕')}</div>
        <div class="task-info">
          <div class="task-title-row">
            <span class="task-title">${escapeHtml(t.title)}</span>
            ${projectName ? `<span class="project-tag">${escapeHtml(projectName)}</span>` : ''}
          </div>
          ${t.description ? `<div class="task-desc">${escapeHtml(t.description)}</div>` : ''}
        </div>
        <button class="delete-task-btn" title="Xóa">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
          </svg>
        </button>
      </div>
    `;
  }).join('');

  // Add handlers
  list.querySelectorAll('.delete-task-btn').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const itemEl = e.target.closest('.task-item');
      const id = itemEl.dataset.id;
      const title = itemEl.querySelector('.task-title').textContent;
      
      if (confirm(`Bạn có chắc muốn xóa công việc "${title}"?`)) {
        await deleteItemFromFirestore('tasks', id);
      }
    });
  });

  list.querySelectorAll('.task-checkbox').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const itemEl = e.target.closest('.task-item');
      const id = itemEl.dataset.id;
      const task = tasks.find(t => t.id === id);
      if (task) {
        const docRef = doc(db, 'users', auth.currentUser.uid, 'tasks', id);
        await updateDoc(docRef, { completed: !task.completed, lastModified: new Date() });
      }
    });
  });
}

function renderShopping() {
  // Now integrated into renderTasks
}

function renderCalendar() {
  renderDateStrip();
  renderDayView();
}

function changeSelectedDate(days) {
  calendarSelectedDate.setDate(calendarSelectedDate.getDate() + days);
  renderCalendar();
}

function renderDateStrip() {
  const strip = document.getElementById('calendar-date-strip');
  const display = document.getElementById('calendar-date-display');
  
  // Update display text
  const isToday = calendarSelectedDate.toDateString() === new Date().toDateString();
  display.textContent = isToday ? (currentLanguage === 'vi' ? 'Hôm nay' : 'Today') : 
    calendarSelectedDate.toLocaleDateString(currentLanguage === 'vi' ? 'vi-VN' : 'en-US', { day: 'numeric', month: 'short' });

  // Generate 7 days centered on selected date
  const dates = [];
  const start = new Date(calendarSelectedDate);
  start.setDate(start.getDate() - 3);

  for (let i = 0; i < 7; i++) {
    const d = new Date(start);
    d.setDate(d.getDate() + i);
    dates.push(d);
  }

  strip.innerHTML = dates.map(d => {
    const isActive = d.toDateString() === calendarSelectedDate.toDateString();
    const isTodayD = d.toDateString() === new Date().toDateString();
    return `
      <div class="date-item ${isActive ? 'active' : ''} ${isTodayD ? 'is-today' : ''}" data-date="${d.toISOString()}">
        <span class="date-day-name">${d.toLocaleDateString(currentLanguage === 'vi' ? 'vi-VN' : 'en-US', { weekday: 'short' }).replace('.', '')}</span>
        <span class="date-number">${d.getDate()}</span>
      </div>
    `;
  }).join('');

  strip.querySelectorAll('.date-item').forEach(item => {
    item.addEventListener('click', () => {
      calendarSelectedDate = new Date(item.dataset.date);
      renderCalendar();
    });
  });
}

function renderDayView() {
  const container = document.getElementById('calendar-events-container');
  const gutter = document.querySelector('.time-gutter');
  const empty = document.getElementById('no-events');
  
  // Clear
  container.innerHTML = '';
  gutter.innerHTML = '';

  // Generate 24 hours
  for (let h = 0; h < 24; h++) {
    const timeLabel = document.createElement('div');
    timeLabel.className = 'time-label';
    timeLabel.textContent = `${h.toString().padStart(2, '0')}:00`;
    gutter.appendChild(timeLabel);

    const gridLine = document.createElement('div');
    gridLine.className = 'grid-line';
    gridLine.style.top = `${(h / 24) * 100}%`;
    container.appendChild(gridLine);
  }

  // Filter events for selected day
  const selectedDayStart = new Date(calendarSelectedDate);
  selectedDayStart.setHours(0, 0, 0, 0);
  const selectedDayEnd = new Date(calendarSelectedDate);
  selectedDayEnd.setHours(23, 59, 59, 999);

  const dayEvents = calendarEvents.filter(e => {
    return e.startTime < selectedDayEnd && e.endTime >= selectedDayStart;
  });

  if (dayEvents.length === 0) {
    empty.classList.remove('hidden');
    return;
  } else {
    empty.classList.add('hidden');
  }

  // Overlap Detection Logic
  const processedEvents = dayEvents.map((e, idx) => {
    let sMins = 0;
    let eMins = 24 * 60;
    if (e.startTime > selectedDayStart) sMins = e.startTime.getHours() * 60 + e.startTime.getMinutes();
    if (e.endTime < selectedDayEnd) eMins = e.endTime.getHours() * 60 + e.endTime.getMinutes();
    return { event: e, idx, sMins, eMins };
  });

  const columns = new Array(processedEvents.length).fill(0);
  const maxColumns = new Array(processedEvents.length).fill(1);

  for (let i = 0; i < processedEvents.length; i++) {
    const ev1 = processedEvents[i];
    let overlaps = [i];
    
    for (let j = 0; j < processedEvents.length; j++) {
      if (i === j) continue;
      const ev2 = processedEvents[j];
      if (ev1.sMins < ev2.eMins && ev1.eMins > ev2.sMins) {
        overlaps.push(j);
      }
    }
    
    overlaps.sort((a, b) => a - b);
    columns[i] = overlaps.indexOf(i);
    maxColumns[i] = overlaps.length;
  }

  processedEvents.forEach(({ event, idx, sMins, eMins }) => {
    const top = (sMins / (24 * 60)) * 100;
    const height = Math.max(((eMins - sMins) / (24 * 60)) * 100, 2);

    const col = columns[idx];
    const totalCols = maxColumns[idx];
    const width = 100 / totalCols;
    const left = col * width;

    const block = document.createElement('div');
    block.className = 'event-block';
    block.style.top = `${top}%`;
    block.style.height = `${height}%`;
    block.style.left = `calc(${left}% + 2px)`; // Add slight padding
    block.style.width = `calc(${width}% - 4px)`;
    block.style.backgroundColor = event.color || 'var(--primary)';
    block.style.borderLeft = `3px solid ${darkenColor(event.color || '#6366f1', 20)}`;
    
    block.innerHTML = `
      <div class="event-block-title">${escapeHtml(event.title)}</div>
      <div class="event-block-time">${event.startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })}</div>
    `;

    container.appendChild(block);
  });

  // Scroll to current time if today
  if (calendarSelectedDate.toDateString() === new Date().toDateString()) {
    const nowMins = new Date().getHours() * 60 + new Date().getMinutes();
    const scrollPos = (nowMins / (24 * 60)) * document.querySelector('.day-view').scrollHeight;
    document.querySelector('.day-view').scrollTop = Math.max(0, scrollPos - 100);
  }
}

function darkenColor(col, amt) {
  let usePound = false;
  if (col[0] === "#") { col = col.slice(1); usePound = true; }
  let num = parseInt(col, 16);
  let r = (num >> 16) - amt;
  if (r > 255) r = 255; else if (r < 0) r = 0;
  let b = ((num >> 8) & 0x00FF) - amt;
  if (b > 255) b = 255; else if (b < 0) b = 0;
  let g = (num & 0x0000FF) - amt;
  if (g > 255) g = 255; else if (g < 0) g = 0;
  return (usePound ? "#" : "") + (g | (b << 8) | (r << 16)).toString(16).padStart(6, '0');
}

async function updateTOTPCodes() {
  const totpCodes = document.querySelectorAll('.totp-code');
  if (totpCodes.length === 0) return;

  for (const el of totpCodes) {
    const secret = el.dataset.secret;
    const digits = parseInt(el.dataset.digits) || 6;
    const period = parseInt(el.dataset.period) || 30;
    
    if (secret) {
      let decryptedSecret = secret;
      if (encryptionKey && secret.length > 20) {
        try {
          decryptedSecret = await Encryption.decrypt(secret, encryptionKey);
        } catch (e) {
          // Fallback if not encrypted or decryption fails
        }
      }

      const code = await generateTOTP(decryptedSecret, digits, period);
      const digitsEl = el.querySelector('.digits') || el;
      digitsEl.textContent = `${code.slice(0, 3)} ${code.slice(3)}`;
      el.dataset.code = code;
      
      const parent = el.closest('.totp-code-display') || el.parentElement;
      const remainingEl = parent.querySelector('.totp-timer-mini span');
      const barEl = parent.querySelector('.timer-bar');
      if (remainingEl && barEl) {
        const remaining = getRemainingSeconds(period);
        remainingEl.textContent = `${remaining}s`;
        barEl.style.width = `${(remaining / period) * 100}%`;
      }
    }
  }
}

// TOTP Generation (RFC 6238)
async function generateTOTP(secret, digits = 6, period = 30) {
  try {
    const key = base32Decode(secret.replace(/\s/g, '').toUpperCase());
    const counter = Math.floor(Date.now() / 1000 / period);
    const counterBytes = intToBytes(counter);
    
    const cryptoKey = await crypto.subtle.importKey(
      'raw', key,
      { name: 'HMAC', hash: 'SHA-1' },
      false, ['sign']
    );
    
    const signature = await crypto.subtle.sign('HMAC', cryptoKey, counterBytes);
    const hmac = new Uint8Array(signature);
    
    const offset = hmac[hmac.length - 1] & 0x0f;
    const binary = 
      ((hmac[offset] & 0x7f) << 24) |
      ((hmac[offset + 1] & 0xff) << 16) |
      ((hmac[offset + 2] & 0xff) << 8) |
      (hmac[offset + 3] & 0xff);
    
    const otp = binary % Math.pow(10, digits);
    return otp.toString().padStart(digits, '0');
  } catch (e) {
    console.error('TOTP error:', e);
    return '------';
  }
}

function base32Decode(input) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
  let bits = '';
  for (const char of input) {
    if (char === '=') continue;
    const index = chars.indexOf(char);
    if (index === -1) continue;
    bits += index.toString(2).padStart(5, '0');
  }
  const bytes = new Uint8Array(Math.floor(bits.length / 8));
  for (let i = 0; i < bytes.length; i++) {
    bytes[i] = parseInt(bits.slice(i * 8, (i + 1) * 8), 2);
  }
  return bytes;
}

function intToBytes(num) {
  const bytes = new Uint8Array(8);
  for (let i = 7; i >= 0; i--) {
    bytes[i] = num & 0xff;
    num = Math.floor(num / 256);
  }
  return bytes;
}

function getRemainingSeconds(period = 30) {
  return period - (Math.floor(Date.now() / 1000) % period);
}

// Actions
async function fillCredentials(account) {
  if (!account) return;
  
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    
    await chrome.tabs.sendMessage(tab.id, {
      action: 'fill',
      username: account.username,
      password: account.password
    });
    
    showToast('Đã điền thông tin!');
    window.close();
  } catch (e) {
    console.error('Fill error:', e);
    showToast('Không thể tự động điền');
  }
}

async function copyPassword(account) {
  if (!account?.password) return;
  await copyToClipboard(account.password);
  showToast('Đã sao chép mật khẩu!');
}

async function copyToClipboard(text) {
  await navigator.clipboard.writeText(text);
}

function showToast(message) {
  const existing = document.querySelector('.copied-toast');
  if (existing) existing.remove();
  
  const toast = document.createElement('div');
  toast.className = 'copied-toast';
  toast.textContent = message;
  document.body.appendChild(toast);
  
  setTimeout(() => toast.remove(), 2000);
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// Update TOTP every second
setInterval(() => {
  if (screens.main && screens.main.classList.contains('active')) {
    updateTOTPCodes();
  }
}, 1000);
