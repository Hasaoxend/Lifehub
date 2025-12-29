package com.test.lifehub.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.test.lifehub.R;
import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.services.LifeHubAutofillService;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;
import com.test.lifehub.features.four_calendar.repository.CalendarRepository;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;
import com.test.lifehub.features.one_accounts.ui.AccountFragment;
import com.test.lifehub.features.two_productivity.ui.ProductivityFragment;
import com.test.lifehub.features.three_settings.ui.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MainActivity - Activity chính của ứng dụng Lifehub
 * 
 * === CHỨC NĂNG CHÍNH ===
 * 1. Điều hướng giữa 3 Fragment chính qua BottomNavigationView:
 *    - AccountFragment: Quản lý tài khoản (mật khẩu, TOTP)
 *    - ProductivityFragment: Ghi chú, công việc, dự án
 *    - SettingsFragment: Cài đặt ứng dụng
 * 
 * 2. Khởi tạo Firestore Listeners cho tất cả repositories:
 *    - Đảm bảo dữ liệu realtime được đồng bộ từ Firestore
 *    - Tự động cập nhật UI khi data thay đổi
 * 
 * 3. Quản lý permissions (Android 12+, 13+):
 *    - POST_NOTIFICATIONS: Quyền hiển thị thông báo
 *    - SCHEDULE_EXACT_ALARM: Quyền đặt báo thức chính xác cho reminders
 * 
 * === DEPENDENCY INJECTION (Hilt) ===
 * @AndroidEntryPoint đánh dấu để Hilt tự động inject các dependencies:
 * - TotpRepository: Quản lý mã TOTP (2FA)
 * - AccountRepository: Quản lý tài khoản
 * - CalendarRepository: Quản lý sự kiện lịch
 * - ProductivityRepository: Quản lý notes/tasks/projects
 * 
 * === LUỒNG HOẠT ĐỘNG ===
 * 1. User login thành công ở LoginActivity
 * 2. Chạy MainActivity.onCreate()
 * 3. Gọi startListening() cho tất cả repositories
 * 4. Firestore bắt đầu lắng nghe thay đổi dữ liệu
 * 5. Hiển thị AccountFragment mặc định
 * 6. User có thể navigate qua BottomNavigationView
 * 
 * === PHÁT TRIỂN TIẾP ===
 * TODO: Thêm Fragment mới cho tính năng Calendar
 * TODO: Implement deep linking cho notifications
 * TODO: Thêm animation chuyển fragment
 * 
 * @version 1.0.0
 * @since 2025-12-05
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    // ===== UI COMPONENTS =====
    private BottomNavigationView bottomNav;  // Thanh điều hướng dưới cùng
    
    // ===== REPOSITORIES (Hilt Injection) =====
    // Các repository này được inject tự động bởi Hilt
    // Không cần khởi tạo thủ công
    @Inject
    TotpRepository totpRepository;  // Quản lý mã TOTP 2FA  // Quản lý mã TOTP 2FA
    
    @Inject
    AccountRepository accountRepository;  // Quản lý tài khoản (mật khẩu đã mã hóa)
    
    @Inject
    CalendarRepository calendarRepository;  // Quản lý sự kiện lịch
    
    @Inject
    ProductivityRepository productivityRepository;  // Quản lý notes, tasks, projects
    
    @Inject
    SessionManager sessionManager;  // Quản lý session và preferences
    
    @Inject
    EncryptionManager encryptionManager;  // Quản lý mã hóa đa nền tảng

    private boolean hasMigrated = false;

    // ===== PERMISSION LAUNCHER =====
    /**
     * ActivityResultLauncher để xin quyền runtime
     * 
     * Sử dụng cho:
     * - POST_NOTIFICATIONS (Android 13+): Hiển thị thông báo
     * 
     * Cách sử dụng:
     * requestPermissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
     * 
     * Kết quả được xử lý trong callback registerForActivityResult
     */
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                // Xử lý kết quả sau khi người dùng chọn "Cho phép" hoặc "Từ chối"
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.POST_NOTIFICATIONS))) {
                    // Quyền thông báo được cấp
                    Toast.makeText(this, R.string.permission_notification_granted, Toast.LENGTH_SHORT).show();
                } else {
                    // Quyền thông báo bị từ chối
                    Toast.makeText(this, R.string.permission_notification_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- BẢO MẬT: Chống chụp màn hình/quay màn hình ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        // --------------------------------------------------

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== BƯỚC 1: KIỂM TRA ENCRYPTION STATUS =====
        if (!encryptionManager.isUnlocked()) {
            android.util.Log.d("MainActivity", "Encryption is locked, checking setup status...");
            
            encryptionManager.checkSetupStatus(result -> {
                android.util.Log.d("MainActivity", "Setup status result: " + result);
                if (result == EncryptionManager.InitResult.NEEDS_SETUP) {
                    android.util.Log.d("MainActivity", "Redirecting to Setup");
                    Intent intent = new Intent(this, com.test.lifehub.ui.PasscodeSetupActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    android.util.Log.d("MainActivity", "Redirecting to Unlock (MasterPasswordActivity)");
                    // Mặc định hoặc FAILURE (có salt nhưng chưa unlock) -> Sang màn hình nhập PIN
                    Intent intent = new Intent(this, com.test.lifehub.features.masterpassword.MasterPasswordActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            });
            return;
        }

        // ===== BƯỚC 2: KHỚI TẠO FIRESTORE LISTENERS =====
        totpRepository.startListening();
        accountRepository.startListening();
        calendarRepository.startListening();
        productivityRepository.startListening();
        
        // ===== BƯỚC 2.5: AUTO-MIGRATE TOTP LEGACY ENCRYPTION =====
        // Migrate TOTP secrets from old EncryptionHelper to new EncryptionManager
        android.util.Log.d("MainActivity", "🛠️ Triggering TOTP migration check...");
        totpRepository.migrateTotpEncryption();

        // ===== BƯỚC 1.5: AUTO-MIGRATE LEGACY ENCRYPTION =====
        // Tự động chuyển đổi dữ liệu cũ sang chuẩn Cross-platform
        // để Web có thể đọc được. Chỉ chạy 1 lần mỗi phiên app.
        accountRepository.getAllAccounts().observe(this, accounts -> {
            if (!hasMigrated && accounts != null && !accounts.isEmpty()) {
                hasMigrated = true;
                android.util.Log.d("MainActivity", "🛠️ Checking for legacy data migration...");
                accountRepository.migrateEncryption(encryptionManager, new AccountRepository.MigrationCallback() {
                    @Override
                    public void onProgress(int current, int total) {
                        android.util.Log.d("MainActivity", "Migration progress: " + current + "/" + total);
                    }

                    @Override
                    public void onComplete(int successCount, int failedCount) {
                        if (successCount > 0) {
                            android.util.Log.d("MainActivity", "✅ Migration completed: updated " + successCount + " legacy accounts.");
                            Toast.makeText(MainActivity.this, "Đã cập nhật bảo mật cho " + successCount + " tài khoản.", Toast.LENGTH_LONG).show();
                        } else {
                            android.util.Log.d("MainActivity", "Migration check done. No legacy data found.");
                        }
                    }
                });
            }
        });

        // ===== BƯỚC 2: SETUP UI =====

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        // ===== BƯỚC 3: HIỂN THỊ FRAGMENT MẶC ĐỊNH =====
        // savedInstanceState == null -> Lần đầu mở activity (không phải restore)
        // Hiển thị AccountFragment làm trang chủ
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new AccountFragment()).commit();
        }
        
        // ===== BƯỚC 4: SETUP AUTOFILL SERVICE =====
        // Sync biometric state và accounts cache cho AutofillService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupAutofillService();
        }

        // --- XÓA LOGIC XIN QUYỀN ---
        // (Như đã khuyên, logic này nên được chuyển vào
        // nơi cần thiết, ví dụ: khi bấm nút tạo Nhắc nhở)
        // checkAndRequestPermissions();
        // ---------------------------------
    }
    
    /**
     * Setup Autofill Service với cache và biometric state
     * Gọi sau khi repositories đã start listening
     */
    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    private void setupAutofillService() {
        // 1. Sync autofill enabled state (cần cả biometric + autofill đều ON)
        boolean biometricEnabled = sessionManager.isBiometricEnabled();
        boolean autofillEnabled = sessionManager.isAutofillEnabled();
        LifeHubAutofillService.setBiometricEnabled(biometricEnabled && autofillEnabled);
        
        // 2. Observe accounts, tạo COPY với passwords đã decrypt rồi sync với cache
        // QUAN TRỌNG: Tạo copy mới vì LiveData objects không nên modify trực tiếp
        accountRepository.getAllAccounts().observe(this, accounts -> {
            if (accounts != null && !accounts.isEmpty()) {
                List<AccountEntry> decryptedAccounts = new ArrayList<>();
                
                for (AccountEntry original : accounts) {
                    // Tạo copy mới
                    AccountEntry copy = new AccountEntry();
                    copy.documentId = original.documentId;
                    copy.serviceName = original.serviceName;
                    copy.username = original.username;
                    copy.websiteUrl = original.websiteUrl;
                    copy.notes = original.notes;
                    copy.userOwnerId = original.userOwnerId;
                    
                    // Luôn thử decrypt - encryptionHelper.decrypt() đã có fallback
                    // Nếu decrypt thất bại (password đã plain text), nó trả về string gốc
                    if (original.password != null && !original.password.isEmpty()) {
                        copy.password = encryptionManager.decrypt(original.password);
                    } else {
                        copy.password = "";
                    }
                    
                    decryptedAccounts.add(copy);
                }
                
                android.util.Log.d("MainActivity", "Autofill cache updated with " + decryptedAccounts.size() + " accounts (passwords decrypted)");
                LifeHubAutofillService.updateAccountsCache(decryptedAccounts);
            }
        });
    }

    // ===== NAVIGATION LISTENER =====
    /**
     * Xử lý sự kiện khi user click vào item trong BottomNavigationView
     * 
     * === LUỒNG HOẠT ĐỘNG ===
     * 1. User click vào 1 trong 3 icon: Accounts, Productivity, Settings
     * 2. Check item.getItemId() để biết icon nào được click
     * 3. Tạo Fragment tương ứng
     * 4. Thay thế fragment hiện tại bằng fragment mới
     * 5. Return true để highlight icon đã chọn
     * 
     * === THÊM FRAGMENT MỚI ===
     * Để thêm fragment mới (ví dụ: CalendarFragment):
     * 1. Thêm item vào bottom_nav_menu.xml:
     *    <item android:id="@+id/navigation_calendar" .../>
     * 2. Thêm case mới vào listener:
     *    else if (itemId == R.id.navigation_calendar) {
     *        selectedFragment = new CalendarFragment();
     *    }
     */
    private final NavigationBarView.OnItemSelectedListener navListener =
            item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                // (Sửa lại tên package cho đúng)
                if (itemId == R.id.navigation_accounts) {
                    selectedFragment = new AccountFragment();
                } else if (itemId == R.id.navigation_productivity) {
                    selectedFragment = new ProductivityFragment();
                } else if (itemId == R.id.navigation_settings) {
                    selectedFragment = new SettingsFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            };

    // ===== PERMISSION MANAGEMENT =====
    /**
     * Kiểm tra và yêu cầu các quyền cần thiết
     * 
     * === LƯU Ý QUAN TRỌNG ===
     * Hàm này HIỆN KHÔNG ĐƯỢC GỌI trong onCreate()
     * Lý do: Không nên xin quyền ngay khi mở app (UX kém)
     * 
     * === KHI NÀO NÊN XIN QUYỀN? ===
     * - POST_NOTIFICATIONS: Khi user tạo reminder/notification lần đầu
     * - SCHEDULE_EXACT_ALARM: Khi user set alarm lần đầu
     * 
     * === CÁCH PHÁT TRIỂN TIẾP ===
     * Option 1: Gọi hàm này trong TaskDetailActivity khi user tạo reminder
     * Option 2: Gọi hàm này trong SettingsFragment khi user bật notifications
     * 
     * === CÁC QUYỀN ĐƯỢC XỪA LÝ ===
     * 1. POST_NOTIFICATIONS (Android 13+/API 33+):
     *    - Quyền hiển thị notification
     *    - Bắt buộc phải xin runtime permission
     * 
     * 2. SCHEDULE_EXACT_ALARM (Android 12+/API 31+):
     *    - Quyền đặt báo thức chính xác
     *    - Không cần runtime permission, chỉ cần dẫn user vào Settings
     * 
     * @see #requestPermissionLauncher Kiểm tra kết quả xin quyền
     */
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // 1. Quyền Thông báo (POST_NOTIFICATIONS) - Bắt buộc cho Android 13 (API 33)+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // 2. Quyền Đặt báo thức chính xác (SCHEDULE_EXACT_ALARM) - Bắt buộc cho Android 12 (API 31)+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_alarm_title)
                        .setMessage(R.string.permission_alarm_message)
                        .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.later, null)
                        .show();
            }
        }

        // Nếu có quyền cần yêu cầu (hiện tại chỉ có POST_NOTIFICATIONS)
        if (!permissionsToRequest.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
}