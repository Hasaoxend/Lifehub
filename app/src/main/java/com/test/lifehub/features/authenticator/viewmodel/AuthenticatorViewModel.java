package com.test.lifehub.features.authenticator.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.features.authenticator.data.TotpAccount;
import com.test.lifehub.features.authenticator.repository.TotpRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel cho Authenticator
 * Quản lý dữ liệu TOTP accounts từ Firestore
 */
@HiltViewModel
public class AuthenticatorViewModel extends ViewModel {

    private static final String TAG = "AuthenticatorViewModel";
    
    private final TotpRepository repository;
    private final LiveData<List<TotpAccount>> allAccounts;

    @Inject
    public AuthenticatorViewModel(TotpRepository repository) {
        Log.d(TAG, "ViewModel created, instance: " + this.hashCode());
        this.repository = repository;
        // Khởi tạo ngay để nhận LiveData từ Repository
        this.allAccounts = repository.getAllAccounts();
        Log.d(TAG, "LiveData initialized from repository");
    }

    public LiveData<List<TotpAccount>> getAllAccounts() {
        Log.d(TAG, "getAllAccounts() called, returning LiveData instance: " + allAccounts.hashCode());
        return allAccounts;
    }

    public LiveData<TotpAccount> getAccountById(String documentId) {
        return repository.getAccountById(documentId);
    }

    public void insert(TotpAccount account, TotpRepository.OnCompleteListener listener) {
        repository.insert(account, listener);
    }

    public void update(TotpAccount account, TotpRepository.OnCompleteListener listener) {
        repository.update(account, listener);
    }

    public void delete(String documentId, TotpRepository.OnCompleteListener listener) {
        repository.delete(documentId, listener);
    }
}
