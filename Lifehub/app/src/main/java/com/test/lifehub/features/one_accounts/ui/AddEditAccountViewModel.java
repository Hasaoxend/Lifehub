package com.test.lifehub.features.one_accounts.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

/**
 * ViewModel cho AddEditAccountActivity.
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 *
 * Nhiệm vụ:
 * 1. Cung cấp các hàm Thêm/Sửa/Xóa (gọi đến Repository).
 * 2. Cung cấp LiveData cho MỘT tài khoản (để chỉnh sửa).
 */
public class AddEditAccountViewModel extends AndroidViewModel {

    private final AccountRepository mRepository;

    public AddEditAccountViewModel(@NonNull Application application) {
        super(application);
        mRepository = new AccountRepository(application);
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