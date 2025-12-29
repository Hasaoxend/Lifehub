# LifeHub Extension - Chrome Password Manager

Tiện ích mở rộng Chrome để quản lý mật khẩu và xác thực 2FA đồng bộ với LifeHub.

## Tính năng

- 🔐 **Quản lý mật khẩu**: Xem và tự động điền tài khoản
- 🔑 **Authenticator 2FA**: Xem mã TOTP xác thực
- ⚡ **Tự động điền**: Điền username/password vào form đăng nhập
- ☁️ **Đồng bộ**: Dữ liệu sync realtime với app Android

## Cài đặt

### 1. Tạo PNG Icons

Extension cần các file PNG icon. Tạo từ `icons/icon.svg`:

```bash
# Sử dụng ImageMagick hoặc tool online
# Tạo icon16.png, icon32.png, icon48.png, icon128.png
```

Hoặc tạo thủ công với kích thước:
- `icon16.png`: 16x16
- `icon32.png`: 32x32  
- `icon48.png`: 48x48
- `icon128.png`: 128x128

### 2. Cấu hình Firebase

Cập nhật Firebase config trong `popup/popup.js`:

```javascript
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};
```

### 3. Load Extension vào Chrome

1. Mở Chrome và vào `chrome://extensions/`
2. Bật **Developer mode** (góc phải trên)
3. Click **Load unpacked**
4. Chọn thư mục `extension/`

## Sử dụng

### Đăng nhập
1. Click icon LifeHub trên toolbar
2. Nhập email và mật khẩu tài khoản LifeHub
3. Đăng nhập thành công sẽ hiển thị danh sách tài khoản

### Tự động điền
1. Truy cập trang đăng nhập (VD: facebook.com)
2. Click icon LifeHub
3. Chọn tài khoản phù hợp
4. Click nút ➡️ để tự động điền

### Xem mã 2FA
1. Click tab "2FA" trong popup
2. Mã TOTP sẽ tự động cập nhật mỗi 30 giây
3. Click vào mã để sao chép

## Cấu trúc thư mục

```
extension/
├── manifest.json       # Extension manifest
├── background.js       # Service worker
├── content.js          # Content script (inject vào pages)
├── content.css         # Styles cho content script
├── popup/
│   ├── popup.html      # Popup UI
│   ├── popup.css       # Popup styles
│   └── popup.js        # Popup logic
└── icons/
    ├── icon.svg        # Source SVG
    ├── icon16.png      # 16x16 icon
    ├── icon32.png      # 32x32 icon
    ├── icon48.png      # 48x48 icon
    └── icon128.png     # 128x128 icon
```

## Lưu ý bảo mật

- Extension sử dụng Firebase Authentication
- Dữ liệu được sync qua Firestore với security rules
- Mật khẩu được mã hóa AES-256 trên app Android
- Extension hiện chưa decrypt password (sử dụng password từ Firestore trực tiếp)

## TODO

- [ ] Implement password decryption trên client
- [ ] Firefox extension support
- [ ] Password generator
- [ ] Import/Export
