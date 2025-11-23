# Tính Năng Tài Khoản Thống Nhất (Unified Accounts)

## 📋 Tổng Quan

Tính năng này cho phép người dùng quản lý **cả tài khoản mật khẩu VÀ tài khoản TOTP (Authenticator)** trong cùng một giao diện, tương tự như **Microsoft Authenticator**.

### Điểm Nổi Bật

✅ **Giao diện thống nhất** - Hiển thị tất cả tài khoản trong 1 danh sách  
✅ **Icon thương hiệu** - 12 dịch vụ phổ biến có icon riêng (Google, Facebook, GitHub...)  
✅ **Letter Avatar** - Dịch vụ không xác định hiển thị chữ cái đầu với màu nhất quán  
✅ **TOTP tự động** - Mã xác thực 6 số tự động cập nhật mỗi giây  
✅ **Tìm kiếm nhanh** - SearchView hỗ trợ lọc theo tên dịch vụ và username  
✅ **Copy nhanh** - Nhấn vào mã TOTP để copy vào clipboard  
✅ **Bảo mật cao** - TOTP secrets được mã hóa AES256-GCM  

---

## 🏗️ Kiến Trúc

### 1. Cấu Trúc Thư Mục

```
app/src/main/java/com/test/lifehub/
├── features/
│   ├── one_accounts/
│   │   ├── data/
│   │   │   ├── UnifiedAccountItem.java          ← Model thống nhất
│   │   │   └── AccountEntry.java                ← Model tài khoản mật khẩu
│   │   ├── ui/
│   │   │   ├── AccountFragment.java             ← Fragment chính
│   │   │   ├── UnifiedAccountAdapter.java       ← Adapter RecyclerView
│   │   │   └── AccountViewModel.java            ← ViewModel cho password accounts
│   │   └── viewmodel/
│   │       └── UnifiedAccountViewModel.java     ← ViewModel thống nhất
│   └── authenticator/
│       ├── ui/
│       │   ├── QRScannerActivity.java           ← Quét QR code
│       │   └── AddTotpAccountActivity.java      ← Thêm TOTP thủ công
│       └── util/
│           └── TotpManager.java                 ← Tạo mã OTP
└── core/
    └── util/
        ├── ServiceIconHelper.java               ← Tạo icon dịch vụ
        └── SessionManager.java                  ← Lưu trữ TOTP (encrypted)
```

### 2. Luồng Dữ Liệu

```
┌─────────────────────┐
│  AccountFragment    │  ← UI Layer
└──────────┬──────────┘
           │
           ↓
┌─────────────────────┐
│ UnifiedAccountVM    │  ← ViewModel Layer (kết hợp 2 nguồn dữ liệu)
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     ↓           ↓
┌─────────┐  ┌──────────────┐
│Firebase │  │SessionManager│  ← Data Layer
│Firestore│  │(Encrypted SP)│
└─────────┘  └──────────────┘
     ↓              ↓
[Password]      [TOTP]
Accounts        Accounts
```

---

## 📦 Các Thành Phần Chính

### 1. UnifiedAccountItem.java

**Mô tả**: Model đại diện cho cả 2 loại tài khoản (PASSWORD và TOTP)

**Thuộc tính**:
- `AccountType type` - Loại tài khoản (PASSWORD hoặc TOTP)
- `String serviceName` - Tên dịch vụ (Google, Facebook, GitHub...)
- `String username` - Tên người dùng hoặc email
- `String password` - Mật khẩu (chỉ cho PASSWORD)
- `String secret` - Mã bí mật Base32 (chỉ cho TOTP)

**Constructor**:
```java
// Tài khoản mật khẩu
UnifiedAccountItem(String id, String serviceName, String username, 
                   String password, String notes, long timestamp)

// Tài khoản TOTP
UnifiedAccountItem(String serviceName, String username, 
                   String secret, String issuer)
```

---

### 2. ServiceIconHelper.java

**Mô tả**: Tạo icon cho các dịch vụ (branded icons hoặc letter avatars)

**Chức năng chính**:

#### `generateLetterAvatar(String serviceName, int size)`
Tạo avatar hình tròn với chữ cái đầu tiên của tên dịch vụ.

**Thuật toán**:
1. Lấy chữ cái đầu tiên, viết hoa
2. Chọn màu nền dựa trên hash của tên (nhất quán cho cùng tên)
3. Vẽ hình tròn màu nền
4. Vẽ chữ cái màu trắng ở giữa

**Ví dụ**:
- "GitHub" → Hình tròn đen, chữ "G" trắng
- "Unknown Service" → Hình tròn màu ngẫu nhiên, chữ "U" trắng

#### `getColorForService(String serviceName)`
Trả về màu cho dịch vụ.

**Logic**:
1. Kiểm tra trong `SERVICE_COLORS` (12 dịch vụ phổ biến)
2. Nếu không có, dùng hash % DEFAULT_COLORS.length
3. Cùng tên luôn cho cùng màu

**12 Dịch vụ Có Màu Riêng**:
- Google (#DB4437)
- Facebook (#1877F2)
- Microsoft (#00A4EF)
- GitHub (#24292E)
- Twitter (#1DA1F2)
- Amazon (#FF9900)
- Apple (#000000)
- LinkedIn (#0077B5)
- Instagram (#E4405F)
- Discord (#5865F2)
- Slack (#4A154B)
- Dropbox (#0061FF)

---

### 3. UnifiedAccountAdapter.java

**Mô tả**: RecyclerView Adapter hiển thị 2 loại item khác nhau

**View Types**:
- `VIEW_TYPE_PASSWORD = 0` - Item tài khoản mật khẩu
- `VIEW_TYPE_TOTP = 1` - Item tài khoản TOTP

**ViewHolder**:

#### PasswordViewHolder
Layout: `item_unified_account_password.xml`

Hiển thị:
- Icon dịch vụ (48dp, hình tròn)
- Tên dịch vụ
- Username
- Nút menu (3 chấm) để xóa

#### TotpViewHolder
Layout: `item_unified_account_totp.xml`

Hiển thị:
- Icon dịch vụ (48dp, hình tròn)
- Tên dịch vụ
- Username
- **Mã TOTP 6 số** (font monospace)
- **ProgressBar** hiển thị thời gian còn lại (30 giây)
- Nút menu để xóa

**Auto-update TOTP**:
```java
public void updateCodes() {
    long currentTime = System.currentTimeMillis() / 1000;
    int secondsRemaining = 30 - (int)(currentTime % 30);
    
    // Cập nhật mã OTP mới mỗi 30 giây
    // Cập nhật progress bar mỗi giây
}
```

---

### 4. UnifiedAccountViewModel.java

**Mô tả**: ViewModel kết hợp dữ liệu từ 2 nguồn

**LiveData**:
- `MediatorLiveData<List<UnifiedAccountItem>> unifiedAccountsLiveData`
  - Kết hợp password accounts từ Firebase
  - Và TOTP accounts từ SessionManager

**Phương thức chính**:

#### `combineAccounts(List<AccountEntry> passwordAccounts)`
```java
1. Tạo danh sách rỗng
2. Thêm tất cả password accounts (từ Firebase)
3. Thêm tất cả TOTP accounts (từ SessionManager)
4. Sắp xếp theo tên dịch vụ (A-Z)
5. Cập nhật LiveData
```

#### `getTotpAccountsFromSession()`
```java
1. Đọc JSON từ EncryptedSharedPreferences
2. Parse JSONArray
3. Tạo UnifiedAccountItem cho mỗi TOTP
4. Trả về List<UnifiedAccountItem>
```

#### `deletePasswordAccount(AccountEntry account)`
Xóa tài khoản mật khẩu khỏi Firebase

#### `deleteTotpAccount(String serviceName, String username)`
```java
1. Đọc JSON từ SessionManager
2. Lọc bỏ account cần xóa
3. Lưu lại JSON mới
4. Refresh danh sách
```

---

### 5. AccountFragment.java

**Mô tả**: Fragment chính hiển thị danh sách thống nhất

**Thành phần UI**:
- `RecyclerView` - Hiển thị danh sách
- `SearchView` - Tìm kiếm tài khoản
- `FloatingActionButton` - Nút thêm tài khoản
- `TextView` (empty state) - "No accounts yet"

**Luồng hoạt động**:

#### Khởi tạo
```java
1. Setup ViewModel (UnifiedAccountViewModel)
2. Setup RecyclerView với UnifiedAccountAdapter
3. Setup SearchView để lọc
4. Setup FAB để mở BottomSheet
5. Setup Handler để auto-update TOTP (mỗi 1 giây)
```

#### Thêm Tài Khoản (FAB Click)
```java
Hiển thị BottomSheet với 3 tùy chọn:
1. Add Password Account → AddEditAccountActivity
2. Scan QR Code → QRScannerActivity
3. Manual Entry → AddTotpAccountActivity
```

#### Click Vào Item
```java
- Password Account → Mở AccountDetailActivity
- TOTP Account → Copy mã OTP vào Clipboard + Toast
```

#### Xóa Tài Khoản
```java
1. Hiển thị MaterialAlertDialog xác nhận
2. Nếu OK:
   - Password: viewModel.deletePasswordAccount()
   - TOTP: viewModel.deleteTotpAccount()
3. Hiển thị Toast "Đã xóa"
```

#### Tìm Kiếm
```java
filterAccounts(String query) {
    1. Lọc danh sách theo serviceName và username
    2. Cập nhật adapter với danh sách đã lọc
    3. Cập nhật empty state
}
```

#### Auto-update TOTP
```java
Handler + Runnable chạy mỗi 1 giây:
1. Gọi adapter.updateCodes()
2. Cập nhật tất cả mã OTP và progress bars
3. Schedule lại sau 1 giây
```

---

## 🔐 Bảo Mật

### Lưu Trữ TOTP Secrets

**SessionManager** sử dụng **EncryptedSharedPreferences** với:
- **Mã hóa**: AES256-GCM
- **Key**: Android Keystore (hardware-backed)
- **Format**: JSON Array

```json
[
  {
    "serviceName": "Google",
    "username": "user@gmail.com",
    "secret": "JBSWY3DPEHPK3PXP",
    "issuer": "Google"
  }
]
```

### Quyền Cần Thiết

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />  
<!-- Để quét QR code -->

<uses-permission android:name="android.permission.INTERNET" />
<!-- Để đồng bộ với Firebase -->
```

---

## 🎨 Giao Diện

### Layout Files

#### fragment_accounts.xml
```xml
CoordinatorLayout
├── AppBarLayout
│   ├── MaterialToolbar (title: "Accounts")
│   └── SearchView (tìm kiếm)
├── FrameLayout
│   ├── RecyclerView (danh sách)
│   └── TextView (empty state)
└── FloatingActionButton (thêm account)
```

#### item_unified_account_password.xml
```xml
MaterialCardView
└── ConstraintLayout
    ├── ImageView (icon 48dp, circular)
    ├── TextView (service name, bold)
    ├── TextView (username, secondary color)
    └── ImageButton (menu 3 chấm)
```

#### item_unified_account_totp.xml
```xml
MaterialCardView
└── ConstraintLayout
    ├── ImageView (icon 48dp, circular)
    ├── TextView (service name, bold)
    ├── TextView (username, secondary color)
    ├── TextView (TOTP code, monospace, 24sp)
    ├── ProgressBar (horizontal, thời gian còn lại)
    └── ImageButton (menu 3 chấm)
```

#### bottom_sheet_add_account_unified.xml
```xml
LinearLayout (vertical)
├── TextView (title: "Add Account")
├── CardView (Add Password)
├── CardView (Scan QR Code)
└── CardView (Manual Entry)
```

---

## 📱 Luồng Sử Dụng

### 1. Thêm Tài Khoản Mật Khẩu
```
1. Nhấn FAB (+)
2. Chọn "Add Password"
3. Nhập: Service name, Username, Password, Notes
4. Lưu → Firebase Firestore
5. Tự động hiển thị trong danh sách
```

### 2. Thêm TOTP từ QR Code
```
1. Nhấn FAB (+)
2. Chọn "Scan QR Code"
3. Cho phép quyền Camera
4. Quét QR code (format: otpauth://totp/...)
5. Parse URL → Lấy secret, issuer, account
6. Lưu vào EncryptedSharedPreferences
7. Tự động hiển thị trong danh sách
```

### 3. Thêm TOTP Thủ Công
```
1. Nhấn FAB (+)
2. Chọn "Manual Entry"
3. Nhập: Service name, Username/Email, Secret key
4. Lưu vào EncryptedSharedPreferences
5. Tự động hiển thị trong danh sách
```

### 4. Sử Dụng Mã TOTP
```
1. Nhìn vào danh sách → thấy mã 6 số
2. Nhấn vào item TOTP
3. Mã được copy vào Clipboard
4. Toast: "Code copied: 123456"
5. Paste vào website cần xác thực
```

### 5. Xóa Tài Khoản
```
1. Nhấn nút menu (3 chấm) bên phải item
2. Dialog xác nhận: "Bạn có chắc muốn xóa?"
3. Nhấn "Delete"
4. Tài khoản bị xóa khỏi Firebase/SessionManager
5. Toast: "Account deleted"
```

---

## 🧪 TOTP - Cách Hoạt Động

### Thuật Toán TOTP (RFC 6238)

```java
TOTP = HOTP(Secret, Time)

Trong đó:
- Secret: Mã bí mật Base32
- Time: Unix timestamp / 30 (mỗi 30 giây đổi mã)
- HOTP: HMAC-based One-Time Password (RFC 4226)
```

### TotpManager.java

#### `generateCode(String secret)`
```java
1. Decode secret từ Base32 → byte[]
2. Tính time = Unix timestamp / 30
3. Tính HMAC-SHA1(secret, time)
4. Dynamic truncation → 31-bit number
5. Lấy 6 chữ số cuối
6. Pad leading zeros nếu cần
7. Trả về String 6 ký tự (VD: "042391")
```

#### Ví Dụ Cụ Thể
```
Secret: "JBSWY3DPEHPK3PXP"
Time: 1732492800 / 30 = 57749760

HMAC-SHA1(secret_bytes, time_bytes)
→ [0x1f, 0x86, 0x98, 0x69, 0x0e, ...]

Dynamic Truncation:
→ offset = last_byte & 0x0F = 5
→ extract 4 bytes starting at offset 5
→ convert to int = 123456789

Modulo 1,000,000:
→ 123456789 % 1000000 = 456789

Format:
→ "456789"
```

### Auto-Update Mechanism

```java
Handler handler = new Handler();
Runnable updateRunnable = new Runnable() {
    @Override
    public void run() {
        // 1. Tính thời gian hiện tại
        long currentTime = System.currentTimeMillis() / 1000;
        int secondsRemaining = 30 - (int)(currentTime % 30);
        
        // 2. Cập nhật progress bar
        progressBar.setProgress(secondsRemaining * 100 / 30);
        
        // 3. Nếu cần, tạo mã mới (khi secondsRemaining = 30)
        if (secondsRemaining == 30) {
            String newCode = totpManager.generateCode(secret);
            codeTextView.setText(newCode);
        }
        
        // 4. Schedule lại sau 1 giây
        handler.postDelayed(this, 1000);
    }
};
handler.post(updateRunnable);
```

---

## 🔄 Lifecycle Management

### Fragment Lifecycle

```java
onCreateView()
├── initViews()
├── setupViewModel()
├── setupRecyclerView()
├── setupSearchView()
├── setupFab()
└── setupTotpUpdater()

onResume()
└── Start TOTP auto-update (Handler.post)

onPause()
└── Stop TOTP auto-update (Handler.removeCallbacks)

onDestroyView()
└── Cleanup Handler
```

---

## 📊 Dependencies

### build.gradle.kts (app level)

```kotlin
dependencies {
    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Firebase
    implementation("com.google.firebase:firebase-firestore:24.10.0")
    
    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // ZXing (QR Code Scanning)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    
    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    
    // CameraX
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Apache Commons Codec (Base32)
    implementation("commons-codec:commons-codec:1.16.0")
    
    // Material Design 3
    implementation("com.google.android.material:material:1.11.0")
}
```

---

## 🐛 Troubleshooting

### Lỗi Thường Gặp

#### 1. "Cannot resolve symbol 'AccountEntry'"
**Nguyên nhân**: Import sai package

**Giải pháp**:
```java
// SAI
import com.test.lifehub.core.data.model.AccountEntry;

// ĐÚNG
import com.test.lifehub.features.one_accounts.data.AccountEntry;
```

#### 2. "Duplicate class: UnifiedAccountViewModel"
**Nguyên nhân**: Có 2 file cùng tên ở 2 package khác nhau

**Giải pháp**:
```bash
# Xóa file sai
Remove-Item "path/to/wrong/UnifiedAccountViewModel.java"
```

#### 3. TOTP Code không cập nhật
**Nguyên nhân**: Handler không được start

**Giải pháp**:
```java
@Override
public void onResume() {
    super.onResume();
    totpUpdateHandler.post(totpUpdateRunnable); // ← Đảm bảo dòng này có
}
```

#### 4. QR Scanner crash
**Nguyên nhân**: Thiếu quyền Camera

**Giải pháp**:
```java
// Kiểm tra quyền trước khi mở scanner
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(activity, 
        new String[]{Manifest.permission.CAMERA}, REQUEST_CODE);
}
```

---

## 📈 Performance

### Optimizations

1. **ViewHolder Pattern**: RecyclerView tái sử dụng views
2. **DiffUtil**: Chỉ cập nhật items thay đổi (nếu cần)
3. **Bitmap Caching**: Cache letter avatars để tránh tạo lại
4. **LiveData**: Tự động cập nhật UI khi data thay đổi
5. **Handler Throttling**: Chỉ update UI mỗi 1 giây (không phải mỗi frame)

### Memory Usage

- Letter Avatar: 120x120px ARGB_8888 = ~56KB mỗi icon
- Tối đa ~50 accounts = ~2.8MB cho icons
- JSON TOTP data: ~200 bytes/account

---

## 🚀 Future Enhancements

### Tính Năng Có Thể Thêm

1. **Backup/Restore**
   - Export tất cả accounts ra file mã hóa
   - Import từ file backup

2. **Biometric Lock**
   - Yêu cầu vân tay/khuôn mặt trước khi xem accounts

3. **Cloud Sync**
   - Đồng bộ TOTP accounts qua Firebase (encrypted)

4. **Password Generator**
   - Tạo mật khẩu mạnh tự động

5. **Breach Detection**
   - Kiểm tra mật khẩu có bị lộ không (HaveIBeenPwned API)

6. **Categories/Tags**
   - Phân loại accounts (Work, Personal, Banking...)

7. **Dark Mode**
   - Theme tối cho ban đêm

8. **Widgets**
   - Widget hiển thị TOTP codes trên home screen

---

## 👨‍💻 Tác Giả

- **Developer**: [Tên của bạn]
- **GitHub**: https://github.com/Hasaoxend/Lifehub
- **Ngày tạo**: 24/11/2025

---

## 📄 License

MIT License - Xem file LICENSE để biết thêm chi tiết

---

## 🙏 Credits

- **TOTP Algorithm**: RFC 6238
- **QR Code Library**: ZXing
- **Icons**: Material Design Icons
- **Inspiration**: Microsoft Authenticator, Google Authenticator
