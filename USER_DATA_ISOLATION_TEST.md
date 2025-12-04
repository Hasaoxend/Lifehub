# 🔒 KIỂM TRA PHÂN LY DỮ LIỆU NGƯỜI DÙNG

## ✅ CÁC LỚP BẢO MẬT ĐÃ TRIỂN KHAI

App LifeHub có **4 tầng bảo mật** để đảm bảo dữ liệu của User A **KHÔNG BAO GIỜ** hiện lên User B:

### **Tầng 1: Firestore Path Isolation** 🏗️
```
users/{userId}/accounts/{accountId}
users/{userId}/totp_accounts/{totpId}
users/{userId}/notes/{noteId}
users/{userId}/tasks/{taskId}
users/{userId}/projects/{projectId}
users/{userId}/calendar_events/{eventId}
```
- Mỗi user có collection **RIÊNG BIỆT**
- Không thể truy cập cross-path

### **Tầng 2: Firestore Security Rules** 🛡️
```javascript
match /users/{userId}/accounts/{accountId} {
  allow read, write: if request.auth.uid == userId;
}
```
- Firebase **TỰ ĐỘNG CHẶN** mọi request không hợp lệ
- User A không thể đọc data của User B ngay cả khi hack code

### **Tầng 3: Repository User Tracking** 👤
```java
public void startListening() {
    String newUserId = mAuth.getCurrentUser().getUid();
    
    // Detect user change
    if (currentUserId != null && !currentUserId.equals(newUserId)) {
        stopListening();           // Stop old listener
        mAllData.setValue(new ArrayList<>()); // Clear old data
    }
}
```
- Auto-detect khi user thay đổi
- Clear data ngay lập tức

### **Tầng 4: Logout Flow** 🚪
```java
btnLogout.setOnClickListener(v -> {
    totpRepository.stopListening();
    accountRepository.stopListening();
    calendarRepository.stopListening();
    productivityRepository.stopListening();
    sessionManager.logoutUser();
    mAuth.signOut();
});
```
- Stop tất cả Firestore listeners
- Clear session
- SignOut Firebase Auth

---

## 🧪 TEST SCRIPT - KIỂM TRA PHÂN LY DỮ LIỆU

### ⚠️ CHÚ Ý QUAN TRỌNG
Trước khi test, **BẮT BUỘC** phải:
1. ✅ Deploy Firestore Rules lên Firebase Console
2. ✅ Kiểm tra Rules đã active (vào Firebase Console → Firestore Database → Rules)
3. ✅ Build và cài app mới nhất

---

### 📱 Test Case 1: Logout → Login User Khác

**Mục đích:** Verify không có data leak khi đổi user

#### Bước 1: Setup User A
```
1. Đăng ký/Login User A (ví dụ: userA@test.com)
2. Tạo dữ liệu:
   
   📧 Module Accounts:
   - Account 1: Gmail (user: userA@gmail.com, pass: PasswordA123)
   - Account 2: Facebook (user: userA, pass: FbPassA)
   
   🔐 Module Authenticator:
   - TOTP 1: Google (secret: JBSWY3DPEHPK3PXP)
   - TOTP 2: GitHub (secret: HXDMVJECJJWSRB3H)
   
   📝 Module Notes:
   - Note 1: "User A's Secret Note"
   - Note 2: "User A's Work Note"
   
   ✅ Module Tasks:
   - Task 1: "User A Buy Milk"
   - Task 2: "User A Meeting"
   
   📅 Module Calendar:
   - Event 1: "User A Birthday" (01/01/2026)
   - Event 2: "User A Vacation" (15/02/2026)

3. Verify data hiển thị đầy đủ
```

#### Bước 2: Logout
```
4. Vào Settings → Logout
5. KIỂM TRA Logcat phải có:
   ✅ "Stopping all Firestore listeners on logout"
   ✅ "Removing Firestore listener for user: [User A UID]" (4 dòng)
6. App quay về Login screen
```

#### Bước 3: Login User B & Verify
```
7. Đăng ký/Login User B (ví dụ: userB@test.com)
8. ✅ VERIFY - Tất cả module phải TRỐNG:
   
   📧 Accounts: "No accounts yet"
   🔐 Authenticator: "No accounts configured"
   📝 Notes: "No notes" 
   ✅ Tasks: "No tasks"
   📅 Calendar: "No events"
   
9. Kiểm tra Logcat KHÔNG CÓ:
   ❌ "userA@gmail.com"
   ❌ "User A's Secret Note"
   ❌ "User A Birthday"
   ❌ Bất kỳ dữ liệu nào của User A
```

#### Bước 4: Tạo data User B
```
10. Tạo dữ liệu cho User B:
    - Account: Twitter (user: userB, pass: TwitterB)
    - Note: "User B's Note"
    - Task: "User B Shopping"
    - Event: "User B Meeting" (20/03/2026)
    
11. ✅ VERIFY: Chỉ thấy data của User B
```

#### Bước 5: Logout và Login lại User A
```
12. Logout User B
13. Login lại User A
14. ✅ VERIFY:
    - Vẫn thấy đầy đủ data của User A
    - KHÔNG thấy data của User B
```

---

### 📱 Test Case 2: Multi-Device (2 Thiết Bị)

**Mục đích:** Verify realtime sync & data isolation

#### Setup
```
Thiết bị 1: Emulator/Phone 1
Thiết bị 2: Emulator/Phone 2
```

#### Test
```
1. Device 1: Login User A
2. Device 1: Tạo Account "Gmail Test"
3. Device 2: Login User A (cùng account)
4. Device 2: ✅ VERIFY: Thấy "Gmail Test" xuất hiện (realtime sync)
5. Device 2: Tạo Account "Facebook Test"
6. Device 1: ✅ VERIFY: Thấy "Facebook Test" xuất hiện tự động
7. Device 2: Logout
8. Device 2: Login User B
9. Device 2: ✅ VERIFY: KHÔNG thấy data của User A
10. Device 1: ✅ VERIFY: Vẫn thấy đầy đủ data User A
```

---

### 📱 Test Case 3: Firestore Direct Access (Advanced)

**Mục đích:** Verify Firebase Security Rules block trái phép

#### Bước 1: Chuẩn bị
```
1. Login User A
2. Copy User A UID từ Logcat:
   "User ID: abc123xyz" → UID = abc123xyz
3. Tạo 1 Calendar Event: "Private Event A"
```

#### Bước 2: Test Firestore Console
```
4. Mở Firebase Console → Firestore Database
5. Tìm path: users/abc123xyz/calendar_events
6. ✅ VERIFY: Thấy "Private Event A"
7. Copy Event ID (ví dụ: evt001)
```

#### Bước 3: Login User B và hack path
```
8. Logout, Login User B
9. Copy User B UID từ Logcat: def456uvw
10. Mở Firebase Console
11. Thử đọc path của User A:
    users/abc123xyz/calendar_events/evt001
12. ✅ VERIFY: Firebase hiển thị "Missing or insufficient permissions"
    (Nếu Security Rules đã deploy đúng)
```

---

### 📱 Test Case 4: Rapid User Switch

**Mục đích:** Verify no data flash during switch

```
1. Login User A
2. Quan sát màn hình Accounts
3. Logout
4. NGAY LẬP TỨC login User B
5. ✅ VERIFY:
   - Màn hình KHÔNG flash/hiển thị tạm thời data của User A
   - Chuyển thẳng sang "No accounts yet"
6. Kiểm tra Logcat:
   ✅ "User changed from [A] to [B], clearing old data"
   ✅ "Stopped all Firestore listeners"
```

---

## 🚨 DẤU HIỆU DATA LEAK (Nếu thấy = CÓ BUG)

### ❌ NGUY HIỂM - Báo ngay nếu thấy:

1. **Flash Data của User Khác**
   - Login User B nhưng thấy thoáng qua data User A
   - → BUG: LiveData chưa clear kịp

2. **Data Persistence Cross-User**
   - Logout User A, Login User B vẫn thấy 1-2 items của A
   - → BUG: Listener chưa stop

3. **Logcat Warnings**
   ```
   ❌ "Detected userOwnerId mismatch"
   ❌ "Warning: Data belongs to different user"
   ```
   - → BUG: Firestore trả data sai user (Rules chưa deploy)

4. **Firestore Console Access**
   - User B có thể đọc path của User A
   - → BUG: Security Rules chưa active

---

## ✅ KẾT LUẬN

### Nếu TẤT CẢ test cases PASS:

**→ DỮ LIỆU NGƯỜI DÙNG ĐƯỢC BẢO VỆ 100%** ✅

- ✅ User A không thể thấy data User B
- ✅ User B không thể thấy data User A
- ✅ Logout/Login không leak data
- ✅ Multi-device realtime sync đúng user
- ✅ Firebase Security Rules block trái phép
- ✅ Client-side validation đúng

### Các lớp bảo vệ:

```
User A Data ──────────────────────────────────────
                                                  │
    ┌─────────────────────────────────────────┐  │
    │  Firestore Security Rules               │  │
    │  ✅ Allow read/write: userId == auth    │  │
    └─────────────────────────────────────────┘  │
                     │                            │
    ┌─────────────────────────────────────────┐  │
    │  Repository User Tracking               │  │
    │  ✅ Detect user change → clear data     │  │
    └─────────────────────────────────────────┘  │
                     │                            │
    ┌─────────────────────────────────────────┐  │
    │  Logout Flow                            │  │
    │  ✅ Stop listeners → clear session      │  │
    └─────────────────────────────────────────┘  │
                     │                            │
    ┌─────────────────────────────────────────┐  │
    │  Path Isolation                         │  │
    │  ✅ users/{userId}/...                  │  │
    └─────────────────────────────────────────┘  │
                                                  │
User B Data ──────────────────────────────────────
       (HOÀN TOÀN TÁCH BIỆT)
```

---

## 📋 CHECKLIST TRƯỚC KHI RELEASE

- [ ] Firestore Rules đã deploy lên Firebase Console
- [ ] Test Case 1 PASS (Logout → Login khác)
- [ ] Test Case 2 PASS (Multi-device)
- [ ] Test Case 3 PASS (Firestore permissions)
- [ ] Test Case 4 PASS (Rapid switch)
- [ ] Logcat sạch (không có warning userOwnerId mismatch)
- [ ] Firebase Console: User B không đọc được path User A

---

## 🛡️ BẢO VỆ THÊM (Khuyến nghị)

### 1. Enable Firestore Audit Logs (Firebase Console)
```
Firebase Console → Firestore → Security → Audit Logs
→ Enable để theo dõi mọi truy cập
```

### 2. Monitor Production (sau khi release)
```
Kiểm tra Firebase Console → Firestore → Usage
- Nếu thấy spike đột ngột queries → có thể bị hack
```

### 3. Code Review Checklist
Mỗi khi thêm feature mới:
- [ ] Có set `userOwnerId` khi insert?
- [ ] Có validate `userOwnerId` khi update/delete?
- [ ] Có stop listener trong logout flow?
- [ ] Có clear LiveData khi user change?

---

**📞 HỖ TRỢ:** Nếu có test case nào FAIL, kiểm tra lại:
1. Firestore Rules đã deploy chưa?
2. App đã rebuild với code mới nhất chưa?
3. Logcat có warning gì không?
