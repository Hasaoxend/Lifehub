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

@Singleton
public class AccountRepository {

    private static final String TAG = "AccountRepository";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private final MutableLiveData<List<AccountEntry>> mAllAccounts = new MutableLiveData<>();
    
    // ✅ THÊM: Track listener để quản lý lifecycle
    private boolean isListening = false;
    private String currentUserId = null;
    private ListenerRegistration listenerRegistration = null;

    @Inject
    public AccountRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        
        // ✅ SỬA LỖI: Khởi tạo listener ngay trong constructor
        startListening();
    }

    private CollectionReference getAccountCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users").document(user.getUid()).collection("accounts");
        }
        return null;
    }

    /**
     * ✅ SỬA LỖI: Bắt đầu lắng nghe thay đổi từ Firestore
     * Tương tự TotpRepository - kiểm tra user thay đổi và reset listener
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
        
        listenerRegistration = ref.orderBy("serviceName", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "❌ Error listening to accounts", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<AccountEntry> accounts = snapshot.toObjects(AccountEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            accounts.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
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

    public LiveData<List<AccountEntry>> getAllAccounts() {
        Log.d(TAG, "getAllAccounts() called, isListening: " + isListening);
        return mAllAccounts;
    }

    // --- ĐÂY LÀ HÀM BẠN BỊ THIẾU TRƯỚC ĐÓ ---
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

    public void insert(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null) {
            account.userOwnerId = mAuth.getCurrentUser().getUid();
            ref.add(account);
        }
    }

    public void update(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null && account.documentId != null) ref.document(account.documentId).set(account);
    }

    public void delete(AccountEntry account) {
        CollectionReference ref = getAccountCollection();
        if (ref != null && account.documentId != null) ref.document(account.documentId).delete();
    }
}