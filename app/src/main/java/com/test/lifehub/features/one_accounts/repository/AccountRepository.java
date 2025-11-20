package com.test.lifehub.features.one_accounts.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

// Đổi thành Singleton để dùng chung cho toàn app
@Singleton
public class AccountRepository {

    private static final String TAG = "AccountRepository";
    private static final String COLLECTION_NAME = "accounts";

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private final MutableLiveData<List<AccountEntry>> mAllAccounts = new MutableLiveData<>();

    @Inject
    public AccountRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        // Không khởi tạo collection ở đây để tránh lỗi null khi chưa đăng nhập
    }

    // Hàm helper để luôn lấy đúng collection của user hiện tại
    private CollectionReference getAccountCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users")
                    .document(user.getUid())
                    .collection(COLLECTION_NAME);
        }
        return null;
    }

    public void startListening() {
        CollectionReference ref = getAccountCollection();
        if (ref == null) return;

        ref.orderBy("serviceName", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<AccountEntry> accounts = snapshot.toObjects(AccountEntry.class);
                        // Map Document ID
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            accounts.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        mAllAccounts.setValue(accounts);
                    }
                });
    }

    public LiveData<List<AccountEntry>> getAllAccounts() {
        // Gọi startListening mỗi khi UI yêu cầu dữ liệu để đảm bảo realtime
        startListening();
        return mAllAccounts;
    }

    public void insert(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null) {
            account.userOwnerId = mAuth.getCurrentUser().getUid();
            ref.add(account);
        }
    }

    public void update(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null && account.documentId != null) {
            ref.document(account.documentId).set(account);
        }
    }

    public void delete(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null && account.documentId != null) {
            ref.document(account.documentId).delete();
        }
    }

    public LiveData<AccountEntry> getAccountById(String documentId) {
        MutableLiveData<AccountEntry> result = new MutableLiveData<>();
        CollectionReference ref = getAccountCollection();

        if (ref != null && documentId != null) {
            ref.document(documentId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            AccountEntry account = documentSnapshot.toObject(AccountEntry.class);
                            if (account != null) {
                                // Quan trọng: Gán lại ID từ snapshot vào object để sau này update đúng dòng
                                account.documentId = documentSnapshot.getId();
                                result.setValue(account);
                            }
                        } else {
                            result.setValue(null);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting account by id", e);
                        result.setValue(null);
                    });
        }
        return result;
    }
}