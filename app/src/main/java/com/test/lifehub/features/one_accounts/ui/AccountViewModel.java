package com.test.lifehub.features.one_accounts.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel cho AccountFragment.
 * (Phiên bản đã refactor để dùng Hilt)
 */
@HiltViewModel
public class AccountViewModel extends ViewModel {

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
        this.mRepository = repository;
        // Lấy LiveData từ Repository
        mAllAccounts = mRepository.getAllAccounts();
    }

    /**
     * Trả về LiveData chứa danh sách tất cả tài khoản.
     */
    public LiveData<List<AccountEntry>> getAllAccounts() {
        return mAllAccounts;
    }

    /**
     * ✅ HÀM MỚI ĐƯỢC THÊM VÀO
     * Yêu cầu Repository xóa một tài khoản.
     * Hàm này sẽ được gọi từ AccountFragment.
     */
    public void delete(AccountEntry account) {
        mRepository.delete(account); // Gọi hàm delete() trong Repository
    }
}