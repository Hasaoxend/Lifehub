# KIỂM TRA CHUỖI HARDCODED - AUDIT REPORT

## ⚠️ TÓM TẮT
Phát hiện **NHIỀU** chuỗi hardcoded trong code Java và XML chưa sử dụng string resources, gây khó khăn cho việc đa ngôn ngữ.

---

## 📋 DANH SÁCH CHUỖI HARDCODED THEO FILE

### 1. **RegisterPasswordActivity.java** (Nhiều nhất - 20+ strings)

#### Errors:
- `"Mật khẩu không được chứa khoảng trắng"` (line 170, 323)
- `"Mật khẩu không khớp"` (line 292, 318)
- `"Lỗi: Không tìm thấy email"` (line 82)

#### Password Strength:
- `"Yếu"` (line 220)
- `"Trung bình"` (line 226)
- `"Khá"` (line 232)
- `"Mạnh"` (line 238)
- `"Chưa nhập"` (line 247)

#### Email Display:
- `"Email: " + mEmail` (line 94) - Nên dùng `getString(R.string.email_display_format, mEmail)`

#### Toasts:
- `"Mật khẩu chưa đủ mạnh"` (line 313)
- `"Vui lòng đợi..."` (line 450)
- `"Tạo tài khoản thất bại"` (line 338)

#### Error Messages:
- `"Lỗi không xác định"` (line 360)
- `"Email đã được sử dụng"` (line 363)
- `"Lỗi kết nối mạng"` (line 365)
- `"Mật khẩu quá yếu"` (line 367)

#### Dialog:
- `"Đăng ký Thành công!"` (line 409)
- `"Một email xác thực đã được gửi đến " + mEmail + "..."` (line 410)

---

### 2. **RegisterEmailActivity.java**

- `"Vui lòng nhập email"` (line 74)
- `"Email không hợp lệ"` (line 81)
- `"Email này đã được sử dụng"` (line 100)
- `"Lỗi kiểm tra email: "` (line 111)

---

### 3. **LoginActivity.java**

- `"Đăng nhập thất bại"` (line 100)
- `"Email không được để trống"` (line 116)
- `"Email không hợp lệ"` (line 120)
- `"Đã gửi link khôi phục, vui lòng kiểm tra email."` (line 124)
- `"Lỗi: " + e.getMessage()` (line 125)

---

### 4. **PermissionRequestActivity.java**

- `"Vui lòng bật quyền 'Alarms & reminders'"` (line 179)
- `"Tiếp tục"` (line 228) - **Đã có** `R.string.continue_text`
- `"Vui lòng cấp quyền bắt buộc"` (line 231)
- `"✓ Đã cấp"` (line 300)
- `"✗ Chưa cấp"` (line 304)

---

### 5. **MainActivity.java**

- `"Đã cấp quyền thông báo!"` (line 64)
- `"Bạn đã từ chối quyền thông báo. Tính năng nhắc nhở có thể không hoạt động."` (line 67)
- `"Cần cấp quyền Báo thức"` (line 142)
- `"Để tính năng Nhắc nhở hoạt động chính xác, LifeHub cần quyền \"Đặt báo thức và lời nhắc\"."` (line 143)

---

### 6. **LanguageSelectionActivity.java**

- `"Please select a language / Vui lòng chọn ngôn ngữ"` (line 62) - **Đã có** `R.string.please_select_language`

---

### 7. **CalculatorActivity.java**

- `"C"` (line 154, 164)
- `"AC"` (line 183, 231, 249, 272)
- `"Lỗi"` (line 227) - **Đã có** `R.string.error`
- `"Đã xóa lịch sử"` (line 349)

---

### 8. **WeatherActivity.java**

- `"Đang làm mới thời tiết..."` (line 119)
- `"Vui lòng chọn một thành phố trước"` (line 123)
- `"Lỗi tìm kiếm: " + t.getMessage()` (line 222)
- `"Không tìm thấy thời tiết cho: " + city` (line 251)
- `"Lỗi mạng: " + t.getMessage()` (line 263)

---

### 9. **TaskListActivity.java**

- `"Vui lòng xóa thư mục bằng menu 3 chấm"` (line 335)

---

### 10. **AddEditTaskDialog.java**

- `"Sửa Công việc"` (line 106)
- `"Thêm Đồ Mua sắm"` (line 130)
- `"Thêm Công việc Mới"` (line 132)
- `"Nhắc lúc: " + DateFormat.format("HH:mm, dd/MM", mReminderDate)` (line 165)
- `"Vui lòng chọn thời gian trong tương lai"` (line 192)
- `"Nội dung không được để trống"` (line 217)
- `"Đã thêm"` (line 237)
- `"Đã cập nhật"` (line 261)

---

### 11. **AddEditProjectDialog.java**

- `"Đổi tên Thư mục"` (line 99)
- `"Thư mục Mới"` (line 102)
- `"Tên thư mục không được để trống"` (line 121)
- `"Đã tạo thư mục"` (line 128)
- `"Đã cập nhật"` (line 133)

---

### 12. **AddEditNoteActivity.java**

- `"Nhắc lúc: " + DateFormat.format("HH:mm, dd/MM/yyyy", mReminderDate)` (line 114)
- `"Vui lòng chọn thời gian trong tương lai"` (line 144)
- `"Đã xóa Ghi chú"` (line 197)
- `"Vui lòng nhập Tiêu đề và Nội dung"` (line 208)
- `"Đã lưu Ghi chú"` (line 228)
- `"Đã cập nhật Ghi chú"` (line 255)

---

### 13. **SettingsFragment.java**

- `"Thiết bị không hỗ trợ hoặc chưa cài đặt vân tay"` (line 122)
- `"Đăng nhập bằng vân tay đã " + status` (line 220)
- `"Xác thực thất bại: " + msg` (line 237)
- `"Không thể thay đổi cài đặt: " + msg` (line 244)

---

### 14. **ChangePasswordActivity.java**

- `"Không tìm thấy thông tin người dùng"` (line 109)
- `"Xác thực thất bại. Vui lòng kiểm tra lại mật khẩu."` (line 126)
- `"Không tìm thấy email người dùng"` (line 134)
- `"Lỗi gửi email: "` (line 163)
- `"Vui lòng nhập mật khẩu hiện tại"` (line 103)
- `"Mật khẩu không chính xác"` (line 125)
- `"Vui lòng nhập mật khẩu mới"` (line 278)
- `"Mật khẩu phải có ít nhất 8 ký tự"` (line 281)
- `"Mật khẩu phải có ít nhất 1 chữ in hoa"` (line 284)
- `"Mật khẩu phải có ít nhất 1 chữ thường"` (line 287)
- `"Mật khẩu phải có ít nhất 1 chữ số"` (line 290)
- `"Mật khẩu phải có ít nhất 1 ký tự đặc biệt (@#$%^&+=!)"` (line 293)
- `"Mật khẩu mới không được trùng với mật khẩu cũ"` (line 296)
- `"Vui lòng xác nhận mật khẩu"` (line 301)
- `"Mật khẩu xác nhận không khớp"` (line 304)
- `"Lỗi cập nhật mật khẩu: "` (line 329)

---

### 15. **PermissionsSettingsActivity.java**

- `"Quyền này được quản lý tự động bởi hệ thống"` (line 138)
- `"Đã cấp quyền"` (line 193)
- `"Đã cấp"` (line 244)
- `"Chưa cấp"` (line 247)

---

### 16. **AddEditEventDialog.java** (Calendar)

- `"Sửa Sự kiện"` (line 93)
- `"Sự kiện Mới"` (line 96)
- `"Đã xóa sự kiện"` (line 170)
- `"Ngày kết thúc không được ở quá khứ"` (line 201)
- `"Ngày bắt đầu không được quá 5 năm trong tương lai"` (line 212)
- `"Thời gian kết thúc phải sau thời gian bắt đầu"` (line 221)
- `"Vui lòng nhập tiêu đề"` (line 253)
- `"Đã cập nhật sự kiện"` (line 304)
- `"Đã thêm sự kiện"` (line 307)

---

### 17. **WeekViewAdapter.java**

- `"+" + (data.events.size() - 3) + " sự kiện"` (line 82)

---

### 18. **PasswordGeneratorDialog.java**

- `"Độ dài: " + mLength` (line 93)
- `"Đã sao chép mật khẩu!"` (line 119) - **Đã có** `R.string.password_copied`

---

### 19. **AccountDetailActivity.java**

- `"••••••••"` (line 116, 154, 180) - Password masking
- `"Xác thực thất bại: " + errorMessage` (line 225)
- `"Đã sao chép " + label` (line 250)

---

### 20. **AddEditAccountActivity.java**

- `"Thiếu thông tin bắt buộc"` (line 256)
- `"Đã lưu (Bảo mật)"` (line 290)

---

### 21. **AccountsListFragment.java & AccountFragment.java**

- `"Đã xóa tài khoản"` (line 53) - **Đã có** `R.string.account_deleted`
- `"Code copied: " + code` (line 235)
- `"Account deleted"` (line 257)
- `"Failed to delete account"` (line 261)
- `"Error: Invalid document ID"` (line 287)
- `"Authenticator deleted"` (line 297)
- `"Failed to delete authenticator: " + error` (line 305)

---

### 22. **TotpAccountsFragment.java**

- `"Đã xóa tài khoản"` (line 288)
- `"Lỗi: " + error` (line 293)
- `"Đã sao chép mã: " + text` (line 307) - **Đã có** `R.string.code_copied`

---

### 23. **AuthenticatorActivity.java**

- `"Đã thêm tài khoản"` (line 218)
- `"Lỗi: " + error` (line 224)
- `"Đã sao chép mã: " + text` (line 239)

---

### 24. **QRScannerActivity.java**

- `"Lỗi khởi động camera: " + e.getMessage()` (line 107)
- `"Lỗi khi bind camera: " + e.getMessage()` (line 170)
- `"Cần quyền camera để quét QR code"` (line 196)

---

### 25. **AlarmHelper.java**

- `"Vui lòng cấp quyền Báo thức trong Cài đặt > Ứng dụng > LifeHub > Quyền"` (line 57)
- `"Không thể đặt báo thức do thiếu quyền"` (line 89)

---

## 🗂️ HARDCODED STRINGS TRONG XML LAYOUTS

### Layout Files với hardcoded text:

1. **dialog_day_events.xml**: `"Không có sự kiện"` (line 31)
2. **fragment_accounts_list.xml**: `"Chưa có tài khoản nào"` (line 40)
3. **fragment_productivity.xml**:
   - `"Năng suất"` (line 16)
   - `"Ghi chú"` (line 48)
   - `"Công việc (To-do)"` (line 84)
   - `"Danh sách Mua sắm"` (line 120)
   - `"Máy tính"` (line 159)
   - `"Thời tiết"` (line 198)
   - `"Lịch"` (line 237)

4. **item_custom_field.xml**:
   - `"Tên đề mục"` (line 32)
   - `"Nội dung"` (line 60)

5. **item_intro_slide.xml**:
   - `"Tiêu đề giới thiệu"` (line 21)
   - `"Mô tả ngắn về tính năng của ứng dụng LifeHub."` (line 31)

6. **tab_day_item.xml**:
   - `"Mon"` (line 14) - **ĐÃ FIX** bằng code Java
   - `"4"` (line 23) - Placeholder

7. **item_year_month.xml**:
   - `"December"` (line 21) - **ĐÃ FIX** bằng code Java

8. **item_permission_request.xml**: `"Bắt buộc"` (line 55)

9. **fragment_totp_accounts.xml**:
   - `"Chưa có tài khoản Authenticator"` (line 43)
   - `"Nhấn vào nút + để thêm tài khoản đầu tiên"` (line 52)

10. **item_city_result.xml**: `"Hanoi, VN"` (line 8) - Placeholder

11. **item_history.xml**: `"1 + 2 = 3"` (line 16) - Placeholder

---

## 📊 THỐNG KÊ

- **Tổng số file Java có hardcoded strings**: ~25 files
- **Tổng số file XML có hardcoded strings**: ~11 files
- **Ước tính số chuỗi cần thêm vào strings.xml**: **150-200 strings**

---

## ✅ ĐỀ XUẤT HÀNH ĐỘNG

### Ưu tiên cao (Critical):
1. **RegisterPasswordActivity.java** - 20+ strings
2. **ChangePasswordActivity.java** - 15+ strings
3. **AddEditEventDialog.java** - 10+ strings
4. **LoginActivity.java** - 8+ strings

### Ưu tiên trung bình:
5. **WeatherActivity.java**
6. **AddEditTaskDialog.java**
7. **AddEditNoteActivity.java**
8. **SettingsFragment.java**

### Ưu tiên thấp (Informational):
9. XML layouts (có thể để placeholder)
10. Error messages động (có thể format)

---

## 🔧 CÁCH SỬA

### Ví dụ 1: TextView.setText()
```java
// ❌ SAI
tvPasswordStrength.setText("Yếu");

// ✅ ĐÚNG
tvPasswordStrength.setText(R.string.password_strength_weak);
```

### Ví dụ 2: Toast.makeText()
```java
// ❌ SAI
Toast.makeText(this, "Đã lưu Ghi chú", Toast.LENGTH_SHORT).show();

// ✅ ĐÚNG
Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
```

### Ví dụ 3: TextInputLayout.setError()
```java
// ❌ SAI
layoutPassword.setError("Mật khẩu không được chứa khoảng trắng");

// ✅ ĐÚNG
layoutPassword.setError(getString(R.string.error_password_no_spaces));
```

### Ví dụ 4: Format strings với tham số
```java
// ❌ SAI
tvEmailDisplay.setText("Email: " + mEmail);

// ✅ ĐÚNG (đã có sẵn)
tvEmailDisplay.setText(getString(R.string.email_display_format, mEmail));
```

### Ví dụ 5: XML Layout
```xml
<!-- ❌ SAI -->
<TextView
    android:text="Không có sự kiện" />

<!-- ✅ ĐÚNG -->
<TextView
    android:text="@string/no_events" />
```

---

## 🎯 KẾT LUẬN

**Chương trình CHƯA SẴN SÀNG hoàn toàn cho đa ngôn ngữ** do còn nhiều chuỗi hardcoded.

### Tác động:
- ❌ Người dùng chọn English nhưng vẫn thấy tiếng Việt ở nhiều chỗ
- ❌ Không thể dịch sang ngôn ngữ khác dễ dàng
- ❌ Vi phạm best practice Android development

### Giải pháp:
Cần thêm **~150-200 string resources** vào `values/strings.xml` và `values-vi/strings.xml`, sau đó thay thế tất cả hardcoded strings bằng `R.string.*` hoặc `getString(R.string.*)`.

---

**Ngày tạo**: 2025-12-04  
**Tác giả**: GitHub Copilot Audit
