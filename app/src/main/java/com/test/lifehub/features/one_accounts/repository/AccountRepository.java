package com.test.lifehub.features.one_accounts.repository;

import android.app.Application; // <-- XÓA IMPORT NÀY
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.List;

import javax.inject.Inject; // <-- THÊM IMPORT NÀY
import javax.inject.Singleton; // <-- THÊM IMPORT NÀY

/**
 * Repository (Kho chứa) cho Module Tài khoản.
 * (Phiên bản đã refactor để dùng Hilt)
 */
@Singleton // <-- THÊM CHÚ THÍCH NÀY (Báo Hilt chỉ tạo 1 instance)
public class AccountRepository {

    private static final String TAG = "AccountRepository";
    private static final String COLLECTION_NAME = "accounts";

    // --- Dependencies (Được tiêm vào) ---
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private CollectionReference mAccountsCollection; // Trỏ đến /users/{UID}/accounts

    // LiveData để UI quan sát
    private final MutableLiveData<List<AccountEntry>> mAllAccounts = new MutableLiveData<>();

    /**
     * SỬA LẠI CONSTRUCTOR:
     * Dùng @Inject để Hilt tự động "tiêm" dependencies
     * (những thứ đã được định nghĩa trong AppModule).
     */
    @Inject
    public AccountRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth; // <-- SỬA LẠI
        this.mDb = db; // <-- SỬA LẠI

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Đây là chìa khóa: Trỏ CSDL vào đúng thư mục của người dùng
            // (Khớp với Luật Bảo mật: /users/{userId}/accounts)
            mAccountsCollection = mDb.collection("users")
                    .document(user.getUid())
                    .collection(COLLECTION_NAME);

            // Bắt đầu lắng nghe (listen) thay đổi từ Firestore
            listenForAccountChanges();
        }
        // Nếu user == null (đã đăng xuất), mAccountsCollection sẽ là null,
        // mAllAccounts sẽ là rỗng, điều này là chính xác.
    }

    /**
     * Lắng nghe các thay đổi TRONG THỜI GIAN THỰC từ Firestore.
     * Tự động cập nhật LiveData khi có gì đó thay đổi.
     */
    private void listenForAccountChanges() {
        if (mAccountsCollection == null) return;

        mAccountsCollection.orderBy("serviceName", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Lỗi khi lắng nghe Firestore", e);
                        return;
                    }

                    if (snapshot != null) {
                        // Chuyển đổi (convert) snapshot thành danh sách
                        List<AccountEntry> accounts = snapshot.toObjects(AccountEntry.class);

                        // Gán documentId (ID tài liệu) cho mỗi mục (Rất quan trọng cho Sửa/Xóa)
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            accounts.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }

                        // Đẩy danh sách mới vào LiveData, UI sẽ tự động cập nhật
                        mAllAccounts.setValue(accounts);
                    }
                });
    }

    /**
     * Lấy danh sách tất cả tài khoản (dưới dạng LiveData).
     */
    public LiveData<List<AccountEntry>> getAllAccounts() {
        return mAllAccounts;
    }

    /**
     * Lấy một tài khoản cụ thể bằng ID (documentId) của nó.
     */
    public LiveData<AccountEntry> getAccountById(String documentId) {
        MutableLiveData<AccountEntry> accountData = new MutableLiveData<>();
        if (mAccountsCollection == null) {
            return accountData; // Trả về rỗng
        }

        mAccountsCollection.document(documentId).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Lỗi khi lấy 1 tài khoản", e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                AccountEntry account = snapshot.toObject(AccountEntry.class);
                if (account != null) {
                    account.documentId = snapshot.getId(); // Gán ID
                    accountData.setValue(account);
                }
            } else {
                Log.d(TAG, "Không tìm thấy tài khoản (hoặc đã bị xóa)");
                accountData.setValue(null);
            }
        });
        return accountData;
    }

    /**
     * Thêm một tài khoản mới vào Firestore (Bất đồng bộ).
     */
    public void insert(AccountEntry account) {
        if (mAccountsCollection == null || mAuth.getCurrentUser() == null) return;

        // Đánh dấu "chủ sở hữu" (owner) (Rất quan trọng cho Luật Bảo mật)
        account.userOwnerId = mAuth.getCurrentUser().getUid();

        mAccountsCollection.add(account)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Đã thêm tài khoản: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi khi thêm tài khoản", e));
    }

    /**
     * Cập nhật một tài khoản đã tồn tại.
     * (Chúng ta dùng .set() để ghi đè toàn bộ POJO)
     */
    public void update(AccountEntry account) {
        if (mAccountsCollection == null || account.documentId == null) {
            Log.w(TAG, "Lỗi Update: Collection hoặc Document ID bị null");
            return;
        }

        // Ghi đè toàn bộ tài liệu bằng đối tượng mới
        mAccountsCollection.document(account.documentId).set(account)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã cập nhật tài khoản"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi khi cập nhật", e));
    }

    /**
     * Xóa một tài khoản.
     */
    public void delete(AccountEntry account) {
        if (mAccountsCollection == null || account.documentId == null) {
            Log.w(TAG, "Lỗi Delete: Collection hoặc Document ID bị null");
            return;
        }

        mAccountsCollection.document(account.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã xóa tài khoản"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi khi xóa", e));
    }
}