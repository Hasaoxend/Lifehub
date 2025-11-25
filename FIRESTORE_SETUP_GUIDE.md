# 🔥 Firestore Setup Guide - Lifehub App

## 📋 Cấu trúc Database

### Collection Structure

```
firestore
└── users (collection)
    └── {userId} (document)
        ├── accounts (subcollection) - Mật khẩu đã lưu
        │   └── {accountId}
        │       ├── serviceName: string
        │       ├── username: string
        │       ├── encryptedPassword: string (AES-256-GCM)
        │       ├── userOwnerId: string
        │       └── createdAt: timestamp
        │
        ├── totp_accounts (subcollection) - TOTP/2FA Authenticator
        │   └── {totpAccountId}
        │       ├── accountName: string (email/username)
        │       ├── issuer: string (Google, Facebook, GitHub...)
        │       ├── secretKey: string (AES-256-GCM encrypted)
        │       ├── userOwnerId: string
        │       ├── createdAt: timestamp
        │       └── updatedAt: timestamp
        │
        ├── notes (subcollection) - Ghi chú
        │   └── {noteId}
        │       ├── title: string
        │       ├── content: string
        │       ├── userOwnerId: string
        │       └── createdAt: timestamp
        │
        ├── tasks (subcollection) - Công việc/Shopping list
        │   └── {taskId}
        │       ├── title: string
        │       ├── completed: boolean
        │       ├── priority: number
        │       ├── userOwnerId: string
        │       └── createdAt: timestamp
        │
        ├── projects (subcollection) - Thư mục/Projects
        │   └── {projectId}
        │       ├── name: string
        │       ├── color: string
        │       ├── userOwnerId: string
        │       └── createdAt: timestamp
        │
        └── calendar_events (subcollection) - Lịch công việc
            └── {eventId}
                ├── title: string
                ├── description: string
                ├── startTime: timestamp
                ├── endTime: timestamp
                ├── userOwnerId: string
                └── createdAt: timestamp
```

---

## 🔐 Security Rules

### Firestore Rules (firestore.rules)

Copy nội dung sau và paste vào **Firestore Rules** trên Firebase Console:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
  
    match /users/{userId} {
      // ✅ Cho phép người dùng đọc, tạo, cập nhật, xóa
      // tài liệu của CHÍNH HỌ.
      allow read, write: if request.auth != null && request.auth.uid == userId;

      // Module 1: Tài khoản (Passwords)
      match /accounts/{accountId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Module 1: Authenticator (TOTP/2FA)
      match /totp_accounts/{totpAccountId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Module 2: Năng suất (Ghi chú)
      match /notes/{noteId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Module 2: Năng suất (Công việc/Mua sắm)
      match /tasks/{taskId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Module 2: Năng suất (Projects - Thư mục)
      match /projects/{projectId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      // Module 4: Calendar
      match /calendar_events/{eventId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
}
```

---

## 🚀 Hướng dẫn Setup

### Bước 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Click **Add project** hoặc chọn project hiện có
3. Nhập tên project: `Lifehub`
4. Enable Google Analytics (optional)
5. Click **Create project**

### Bước 2: Thêm Android App

1. Trong Firebase Console, click icon Android
2. Nhập **Android package name**: `com.test.lifehub`
3. Nhập **App nickname**: `Lifehub`
4. Nhập **SHA-1**: (Debug signing certificate)
   ```bash
   # Windows
   cd C:\Users\{YourUsername}\.android
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Download `google-services.json`
6. Copy file vào: `app/google-services.json`

### Bước 3: Enable Authentication

1. Firebase Console → **Authentication**
2. Click **Get started**
3. Tab **Sign-in method**
4. Enable **Email/Password**
5. Click **Save**

### Bước 4: Setup Firestore Database

1. Firebase Console → **Firestore Database**
2. Click **Create database**
3. Chọn **Start in production mode** (sẽ cập nhật rules sau)
4. Chọn location: `asia-southeast1` (Singapore)
5. Click **Enable**

### Bước 5: Update Firestore Rules

1. Firestore Database → Tab **Rules**
2. Copy rules từ trên
3. Paste vào editor
4. Click **Publish**

### Bước 6: Create Indexes (Optional - nếu cần)

Nếu app báo lỗi cần index, Firebase sẽ tự động tạo link. Click vào link đó để tạo index.

---

## 🔒 Mã hóa Dữ liệu

### Secret Keys được mã hóa

**TOTP Secret Keys** (`totp_accounts.secretKey`) và **Passwords** (`accounts.encryptedPassword`) được mã hóa bằng **AES-256-GCM** trước khi lưu lên Firestore.

#### Encryption Flow:

```
[Client Side]
Secret Key (plain) 
    → EncryptionHelper.encrypt()
    → AES-256-GCM
    → Encrypted Base64 String
    → Upload to Firestore

[Firestore]
secretKey: "aGVsbG8gd29ybGQ..." (encrypted)

[Client Side - When Reading]
Download from Firestore
    → EncryptedHelper.decrypt()
    → Original Secret Key (plain)
    → Generate TOTP code
```

#### Implementation:

```java
// TotpRepository.java - Line 115-125
try {
    String encryptedSecret = EncryptionHelper.encrypt(account.getSecretKey());
    account.setSecretKey(encryptedSecret);
} catch (Exception e) {
    Log.e(TAG, "Error encrypting secret key", e);
    if (listener != null) listener.onFailure("Encryption failed");
    return;
}
```

```java
// AuthenticatorActivity.java - observeAccounts()
try {
    // Giải mã secret key
    String decryptedSecret = EncryptionHelper.decrypt(account.getSecretKey());
    accounts.add(new TotpAccountItem(
        account.getDocumentId(),
        account.getAccountName(),
        account.getIssuer(),
        decryptedSecret
    ));
} catch (Exception e) {
    Log.e(TAG, "Error decrypting secret for account", e);
}
```

### Encryption Algorithm: AES-256-GCM

- **Algorithm**: AES (Advanced Encryption Standard)
- **Key Size**: 256 bits
- **Mode**: GCM (Galois/Counter Mode) - Authenticated encryption
- **Key Storage**: Android Keystore System
- **Master Key**: Generated using `MasterKey.Builder()`

---

## 📊 Indexes

Hiện tại app sử dụng các query đơn giản nên chưa cần composite indexes. Nếu sau này cần, Firebase sẽ báo lỗi kèm link tạo index.

### Current Queries:

```java
// TotpRepository.java
ref.orderBy("issuer", Query.Direction.ASCENDING)

// AccountRepository.java
ref.orderBy("serviceName", Query.Direction.ASCENDING)
```

---

## 🧪 Testing

### Test Firestore Connection:

1. Build và run app
2. Đăng ký tài khoản mới
3. Thêm TOTP account (Scan QR hoặc manual)
4. Kiểm tra Firebase Console → Firestore Database
5. Xem collection: `users/{userId}/totp_accounts`
6. Verify `secretKey` là encrypted string (không phải plain text)

### Security Test:

1. Tạo 2 user accounts: UserA và UserB
2. UserA thêm TOTP account
3. Logout UserA, login UserB
4. UserB **KHÔNG** thấy TOTP accounts của UserA ✅
5. UserB thêm TOTP account riêng
6. Login lại UserA, chỉ thấy accounts của mình ✅

---

## 🛠️ Troubleshooting

### Lỗi: "PERMISSION_DENIED"

**Nguyên nhân**: Firestore rules chưa đúng hoặc user chưa đăng nhập

**Giải pháp**:
1. Kiểm tra user đã login chưa (`FirebaseAuth.getCurrentUser()`)
2. Verify Firestore rules đã publish
3. Check userId trong rules khớp với `request.auth.uid`

### Lỗi: "Failed to decrypt"

**Nguyên nhân**: Master key bị thay đổi hoặc data corrupt

**Giải pháp**:
1. Clear app data
2. Login lại
3. Re-add TOTP accounts

### Lỗi: "Index not found"

**Nguyên nhân**: Query cần composite index

**Giải pháp**:
1. Click vào link trong error message
2. Firebase sẽ tự động tạo index
3. Đợi 1-2 phút cho index build xong

---

## 📈 Best Practices

### 1. Batch Operations

Khi cần xóa nhiều accounts:

```java
WriteBatch batch = mDb.batch();
for (String docId : accountIds) {
    batch.delete(ref.document(docId));
}
batch.commit();
```

### 2. Offline Persistence

Enable offline cache:

```java
// LifeHubApp.java or Dependency Module
FirebaseFirestore db = FirebaseFirestore.getInstance();
db.setPersistenceEnabled(true);
```

### 3. Backup Strategy

**Firestore tự động backup**, nhưng nên export định kỳ:
- Firebase Console → Firestore → Export data
- Lưu vào Cloud Storage

---

## 🔐 Security Checklist

- [x] Firestore rules chỉ cho phép user đọc/ghi data của mình
- [x] Secret keys được mã hóa AES-256-GCM
- [x] Master key lưu trong Android Keystore
- [x] Không hardcode API keys trong code
- [x] google-services.json trong .gitignore
- [x] ProGuard rules bảo vệ EncryptionHelper
- [x] Network traffic qua HTTPS (Firebase mặc định)

---

## 📚 References

- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Android Keystore System](https://developer.android.com/training/articles/keystore)
- [AES-GCM Encryption](https://en.wikipedia.org/wiki/Galois/Counter_Mode)
- [Firebase Best Practices](https://firebase.google.com/docs/firestore/best-practices)

---

**Ngày tạo**: 25/11/2025  
**Phiên bản**: 1.0  
**Tác giả**: GitHub Copilot
