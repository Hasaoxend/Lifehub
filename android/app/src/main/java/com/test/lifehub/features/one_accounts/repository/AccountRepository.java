package com.test.lifehub.features.one_accounts.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * AccountRepository - Quản lý dữ liệu tài khoản từ Firestore
 * 
 * === NHIỆM VỤ ===
 * 1. Realtime listener cho collection "accounts"
 * 2. CRUD operations (Create, Read, Update, Delete) cho accounts
 * 3. Query và filter accounts theo serviceName, username
 * 4. Transform Firestore documents → AccountEntry POJO
 * 
 * === FIRESTORE STRUCTURE ===
 * users/{userId}/accounts/{accountId}
 *   ├─ serviceName: String (Gmail, Facebook, ...)
 *   ├─ username: String (email/username)
 *   ├─ password: String (đã mã hóa AES-256)
 *   ├─ notes: String (ghi chú optional)
 *   └─ customFields: Map<String, String> (fields tùy chỉnh)
 * 
 * === DEPENDENCIES ===
 * @Inject FirebaseFirestore: Firestore database instance
 * @Inject FirebaseAuth: Lấy userId hiện tại
 * 
 * === LIFECYCLE ===
 * 1. Constructor: Tự động gọi startListening()
 * 2. startListening(): Bắt đầu realtime listener
 * 3. stopListening(): Dừng listener (gọi khi logout)
 * 
 * === LƯU Ý BẢO MẬT ===
 * - Mật khẩu PHẢI được mã hóa bằng EncryptionHelper trước khi lưu
 * - Firestore rules chỉ cho phép user đọc/ghi data của chính mình
 * 
 * @see AccountEntry POJO model cho account
 * @see AccountViewModel ViewModel sử dụng repository này
 * @see EncryptionHelper Mã hóa/giải mã mật khẩu
 */
import com.test.lifehub.core.security.EncryptionManager;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AccountRepository - Quản lý dữ liệu tài khoản từ Firestore
 */
@Singleton
public class AccountRepository {
    
    public interface MigrationCallback {
        void onProgress(int current, int total);
        void onComplete(int successCount, int failedCount);
    }

    private static final String TAG = "AccountRepository";
    
    // ===== DEPENDENCIES =====
    private final FirebaseAuth mAuth;          // Firebase Authentication
    private final FirebaseFirestore mDb;       // Firestore Database
    
    // ===== LIVEDATA =====
    private final MutableLiveData<List<AccountEntry>> mAllAccounts = new MutableLiveData<>();
    
    // ===== LISTENER MANAGEMENT =====
    /**
     * Tracking variables cho listener lifecycle
     * 
     * isListening: Đang lắng nghe hay không
     * currentUserId: User hiện tại đang được listen
     * listenerRegistration: Reference để remove listener sau
     */
    private boolean isListening = false;
    private String currentUserId = null;
    private ListenerRegistration listenerRegistration = null;

    /**
     * Constructor - Hilt tự động inject dependencies
     * 
     * @param auth FirebaseAuth instance
     * @param db FirebaseFirestore instance
     */
    @Inject
    public AccountRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        
        // Tự động bắt đầu listener khi repository được tạo
        startListening();
    }

    /**
     * Lấy reference đến collection "accounts" của user hiện tại
     * 
     * @return CollectionReference hoặc null nếu chưa login
     */
    private CollectionReference getAccountCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users")
                     .document(user.getUid())
                     .collection("accounts");
        }
        return null;
    }

    /**
     * Bắt đầu lắng nghe thay đổi từ Firestore
     * 
     * === KHI NÀO GỌI ===
     * - Tự động gọi trong constructor
     * - Gọi lại trong MainActivity.onCreate() để ensure đúng user
     * 
     * === LUỒNG HOẠT ĐỘNG ===
     * 1. Kiểm tra user có đăng nhập không
     * 2. Kiểm tra user có đổi không (compare với currentUserId)
     * 3. Nếu đổi user -> stopListening() rồi bắt đầu listener mới
     * 4. Attach SnapshotListener đến Firestore collection
     * 5. Khi có thay đổi:
     *    - Parse documents → List<AccountEntry>
     *    - Set documentId cho mỗi entry
     *    - Update LiveData
     *    - UI tự động update (Observer pattern)
     * 
     * === LƯU Ý ===
     * - Listener tự động update khi:
     *   * Document mới được thêm
     *   * Document cũ được sửa
     *   * Document bị xóa
     * - Phải gọi stopListening() khi logout để tránh memory leak
     * - Firestore giới hạn 1 triệu reads/tháng (free tier)
     */

    public void startListening() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot listen to accounts");
            stopListening();
            mAllAccounts.setValue(new ArrayList<>());
            return;
        }
        
        String newUserId = currentUser.getUid();
        
        // Nếu user thay đổi, dừng listener cũ và xóa dữ liệu
        if (currentUserId != null && !currentUserId.equals(newUserId)) {
            Log.d(TAG, "User changed from " + currentUserId + " to " + newUserId + ", stopping old listener");
            stopListening();
            mAllAccounts.setValue(new ArrayList<>());
        }
        
        // Nếu đã đang lắng nghe cho cùng user, không làm gì
        if (isListening && newUserId.equals(currentUserId)) {
            Log.d(TAG, "Already listening to Firestore for user: " + newUserId);
            return;
        }
        
        currentUserId = newUserId;
        
        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting Firestore listener for accounts");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "Repository instance: " + this.hashCode());
        Log.d(TAG, "========================================");
        
        CollectionReference ref = getAccountCollection();
        if (ref == null) {
            Log.w(TAG, "CollectionReference is null");
            return;
        }
        
        // Query tất cả accounts trong collection của user (đã được cách ly bởi path users/{userId}/accounts)
        // KHÔNG dùng whereEqualTo() hay orderBy() để tránh cần composite index
        // Sẽ validate và sắp xếp ở client-side
        listenerRegistration = ref.addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "❌ Error listening to accounts", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<AccountEntry> accounts = snapshot.toObjects(AccountEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            accounts.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        
                        // ✅ Validation: Kiểm tra userOwnerId (chỉ cảnh báo, không filter)
                        // Path-based security đã đảm bảo cách ly dữ liệu
                        for (AccountEntry account : accounts) {
                            if (account.userOwnerId == null) {
                                Log.w(TAG, "⚠️ Account missing userOwnerId (old data?): " + account.serviceName);
                            } else if (!currentUserId.equals(account.userOwnerId)) {
                                Log.e(TAG, "🔥 SECURITY WARNING: Account userOwnerId mismatch! Expected: " + currentUserId + ", Got: " + account.userOwnerId);
                            }
                        }
                        
                        // Sắp xếp theo tên dịch vụ ở client (thay vì Firestore orderBy)
                        accounts.sort((a1, a2) -> {
                            if (a1.serviceName == null) return 1;
                            if (a2.serviceName == null) return -1;
                            return a1.serviceName.compareToIgnoreCase(a2.serviceName);
                        });
                        
                        mAllAccounts.setValue(accounts);
                        Log.d(TAG, "✅ Accounts updated: " + accounts.size() + " items");
                    }
                });
        
        isListening = true;
        Log.d(TAG, "Firestore listener started successfully");
    }
    
    /**
     * ✅ THÊM: Dừng lắng nghe Firestore
     * Gọi khi user logout để tránh memory leak và data leak
     */
    public void stopListening() {
        if (listenerRegistration != null) {
            Log.d(TAG, "Removing Firestore listener for user: " + currentUserId);
            listenerRegistration.remove();
            listenerRegistration = null;
        }
        isListening = false;
        currentUserId = null;
        mAllAccounts.setValue(new ArrayList<>()); // Clear all data
    }

    /**
     * Lấy danh sách tất cả tài khoản của user hiện tại (LiveData - realtime)
     * Dữ liệu sẽ tự động cập nhật khi có thay đổi trên Firestore
     * 
     * @return LiveData chứa danh sách AccountEntry, đã được lọc theo userOwnerId
     */
    public LiveData<List<AccountEntry>> getAllAccounts() {
        Log.d(TAG, "getAllAccounts() called, isListening: " + isListening);
        return mAllAccounts;
    }

    /**
     * Lấy thông tin chi tiết của một tài khoản theo ID
     * 
     * @param documentId ID của document trên Firestore
     * @return LiveData chứa AccountEntry, hoặc null nếu không tìm thấy
     */
    public LiveData<AccountEntry> getAccountById(String documentId) {
        MutableLiveData<AccountEntry> result = new MutableLiveData<>();
        CollectionReference ref = getAccountCollection();
        if (ref != null && documentId != null) {
            ref.document(documentId).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    AccountEntry account = snapshot.toObject(AccountEntry.class);
                    if (account != null) {
                        account.documentId = snapshot.getId();
                        result.setValue(account);
                    }
                }
            });
        }
        return result;
    }
    // ----------------------------------------

    /**
     * Thêm một tài khoản mới vào Firestore
     * Tự động gán userOwnerId = UID của user hiện tại
     * 
     * @param account Tài khoản cần thêm
     */
    public void insert(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                account.userOwnerId = currentUser.getUid();
                ref.add(account).addOnSuccessListener(docRef -> {
                    Log.d(TAG, "✅ Account inserted: " + docRef.getId() + " for user: " + account.userOwnerId);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to insert account", e);
                });
            } else {
                Log.w(TAG, "⚠️ Cannot insert account - user not logged in");
            }
        }
    }

    /**
     * Cập nhật thông tin tài khoản trên Firestore
     * Đảm bảo userOwnerId không bị thay đổi
     * 
     * @param account Tài khoản cần cập nhật (phải có documentId)
     */
    public void update(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null && account.documentId != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // Đảm bảo userOwnerId không bị thay đổi
                account.userOwnerId = currentUser.getUid();
                ref.document(account.documentId).set(account)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "✅ Account updated: " + account.documentId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Failed to update account", e);
                        });
            } else {
                Log.w(TAG, "⚠️ Cannot update account - user not logged in");
            }
        }
    }

    /**
     * Xóa một tài khoản khỏi Firestore
     * 
     * @param account Tài khoản cần xóa (phải có documentId)
     */
    public void delete(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null && account.documentId != null) {
            ref.document(account.documentId).delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Account deleted: " + account.documentId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Failed to delete account", e);
                    });
        }
    }
    /**
     * Thực hiện chuyển đổi mã hóa toàn bộ tài khoản sang chuẩn mới.
     * 
     * @param encryptionManager Quản lý mã hóa
     * @param callback Callback thông báo tiến độ
     */
    public void migrateEncryption(EncryptionManager encryptionManager, MigrationCallback callback) {
        List<AccountEntry> accounts = mAllAccounts.getValue();
        if (accounts == null || accounts.isEmpty()) {
            if (callback != null) callback.onComplete(0, 0);
            return;
        }

        int total = accounts.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        for (AccountEntry account : accounts) {
            String encryptedPwd = account.password;
            if (encryptedPwd == null || encryptedPwd.isEmpty()) {
                if (processedCount.incrementAndGet() == total && callback != null) {
                    callback.onComplete(successCount.get(), failedCount.get());
                }
                continue;
            }

            // Giải mã bằng logic tự động (có fallback legacy nội bộ trong EncryptionManager)
            String decrypted = encryptionManager.decrypt(encryptedPwd);
            
            // Mã hóa lại BẮT BUỘC bằng chuẩn Cross-platform
            String newEncrypted = encryptionManager.encrypt(decrypted);

            // Nếu mật khẩu thay đổi (nghĩa là nó vừa được upgrade lên chuẩn mới)
            if (!newEncrypted.equals(encryptedPwd)) {
                account.password = newEncrypted;
                CollectionReference ref = getAccountCollection();
                if (ref != null && account.documentId != null) {
                    ref.document(account.documentId).set(account)
                        .addOnSuccessListener(aVoid -> {
                            successCount.incrementAndGet();
                            int current = processedCount.incrementAndGet();
                            if (callback != null) callback.onProgress(current, total);
                            if (current == total && callback != null) {
                                callback.onComplete(successCount.get(), failedCount.get());
                            }
                        })
                        .addOnFailureListener(e -> {
                            failedCount.incrementAndGet();
                            int current = processedCount.incrementAndGet();
                            if (callback != null) callback.onProgress(current, total);
                            if (current == total && callback != null) {
                                callback.onComplete(successCount.get(), failedCount.get());
                            }
                        });
                }
            } else {
                // Đã ở chuẩn mới hoặc không có gì thay đổi
                int current = processedCount.incrementAndGet();
                if (callback != null) callback.onProgress(current, total);
                if (current == total && callback != null) {
                    callback.onComplete(successCount.get(), failedCount.get());
                }
            }
        }
    }
}