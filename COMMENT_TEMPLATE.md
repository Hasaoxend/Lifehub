# COMMENT TEMPLATE - HƯỚNG DẪN VIẾT COMMENT CHO DỰ ÁN LIFEHUB

## MỤC ĐÍCH
File này chứa template comment chuẩn để developers dễ dàng:
- Hiểu code hiện tại
- Phát triển tính năng mới
- Maintain và debug

---

## 1. TEMPLATE CHO CLASS/INTERFACE

```java
/**
 * [Tên class] - [Mô tả ngắn gọn chức năng]
 * 
 * === CHỨC NĂNG CHÍNH ===
 * 1. [Chức năng 1]
 * 2. [Chức năng 2]
 * 3. [Chức năng 3]
 * 
 * === DEPENDENCY INJECTION ===
 * @Inject [Dependency 1]: [Mô tả]
 * @Inject [Dependency 2]: [Mô tả]
 * 
 * === LUỒNG HOẠT ĐỘNG ===
 * 1. [Bước 1]
 * 2. [Bước 2]
 * 3. [Bước 3]
 * 
 * === PHÁT TRIỂN TIẾP ===
 * TODO: [Tính năng cần thêm]
 * TODO: [Cải tiến cần làm]
 * 
 * @author [Tên dev]
 * @version [Version]
 * @since [Ngày tạo]
 */
@AndroidEntryPoint  // hoặc @HiltViewModel, @Singleton, ...
public class ExampleActivity extends AppCompatActivity {
    
    // ===== UI COMPONENTS =====
    // Nhóm các view components với mô tả ngắn
    private TextView tvTitle;        // Hiển thị tiêu đề
    private Button btnSubmit;        // Nút gửi form
    
    // ===== REPOSITORIES/VIEWMODELS =====
    @Inject
    ExampleRepository repository;   // Quản lý data từ Firestore
    
    // ===== STATE VARIABLES =====
    private String currentUserId;    // ID user hiện tại
    private boolean isLoading;       // Trạng thái loading
    
    // ... code tiếp
}
```

---

## 2. TEMPLATE CHO METHOD/FUNCTION

```java
/**
 * [Mô tả ngắn gọn function làm gì]
 * 
 * === CHỨC NĂNG ===
 * [Giải thích chi tiết chức năng]
 * 
 * === THAM SỐ ===
 * @param param1 [Mô tả tham số 1]
 * @param param2 [Mô tả tham số 2]
 * 
 * === GIÁ TRỊ TRẢ VỀ ===
 * @return [Mô tả giá trị trả về]
 * 
 * === LUỒNG XỬ LÝ ===
 * 1. [Bước 1: Validate input]
 * 2. [Bước 2: Xử lý logic]
 * 3. [Bước 3: Return kết quả]
 * 
 * === LƯU Ý ===
 * - [Lưu ý quan trọng khi sử dụng]
 * - [Edge cases cần chú ý]
 * 
 * === VÍ DỤ SỬ DỤNG ===
 * ```java
 * String result = exampleMethod("input", 123);
 * ```
 * 
 * @throws [Exception] [Khi nào throw exception]
 * @see [RelatedClass] [Link đến class liên quan]
 */
public String exampleMethod(String input, int number) {
    // === BƯỚC 1: VALIDATE INPUT ===
    if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("Input không được rỗng");
    }
    
    // === BƯỚC 2: XỬ LÝ LOGIC ===
    String result = processData(input, number);
    
    // === BƯỚC 3: RETURN KẾT QUẢ ===
    return result;
}
```

---

## 3. TEMPLATE CHO INLINE COMMENTS

### 3.1. Comment cho biến quan trọng
```java
// ===== CONFIGURATION =====
private static final String API_KEY = "abc123";  // OpenWeatherMap API key
private static final int TIMEOUT = 30000;        // Timeout 30 seconds
private static final int MAX_RETRY = 3;          // Retry tối đa 3 lần

// ===== FIRESTORE COLLECTIONS =====
private static final String COLLECTION_USERS = "users";      // Root collection
private static final String COLLECTION_ACCOUNTS = "accounts"; // Subcollection
```

### 3.2. Comment cho logic phức tạp
```java
// === XỬ LÝ EDGE CASE ===
// Trường hợp user vừa đăng ký (chưa có data)
// -> Tạo collection rỗng để tránh null
if (userData == null) {
    userData = createEmptyUserData();
}

// === WORKAROUND CHO BUG FIREBASE ===
// Firebase không support query với 2 điều kiện OR
// -> Phải query 2 lần rồi merge results
List<Task> highPriority = queryTasksByPriority(3);
List<Task> mediumPriority = queryTasksByPriority(2);
List<Task> mergedTasks = mergeLists(highPriority, mediumPriority);
```

### 3.3. Comment cho TODO/FIXME
```java
// TODO: Implement pagination khi list > 100 items
// TODO: Thêm cache để giảm API calls
// TODO: [Tên dev] - Optimize algorithm này (O(n²) -> O(n log n))

// FIXME: Bug khi user click nhanh 2 lần
// FIXME: Memory leak ở listener này - cần remove trong onDestroy()

// HACK: Workaround tạm thời cho bug Android 13
// HACK: Hard-code value này - cần refactor sau

// NOTE: Không được xóa dòng này - cần cho compatibility
// NOTE: Value này được sync với backend team
```

---

## 4. TEMPLATE CHO REPOSITORY

```java
/**
 * [Name]Repository - Quản lý data [entity] từ Firestore
 * 
 * === NHIỆM VỤ ===
 * 1. Realtime listener cho collection [entity]
 * 2. CRUD operations (Create, Read, Update, Delete)
 * 3. Query và filter data
 * 4. Transform Firestore documents → POJO
 * 
 * === FIRESTORE STRUCTURE ===
 * users/{userId}/[entity]/{entityId}
 *   ├─ field1: [type]
 *   ├─ field2: [type]
 *   └─ field3: [type]
 * 
 * === DEPENDENCIES ===
 * @Inject FirebaseFirestore: Firestore instance
 * @Inject FirebaseAuth: Lấy userId hiện tại
 * 
 * === LIFECYCLE ===
 * 1. startListening(): Bắt đầu realtime listener (gọi ở MainActivity)
 * 2. stopListening(): Dừng listener (gọi khi logout)
 * 
 * @see [Entity]Entry POJO model
 * @see [Entity]ViewModel ViewModel sử dụng repository này
 */
@Singleton
public class ExampleRepository {
    
    private static final String TAG = "ExampleRepository";
    private static final String COLLECTION_NAME = "examples";
    
    // ===== DEPENDENCIES =====
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    // ===== LIVEDATA =====
    private final MutableLiveData<List<ExampleEntry>> examplesLiveData = new MutableLiveData<>();
    
    // ===== LISTENERS =====
    private ListenerRegistration examplesListener;  // Giữ reference để remove sau
    
    @Inject
    public ExampleRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }
    
    /**
     * Bắt đầu lắng nghe thay đổi từ Firestore
     * 
     * === KHI NÀO GỌI ===
     * - Ngay sau khi user login thành công
     * - Trong MainActivity.onCreate()
     * 
     * === LUỒNG HOẠT ĐỘNG ===
     * 1. Lấy userId từ FirebaseAuth
     * 2. Tạo CollectionReference đến Firestore
     * 3. Attach SnapshotListener
     * 4. Khi có thay đổi:
     *    - Parse documents → List<Entry>
     *    - Update LiveData
     *    - UI tự động update (Observer pattern)
     * 
     * === LƯU Ý ===
     * - Listener sẽ tự động update khi:
     *   * Document mới được thêm
     *   * Document cũ được sửa
     *   * Document bị xóa
     * - Phải gọi stopListening() khi logout để tránh memory leak
     */
    public void startListening() {
        String userId = auth.getCurrentUser().getUid();
        
        // === ATTACH REALTIME LISTENER ===
        examplesListener = db.collection("users")
            .document(userId)
            .collection(COLLECTION_NAME)
            .addSnapshotListener((snapshots, error) -> {
                // === XỬ LÝ LỖI ===
                if (error != null) {
                    Log.e(TAG, "Listen failed", error);
                    return;
                }
                
                // === PARSE DOCUMENTS ===
                List<ExampleEntry> examples = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots) {
                    ExampleEntry entry = doc.toObject(ExampleEntry.class);
                    entry.documentId = doc.getId();  // Lưu documentId
                    examples.add(entry);
                }
                
                // === UPDATE LIVEDATA ===
                examplesLiveData.setValue(examples);
            });
    }
    
    /**
     * Dừng listener (gọi khi logout)
     */
    public void stopListening() {
        if (examplesListener != null) {
            examplesListener.remove();
        }
    }
    
    /**
     * Thêm document mới
     * 
     * @param entry Entry cần thêm
     */
    public void insertExample(ExampleEntry entry) {
        String userId = auth.getCurrentUser().getUid();
        
        // === GHI VÀO FIRESTORE ===
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NAME)
            .add(entry)  // Auto-generate document ID
            .addOnSuccessListener(ref -> Log.d(TAG, "Added: " + ref.getId()))
            .addOnFailureListener(e -> Log.e(TAG, "Error adding", e));
    }
    
    /**
     * Cập nhật document
     * 
     * @param entry Entry cần cập nhật (phải có documentId)
     */
    public void updateExample(ExampleEntry entry) {
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NAME)
            .document(entry.documentId)
            .set(entry)  // Overwrite document
            .addOnSuccessListener(v -> Log.d(TAG, "Updated"))
            .addOnFailureListener(e -> Log.e(TAG, "Error updating", e));
    }
    
    /**
     * Xóa document
     * 
     * @param entry Entry cần xóa (phải có documentId)
     */
    public void deleteExample(ExampleEntry entry) {
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("users")
            .document(userId)
            .collection(COLLECTION_NAME)
            .document(entry.documentId)
            .delete()
            .addOnSuccessListener(v -> Log.d(TAG, "Deleted"))
            .addOnFailureListener(e -> Log.e(TAG, "Error deleting", e));
    }
    
    /**
     * LiveData để UI observe
     */
    public LiveData<List<ExampleEntry>> getAllExamples() {
        return examplesLiveData;
    }
}
```

---

## 5. TEMPLATE CHO VIEWMODEL

```java
/**
 * [Name]ViewModel - ViewModel cho [Screen/Feature]
 * 
 * === NHIỆM VỤ ===
 * 1. Giữ data khi configuration change (rotate màn hình)
 * 2. Expose LiveData cho UI observe
 * 3. Delegate CRUD operations cho Repository
 * 4. Transform/Filter data nếu cần
 * 
 * === DEPENDENCIES ===
 * @Inject [Name]Repository: Repository quản lý data
 * 
 * === LIVEDATA EXPOSED ===
 * - allExamples: LiveData<List<Entry>> - Tất cả items
 * - filteredExamples: LiveData<List<Entry>> - Items sau khi filter
 * 
 * === CÁCH SỬ DỤNG ===
 * ```java
 * // Trong Fragment/Activity
 * viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);
 * viewModel.getAllExamples().observe(this, examples -> {
 *     adapter.setExamples(examples);
 * });
 * ```
 */
@HiltViewModel
public class ExampleViewModel extends ViewModel {
    
    private final ExampleRepository repository;
    private final LiveData<List<ExampleEntry>> allExamples;
    
    @Inject
    public ExampleViewModel(ExampleRepository repository) {
        this.repository = repository;
        this.allExamples = repository.getAllExamples();
    }
    
    // ===== GETTERS FOR UI =====
    public LiveData<List<ExampleEntry>> getAllExamples() {
        return allExamples;
    }
    
    // ===== CRUD OPERATIONS =====
    /**
     * Thêm item mới
     * @param entry Entry cần thêm
     */
    public void insert(ExampleEntry entry) {
        repository.insertExample(entry);
    }
    
    /**
     * Cập nhật item
     * @param entry Entry cần cập nhật
     */
    public void update(ExampleEntry entry) {
        repository.updateExample(entry);
    }
    
    /**
     * Xóa item
     * @param entry Entry cần xóa
     */
    public void delete(ExampleEntry entry) {
        repository.deleteExample(entry);
    }
}
```

---

## 6. TEMPLATE CHO DATA MODEL (POJO)

```java
/**
 * [Name]Entry - POJO cho [Entity]
 * 
 * === FIRESTORE MAPPING ===
 * Class này map trực tiếp với Firestore document:
 * users/{userId}/[collection]/{documentId}
 *   ├─ field1: [type]  → private [type] field1
 *   ├─ field2: [type]  → private [type] field2
 *   └─ field3: [type]  → private [type] field3
 * 
 * === ANNOTATIONS ===
 * @Exclude: Field không lưu vào Firestore (documentId)
 * @PropertyName: Map với tên field khác trong Firestore
 * @ServerTimestamp: Tự động set timestamp khi write
 * 
 * === GETTERS/SETTERS ===
 * - Firestore yêu cầu PHẢI có getters/setters public
 * - Constructor rỗng (no-arg) bắt buộc
 * 
 * @see ExampleRepository Repository sử dụng model này
 */
public class ExampleEntry {
    
    // ===== FIRESTORE DOCUMENT ID =====
    @Exclude
    public String documentId;  // Không lưu vào Firestore, chỉ dùng local
    
    // ===== FIELDS (PRIVATE) =====
    private String title;           // Tiêu đề
    private String description;     // Mô tả
    private Date createdDate;       // Ngày tạo
    private String userOwnerId;     // ID người tạo
    
    /**
     * Constructor rỗng - BẮT BUỘC cho Firestore
     */
    public ExampleEntry() {
        // Firestore cần constructor này để deserialize
    }
    
    /**
     * Constructor với tham số (optional)
     */
    public ExampleEntry(String title, String description) {
        this.title = title;
        this.description = description;
        this.createdDate = new Date();
    }
    
    // ===== GETTERS & SETTERS =====
    // Firestore dùng reflection để gọi getters/setters
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
    
    public String getUserOwnerId() { return userOwnerId; }
    public void setUserOwnerId(String userOwnerId) { this.userOwnerId = userOwnerId; }
    
    // ===== EXCLUDE FIELDS =====
    @Exclude
    public String getDocumentId() { return documentId; }
    
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}
```

---

## 7. TEMPLATE CHO ACTIVITY

```java
/**
 * [Name]Activity - Activity cho màn hình [Screen]
 * 
 * === CHỨC NĂNG ===
 * 1. [Chức năng 1]
 * 2. [Chức năng 2]
 * 
 * === UI COMPONENTS ===
 * - [View 1]: [Mô tả]
 * - [View 2]: [Mô tả]
 * 
 * === LIFECYCLE ===
 * onCreate() -> setupUI() -> loadData() -> observeData()
 * 
 * === INTENT EXTRAS ===
 * - KEY_USER_ID: String - ID của user (required)
 * - KEY_MODE: int - Chế độ VIEW=0, EDIT=1 (optional, default=0)
 * 
 * === CÁCH GỌI TỪ ACTIVITY KHÁC ===
 * ```java
 * Intent intent = new Intent(context, ExampleActivity.class);
 * intent.putExtra(ExampleActivity.KEY_USER_ID, userId);
 * startActivity(intent);
 * ```
 * 
 * @see ExampleViewModel ViewModel được sử dụng
 * @see ExampleAdapter Adapter cho RecyclerView
 */
@AndroidEntryPoint
public class ExampleActivity extends AppCompatActivity {
    
    // ===== INTENT KEYS =====
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_MODE = "mode";
    
    // ===== UI COMPONENTS =====
    private TextView tvTitle;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    
    // ===== VIEWMODEL =====
    private ExampleViewModel viewModel;
    
    // ===== ADAPTERS =====
    private ExampleAdapter adapter;
    
    /**
     * Khởi tạo Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_example);
        
        // === BƯỚC 1: ÁNH XẠ VIEWS ===
        findViews();
        
        // === BƯỚC 2: SETUP UI ===
        setupUI();
        
        // === BƯỚC 3: KHỞI TẠO VIEWMODEL ===
        viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);
        
        // === BƯỚC 4: OBSERVE LIVEDATA ===
        observeData();
    }
    
    /**
     * Ánh xạ views từ XML
     */
    private void findViews() {
        tvTitle = findViewById(R.id.tv_title);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    /**
     * Setup UI components
     */
    private void setupUI() {
        // Setup RecyclerView
        adapter = new ExampleAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * Observe LiveData từ ViewModel
     */
    private void observeData() {
        viewModel.getAllExamples().observe(this, examples -> {
            // Update UI khi data thay đổi
            adapter.setExamples(examples);
            progressBar.setVisibility(View.GONE);
        });
    }
}
```

---

## 8. TEMPLATE CHO FRAGMENT

```java
/**
 * [Name]Fragment - Fragment cho tab/screen [Screen]
 * 
 * === CHỨC NĂNG ===
 * 1. [Chức năng 1]
 * 2. [Chức năng 2]
 * 
 * === LIFECYCLE ===
 * onCreateView() -> Inflate layout
 * onViewCreated() -> Setup UI + ViewModel + Observers
 * onDestroyView() -> Clean up
 * 
 * === DEPENDENCIES ===
 * - [Name]ViewModel: Quản lý data
 * 
 * @see [Name]Adapter Adapter cho RecyclerView
 */
public class ExampleFragment extends Fragment {
    
    // ===== UI COMPONENTS =====
    private TextView tvTitle;
    private RecyclerView recyclerView;
    
    // ===== VIEWMODEL =====
    private ExampleViewModel viewModel;
    
    // ===== ADAPTERS =====
    private ExampleAdapter adapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout
        return inflater.inflate(R.layout.fragment_example, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // === BƯỚC 1: ÁNH XẠ VIEWS ===
        tvTitle = view.findViewById(R.id.tv_title);
        recyclerView = view.findViewById(R.id.recycler_view);
        
        // === BƯỚC 2: SETUP UI ===
        setupUI();
        
        // === BƯỚC 3: KHỞI TẠO VIEWMODEL ===
        viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);
        
        // === BƯỚC 4: OBSERVE DATA ===
        observeData();
    }
    
    private void setupUI() {
        adapter = new ExampleAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void observeData() {
        viewModel.getAllExamples().observe(getViewLifecycleOwner(), examples -> {
            adapter.setExamples(examples);
        });
    }
}
```

---

## 9. QUY TẮC CHUNG

### 9.1. Khi nào cần comment?
✅ **CẦN:**
- Javadoc cho public class/method
- Logic phức tạp khó hiểu
- Workarounds/hacks
- TODOs/FIXMEs
- Business logic quan trọng
- Edge cases đặc biệt

❌ **KHÔNG CẦN:**
- Code tự giải thích (self-documenting)
- Getter/setter đơn giản
- Comment dư thừa: `i++; // tăng i lên 1`

### 9.2. Ngôn ngữ
- ✅ Tiếng Việt: Dễ hiểu cho team Việt Nam
- ✅ Tiếng Anh: Thuật ngữ kỹ thuật, tên biến, method

### 9.3. Format
- Sử dụng `===` để phân section rõ ràng
- Indent đúng với code
- Comment trước code, không comment cuối dòng (trừ khi ngắn gọn)

### 9.4. Update comment
- ⚠️ Khi sửa code, PHẢI update comment tương ứng
- ⚠️ Comment lỗi thời nguy hiểm hơn không có comment

---

## 10. VÍ DỤ REAL CODE (WeatherActivity)

```java
/**
 * WeatherActivity - Hiển thị thông tin thời tiết
 * 
 * === CHỨC NĂNG ===
 * - Lấy dữ liệu thời tiết từ OpenWeatherMap API
 * - Hiển thị nhiệt độ, độ ẩm, tình trạng thời tiết
 * - Cho phép chọn thành phố từ danh sách 14 thành phố phổ biến VN
 * - Tìm kiếm thành phố khác qua API Geocoding
 * - Tự động lưu thành phố đã chọn vào SharedPreferences
 * 
 * === API ===
 * OpenWeatherMap API:
 * - Weather: https://api.openweathermap.org/data/2.5/weather
 * - Geocoding: https://api.openweathermap.org/geo/1.0/direct
 * 
 * === DEPENDENCIES ===
 * @Inject WeatherApiService: Retrofit service gọi API
 * @Inject PreferenceManager: Lưu/đọc thành phố đã chọn
 * 
 * === LUỒNG HOẠT ĐỘNG ===
 * 1. onCreate() -> Đọc thành phố đã lưu
 * 2. Nếu có -> fetchWeather(city)
 * 3. Nếu không -> Hiện prompt "Vui lòng chọn thành phố"
 * 4. User chọn thành phố -> fetchWeather() -> Lưu vào SharedPreferences
 * 5. Hiển thị thông tin thời tiết
 * 
 * @version 1.0.0
 * @since 2025-12-05
 */
@AndroidEntryPoint
public class WeatherActivity extends AppCompatActivity {
    
    // ===== API CONFIGURATION =====
    /**
     * OpenWeatherMap API Key
     * 
     * FREE TIER LIMITS:
     * - 60 calls/minute
     * - 1,000,000 calls/month
     * 
     * Nếu vượt quota, đăng ký key mới tại:
     * https://openweathermap.org/api
     */
    private static final String API_KEY = "REMOVED_API_KEY";
    
    // ===== LOG TAGS =====
    private static final String TAG_SEARCH = "WeatherSearchError";
    private static final String TAG_FETCH = "WeatherFetchError";
    
    // ... rest of code
}
```

---

**HẾT TEMPLATE**

*Developers: Vui lòng tuân thủ template này khi viết code mới!*
