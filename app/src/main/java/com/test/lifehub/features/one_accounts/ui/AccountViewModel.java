package com.test.lifehub.features.one_accounts.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * AccountViewModel - ViewModel cho AccountFragment
 * 
 * === MỤC ĐÍCH ===
 * Quản lý danh sách tài khoản (accounts) và expose LiveData cho UI.
 * Delegate các operations (CRUD) xuống AccountRepository.
 * 
 * === KIẾN TRÚC MVVM ===
 * ```
 * AccountFragment (View)
 *        |
 *        | observe mAllAccounts
 *        v
 * AccountViewModel <- ĐÂY
 *        |
 *        | delegate delete()
 *        v
 * AccountRepository
 *        |
 *        | Firestore + EncryptionHelper
 *        v
 * Firestore: users/{userId}/accounts/{accountId}
 * ```
 * 
 * === TÍNH NĂNG ===
 * 1. Hiển thị danh sách tài khoản:
 *    - mAllAccounts tự động sync với Firestore realtime
 *    - Fragment observe và hiển thị trên RecyclerView
 * 
 * 2. Xóa tài khoản:
 *    - delete(account) -> repository.delete(account)
 *    - Firestore tự động cập nhật mAllAccounts
 *    - UI tự động refresh nhờ observer
 * 
 * === LIVEDATA FLOW ===
 * ```
 * Firestore SnapshotListener (Repository)
 *         |
 *         | onSnapshot()
 *         v
 * mAllAccounts.setValue(accounts)  (Repository)
 *         |
 *         | LiveData propagation
 *         v
 * mAllAccounts  (ViewModel - expose ra Fragment)
 *         |
 *         | observe()
 *         v
 * AccountFragment.onChanged(accounts)
 *         |
 *         v
 * RecyclerView.submitList(accounts)
 * ```
 * 
 * === HILT INJECTION ===
 * @HiltViewModel annotation cho phép:
 * - Tự động inject AccountRepository qua constructor
 * - Fragment không cần tạo ViewModel thủ công
 * - Lifecycle-aware (destroy cùng Fragment)
 * 
 * Ví dụ:
 * ```java
 * // Fragment không cần new AccountViewModel()
 * @AndroidEntryPoint
 * public class AccountFragment extends Fragment {
 *     private AccountViewModel viewModel;
 * 
 *     @Override
 *     public void onViewCreated(View view, Bundle savedInstanceState) {
 *         // Hilt tự động inject
 *         viewModel = new ViewModelProvider(this).get(AccountViewModel.class);
 *     }
 * }
 * ```
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * ```java
 * // Fragment
 * @AndroidEntryPoint
 * public class AccountFragment extends Fragment {
 *     private AccountViewModel viewModel;
 *     private AccountAdapter adapter;
 * 
 *     @Override
 *     public void onViewCreated(View view, Bundle savedInstanceState) {
 *         viewModel = new ViewModelProvider(this).get(AccountViewModel.class);
 * 
 *         // 1. Observe danh sách accounts
 *         viewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
 *             adapter.submitList(accounts);
 *         });
 * 
 *         // 2. Xóa account khi user swipe
 *         adapter.setOnDeleteListener(account -> {
 *             viewModel.delete(account);
 *             Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
 *         });
 *     }
 * }
 * ```
 * 
 * === BẢO MẬT ===
 * ⚠️ MẬT KHẨU ĐÃ ĐƯỢC MÃ HÓA:
 * - AccountEntry.password là encrypted string (AES-256-GCM)
 * - ViewModel KHÔNG giải mã (giữ nguyên encrypted)
 * - Giải mã chỉ khi user click "Show Password" trong detail screen
 * 
 * === SCOPE ===
 * @HiltViewModel:
 * - 1 instance per Fragment
 * - Survive configuration changes (rotate screen)
 * - Destroyed khi Fragment destroyed
 * 
 * === LƯU Ý QUAN TRỌNG ===
 * 1. ViewModel KHÔNG giữ reference đến Context/View/Fragment
 * 2. Chỉ expose LiveData (read-only), không expose MutableLiveData
 * 3. Chỉ delegate CRUD, không thực hiện Firestore operations trực tiếp
 * 4. Realtime updates tự động - không cần gọi refresh()
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm search accounts theo serviceName/username
 * TODO: Filter accounts theo loại (email, banking, social media)
 * TODO: Sort accounts (theo tên, ngày tạo)
 * TODO: Thêm batch delete (xóa nhiều accounts)
 * FIXME: Xử lý delete failure (network error)
 * 
 * @see AccountRepository Data source với Firestore
 * @see AccountFragment UI layer
 * @see AccountEntry Data model (encrypted password)
 * @see EncryptionHelper Mã hóa/giải mã password
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