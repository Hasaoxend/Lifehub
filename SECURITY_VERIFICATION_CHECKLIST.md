ho thanhhf 1 # CHECKLIST KIỂM TRA BẢO MẬT - LIFEHUB APP
**Ngày:** 4/12/2025  
**Phiên bản:** Post-Fix Security Patch

---

## ✅ CÁC BIỆN PHÁP BẢO MẬT ĐÃ TRIỂN KHAI

### 1. **Firestore Security Rules** ⭐⭐⭐⭐⭐

**File:** `firestore.rules`

```
✅ Path-based isolation: users/{userId}/...
✅ Rule: allow read, write: if request.auth.uid == userId
✅ Áp dụng cho TẤT CẢ collections:
   - accounts
   - totp_accounts
   - notes
   - tasks
   - projects
   - calendar_events
```

**Kết quả:** User A KHÔNG THỂ đọc/ghi data của User B ở Firestore level.

---

### 2. **Repository User Tracking & Listener Management** ⭐⭐⭐⭐⭐

#### ✅ TotpRepository
```java
✅ private String currentUserId
✅ private ListenerRegistration listener
✅ startListening() - Detect user change, clear old data
✅ stopListening() - Remove listener, clear LiveData
✅ Gọi trong: SettingsFragment.logout(), ChangePasswordActivity.logout()
```

#### ✅ AccountRepository
```java
✅ private String currentUserId
✅ private ListenerRegistration listener
✅ startListening() - Detect user change, clear old data
✅ stopListening() - Remove listener, clear LiveData
✅ Gọi trong: SettingsFragment.logout(), ChangePasswordActivity.logout()
```

#### ✅ CalendarRepository
```java
✅ private String currentUserId
✅ private ListenerRegistration listener
✅ startListening() - Detect user change, clear old data
✅ stopListening() - Remove listener, clear LiveData
✅ Gọi trong: SettingsFragment.logout(), ChangePasswordActivity.logout()
✅ Ownership validation trong update/delete
```

#### ✅ ProductivityRepository (FIXED)
```java
✅ private String currentUserId
✅ private ListenerRegistration notesListener, tasksListener, projectsListener
✅ startListening() - Detect user change, clear old data
✅ stopListening() - Remove ALL listeners
✅ clearAllData() - Clear all 4 LiveData (notes, tasks, shopping, projects)
✅ Gọi trong: SettingsFragment.logout(), ChangePasswordActivity.logout()
```

---

### 3. **Logout Flow** ⭐⭐⭐⭐⭐

#### ✅ SettingsFragment.java
```java
btnLogout.setOnClickListener(v -> {
    ✅ totpRepository.stopListening();
    ✅ accountRepository.stopListening();
    ✅ calendarRepository.stopListening();
    ✅ productivityRepository.stopListening();
    ✅ sessionManager.logoutUser();
    ✅ mAuth.signOut();
    ✅ Navigate to LoginActivity with CLEAR_TASK flag
});
```

#### ✅ ChangePasswordActivity.java
```java
private void logoutAndRedirect() {
    ✅ totpRepository.stopListening();
    ✅ accountRepository.stopListening();
    ✅ calendarRepository.stopListening();
    ✅ productivityRepository.stopListening();
    ✅ sessionManager.logoutUser();
    ✅ mAuth.signOut();
    ✅ Navigate to LoginActivity with CLEAR_TASK flag
}
```

---

### 4. **Login Flow** ⭐⭐⭐⭐⭐

#### ✅ MainActivity.java
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    ✅ totpRepository.startListening();
    ✅ accountRepository.startListening();
    ✅ calendarRepository.startListening();
    // ProductivityRepository auto-starts in constructor
}
```

**Kết quả:** Khi user login, repositories tự động load đúng data của user đó.

---

### 5. **Data Models** ⭐⭐⭐⭐⭐

```java
✅ AccountEntry.userOwnerId
✅ TotpAccount.userOwnerId
✅ NoteEntry.userOwnerId
✅ TaskEntry.userOwnerId
✅ ProjectEntry.userOwnerId
✅ CalendarEvent.userOwnerId
```

**Tất cả repositories SET userOwnerId = currentUser.getUid() khi insert/update.**

---

### 6. **Encrypted Storage** ⭐⭐⭐⭐⭐

#### ✅ SessionManager
```java
✅ EncryptedSharedPreferences
✅ MasterKey: AES256-GCM
✅ PrefKey encryption: AES256-SIV
✅ PrefValue encryption: AES256-GCM
```

#### ✅ EncryptionHelper
```java
✅ TOTP secrets encrypted before saving to Firestore
```

---

## 🧪 TEST CASES - PHẢI THỬ NGHIỆM

### Test Case 1: **Logout → Login User Khác**

**Mục đích:** Verify không có data leak

**Bước thực hiện:**
```
1. Login User A (email: userA@test.com)
2. Tạo data:
   - 3 accounts (Gmail, Facebook, Twitter)
   - 2 TOTP (Google, GitHub)
   - 5 notes
   - 3 tasks
   - 2 calendar events
3. Logout (Settings → Logout)
4. KIỂM TRA: Logcat phải có logs:
   "Stopping all Firestore listeners on logout"
   "Stopped all Firestore listeners" (ProductivityRepository)
   "Removing Firestore listener for user: ..." (TotpRepository)
   "Removing Firestore listener for user: ..." (AccountRepository)
   "Removing Firestore listener for user: ..." (CalendarRepository)
5. Login User B (email: userB@test.com)
6. ✅ VERIFY:
   - Accounts list: EMPTY (không thấy Gmail/Facebook/Twitter của User A)
   - TOTP list: EMPTY (không thấy Google/GitHub của User A)
   - Notes list: EMPTY
   - Tasks list: EMPTY
   - Calendar: EMPTY
7. Tạo data của User B
8. ✅ VERIFY: Chỉ thấy data của User B
```

**Expected Result:**
```
❌ KHÔNG được thấy data của User A trong bất kỳ màn hình nào
✅ Tất cả lists phải EMPTY ngay lập tức
✅ Không có delay hiển thị data cũ
```

---

### Test Case 2: **Switch User Nhanh (Race Condition)**

**Mục đích:** Verify handle đúng race condition

**Bước thực hiện:**
```
1. Login User A
2. Load đầy data
3. Logout
4. NGAY LẬP TỨC login User B (trong vòng 1 giây)
5. ✅ VERIFY:
   - MainActivity.onCreate() gọi startListening()
   - ProductivityRepository detect user change
   - Logcat: "User changed from {userA_uid} to {userB_uid}, clearing old data"
   - Logcat: "Cleared all LiveData"
6. Check UI:
   ✅ KHÔNG thấy data của User A (ngay cả flash nhanh)
   ✅ Chỉ thấy data của User B
```

---

### Test Case 3: **App Restart (Kill Process)**

**Mục đích:** Verify app restart an toàn

**Bước thực hiện:**
```
1. Login User A
2. Load data
3. Kill app (Settings → Force stop hoặc swipe away)
4. Mở app lại
5. ✅ VERIFY:
   - App quay về LoginActivity (session đã logout nếu không dùng biometric)
   - HOẶC nếu dùng biometric → auto login User A
   - Repository chỉ load data của User A
   - KHÔNG load data cũ từ cache
```

---

### Test Case 4: **Concurrent Login (2 Devices)**

**Mục đích:** Verify multi-device safety

**Bước thực hiện:**
```
Device 1:
1. Login User A
2. Tạo account "Test Account 1"
3. Để app chạy

Device 2:
1. Login User A (cùng account)
2. ✅ VERIFY: Thấy "Test Account 1" (realtime sync)
3. Tạo account "Test Account 2"

Device 1:
✅ VERIFY: Tự động thấy "Test Account 2" xuất hiện (Firestore listener)

Device 2:
Logout

Device 1:
✅ VERIFY: Vẫn đăng nhập, vẫn thấy đầy đủ data
```

---

### Test Case 5: **Ownership Validation (Calendar)**

**Mục đích:** Verify cannot update/delete other user's data

**Bước thực hiện:**
```
1. Login User A
2. Tạo calendar event "Meeting A"
3. Inspect Firestore:
   users/{userA_uid}/calendar_events/{eventId}
   - userOwnerId: {userA_uid}
4. Logout
5. Login User B
6. Thử update event của User A bằng cách:
   - Manually change documentId trong code (if possible)
   - Hoặc inject documentId từ User A
7. ✅ VERIFY Logcat:
   "❌ SECURITY VIOLATION: User {userB_uid} attempted to update event owned by {userA_uid}"
8. Check Firestore:
   ✅ Event của User A KHÔNG bị thay đổi
```

---

### Test Case 6: **Memory Leak Check**

**Mục đích:** Verify no memory leak from listeners

**Bước thực hiện:**
```
1. Enable Profiler trong Android Studio
2. Login User A
3. Load data
4. Logout
5. Repeat 10 lần
6. ✅ VERIFY Memory Profiler:
   - Heap size KHÔNG tăng liên tục
   - Listeners được remove (check Instances count)
   - LiveData observers cleared
```

---

### Test Case 7: **Firestore Rules Validation**

**Mục đích:** Verify server-side security

**Bước thực hiện:**
```
1. Mở Firestore console
2. Vào Rules Playground
3. Test rules:

Test 1: User A read User B's account
   - Simulate auth: userA_uid
   - Path: /users/{userB_uid}/accounts/{accountId}
   - Operation: get
   - ❌ Expected: DENIED

Test 2: User A read own account
   - Simulate auth: userA_uid
   - Path: /users/{userA_uid}/accounts/{accountId}
   - Operation: get
   - ✅ Expected: ALLOWED

Test 3: Unauthenticated read
   - Simulate: No auth
   - Path: /users/{userA_uid}/accounts/{accountId}
   - Operation: get
   - ❌ Expected: DENIED
```

---

## 📊 CHECKLIST KIỂM TRA NHANH

### Trước khi Deploy Production:

- [ ] **Test Case 1** - Logout → Login user khác: PASSED
- [ ] **Test Case 2** - Switch user nhanh: PASSED
- [ ] **Test Case 3** - App restart: PASSED
- [ ] **Test Case 4** - Concurrent login: PASSED
- [ ] **Test Case 5** - Ownership validation: PASSED
- [ ] **Test Case 6** - Memory leak: PASSED
- [ ] **Test Case 7** - Firestore rules: PASSED

### Code Review:

- [ ] Tất cả repositories có `startListening()` và `stopListening()`
- [ ] Tất cả logout flows gọi `.stopListening()` cho ALL repos
- [ ] MainActivity.onCreate() gọi `.startListening()` cho ALL repos
- [ ] Tất cả data models có `userOwnerId` field
- [ ] Tất cả insert/update operations set `userOwnerId`
- [ ] CalendarRepository có ownership validation
- [ ] Firestore rules deploy và test

### Logcat Verification:

```bash
# Khi logout, phải thấy:
adb logcat | grep "Stopping all Firestore listeners"
adb logcat | grep "Stopped all Firestore listeners"
adb logcat | grep "Removing Firestore listener"
adb logcat | grep "Cleared all LiveData"

# Khi user change, phải thấy:
adb logcat | grep "User changed from"
adb logcat | grep "clearing old data"

# Khi ownership violation, phải thấy:
adb logcat | grep "SECURITY VIOLATION"
```

---

## 🚨 CẢNH BÁO

### KHÔNG BAO GIỜ:
- ❌ Remove `.stopListening()` calls từ logout flows
- ❌ Remove user change detection từ repositories
- ❌ Disable Firestore security rules
- ❌ Skip `userOwnerId` validation
- ❌ Cache data across user sessions

### LUÔN LUÔN:
- ✅ Clear LiveData khi user logout
- ✅ Remove Firestore listeners khi không cần
- ✅ Set `userOwnerId` khi create/update documents
- ✅ Test logout → login flow sau mỗi code change
- ✅ Monitor logcat cho security warnings

---

## 📈 METRICS MONITORING (Production)

### Cần monitor:
1. **Authentication Events:**
   - Login success rate
   - Logout frequency
   - Session duration

2. **Firestore Operations:**
   - Read/Write operations per user
   - Permission denied errors (should be 0)
   - Cross-user access attempts (should be 0)

3. **Performance:**
   - Listener count (should decrease after logout)
   - Memory usage pattern
   - App startup time

4. **Security Incidents:**
   - Failed ownership validations
   - Unauthorized access attempts
   - Anomalous data access patterns

---

**✅ Kết luận:** App đã được harden với multiple layers of security. Cần test kỹ trước khi release production!
