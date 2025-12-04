package com.test.lifehub.features.authenticator.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.features.authenticator.data.TotpAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository quản lý dữ liệu TOTP/2FA accounts trên Firestore
 */
@Singleton
public class TotpRepository {

    private static final String TAG = "TotpRepository";
    private static final String COLLECTION_TOTP = "totp_accounts";
    
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private final EncryptionHelper encryptionHelper;
    private final MutableLiveData<List<TotpAccount>> mAllAccounts = new MutableLiveData<>();
    
    private boolean isListening = false; // Cờ để tránh listener trùng lặp
    private String currentUserId = null; // Track current user to detect changes
    private ListenerRegistration listenerRegistration = null; // Store listener to remove later

    @Inject
    public TotpRepository(FirebaseAuth auth, FirebaseFirestore db, EncryptionHelper encryptionHelper) {
        this.mAuth = auth;
        this.mDb = db;
        this.encryptionHelper = encryptionHelper;
        
        // Bắt đầu lắng nghe ngay khi Repository được tạo
        startListening();
    }

    private CollectionReference getTotpCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users").document(user.getUid()).collection(COLLECTION_TOTP);
        }
        return null;
    }

    /**
     * Bắt đầu lắng nghe thay đổi từ Firestore
     */
    public void startListening() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot listen to TOTP accounts");
            // Clear data when no user
            stopListening();
            mAllAccounts.setValue(new ArrayList<>());
            return;
        }
        
        String newUserId = currentUser.getUid();
        
        // If user changed, stop old listener and clear data
        if (currentUserId != null && !currentUserId.equals(newUserId)) {
            Log.d(TAG, "User changed from " + currentUserId + " to " + newUserId + ", stopping old listener");
            stopListening();
            mAllAccounts.setValue(new ArrayList<>()); // Clear old data
        }
        
        if (isListening && newUserId.equals(currentUserId)) {
            Log.d(TAG, "Already listening to Firestore for user: " + newUserId);
            return;
        }
        
        currentUserId = newUserId;
        
        Log.d(TAG, "========================================");
        Log.d(TAG, "Starting Firestore listener");
        Log.d(TAG, "User ID: " + currentUserId);
        Log.d(TAG, "Repository instance: " + this.hashCode());
        Log.d(TAG, "========================================");
        
        CollectionReference ref = getTotpCollection();
        if (ref == null) {
            Log.w(TAG, "CollectionReference is null");
            return;
        }
        
        Log.d(TAG, "Collection path: " + ref.getPath());
        isListening = true;
        
        // Query tất cả TOTP accounts (đã được cách ly bởi path users/{userId}/totp_accounts)
        // KHÔNG dùng whereEqualTo() để tránh vấn đề với dữ liệu cũ không có field userOwnerId
        // Store listener registration so we can remove it later
        listenerRegistration = ref.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Error listening to TOTP accounts: " + e.getMessage(), e);
                    isListening = false; // Reset flag khi lỗi
                    return;
                }
                
                if (snapshot != null) {
                    Log.d(TAG, "Snapshot received. Size: " + snapshot.size() + ", isEmpty: " + snapshot.isEmpty());
                    
                    if (snapshot.isEmpty()) {
                        Log.d(TAG, "No TOTP accounts found in Firestore");
                        mAllAccounts.setValue(new ArrayList<>());
                        return;
                    }
                    
                    List<TotpAccount> accounts = snapshot.toObjects(TotpAccount.class);
                    // Gán documentId cho mỗi account
                    for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                        accounts.get(i).setDocumentId(snapshot.getDocuments().get(i).getId());
                        Log.d(TAG, "Account " + i + ": " + accounts.get(i).getIssuer() + " / " + accounts.get(i).getAccountName());
                    }
                    
                    // ✅ Kiểm tra lại lần nữa để chắc chắn (defense in depth)
                    List<TotpAccount> filteredAccounts = new ArrayList<>();
                    for (TotpAccount account : accounts) {
                        if (currentUserId.equals(account.getUserOwnerId())) {
                            filteredAccounts.add(account);
                        } else {
                            Log.w(TAG, "⚠️ Filtered out TOTP account with wrong userOwnerId: " + account.getUserOwnerId());
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + filteredAccounts.size() + " TOTP accounts from Firestore (filtered from " + accounts.size() + ")");
                    mAllAccounts.setValue(filteredAccounts);
                } else {
                    Log.w(TAG, "Snapshot is null");
                }
            });
    }

    /**
     * Dừng lắng nghe Firestore
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
     * Lấy tất cả TOTP accounts
     * Listener đã được khởi động trong constructor
     */
    public LiveData<List<TotpAccount>> getAllAccounts() {
        Log.d(TAG, "getAllAccounts() called, isListening: " + isListening);
        return mAllAccounts;
    }

    /**
     * Lấy một TOTP account theo ID
     */
    public LiveData<TotpAccount> getAccountById(String documentId) {
        MutableLiveData<TotpAccount> result = new MutableLiveData<>();
        CollectionReference ref = getTotpCollection();
        
        if (ref != null && documentId != null) {
            ref.document(documentId).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    TotpAccount account = snapshot.toObject(TotpAccount.class);
                    if (account != null) {
                        account.setDocumentId(snapshot.getId());
                        result.setValue(account);
                    }
                }
            });
        }
        
        return result;
    }

    /**
     * Thêm TOTP account mới
     */
    public void insert(TotpAccount account, OnCompleteListener listener) {
        CollectionReference ref = getTotpCollection();
        if (ref == null) {
            if (listener != null) listener.onFailure("User not logged in");
            return;
        }

        // Mã hóa secret key trước khi lưu
        try {
            String encryptedSecret = encryptionHelper.encrypt(account.getSecretKey());
            account.setSecretKey(encryptedSecret);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting secret key", e);
            if (listener != null) listener.onFailure("Encryption failed");
            return;
        }

        // Set userOwnerId
        account.setUserOwnerId(mAuth.getCurrentUser().getUid());
        account.setCreatedAt(System.currentTimeMillis());
        account.setUpdatedAt(System.currentTimeMillis());

        ref.add(account)
            .addOnSuccessListener(docRef -> {
                Log.d(TAG, "TOTP account added: " + docRef.getId());
                if (listener != null) listener.onSuccess(docRef.getId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error adding TOTP account", e);
                if (listener != null) listener.onFailure(e.getMessage());
            });
    }

    /**
     * Cập nhật TOTP account
     */
    public void update(TotpAccount account, OnCompleteListener listener) {
        CollectionReference ref = getTotpCollection();
        if (ref == null || account.getDocumentId() == null) {
            if (listener != null) listener.onFailure("Invalid account or user");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("accountName", account.getAccountName());
        updates.put("issuer", account.getIssuer());
        updates.put("updatedAt", System.currentTimeMillis());
        // ✅ Đảm bảo userOwnerId không bị mất khi update
        updates.put("userOwnerId", mAuth.getCurrentUser().getUid());

        ref.document(account.getDocumentId())
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "TOTP account updated: " + account.getDocumentId());
                if (listener != null) listener.onSuccess(account.getDocumentId());
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating TOTP account", e);
                if (listener != null) listener.onFailure(e.getMessage());
            });
    }

    /**
     * Xóa TOTP account
     */
    public void delete(String documentId, OnCompleteListener listener) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot delete: User not logged in");
            if (listener != null) listener.onFailure("User not logged in");
            return;
        }
        
        CollectionReference ref = getTotpCollection();
        if (ref == null || documentId == null) {
            Log.e(TAG, "Cannot delete: Invalid ref or documentId");
            if (listener != null) listener.onFailure("Invalid account or user");
            return;
        }

        String deletePath = "users/" + currentUser.getUid() + "/totp_accounts/" + documentId;
        Log.d(TAG, "========================================");
        Log.d(TAG, "Attempting to delete TOTP account");
        Log.d(TAG, "User ID: " + currentUser.getUid());
        Log.d(TAG, "Document ID: " + documentId);
        Log.d(TAG, "Full path: " + deletePath);
        Log.d(TAG, "========================================");

        ref.document(documentId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "✓ TOTP account deleted successfully: " + documentId);
                if (listener != null) listener.onSuccess(documentId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "✗ Error deleting TOTP account: " + documentId, e);
                Log.e(TAG, "Error message: " + e.getMessage());
                if (listener != null) listener.onFailure(e.getMessage());
            });
    }

    /**
     * Interface callback cho các thao tác async
     */
    public interface OnCompleteListener {
        void onSuccess(String documentId);
        void onFailure(String error);
    }
}
