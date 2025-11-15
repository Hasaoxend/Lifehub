package com.test.lifehub.features.one_accounts.ui;

import android.app.Application; // <-- XÓA IMPORT NÀY

import androidx.annotation.NonNull; // <-- XÓA IMPORT NÀY
import androidx.lifecycle.AndroidViewModel; // <-- XÓA IMPORT NÀY
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel; // <-- THÊM IMPORT NÀY

import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import java.util.List;

import javax.inject.Inject; // <-- THÊM IMPORT NÀY

import dagger.hilt.android.lifecycle.HiltViewModel; // <-- THÊM IMPORT NÀY

/**
 * ViewModel cho AccountFragment.
 * (Phiên bản đã refactor để dùng Hilt)
 */
@HiltViewModel // <-- THÊM CHÚ THÍCH NÀY
public class AccountViewModel extends ViewModel { // <-- SỬA LẠI (bỏ AndroidViewModel)

    // Repository này sẽ được Hilt "tiêm" vào
    private final AccountRepository mRepository;

    // LiveData này được cập nhật TỰ ĐỘNG bởi SnapshotListener của Repository
    private final LiveData<List<AccountEntry>> mAllAccounts;

    /**
     * SỬA LẠI CONSTRUCTOR:
     * Dùng @Inject để Hilt "tiêm" AccountRepository.
     */
    @Inject
    public AccountViewModel(AccountRepository repository) {
        // super(application) BỊ XÓA
        this.mRepository = repository; // <-- SỬA LẠI

        // Lấy LiveData từ Repository
        mAllAccounts = mRepository.getAllAccounts();
    }

    /**
     * Cung cấp một phương thức "getter" công khai (public) để Giao diện (UI)
     * có thể "quan sát" (observe) dữ liệu.
     *
     * @return LiveData chứa danh sách tất cả tài khoản.
     */
    public LiveData<List<AccountEntry>> getAllAccounts() {
        return mAllAccounts;
    }
}