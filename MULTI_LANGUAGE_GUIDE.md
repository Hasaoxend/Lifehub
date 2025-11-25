# 🌐 Tính năng Đa Ngôn Ngữ (Multi-Language Feature)

## 📋 Tổng quan

Ứng dụng Lifehub hiện hỗ trợ **2 ngôn ngữ chính**:
- 🇬🇧 **English** (Tiếng Anh)
- 🇻🇳 **Tiếng Việt**

### ✨ Tính năng chính

1. **Chọn ngôn ngữ lần đầu**: Màn hình chọn ngôn ngữ xuất hiện khi mở app lần đầu tiên
2. **Tự động phát hiện**: Tự động đề xuất ngôn ngữ dựa trên cài đặt hệ thống
3. **Thay đổi trong Settings**: Người dùng có thể đổi ngôn ngữ bất cứ lúc nào trong phần Cài đặt
4. **Áp dụng toàn bộ app**: Ngôn ngữ được áp dụng cho tất cả màn hình và thông báo

---

## 📁 Cấu trúc File

### 1. **LocaleHelper.java**
```
app/src/main/java/com/test/lifehub/core/util/LocaleHelper.java
```

**Chức năng**:
- Quản lý việc áp dụng ngôn ngữ cho Context
- Lưu/lấy ngôn ngữ đã chọn từ SessionManager
- Cung cấp tên hiển thị cho từng ngôn ngữ

**Các method chính**:
- `setLocale(Context, String)` - Áp dụng ngôn ngữ
- `saveLanguage(Context, String)` - Lưu ngôn ngữ đã chọn
- `getLanguage(Context)` - Lấy ngôn ngữ hiện tại
- `getLanguageDisplayName(String)` - Lấy tên hiển thị

### 2. **LanguageSelectionActivity.java**
```
app/src/main/java/com/test/lifehub/ui/LanguageSelectionActivity.java
```

**Chức năng**:
- Màn hình LAUNCHER đầu tiên của app
- Cho phép người dùng chọn ngôn ngữ lần đầu
- Tự động chọn ngôn ngữ dựa trên cài đặt hệ thống

**Flow**:
1. Kiểm tra `sessionManager.isFirstRun()`
2. Nếu đã chọn ngôn ngữ → chuyển đến IntroActivity
3. Nếu chưa → hiển thị màn hình chọn ngôn ngữ
4. Sau khi chọn → lưu và chuyển đến IntroActivity

### 3. **BaseActivity.java**
```
app/src/main/java/com/test/lifehub/core/base/BaseActivity.java
```

**Chức năng**:
- Activity cơ sở cho tất cả Activity khác
- Tự động áp dụng ngôn ngữ trong `attachBaseContext()`

**Cách sử dụng**:
```java
// Thay vì extends AppCompatActivity
public class MyActivity extends BaseActivity {
    // Ngôn ngữ tự động được áp dụng
}
```

### 4. **SessionManager.java** (Updated)
```
app/src/main/java/com/test/lifehub/core/util/SessionManager.java
```

**Method mới**:
- `setLanguage(String)` - Lưu mã ngôn ngữ
- `getLanguage()` - Lấy mã ngôn ngữ đã lưu

### 5. **LifeHubApp.java** (Updated)
```
app/src/main/java/com/test/lifehub/core/LifeHubApp.java
```

**Thay đổi**:
- Thêm `attachBaseContext()` để áp dụng ngôn ngữ toàn app
- Áp dụng ngôn ngữ trong `onCreate()`

### 6. **Tài nguyên đa ngôn ngữ**

#### English (mặc định)
```
app/src/main/res/values/strings.xml
```

#### Tiếng Việt
```
app/src/main/res/values-vi/strings.xml
```

---

## 🎨 Layout Files

### activity_language_selection.xml
```
app/src/main/res/layout/activity_language_selection.xml
```

**Thành phần**:
- Logo/Icon ứng dụng
- Tiêu đề song ngữ
- RadioGroup với 2 lựa chọn:
  - English
  - Tiếng Việt
- Nút "Continue / Tiếp tục"

### fragment_settings.xml (Updated)
Thêm nút "Ngôn ngữ / Language" vào phần Tài khoản

---

## 🔧 Cách hoạt động

### 1. **Khởi động app lần đầu**

```
LanguageSelectionActivity (LAUNCHER)
    ↓
Kiểm tra isFirstRun()
    ↓
[Lần đầu] → Hiển thị màn hình chọn ngôn ngữ
    ↓
Người dùng chọn English/Tiếng Việt
    ↓
Lưu vào SessionManager
    ↓
Áp dụng ngôn ngữ (LocaleHelper.setLocale)
    ↓
Chuyển đến IntroActivity
```

### 2. **Khởi động app lần sau**

```
LanguageSelectionActivity (LAUNCHER)
    ↓
Kiểm tra isFirstRun() → false
    ↓
Chuyển đến IntroActivity ngay lập tức
    ↓
IntroActivity kiểm tra đã login chưa
    ↓
[Đã login] → MainActivity
[Chưa login] → Hiển thị intro screens
```

### 3. **Thay đổi ngôn ngữ trong Settings**

```
SettingsFragment
    ↓
Bấm nút "Ngôn ngữ / Language"
    ↓
Hiển thị MaterialAlertDialog
    ↓
Chọn English hoặc Tiếng Việt
    ↓
Lưu vào SessionManager
    ↓
Áp dụng ngôn ngữ
    ↓
recreate() Activity → Làm mới UI
```

---

## 📝 String Resources

### Common strings (có trong cả 2 ngôn ngữ)

| Key | English | Tiếng Việt |
|-----|---------|------------|
| `app_name` | Lifehub | Lifehub |
| `continue_text` | Continue | Tiếp tục |
| `skip` | Skip | Bỏ qua |
| `language` | Language | Ngôn ngữ |
| `settings` | Settings | Cài đặt |
| `logout` | Logout | Đăng xuất |
| `save` | Save | Lưu |
| `cancel` | Cancel | Hủy |
| `delete` | Delete | Xóa |

### Intro screens

| Key | English | Tiếng Việt |
|-----|---------|------------|
| `intro_welcome_title` | Welcome to LifeHub | Chào mừng đến LifeHub |
| `intro_welcome_desc` | All-in-one life management app... | Ứng dụng quản lý tất cả trong một... |
| `intro_security_title` | Absolute Security | Bảo mật tuyệt đối |
| `intro_security_desc` | Your passwords are encrypted... | Mật khẩu của bạn được mã hóa... |

---

## 🚀 Hướng dẫn sử dụng

### Thêm ngôn ngữ mới (Ví dụ: Korean)

1. **Tạo thư mục tài nguyên**
   ```
   app/src/main/res/values-ko/
   ```

2. **Tạo file strings.xml**
   ```xml
   <resources>
       <string name="app_name">Lifehub</string>
       <string name="language">언어</string>
       <!-- Dịch tất cả string keys -->
   </resources>
   ```

3. **Thêm vào LocaleHelper.java**
   ```java
   public static final String LANGUAGE_KOREAN = "ko";
   
   public static String getLanguageDisplayName(String language) {
       switch (language) {
           case LANGUAGE_KOREAN:
               return "한국어";
           // ...
       }
   }
   ```

4. **Cập nhật UI chọn ngôn ngữ**
   - Thêm RadioButton vào `activity_language_selection.xml`
   - Thêm vào dialog trong `SettingsFragment.java`

### Thêm string mới

1. **Thêm vào values/strings.xml** (English)
   ```xml
   <string name="my_new_string">Hello World</string>
   ```

2. **Thêm vào values-vi/strings.xml** (Tiếng Việt)
   ```xml
   <string name="my_new_string">Xin chào thế giới</string>
   ```

3. **Sử dụng trong code**
   ```java
   // Java
   String text = getString(R.string.my_new_string);
   
   // XML
   android:text="@string/my_new_string"
   ```

---

## ⚙️ AndroidManifest.xml Changes

```xml
<!-- LanguageSelectionActivity là LAUNCHER -->
<activity
    android:name=".ui.LanguageSelectionActivity"
    android:exported="true"
    android:screenOrientation="portrait">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- IntroActivity không còn là LAUNCHER -->
<activity
    android:name=".ui.IntroActivity"
    android:exported="false"
    android:screenOrientation="portrait" />
```

---

## 🎯 Best Practices

### 1. **Luôn dùng String Resources**
❌ **Không nên**:
```java
textView.setText("Hello World");
```

✅ **Nên**:
```java
textView.setText(R.string.greeting);
```

### 2. **String Formatting**
```xml
<string name="welcome_message">Welcome, %1$s!</string>
```

```java
String message = getString(R.string.welcome_message, userName);
```

### 3. **Plurals**
```xml
<plurals name="numberOfTasks">
    <item quantity="one">%d task</item>
    <item quantity="other">%d tasks</item>
</plurals>
```

```java
String text = getResources().getQuantityString(R.plurals.numberOfTasks, count, count);
```

### 4. **Extends BaseActivity**
Tất cả Activity mới nên extends `BaseActivity` thay vì `AppCompatActivity`:

```java
public class MyNewActivity extends BaseActivity {
    // Ngôn ngữ tự động được áp dụng
}
```

---

## 🐛 Troubleshooting

### Ngôn ngữ không thay đổi sau khi chọn
**Nguyên nhân**: Activity không được refresh
**Giải pháp**: Gọi `activity.recreate()` sau khi thay đổi ngôn ngữ

### Một số text vẫn hiển thị tiếng Việt/Anh
**Nguyên nhân**: Hardcoded string trong code/layout
**Giải pháp**: Thay bằng `@string/resource_name`

### App bị crash khi khởi động
**Nguyên nhân**: Thiếu string resource trong values-vi/
**Giải pháp**: Đảm bảo tất cả string keys có trong cả 2 file

---

## 📊 Testing Checklist

- [ ] Màn hình chọn ngôn ngữ hiển thị lần đầu
- [ ] Tự động chọn ngôn ngữ theo hệ thống
- [ ] Ngôn ngữ được lưu sau khi chọn
- [ ] Không hiển thị màn hình chọn ngôn ngữ lần thứ 2
- [ ] Thay đổi ngôn ngữ trong Settings hoạt động
- [ ] Tất cả màn hình áp dụng ngôn ngữ đúng
- [ ] Calendar weekdays hiển thị đúng ngôn ngữ
- [ ] Intro screens hiển thị đúng ngôn ngữ
- [ ] Dialog/Toast hiển thị đúng ngôn ngữ

---

## 📈 Future Enhancements

1. **Thêm ngôn ngữ**:
   - [ ] Tiếng Trung (Chinese - zh)
   - [ ] Tiếng Hàn (Korean - ko)
   - [ ] Tiếng Nhật (Japanese - ja)

2. **RTL Support**: Hỗ trợ ngôn ngữ viết từ phải sang trái (Arabic, Hebrew)

3. **In-app language switching**: Không cần restart app

4. **Crowdsourcing translations**: Cho phép cộng đồng đóng góp bản dịch

---

## 🔗 References

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Supporting Different Languages](https://developer.android.com/training/basics/supporting-devices/languages)
- [Locale Class](https://developer.android.com/reference/java/util/Locale)

---

**Ngày tạo**: 25/11/2025  
**Phiên bản**: 1.0  
**Tác giả**: GitHub Copilot
