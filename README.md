[Uploading GHI_CHU_GIAI_THICH.md…]()
# TÀI LIỆU GIẢI THÍCH DỰ ÁN LIFEHUB

## MỤC LỤC
1. [Tổng quan dự án](#1-tổng-quan-dự-án)
2. [Kiến trúc ứng dụng](#2-kiến-trúc-ứng-dụng)
3. [Các tính năng chính](#3-các-tính-năng-chính)
4. [Giải thích chi tiết các file quan trọng](#4-giải-thích-chi-tiết-các-file-quan-trọng)
5. [Luồng hoạt động](#5-luồng-hoạt-động)
6. [Các công nghệ sử dụng](#6-các-công-nghệ-sử-dụng)

---

## 1. TỔNG QUAN DỰ ÁN

**Lifehub** là ứng dụng Android quản lý cuộc sống cá nhân tích hợp nhiều chức năng:
- 📱 **Quản lý tài khoản**: Lưu trữ mật khẩu với mã hóa AES-256
- ✅ **Năng suất**: Quản lý ghi chú, công việc, dự án
- 📅 **Lịch**: Tạo sự kiện, nhắc nhở
- ⚙️ **Cài đặt**: Đổi mật khẩu, xác thực sinh trắc học, ngôn ngữ
- 🌤️ **Thời tiết**: Xem thông tin thời tiết theo thành phố

**Ngôn ngữ hỗ trợ**: Tiếng Anh và Tiếng Việt (i18n đầy đủ)

---

## 2. KIẾN TRÚC ỨNG DỤNG

### 2.1. Kiến trúc tổng thể: **MVVM + Repository Pattern**

```
┌─────────────────────────────────────────────────────┐
│                   UI Layer                          │
│  (Activities, Fragments, Adapters)                  │
│  - MainActivity, LoginActivity                      │
│  - AccountFragment, ProductivityFragment            │
│  - CalendarFragment, SettingsFragment               │
└────────────────┬────────────────────────────────────┘
                 │ LiveData / ViewModel
┌────────────────┴────────────────────────────────────┐
│                ViewModel Layer                      │
│  - AccountViewModel, ProductivityViewModel          │
│  - CalendarViewModel, LoginViewModel                │
└────────────────┬────────────────────────────────────┘
                 │ Repository
┌────────────────┴────────────────────────────────────┐
│              Repository Layer                       │
│  - AccountRepository, ProductivityRepository        │
│  - CalendarRepository, TotpRepository               │
└────────────────┬────────────────────────────────────┘
                 │ Firestore / Local
┌────────────────┴────────────────────────────────────┐
│               Data Sources                          │
│  - Firebase Firestore (Cloud Database)             │
│  - SharedPreferences (Local Settings)              │
│  - EncryptionHelper (Security)                     │
└─────────────────────────────────────────────────────┘
```

### 2.2. Dependency Injection: **Hilt/Dagger**

Tất cả các Repository, ViewModel, và Service được inject tự động:

```java
@AndroidEntryPoint  // Đánh dấu Activity/Fragment để nhận dependency
public class MainActivity extends AppCompatActivity {
    
    @Inject  // Hilt tự động cung cấp instance
    AccountRepository accountRepository;
}
```

---

## 3. CÁC TÍNH NĂNG CHÍNH

### 3.1. 🔐 HỆ THỐNG XÁC THỰC

#### **LoginActivity** (`ui/LoginActivity.java`)
**Chức năng:**
- Đăng nhập bằng Email/Password qua Firebase Auth
- Xác thực sinh trắc học (vân tay/Face ID)
- Kiểm tra mật khẩu yếu và nhắc đổi
- Quên mật khẩu qua email

**Flow đăng nhập:**
```
1. Mở app → LoginActivity
2. Nhập email + password
3. Firebase Auth verify
4. ✅ Success → MainActivity
   ❌ Fail → Hiển thị lỗi
```

**Xác thực sinh trắc học:**
```java
// BiometricHelper.java
public static void showBiometricPrompt(Activity activity, BiometricAuthListener listener) {
    // Sử dụng BiometricPrompt API từ AndroidX
    // Khi verify thành công → tự động đăng nhập
}
```

#### **RegisterEmailActivity** (`ui/RegisterEmailActivity.java`)
**Chức năng:**
- Đăng ký tài khoản mới với Firebase Auth
- Validate password mạnh (8+ ký tự, chữ hoa, số, ký tự đặc biệt)
- Tự động tạo collection cho user mới trên Firestore

---

### 3.2. 📱 QUẢN LÝ TÀI KHOẢN

#### **AccountFragment** (`features/one_accounts/ui/AccountFragment.java`)
**Hiển thị:**
- Danh sách tài khoản (Gmail, Facebook, Banking, ...)
- Mật khẩu được mã hóa AES-256
- Search + Filter theo tên
- Swipe to delete

**AccountViewModel** (`features/one_accounts/ui/AccountViewModel.java`)
```java
// Quan sát dữ liệu từ Firestore
LiveData<List<AccountEntry>> allAccounts = repository.getAllAccounts();

// CRUD operations
void insertAccount(AccountEntry account);  // Thêm mới
void updateAccount(AccountEntry account);  // Cập nhật
void deleteAccount(AccountEntry account);  // Xóa
```

#### **Mã hóa mật khẩu** (`core/security/EncryptionHelper.java`)
```java
// Mã hóa AES-256
String encryptedPassword = EncryptionHelper.encrypt(plainPassword, secretKey);

// Giải mã
String plainPassword = EncryptionHelper.decrypt(encryptedPassword, secretKey);

// Secret key được sinh từ Android Keystore (bảo mật cao)
```

**Firestore Structure:**
```
users/
  ├─ {userId}/
      ├─ accounts/
          ├─ {accountId}
              ├─ serviceName: "Gmail"
              ├─ username: "example@gmail.com"
              ├─ password: "AES_ENCRYPTED_STRING"
              ├─ notes: "Tài khoản chính"
              ├─ customFields: {...}
```

---

### 3.3. ✅ NĂNG SUẤT (PRODUCTIVITY)

#### **ProductivityFragment** (`features/two_productivity/ui/ProductivityFragment.java`)
**3 Tab chính:**
1. **Ghi chú (Notes)**: Tạo/sửa/xóa ghi chú
2. **Công việc (Tasks)**: Quản lý task, đánh dấu hoàn thành, nhắc nhở
3. **Dự án (Projects)**: Nhóm tasks theo project, hỗ trợ sub-project

#### **ProductivityViewModel**
```java
// LiveData cho UI observe
LiveData<List<NoteEntry>> getAllNotes();
LiveData<List<TaskEntry>> getTasksInRoot();  // Tasks ở root level
LiveData<List<ProjectEntry>> getProjectsInRoot();  // Projects ở root

// Filter theo projectId
void setCurrentProjectId(String projectId);
LiveData<List<TaskEntry>> getTasksInProject();  // Tasks trong project
```

#### **Data Models:**

**NoteEntry** (`features/two_productivity/data/NoteEntry.java`)
```java
public class NoteEntry {
    @Exclude
    public String documentId;  // Firestore document ID
    
    private String title;          // Tiêu đề ghi chú
    private String content;        // Nội dung
    private Date lastModified;     // Thời gian sửa cuối
    private String userOwnerId;    // ID người tạo
    private Date reminderTime;     // Thời gian nhắc (optional)
    
    // Getters/Setters...
}
```

**TaskEntry** (`features/two_productivity/data/TaskEntry.java`)
```java
public class TaskEntry {
    private String name;           // Tên task
    private Date lastModified;
    private boolean completed;     // Đã hoàn thành chưa
    private int taskType;          // 0=Task, 1=Shopping
    private Date reminderTime;     // Nhắc nhở
    private String projectId;      // Thuộc project nào (null = root)
    
    // Getters/Setters...
}
```

**ProjectEntry** (`features/two_productivity/data/ProjectEntry.java`)
```java
public class ProjectEntry {
    private String name;           // Tên project
    private String color;          // Màu sắc (hex)
    private Date createdDate;
    private String projectId;      // Parent project (null = root)
    
    // Getters/Setters...
}
```

#### **Nhắc nhở (Reminders)**
```java
// TaskReminderHelper.java
public static void scheduleReminder(Context context, TaskEntry task) {
    // Sử dụng AlarmManager để tạo reminder
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    
    // Tạo PendingIntent
    Intent intent = new Intent(context, ReminderReceiver.class);
    intent.putExtra("taskName", task.getName());
    
    // Schedule alarm tại reminderTime
    alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.getReminderTime().getTime(), pendingIntent);
}
```

---

### 3.4. 📅 LỊCH (CALENDAR)

#### **CalendarFragment** (`features/four_calendar/ui/CalendarFragment.java`)
**Chức năng:**
- Hiển thị lịch dạng tháng (Material CalendarView)
- Tạo/sửa/xóa sự kiện
- Nhắc nhở trước sự kiện
- Filter sự kiện theo khoảng thời gian

#### **CalendarViewModel**
```java
// Lấy tất cả events
LiveData<List<CalendarEvent>> getAllEvents();

// Filter theo khoảng thời gian
void setDateRange(Date startDate, Date endDate);
LiveData<List<CalendarEvent>> getEventsForRange();

// CRUD
void insertEvent(CalendarEvent event);
void updateEvent(CalendarEvent event);
void deleteEvent(CalendarEvent event);
```

**CalendarEvent** (`features/four_calendar/data/CalendarEvent.java`)
```java
public class CalendarEvent {
    private String title;          // Tiêu đề sự kiện
    private Date startTime;        // Thời gian bắt đầu
    private Date endTime;          // Thời gian kết thúc
    private String location;       // Địa điểm
    private String color;          // Màu sắc (hex)
    private String userOwnerId;
    
    // Getters/Setters...
}
```

---

### 3.5. 🌤️ THỜI TIẾT (WEATHER)

#### **WeatherActivity** (`features/two_productivity/ui/WeatherActivity.java`)

**API**: OpenWeatherMap API

**Chức năng:**
1. Hiển thị thời tiết theo thành phố
2. Danh sách 14 thành phố phổ biến VN sẵn có
3. Tự động lưu thành phố đã chọn
4. Làm mới dữ liệu (10-30 phút)

**Flow:**
```
1. Mở WeatherActivity
2. Đọc thành phố đã lưu từ SharedPreferences
3. Gọi API: GET /weather?q={city}&appid={key}&units=metric&lang=vi
4. Parse JSON response
5. Hiển thị:
   - Tên thành phố
   - Nhiệt độ (°C)
   - Tình trạng (Sunny, Rainy, ...)
   - Độ ẩm (%)
```

**API Service** (`features/two_productivity/data/WeatherApiService.java`)
```java
@GET("weather")
Call<WeatherResponse> getWeather(
    @Query("q") String cityName,      // "Hanoi"
    @Query("appid") String apiKey,    // API key
    @Query("units") String units,     // "metric" → °C
    @Query("lang") String language    // "vi" → tiếng Việt
);
```

**WeatherResponse** (JSON → POJO)
```json
{
  "name": "Hanoi",
  "main": {
    "temp": 25.5,
    "humidity": 75
  },
  "weather": [{
    "description": "mây rải rác"
  }]
}
```

**Danh sách thành phố:**
```java
List<GeoResult> popularCities = Arrays.asList(
    "Hanoi", "Ho Chi Minh City", "Da Nang", "Hai Phong",
    "Can Tho", "Bien Hoa", "Hue", "Nha Trang",
    "Buon Ma Thuot", "Quy Nhon", "Vung Tau", 
    "Thai Nguyen", "Nam Dinh", "Vinh"
);
```

---

### 3.6. ⚙️ CÀI ĐẶT (SETTINGS)

#### **SettingsFragment** (`features/three_settings/ui/SettingsFragment.java`)

**Các tùy chọn:**
1. **Đổi mật khẩu**: 2 bước xác thực
   - Bước 1: Nhập mật khẩu hiện tại (verify)
   - Bước 2: Nhập mật khẩu mới + xác nhận

2. **Sinh trắc học**: Bật/tắt đăng nhập vân tay

3. **Ngôn ngữ**: Chuyển đổi English ↔ Tiếng Việt
   - Sử dụng `LocaleHelper` để thay đổi locale
   - Restart app để áp dụng

4. **Quyền ứng dụng**: Xem và quản lý permissions

5. **Đăng xuất**: Clear session + về LoginActivity

---

## 4. GIẢI THÍCH CHI TIẾT CÁC FILE QUAN TRỌNG

### 4.1. MainActivity.java

**Vai trò**: Activity chính, điều phối navigation giữa các fragment

**Lifecycle:**
```java
onCreate() {
    // 1. Setup UI
    setContentView(R.layout.activity_main);
    
    // 2. Restart Firestore listeners
    // (Quan trọng: Đảm bảo các repository lắng nghe đúng user)
    totpRepository.startListening();
    accountRepository.startListening();
    calendarRepository.startListening();
    productivityRepository.startListening();
    
    // 3. Setup BottomNavigationView
    bottomNav.setOnItemSelectedListener(navListener);
    
    // 4. Hiển thị AccountFragment mặc định
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, new AccountFragment())
        .commit();
}
```

**Bottom Navigation Listener:**
```java
private NavigationBarView.OnItemSelectedListener navListener = item -> {
    Fragment selectedFragment = null;
    
    int itemId = item.getItemId();
    if (itemId == R.id.nav_account) {
        selectedFragment = new AccountFragment();  // Tab Tài khoản
    } else if (itemId == R.id.nav_productivity) {
        selectedFragment = new ProductivityFragment();  // Tab Năng suất
    } else if (itemId == R.id.nav_settings) {
        selectedFragment = new SettingsFragment();  // Tab Cài đặt
    }
    
    // Thay thế fragment
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragment_container, selectedFragment)
        .commit();
    
    return true;
};
```

---

### 4.2. Repository Pattern

**Ví dụ: AccountRepository.java**

**Nhiệm vụ:**
- Trung gian giữa ViewModel và Firestore
- Quản lý realtime listener
- CRUD operations

```java
@Singleton
public class AccountRepository {
    
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    private final MutableLiveData<List<AccountEntry>> accountsLiveData = new MutableLiveData<>();
    private ListenerRegistration accountsListener;
    
    @Inject
    public AccountRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }
    
    /**
     * Bắt đầu lắng nghe thay đổi từ Firestore
     * Được gọi khi user login thành công
     */
    public void startListening() {
        String userId = auth.getCurrentUser().getUid();
        
        // Realtime listener
        accountsListener = db.collection("users")
            .document(userId)
            .collection("accounts")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Log.e(TAG, "Listen failed", error);
                    return;
                }
                
                // Parse documents → AccountEntry
                List<AccountEntry> accounts = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots) {
                    AccountEntry account = doc.toObject(AccountEntry.class);
                    account.documentId = doc.getId();
                    accounts.add(account);
                }
                
                // Update LiveData → UI tự động update
                accountsLiveData.setValue(accounts);
            });
    }
    
    /**
     * Thêm account mới
     */
    public void insertAccount(AccountEntry account) {
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("users")
            .document(userId)
            .collection("accounts")
            .add(account)  // Auto-generate document ID
            .addOnSuccessListener(ref -> Log.d(TAG, "Added: " + ref.getId()))
            .addOnFailureListener(e -> Log.e(TAG, "Error adding", e));
    }
    
    /**
     * Cập nhật account
     */
    public void updateAccount(AccountEntry account) {
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("users")
            .document(userId)
            .collection("accounts")
            .document(account.documentId)
            .set(account)  // Overwrite
            .addOnSuccessListener(v -> Log.d(TAG, "Updated"))
            .addOnFailureListener(e -> Log.e(TAG, "Error updating", e));
    }
    
    /**
     * Xóa account
     */
    public void deleteAccount(AccountEntry account) {
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("users")
            .document(userId)
            .collection("accounts")
            .document(account.documentId)
            .delete()
            .addOnSuccessListener(v -> Log.d(TAG, "Deleted"))
            .addOnFailureListener(e -> Log.e(TAG, "Error deleting", e));
    }
    
    /**
     * LiveData để UI observe
     */
    public LiveData<List<AccountEntry>> getAllAccounts() {
        return accountsLiveData;
    }
    
    /**
     * Dừng listener (khi logout)
     */
    public void stopListening() {
        if (accountsListener != null) {
            accountsListener.remove();
        }
    }
}
```

---

### 4.3. ViewModel Pattern

**Ví dụ: AccountViewModel.java**

**Nhiệm vụ:**
- Giữ data khi configuration change (xoay màn hình)
- Expose LiveData cho UI
- Delegate CRUD operations cho Repository

```java
@HiltViewModel
public class AccountViewModel extends ViewModel {
    
    private final AccountRepository repository;
    private final LiveData<List<AccountEntry>> allAccounts;
    
    @Inject
    public AccountViewModel(AccountRepository repository) {
        this.repository = repository;
        this.allAccounts = repository.getAllAccounts();
    }
    
    // Expose LiveData cho UI
    public LiveData<List<AccountEntry>> getAllAccounts() {
        return allAccounts;
    }
    
    // CRUD operations (delegate to repository)
    public void insert(AccountEntry account) {
        repository.insertAccount(account);
    }
    
    public void update(AccountEntry account) {
        repository.updateAccount(account);
    }
    
    public void delete(AccountEntry account) {
        repository.deleteAccount(account);
    }
}
```

**Sử dụng trong Fragment:**
```java
public class AccountFragment extends Fragment {
    
    private AccountViewModel viewModel;
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        
        // Observe LiveData
        viewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            // Update UI khi data thay đổi
            adapter.setAccounts(accounts);
        });
        
        // Thêm account mới
        btnAdd.setOnClickListener(v -> {
            AccountEntry newAccount = new AccountEntry();
            newAccount.serviceName = "Gmail";
            newAccount.username = "example@gmail.com";
            viewModel.insert(newAccount);
        });
    }
}
```

---

## 5. LUỒNG HOẠT ĐỘNG

### 5.1. Luồng đăng nhập

```
┌─────────────────┐
│  LoginActivity  │
└────────┬────────┘
         │
         ├─ Nhập email/password
         │
         ├─ Firebase Auth verify
         │
         ├─ ✅ Success
         │   ├─ Lưu user session
         │   ├─ Check weak password
         │   │   ├─ Yếu → Dialog nhắc đổi
         │   │   └─ Mạnh → Continue
         │   └─ → MainActivity
         │
         └─ ❌ Fail
             └─ Hiển thị lỗi
```

### 5.2. Luồng CRUD Account

```
AccountFragment
    ↓
    [Observe LiveData]
    ↓
AccountViewModel.getAllAccounts()
    ↓
AccountRepository.getAllAccounts()
    ↓
Firestore Snapshot Listener
    ↓
    [Data changed]
    ↓
LiveData.setValue(newAccounts)
    ↓
Observer callback
    ↓
UI Update (RecyclerView)
```

### 5.3. Luồng tạo nhắc nhở

```
1. User tạo task với reminderTime
2. ProductivityViewModel.insert(task)
3. ProductivityRepository.insertTask()
4. Firestore: /users/{uid}/tasks/{taskId}
5. TaskReminderHelper.scheduleReminder(task)
6. AlarmManager set alarm at reminderTime
7. ⏰ Đến giờ → ReminderReceiver.onReceive()
8. Show notification
```

---

## 6. CÁC CÔNG NGHỆ SỬ DỤNG

### 6.1. Core Libraries

| Library | Version | Mục đích |
|---------|---------|----------|
| **Firebase Auth** | Latest | Xác thực người dùng |
| **Firebase Firestore** | Latest | Database realtime |
| **Hilt/Dagger** | 2.48 | Dependency Injection |
| **AndroidX Lifecycle** | 2.6.x | ViewModel, LiveData |
| **Material Design 3** | 1.10.x | UI Components |
| **Retrofit** | 2.9.0 | HTTP Client (Weather API) |
| **Gson** | 2.10.1 | JSON parsing |
| **BiometricPrompt** | AndroidX | Xác thực sinh trắc học |

### 6.2. Security

**Mã hóa mật khẩu:**
```java
// AES-256-GCM
String encryptedPassword = EncryptionHelper.encrypt(plainText, secretKey);

// Secret key từ Android Keystore
SecretKey key = KeyGenerator.getInstance("AES").generateKey();
KeyStore.getInstance("AndroidKeyStore").setEntry(...);
```

**Firebase Security Rules:**
```javascript
// firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      // Chỉ user đó mới đọc/ghi được data của mình
      allow read, write: if request.auth.uid == userId;
    }
  }
}
```

### 6.3. Internationalization (i18n)

**Cấu trúc:**
```
res/
  ├─ values/
  │   └─ strings.xml           (English - mặc định)
  ├─ values-vi/
      └─ strings.xml           (Tiếng Việt)
```

**Sử dụng:**
```xml
<!-- values/strings.xml -->
<string name="app_name">Lifehub</string>
<string name="login">Login</string>

<!-- values-vi/strings.xml -->
<string name="app_name">Lifehub</string>
<string name="login">Đăng nhập</string>
```

```java
// Trong code
String text = getString(R.string.login);  // Tự động chọn ngôn ngữ
```

---

## 7. CÁC FILE QUAN TRỌNG KHÁC

### 7.1. build.gradle.kts (Module: app)

**Dependencies chính:**
```kotlin
dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    
    // Material Design
    implementation("com.google.android.material:material:1.10.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
```

### 7.2. AndroidManifest.xml

**Permissions:**
```xml
<!-- Internet cho API calls -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Thông báo (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Schedule exact alarms cho reminders -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />

<!-- Biometric -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

**Activities:**
```xml
<application>
    <!-- Splash screen -->
    <activity android:name=".ui.SplashActivity"
              android:theme="@style/SplashTheme"
              android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    
    <!-- Main app -->
    <activity android:name=".ui.MainActivity" />
    <activity android:name=".ui.LoginActivity" />
    <activity android:name=".ui.RegisterEmailActivity" />
    
    <!-- BroadcastReceiver cho reminders -->
    <receiver android:name=".core.util.ReminderReceiver"
              android:exported="false" />
</application>
```

---

## 8. FIRESTORE DATABASE STRUCTURE

```
firestore/
├─ users/
│   ├─ {userId}/                     (Document per user)
│   │   ├─ accounts/                 (Subcollection)
│   │   │   ├─ {accountId}
│   │   │   │   ├─ serviceName: "Gmail"
│   │   │   │   ├─ username: "example@gmail.com"
│   │   │   │   ├─ password: "ENCRYPTED"
│   │   │   │   ├─ notes: "..."
│   │   │   │   └─ customFields: {...}
│   │   │
│   │   ├─ notes/                    (Subcollection)
│   │   │   ├─ {noteId}
│   │   │   │   ├─ title: "Meeting Notes"
│   │   │   │   ├─ content: "..."
│   │   │   │   ├─ lastModified: Timestamp
│   │   │   │   └─ reminderTime: Timestamp (nullable)
│   │   │
│   │   ├─ tasks/                    (Subcollection)
│   │   │   ├─ {taskId}
│   │   │   │   ├─ name: "Complete project"
│   │   │   │   ├─ completed: false
│   │   │   │   ├─ taskType: 0
│   │   │   │   ├─ reminderTime: Timestamp
│   │   │   │   └─ projectId: "projectId123"
│   │   │
│   │   ├─ projects/                 (Subcollection)
│   │   │   ├─ {projectId}
│   │   │   │   ├─ name: "Work Project"
│   │   │   │   ├─ color: "#FF5722"
│   │   │   │   ├─ createdDate: Timestamp
│   │   │   │   └─ projectId: null (root) hoặc "parentId"
│   │   │
│   │   ├─ events/                   (Subcollection)
│   │   │   ├─ {eventId}
│   │   │   │   ├─ title: "Team Meeting"
│   │   │   │   ├─ startTime: Timestamp
│   │   │   │   ├─ endTime: Timestamp
│   │   │   │   ├─ location: "Office"
│   │   │   │   └─ color: "#2196F3"
│   │   │
│   │   └─ totp_codes/               (Subcollection)
│   │       ├─ {totpId}
│   │       │   ├─ serviceName: "Google"
│   │       │   ├─ secretKey: "ENCRYPTED"
│   │       │   └─ issuer: "Google"
```

---

## 9. TESTING

### 9.1. Unit Tests

**Vị trí:** `app/src/test/java/`

**Các test files:**
- `SessionManagerTest.java`: Test SharedPreferences
- `EncryptionHelperTest.java`: Test mã hóa/giải mã
- `CalendarViewModelTest.java`: Test ViewModel logic
- `AccountViewModelTest.java`: Test CRUD operations
- `ProductivityViewModelTest.java`: Test filtering logic

**Chạy tests:**
```bash
.\gradlew test
```

### 9.2. Integration Tests

**Vị trí:** `app/src/androidTest/java/`

**IntegrationTest.java**: Test toàn bộ workflow

---

## 10. BUILD & DEPLOYMENT

### 10.1. Build Debug APK

```bash
# Windows
.\gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

### 10.2. Build Release APK (Signed)

```bash
.\gradlew assembleRelease

# Cần keystore để sign:
# - Tạo keystore: keytool -genkey -v -keystore lifehub.jks -alias lifehub -keyalg RSA -keysize 2048 -validity 10000
# - Config trong build.gradle.kts
```

### 10.3. Run on Device/Emulator

```bash
# Install APK
.\gradlew installDebug

# Hoặc dùng Android Studio:
# Run → Run 'app' (Shift+F10)
```

---

## 11. TROUBLESHOOTING

### 11.1. Lỗi "Firebase Auth not initialized"

**Nguyên nhân:** Thiếu `google-services.json`

**Giải pháp:**
1. Tải file từ Firebase Console
2. Copy vào `app/google-services.json`
3. Rebuild project

### 11.2. Lỗi "Hilt component not found"

**Nguyên nhân:** Chưa add `@AndroidEntryPoint` hoặc `@HiltViewModel`

**Giải pháp:**
```java
// Activity/Fragment
@AndroidEntryPoint
public class MyActivity extends AppCompatActivity { }

// ViewModel
@HiltViewModel
public class MyViewModel extends ViewModel {
    @Inject
    public MyViewModel(Repository repo) { }
}
```

### 11.3. Lỗi "WeatherActivity API key invalid"

**Nguyên nhân:** API key hết hạn hoặc vượt quota

**Giải pháp:**
1. Đăng ký key mới tại https://openweathermap.org/api
2. Thay thế trong `WeatherActivity.java`:
```java
private static final String API_KEY = "YOUR_NEW_KEY_HERE";
```

---

## 12. KẾT LUẬN

**Lifehub** là ứng dụng quản lý cuộc sống đầy đủ với:
- ✅ Kiến trúc MVVM chuẩn
- ✅ Dependency Injection (Hilt)
- ✅ Realtime database (Firestore)
- ✅ Bảo mật cao (AES-256, Biometric)
- ✅ Hỗ trợ đa ngôn ngữ
- ✅ Material Design 3

**Các tính năng nổi bật:**
1. Quản lý tài khoản với mã hóa
2. Năng suất (Notes, Tasks, Projects)
3. Lịch với nhắc nhở
4. Thời tiết realtime
5. Xác thực sinh trắc học

**Technologies:**
- Android SDK 29-36
- Kotlin DSL
- Firebase Suite
- Retrofit + OkHttp
- AndroidX Libraries

---

**Tác giả**: Lifehub Development Team  
**Phiên bản**: 1.0.0  
**Ngày cập nhật**: 5/12/2025
