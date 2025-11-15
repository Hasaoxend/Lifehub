package com.test.lifehub.features.one_accounts.ui;

import android.app.Application; // <-- XÓA IMPORT NÀY

import androidx.annotation.NonNull; // <-- XÓA IMPORT NÀY
import androidx.lifecycle.AndroidViewModel; // <-- XÓA IMPORT NÀY
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel; // <-- THÊM IMPORT NÀY

import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import javax.inject.Inject; // <-- THÊM IMPORT NÀY

import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM IMPORT NÀY

/**
 * ViewModel cho AddEditAccountActivity.
 * (Phiên bản đã refactor để dùng Hilt)
 */
@HiltViewModel // <-- THÊM CHÚ THÍCH NÀY
public class AddEditAccountViewModel extends ViewModel { // <-- SỬA LẠI (bỏ AndroidViewModel)

    // Repository này sẽ được Hilt "tiêm" vào
    private final AccountRepository mRepository;

    /**
     * SỬA LẠI CONSTRUCTOR:
     * Dùng @Inject để Hilt "tiêm" AccountRepository.
     */
    @Inject
    public AddEditAccountViewModel(AccountRepository repository) {
        // super(application) BỊ XÓA
        this.mRepository = repository; // <-- SỬA LẠI
        // Dòng "mRepository = new AccountRepository(application);" đã bị xóa
    }

    /**
     * Lấy một tài khoản duy nhất bằng ID (String) của nó.
     * Trả về LiveData để Giao diện (UI) có thể "quan sát".
     *
     * @param documentId ID tài liệu của Firestore
     * @return LiveData<AccountEntry>
     */
    public LiveData<AccountEntry> getAccountById(String documentId) {
        return mRepository.getAccountById(documentId);
    }

    /**
     * Thêm một tài khoản mới vào Firestore (đã chạy bất đồng bộ).
     * @param account Đối tượng AccountEntry (POJO) mới.
     */
    public void insert(AccountEntry account) {
        mRepository.insert(account);
    }

    /**
     * Cập nhật một tài khoản đã tồn tại (đã chạy bất đồng bộ).
     * @param account Đối tượng AccountEntry (POJO) đã được sửa đổi.
     */
    public void update(AccountEntry account) {
        mRepository.update(account);
    }

    /**
     * Xóa một tài khoản (đã chạy bất đồng bộ).
     * @param account Đối tượng AccountEntry (POJO) cần xóa.
     */
    public void delete(AccountEntry account) {
        mRepository.delete(account);
    }
}