package com.test.lifehub.features.one_accounts.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AddEditAccountViewModel extends ViewModel {

    private final AccountRepository mRepository;
    private final EncryptionHelper mEncryptionHelper;

    @Inject
    public AddEditAccountViewModel(AccountRepository repository, EncryptionHelper encryptionHelper) {
        this.mRepository = repository;
        this.mEncryptionHelper = encryptionHelper;
    }

    public LiveData<AccountEntry> getAccountById(String documentId) {
        return mRepository.getAccountById(documentId);
    }

    public void insert(AccountEntry account) {
        // Mã hóa mật khẩu trước khi lưu
        if (account.password != null) {
            account.password = mEncryptionHelper.encrypt(account.password);
        }
        mRepository.insert(account);
    }

    public void update(AccountEntry account) {
        // Mã hóa mật khẩu trước khi lưu
        if (account.password != null) {
            account.password = mEncryptionHelper.encrypt(account.password);
        }
        mRepository.update(account);
    }

    public void delete(AccountEntry account) {
        mRepository.delete(account);
    }
}