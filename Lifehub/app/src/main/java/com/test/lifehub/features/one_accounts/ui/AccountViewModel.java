package com.test.lifehub.features.one_accounts.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import java.util.List;

/**
 * ViewModel cho AccountFragment.
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 *
 * Nhiệm vụ:
 * 1. Khởi tạo AccountRepository (phiên bản Firestore).
 * 2. Cung cấp LiveData (danh sách tài khoản) cho Fragment.
 */
public class AccountViewModel extends AndroidViewModel {

    // Repository này là phiên bản GỌI FIRESTORE
    private final AccountRepository mRepository;

    // LiveData này được cập nhật TỰ ĐỘNG bởi SnapshotListener của Repository
    private final LiveData<List<AccountEntry>> mAllAccounts;

    public AccountViewModel(@NonNull Application application) {
        super(application);
        // Khởi tạo Repository (Không còn lỗi Room ở đây nữa)
        mRepository = new AccountRepository(application);

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