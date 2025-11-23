# LifeHub Authenticator - TOTP 2FA

## Tính năng mới: Authenticator (Xác thực 2 yếu tố)

Ứng dụng LifeHub đã được cải thiện với tính năng **Authenticator** giống như Google Authenticator, cho phép bạn:

### ✨ Tính năng chính

1. **Quản lý mã TOTP (Time-based One-Time Password)**
   - Tạo mã OTP 6 chữ số cập nhật mỗi 30 giây
   - Hiển thị thanh đếm ngược thời gian còn lại
   - Tự động làm mới mã khi hết hiệu lực

2. **Quét mã QR**
   - Sử dụng camera để quét mã QR từ các dịch vụ
   - Tự động phát hiện URI `otpauth://totp/`
   - Hỗ trợ ML Kit Barcode Scanning cho độ chính xác cao

3. **Nhập thủ công**
   - Nhập tên tài khoản, tên dịch vụ, và secret key
   - Hỗ trợ secret key dạng Base32 (chuẩn TOTP)

4. **Bảo mật**
   - Lưu trữ secret keys bằng EncryptedSharedPreferences
   - Sử dụng AES256-GCM encryption
   - Không gửi dữ liệu về server

### 📱 Cách sử dụng

#### Thêm tài khoản mới

**Phương pháp 1: Quét mã QR**
1. Mở LifeHub → Năng suất → Authenticator
2. Nhấn nút "+"
3. Chọn tab "Quét QR"
4. Cấp quyền camera (nếu cần)
5. Nhấn "Quét mã QR"
6. Đặt mã QR vào khung camera
7. Tài khoản sẽ tự động được thêm

**Phương pháp 2: Nhập thủ công**
1. Mở LifeHub → Năng suất → Authenticator
2. Nhấn nút "+"
3. Chọn tab "Nhập thủ công"
4. Nhập thông tin:
   - **Tên tài khoản**: Email hoặc username của bạn
   - **Tên dịch vụ**: VD: Google, Facebook, GitHub
   - **Secret Key**: Chuỗi Base32 từ dịch vụ
5. Nhấn "Thêm tài khoản"

#### Sử dụng mã OTP

1. Mở danh sách tài khoản trong Authenticator
2. Nhấn vào tài khoản để **sao chép mã** OTP
3. Dán mã vào trang đăng nhập của dịch vụ
4. Mã sẽ tự động làm mới sau 30 giây

#### Xóa tài khoản

1. Nhấn vào biểu tượng thùng rác bên cạnh tài khoản
2. Xác nhận xóa

### 🔧 Cài đặt và Build

#### Dependencies đã thêm

```kotlin
// TOTP & QR Scanner
implementation("com.google.zxing:core:3.5.3")
implementation("com.journeyapps:zxing-android-embedded:4.3.0")
implementation("commons-codec:commons-codec:1.16.0")
implementation("com.google.mlkit:barcode-scanning:17.2.0")
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")
```

#### Permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

#### Build Project

1. Sync Gradle files
2. Build → Clean Project
3. Build → Rebuild Project
4. Run app

### 📁 Cấu trúc code

```
app/src/main/java/com/test/lifehub/
├── core/util/
│   ├── TotpManager.java          # Quản lý TOTP logic
│   └── SessionManager.java       # Lưu trữ tài khoản (đã cập nhật)
├── features/authenticator/ui/
│   ├── AuthenticatorActivity.java     # Màn hình danh sách
│   ├── AddTotpAccountActivity.java    # Màn hình thêm tài khoản
│   ├── QRScannerActivity.java         # Màn hình quét QR
│   └── TotpAccountsAdapter.java       # Adapter cho RecyclerView
└── features/two_productivity/ui/
    └── ProductivityFragment.java      # Đã thêm nút Authenticator
```

### 🔐 Cách hoạt động của TOTP

TOTP (Time-based One-Time Password) hoạt động theo nguyên tắc:

1. **Secret Key**: Một chuỗi bí mật được chia sẻ giữa dịch vụ và ứng dụng
2. **Time Counter**: Unix timestamp hiện tại chia cho 30 (time step)
3. **HMAC-SHA1**: Tạo hash từ secret key và time counter
4. **Dynamic Truncation**: Lấy 6 chữ số từ hash

**Công thức:**
```
TOTP = HOTP(K, T)
T = (Current Unix Time - T0) / X
K = Secret Key
X = Time Step (30 seconds)
```

### 🧪 Test

Để test tính năng:

1. Mở [https://totp.danhersam.com/](https://totp.danhersam.com/)
2. Tạo một QR code test
3. Quét bằng ứng dụng
4. So sánh mã OTP hiển thị

### 📝 Lưu ý

- Mã OTP chỉ có hiệu lực trong 30 giây
- Secret key phải được lưu giữ an toàn
- Không chia sẻ mã QR hoặc secret key
- Khuyến nghị backup danh sách tài khoản

### 🐛 Debug

Nếu gặp lỗi build:
1. File → Invalidate Caches / Restart
2. Xóa folder `.gradle` và `build`
3. Sync Gradle lại

Nếu mã OTP không đúng:
1. Kiểm tra thời gian hệ thống
2. Đảm bảo secret key đúng định dạng Base32
3. Thử nhập lại tài khoản

### 📞 Hỗ trợ

Tạo issue trên GitHub hoặc liên hệ developer.

---

**Phát triển bởi:** LifeHub Team  
**Phiên bản:** 1.0 with Authenticator  
**Ngày cập nhật:** November 23, 2025
