package com.test.lifehub.features.three_settings.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.LocaleHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;
import com.test.lifehub.features.four_calendar.repository.CalendarRepository;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;
import com.test.lifehub.ui.LoginActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements BiometricHelper.BiometricAuthListener {

    private TextView tvUserEmail;
    private LinearLayout btnChangePassword;
    private LinearLayout btnLanguage;
    private LinearLayout btnPermissions;
    private MaterialSwitch switchBiometric;
    private View btnLogout;

    @Inject
    SessionManager sessionManager;
    
    @Inject
    TotpRepository totpRepository;
    
    @Inject
    AccountRepository accountRepository;
    
    @Inject
    CalendarRepository calendarRepository;
    
    @Inject
    ProductivityRepository productivityRepository;

    private FirebaseAuth mAuth;

    // --- CÁC CỜ ĐIỀU KHIỂN LOGIC XÁC THỰC ---
    private boolean isChangePasswordRequest = false;   // Đang yêu cầu đổi mật khẩu?
    private boolean isBiometricToggleRequest = false;  // Đang yêu cầu bật/tắt vân tay?
    private boolean pendingBiometricState = false;     // Trạng thái On/Off mà user vừa bấm
    private boolean isProgrammaticChange = false;      // Cờ để tránh vòng lặp khi code tự chỉnh switch

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        findViews(view);
        setupUI();
        setupListeners();
    }

    private void findViews(View view) {
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnLanguage = view.findViewById(R.id.btn_language);
        btnPermissions = view.findViewById(R.id.btn_permissions);
        switchBiometric = view.findViewById(R.id.switch_biometric);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
        }

        // Load trạng thái vân tay hiện tại
        // Đặt cờ isProgrammaticChange = true để Listener không bị kích hoạt lúc này
        isProgrammaticChange = true;
        switchBiometric.setChecked(sessionManager.isBiometricEnabled());
        isProgrammaticChange = false;
    }

    private void setupListeners() {
        // 1. Đăng xuất
        btnLogout.setOnClickListener(v -> {
            // ✅ BẢO MẬT: Stop ALL Firestore listeners để tránh data leak
            Log.d("SettingsFragment", "Stopping all Firestore listeners on logout");
            totpRepository.stopListening();
            accountRepository.stopListening();
            calendarRepository.stopListening();
            productivityRepository.stopListening();
            
            sessionManager.logoutUser();
            mAuth.signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 2. Xử lý Bật/Tắt vân tay (CÓ XÁC THỰC)
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Nếu thay đổi do code (revert) thì bỏ qua, không xác thực lại
            if (isProgrammaticChange) return;

            // Kiểm tra thiết bị
            if (BiometricHelper.isBiometricAvailable(requireContext())) {
                // Lưu lại trạng thái user mong muốn (Ví dụ: đang Off muốn bật On)
                isBiometricToggleRequest = true;
                pendingBiometricState = isChecked;

                // Gọi xác thực vân tay
                if (getActivity() instanceof AppCompatActivity) {
                    BiometricHelper.showBiometricPrompt((AppCompatActivity) getActivity(), this);
                }
            } else {
                Toast.makeText(getContext(), R.string.settings_biometric_not_supported, Toast.LENGTH_SHORT).show();
                // Không hỗ trợ thì trả về trạng thái cũ
                revertSwitchState(!isChecked);
            }
        });

        // 3. Xử lý Đổi Mật Khẩu
        btnChangePassword.setOnClickListener(v -> {
            // Chỉ yêu cầu xác thực vân tay nếu:
            // 1. Thiết bị hỗ trợ vân tay
            // 2. Người dùng đã bật tính năng vân tay trong app
            if (BiometricHelper.isBiometricAvailable(requireContext()) && sessionManager.isBiometricEnabled()) {
                isChangePasswordRequest = true;
                if (getActivity() instanceof AppCompatActivity) {
                    BiometricHelper.showBiometricPrompt((AppCompatActivity) getActivity(), this);
                }
            } else {
                // Không có vân tay hoặc chưa bật -> Đổi mật khẩu trực tiếp
                openChangePasswordActivity();
            }
        });

        // 4. Xử lý Chọn Ngôn ngữ
        btnLanguage.setOnClickListener(v -> showLanguageDialog());

        // 5. Xử lý Quản lý quyền
        btnPermissions.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PermissionsSettingsActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Hiển thị dialog chọn ngôn ngữ
     */
    private void showLanguageDialog() {
        String currentLanguage = LocaleHelper.getLanguage(requireContext());
        int selectedIndex = currentLanguage.equals(LocaleHelper.LANGUAGE_VIETNAMESE) ? 1 : 0;
        
        String[] languages = {"English", "Tiếng Việt"};
        String[] languageCodes = {LocaleHelper.LANGUAGE_ENGLISH, LocaleHelper.LANGUAGE_VIETNAMESE};
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_language_change)
            .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                String selectedLanguage = languageCodes[which];
                String previousLanguage = LocaleHelper.getLanguage(requireContext());
                
                // Chỉ thực hiện nếu thay đổi ngôn ngữ
                if (!selectedLanguage.equals(previousLanguage)) {
                    // Lưu ngôn ngữ mới
                    LocaleHelper.saveLanguage(requireContext(), selectedLanguage);
                    
                    // Áp dụng ngôn ngữ mới
                    LocaleHelper.setLocale(requireContext(), selectedLanguage);
                    
                    // Hiển thị thông báo restart
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.title_language_changed)
                        .setMessage(getString(R.string.language_changed_restart))
                        .setPositiveButton("OK", (restartDialog, w) -> {
                            // Khởi động lại activity để áp dụng ngôn ngữ mới
                            requireActivity().recreate();
                        })
                        .setCancelable(false)
                        .show();
                }
                
                dialog.dismiss();
            })
            .setNegativeButton("Cancel / Hủy", null)
            .show();
    }

    // Hàm hỗ trợ trả lại trạng thái cũ cho Switch mà không kích hoạt Listener
    private void revertSwitchState(boolean originalState) {
        isProgrammaticChange = true;
        switchBiometric.setChecked(originalState);
        isProgrammaticChange = false;
    }

    // --- CALLBACK TỪ BIOMETRIC HELPER ---

    @Override
    public void onBiometricAuthSuccess() {
        if (isChangePasswordRequest) {
            // Trường hợp 1: Xác thực để Đổi mật khẩu
            isChangePasswordRequest = false;
            openChangePasswordActivity();

        } else if (isBiometricToggleRequest) {
            // Trường hợp 2: Xác thực để Bật/Tắt vân tay
            isBiometricToggleRequest = false;

            // Thành công -> Mới chính thức lưu vào Session
            sessionManager.setBiometricEnabled(pendingBiometricState);

            String status = pendingBiometricState ? "được bật" : "bị tắt";
            Toast.makeText(getContext(), getString(R.string.settings_biometric_status, status), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBiometricAuthError(String errorMessage) {
        handleAuthFailure(errorMessage);
    }

    @Override
    public void onBiometricAuthFailed() {
        // Hệ thống cho phép thử lại, nếu quá số lần sẽ gọi onBiometricAuthError
    }

    private void handleAuthFailure(String msg) {
        if (isChangePasswordRequest) {
            isChangePasswordRequest = false;
            Toast.makeText(getContext(), getString(R.string.settings_auth_failed, msg), Toast.LENGTH_SHORT).show();

        } else if (isBiometricToggleRequest) {
            isBiometricToggleRequest = false;

            // Thất bại -> Trả lại trạng thái Switch cũ
            revertSwitchState(!pendingBiometricState);
            Toast.makeText(getContext(), getString(R.string.settings_cannot_change, msg), Toast.LENGTH_SHORT).show();
        }
    }

    private void openChangePasswordActivity() {
        Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
        startActivity(intent);
    }
}