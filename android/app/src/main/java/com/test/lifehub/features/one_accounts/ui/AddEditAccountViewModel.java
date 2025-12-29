package com.test.lifehub.features.one_accounts.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel cho Add/Edit Account
 * 
 * Sử dụng EncryptionManager để mã hóa mật khẩu trước khi lưu Firestore.
 * EncryptionManager hỗ trợ cả legacy (Android Keystore) và cross-platform (PBKDF2).
 */
@HiltViewModel
public class AddEditAccountViewModel extends ViewModel {

    private final AccountRepository mRepository;
    private final EncryptionManager mEncryptionManager;

    @Inject
    public AddEditAccountViewModel(AccountRepository repository, EncryptionManager encryptionManager) {
        this.mRepository = repository;
        this.mEncryptionManager = encryptionManager;
    }

    public LiveData<AccountEntry> getAccountById(String documentId) {
        return mRepository.getAccountById(documentId);
    }

    public void insert(AccountEntry account) {
        // Mã hóa mật khẩu trước khi lưu (dùng cross-platform encryption)
        if (account.password != null) {
            account.password = mEncryptionManager.encrypt(account.password);
        }
        mRepository.insert(account);
    }

    public void update(AccountEntry account) {
        // Mã hóa mật khẩu trước khi lưu (dùng cross-platform encryption)
        if (account.password != null) {
            account.password = mEncryptionManager.encrypt(account.password);
        }
        mRepository.update(account);
    }

    public void delete(AccountEntry account) {
        mRepository.delete(account);
    }
}