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

@Singleton
public class AccountRepository {

    private static final String TAG = "AccountRepository";
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private final MutableLiveData<List<AccountEntry>> mAllAccounts = new MutableLiveData<>();

    @Inject
    public AccountRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
    }

    private CollectionReference getAccountCollection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            return mDb.collection("users").document(user.getUid()).collection("accounts");
        }
        return null;
    }

    public void startListening() {
        CollectionReference ref = getAccountCollection();
        if (ref == null) return;
        ref.orderBy("serviceName", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot != null) {
                        List<AccountEntry> accounts = snapshot.toObjects(AccountEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            accounts.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        mAllAccounts.setValue(accounts);
                    }
                });
    }

    public LiveData<List<AccountEntry>> getAllAccounts() {
        startListening();
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