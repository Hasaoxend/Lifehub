# BÁO CÁO KIỂM TRA BẢO MẬT - LIFEHUB APP
**Ngày kiểm tra:** 4/12/2025  
**Người thực hiện:** Security Audit  
**Phạm vi:** Data isolation, User session management, Firestore security

---

## 📊 TỔNG QUAN

### ✅ Điểm Mạnh
1. **Firestore Security Rules**: ⭐⭐⭐⭐⭐
   - Path-based isolation: `users/{userId}/...`
   - Chặn cross-user access hoàn toàn ở Firestore level
   - Rule: `allow read, write: if request.auth.uid == userId`

2. **Client-Side Security** (Một phần): ⭐⭐⭐⭐
   - CalendarRepository: Có validation `userOwnerId` khi update/delete
   - AccountRepository: Có warning log khi detect userOwnerId mismatch
   - TotpRepository: Có filtering accounts theo userOwnerId
   - Tất cả Repository đều set `userOwnerId` khi insert/update

3. **Encrypted Storage**: ⭐⭐⭐⭐⭐
   - SessionManager sử dụng EncryptedSharedPreferences
   - MasterKey: AES256-GCM
   - TOTP secrets được mã hóa trước khi lưu Firestore

---

## 🚨 CÁC VẤN ĐỀ BẢO MẬT NGHIÊM TRỌNG

### ❌ **CRITICAL - Data Leak Khi Logout/Switch User**

**Mức độ:** 🔴 CRITICAL  
**CVSS Score:** 8.5 (High)

#### Mô tả vấn đề:
Khi user logout hoặc switch account, dữ liệu của user cũ **VẪN CÒN TRONG MEMORY** (LiveData) và có thể hiển thị cho user mới trong khoảng thời gian ngắn trước khi Firestore listener được cập nhật.

#### Chi tiết kỹ thuật:

**File:** `SettingsFragment.java` (line 95-102)
```java
btnLogout.setOnClickListener(v -> {
    // ✅ Chỉ stop TotpRepository listener
    totpRepository.stopListening();
    
    // ❌ THIẾU: accountRepository.stopListening()
    // ❌ THIẾU: calendarRepository.stopListening()
    // ❌ THIẾU: productivityRepository - không có method stopListening()
    
    sessionManager.logoutUser();
    mAuth.signOut();
    Intent intent = new Intent(requireContext(), LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
});
```

#### Kịch bản tấn công:

1. **User A đăng nhập** → App load dữ liệu vào LiveData:
   - `AccountRepository.mAllAccounts` = [Account1, Account2, Account3]
   - `CalendarRepository.eventsLiveData` = [Event1, Event2]
   - `ProductivityRepository.mAllTasks` = [Task1, Task2]

2. **User A logout**:
   - ✅ `mAuth.signOut()` → Firebase Auth logout
   - ✅ `totpRepository.stopListening()` → TOTP data cleared
   - ❌ `accountRepository.stopListening()` **KHÔNG** được gọi
   - ❌ `calendarRepository.stopListening()` **KHÔNG** được gọi
   - ❌ `ProductivityRepository` **KHÔNG CÓ** method stopListening

3. **User B đăng nhập ngay sau đó**:
   - Repository constructors được gọi lại (vì Singleton/ActivityRetainedScoped)
   - `startListening()` được gọi với userB's UID
   - **NHƯNG** LiveData vẫn chứa data cũ của User A!

4. **UI hiển thị**:
   - Fragment/Activity observe LiveData
   - **HIỂN THỊ DỮ LIỆU CỦA USER A** cho User B trong vài giây
   - Sau đó Firestore listener mới cập nhật data của User B

#### Impact:
- 🔴 **Data leak**: User B có thể thấy passwords, notes, tasks của User A
- 🔴 **Privacy violation**: Vi phạm nghiêm trọng quyền riêng tư
- 🔴 **Compliance risk**: Vi phạm GDPR, CCPA nếu deploy

---

### ⚠️ **MEDIUM - ProductivityRepository Thiếu User Change Detection**

**Mức độ:** 🟡 MEDIUM  
**CVSS Score:** 5.5 (Medium)

#### Vấn đề:
ProductivityRepository không có:
- `currentUserId` tracking
- `stopListening()` method
- User change detection logic

#### File: `ProductivityRepository.java`
```java
@ActivityRetainedScoped
public class ProductivityRepository {
    // ❌ THIẾU: private String currentUserId = null;
    // ❌ THIẾU: private ListenerRegistration listener;
    
    @Inject
    public ProductivityRepository(FirebaseAuth auth, FirebaseFirestore db) {
        // ⚠️ Không check user change
        // ⚠️ Không clear data khi user logout
        listenForNoteChanges();
        listenForTaskChanges();
        listenForProjectChanges();
    }
    
    // ❌ THIẾU: public void stopListening() {...}
}
```

#### So sánh với TotpRepository (Đúng):
```java
public void startListening() {
    String newUserId = currentUser.getUid();
    
    // ✅ Detect user change
    if (currentUserId != null && !currentUserId.equals(newUserId)) {
        stopListening(); // Clear old data
        mAllAccounts.setValue(new ArrayList<>()); // Clear LiveData
    }
    
    currentUserId = newUserId;
    // ... setup new listener
}
```

---

## 🔧 GIẢI PHÁP ĐỀ XUẤT

### 1. **Sửa ProductivityRepository** (PRIORITY: HIGH)

Thêm user tracking và stopListening:

```java
private String currentUserId = null;
private ListenerRegistration notesListener = null;
private ListenerRegistration tasksListener = null;
private ListenerRegistration projectsListener = null;

public void startListening() {
    FirebaseUser currentUser = mAuth.getCurrentUser();
    if (currentUser == null) {
        stopListening();
        clearAllData();
        return;
    }
    
    String newUserId = currentUser.getUid();
    
    // Detect user change
    if (currentUserId != null && !currentUserId.equals(newUserId)) {
        Log.d(TAG, "User changed, clearing old data");
        stopListening();
        clearAllData();
    }
    
    currentUserId = newUserId;
    // ... start listeners
}

public void stopListening() {
    if (notesListener != null) notesListener.remove();
    if (tasksListener != null) tasksListener.remove();
    if (projectsListener != null) projectsListener.remove();
    
    notesListener = null;
    tasksListener = null;
    projectsListener = null;
    currentUserId = null;
}

private void clearAllData() {
    mAllNotes.setValue(new ArrayList<>());
    mAllTasks.setValue(new ArrayList<>());
    mAllShoppingItems.setValue(new ArrayList<>());
    mAllProjects.setValue(new ArrayList<>());
}
```

### 2. **Sửa SettingsFragment Logout** (PRIORITY: CRITICAL)

```java
btnLogout.setOnClickListener(v -> {
    // ✅ Stop ALL repository listeners
    totpRepository.stopListening();
    accountRepository.stopListening();
    calendarRepository.stopListening();
    productivityRepository.stopListening(); // Thêm sau khi implement
    
    sessionManager.logoutUser();
    mAuth.signOut();
    
    Intent intent = new Intent(requireContext(), LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
});
```

### 3. **Thêm Global Logout Handler** (PRIORITY: MEDIUM)

Tạo `LogoutManager` để centralize logout logic:

```java
@Singleton
public class LogoutManager {
    private final AccountRepository accountRepo;
    private final TotpRepository totpRepo;
    private final CalendarRepository calendarRepo;
    private final ProductivityRepository productivityRepo;
    private final SessionManager sessionManager;
    private final FirebaseAuth auth;
    
    public void logout(Context context) {
        // Stop all listeners
        accountRepo.stopListening();
        totpRepo.stopListening();
        calendarRepo.stopListening();
        productivityRepo.stopListening();
        
        // Clear session
        sessionManager.logoutUser();
        auth.signOut();
        
        // Redirect to login
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
```

---

## 📝 CHECKLIST TRIỂN KHAI

- [ ] **1. Sửa ProductivityRepository**
  - [ ] Thêm `currentUserId`, listener tracking
  - [ ] Implement `startListening()` với user change detection
  - [ ] Implement `stopListening()`
  - [ ] Implement `clearAllData()`

- [ ] **2. Sửa SettingsFragment**
  - [ ] Inject AccountRepository, CalendarRepository, ProductivityRepository
  - [ ] Gọi `.stopListening()` cho TẤT CẢ repositories khi logout

- [ ] **3. Sửa ChangePasswordActivity**
  - [ ] Thêm repository cleanup trong `logoutAndRedirect()`

- [ ] **4. Testing**
  - [ ] Test logout → login với user khác → verify NO old data
  - [ ] Test switch user nhanh → verify NO data leak
  - [ ] Test logout → check LiveData cleared

- [ ] **5. Documentation**
  - [ ] Update README với security best practices
  - [ ] Document logout flow

---

## 🎯 MỨC ĐỘ ƯU TIÊN

| Vấn đề | Mức độ | Priority | Thời gian fix |
|--------|--------|----------|---------------|
| Data leak khi logout | 🔴 CRITICAL | P0 | 2-4 giờ |
| ProductivityRepository thiếu stopListening | 🟡 MEDIUM | P1 | 1-2 giờ |
| Thiếu global logout handler | 🟢 LOW | P2 | 1 giờ |

---

## ✅ CÁC BIỆN PHÁP BẢO MẬT ĐÃ TỐT

1. ✅ Firestore Security Rules hoàn hảo
2. ✅ Path-based isolation (users/{userId}/...)
3. ✅ EncryptedSharedPreferences cho sensitive data
4. ✅ TOTP secrets được encrypt
5. ✅ CalendarRepository có ownership validation
6. ✅ Tất cả repositories set userOwnerId khi insert

---

## 📚 TÀI LIỆU THAM KHẢO

- [OWASP Mobile Top 10 - M2: Insecure Data Storage](https://owasp.org/www-project-mobile-top-10/)
- [Firebase Security Best Practices](https://firebase.google.com/docs/rules/basics)
- [Android Security Guidelines](https://developer.android.com/privacy-and-security/security-tips)

---

**Kết luận:** App có nền tảng bảo mật tốt nhưng cần FIX NGAY vấn đề data leak khi logout để tránh rủi ro nghiêm trọng về privacy.
