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
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.one_accounts.ui.AccountFragment;
import com.test.lifehub.features.two_productivity.ui.ProductivityFragment;
import com.test.lifehub.features.three_settings.ui.SettingsFragment;
import com.test.lifehub.core.util.SessionManager; // <-- XÓA IMPORT NÀY

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint; // <-- THÊM IMPORT NÀY

/**
 * Activity chính, "bộ điều khiển" trung tâm của ứng dụng.
 * (Phiên bản đã refactor Hilt và xóa logic không cần thiết)
 */
@AndroidEntryPoint // <-- THÊM CHÚ THÍCH NÀY
public class MainActivity extends AppCompatActivity {

    // private SessionManager sessionManager; // <-- XÓA FIELD NÀY
    private BottomNavigationView bottomNav;
    
    @Inject
    TotpRepository totpRepository;

    // ----- BỘ XIN QUYỀN MỚI (DÙNG CHO THÔNG BÁO) -----
    // (Giữ nguyên)
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                // Xử lý kết quả sau khi người dùng chọn "Cho phép" hoặc "Từ chối"
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.POST_NOTIFICATIONS))) {
                    // Quyền thông báo được cấp
                    Toast.makeText(this, "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
                } else {
                    // Quyền thông báo bị từ chối
                    Toast.makeText(this, "Bạn đã từ chối quyền thông báo. Tính năng nhắc nhở có thể không hoạt động.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Restart Firestore listener for current user
        totpRepository.startListening();

        // --- XÓA LOGIC KIỂM TRA ĐĂNG NHẬP ---
        // LoginActivity đã xử lý việc này. Nếu user đến được đây
        // nghĩa là họ ĐÃ ĐƯỢC PHÉP vào.
        // sessionManager = new SessionManager(this);
        // if (!sessionManager.isLoggedIn()) {
        //     finish();
        //     return;
        // }
        // ------------------------------------

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(navListener);

        // Hiển thị Fragment Trang chính (Tài khoản) làm mặc định
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                    new AccountFragment()).commit();
        }

        // --- XÓA LOGIC XIN QUYỀN ---
        // (Như đã khuyên, logic này nên được chuyển vào
        // nơi cần thiết, ví dụ: khi bấm nút tạo Nhắc nhở)
        // checkAndRequestPermissions();
        // ---------------------------------
    }

    // Bộ lắng nghe (Listener) cho thanh điều hướng dưới
    // (Giữ nguyên)
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

    /**
     * HÀM NÀY VẪN TỒN TẠI, NHƯNG KHÔNG ĐƯỢC GỌI TRONG onCreate()
     * (Giữ nguyên)
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
                        .setTitle("Cần cấp quyền Báo thức")
                        .setMessage("Để tính năng Nhắc nhở hoạt động chính xác, LifeHub cần quyền \"Đặt báo thức và lời nhắc\".")
                        .setPositiveButton("Đi đến Cài đặt", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Để sau", null)
                        .show();
            }
        }

        // Nếu có quyền cần yêu cầu (hiện tại chỉ có POST_NOTIFICATIONS)
        if (!permissionsToRequest.isEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        }
    }
}