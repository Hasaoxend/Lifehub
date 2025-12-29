package com.test.lifehub.features.three_settings.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.provider.Settings;
import android.net.Uri;
import android.os.PowerManager;
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
import com.test.lifehub.core.services.LifeHubAutofillService;
import com.test.lifehub.core.util.LocaleHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;
import com.test.lifehub.features.four_calendar.repository.CalendarRepository;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;
import com.test.lifehub.ui.LoginActivity;
import com.test.lifehub.core.security.EncryptionManager;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements BiometricHelper.BiometricAuthListener {

    private TextView tvUserEmail;
    private LinearLayout btnChangePassword;
    private LinearLayout btnLanguage;
    private LinearLayout btnPermissions;
    private MaterialSwitch switchBiometric;
    private MaterialSwitch switchAutofill;
    private MaterialSwitch switchBackgroundPersistence;
    private TextView tvAutofillHint;
    private TextView tvBackgroundHint;
    private View btnLogout;
    private LinearLayout btnSyncWeb;
    private LinearLayout btnChangePasscode;

    @Inject
    SessionManager sessionManager;
    
    @Inject
    EncryptionManager encryptionManager;
    
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
        switchAutofill = view.findViewById(R.id.switch_autofill);
        switchBackgroundPersistence = view.findViewById(R.id.switch_background_persistence);
        tvAutofillHint = view.findViewById(R.id.tv_autofill_hint);
        tvBackgroundHint = view.findViewById(R.id.tv_background_hint);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnSyncWeb = view.findViewById(R.id.btn_sync_web);
        btnChangePasscode = view.findViewById(R.id.btn_change_passcode);
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
        
        // Load trạng thái Autofill
        boolean autofillEnabled = sessionManager.isAutofillEnabled();
        boolean biometricEnabled = sessionManager.isBiometricEnabled();
        switchAutofill.setChecked(autofillEnabled);
        
        // Hiển thị hint nếu chưa bật biometric
        if (!biometricEnabled) {
            tvAutofillHint.setVisibility(android.view.View.VISIBLE);
            switchAutofill.setEnabled(false);
        }
        
        isProgrammaticChange = false;

        checkBackgroundPersistenceState();
    }

    private void checkBackgroundPersistenceState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());
            isProgrammaticChange = true;
            switchBackgroundPersistence.setChecked(isIgnoring);
            isProgrammaticChange = false;
        } else {
            switchBackgroundPersistence.setVisibility(View.GONE);
            tvBackgroundHint.setVisibility(View.GONE);
        }
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
            
            encryptionManager.lock();
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
        
        // 6. Xử lý Bật/Tắt Autofill
        switchAutofill.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            
            // RÀNG BUỘC: Phải bật Biometric trước
            if (!sessionManager.isBiometricEnabled()) {
                Toast.makeText(getContext(), R.string.settings_autofill_requires_biometric, Toast.LENGTH_SHORT).show();
                // Revert switch
                isProgrammaticChange = true;
                switchAutofill.setChecked(false);
                isProgrammaticChange = false;
                return;
            }
            
            // Lưu trạng thái
            sessionManager.setAutofillEnabled(isChecked);
            
            // Sync với AutofillService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LifeHubAutofillService.setBiometricEnabled(isChecked);
            }
            
            if (isChecked) {
                showAutofillPermissionDialogs();
            } else {
                String msg = getString(R.string.settings_autofill_disabled);
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });


        // 8. Xử lý Đồng bộ hóa dữ liệu Web
        btnSyncWeb.setOnClickListener(v -> handleDataMigration());

        // 9. Xử lý Đổi mã PIN bảo mật
        btnChangePasscode.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.test.lifehub.ui.ChangePasscodeActivity.class);
            startActivity(intent);
        });

        // 9. Xử lý Chạy nền
        switchBackgroundPersistence.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isProgrammaticChange) return;
            
            if (isChecked) {
                showBackgroundPersistenceDialog();
            } else {
                Toast.makeText(getContext(), "Vui lòng vào cài đặt hệ thống để thay đổi tối ưu hóa pin nếu cần.", Toast.LENGTH_LONG).show();
                checkBackgroundPersistenceState(); // Revert UI
            }
        });
    }

    private void showBackgroundPersistenceDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Duy trì chạy nền")
                .setMessage("Để đảm bảo Autofill hoạt động ổn định và không bị hệ thống tự động tắt khi bạn không mở app, LifeHub cần được phép chạy nền không giới hạn.\n\nVui lòng chọn 'Không tối ưu hóa' trong cửa sổ tiếp theo.")
                .setPositiveButton("Thiết lập", (dialog, which) -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Hủy", (dialog, which) -> checkBackgroundPersistenceState())
                .show();
    }

    /**
     * Hiển thị chuỗi Dialog hướng dẫn bật các quyền cần thiết cho Autofill
     */
    private void showAutofillPermissionDialogs() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Kích hoạt Tự động điền")
            .setMessage("Để LifeHub có thể điền mật khẩu trên trình duyệt và các ứng dụng khác, bạn cần thực hiện các bước thiết lập hệ thống.")
            .setPositiveButton("Bắt đầu", (dialog, which) -> showAccessibilityDialog())
            .setNegativeButton("Để sau", null)
            .show();
    }

    private void showAccessibilityDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Bước 1: Hỗ trợ tiếp cận")
            .setMessage("Tìm 'LifeHub' trong danh sách Dịch vụ đã cài đặt và BẬT nó lên.\n\nQuyền này giúp app nhận diện ô nhập liệu trên trình duyệt.")
            .setPositiveButton("Mở Cài đặt", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                showOverlayDialog();
            })
            .setNegativeButton("Bỏ qua", (dialog, which) -> showOverlayDialog())
            .show();
    }

    private void showOverlayDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Bước 2: Hiển thị trên ứng dụng khác")
            .setMessage("Bật quyền này để LifeHub hiển thị nút 'Tự động điền' lơ lửng phía trên các ô nhập mật khẩu.")
            .setPositiveButton("Mở Cài đặt", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                startActivity(intent);
                showDefaultAutofillDialog();
            })
            .setNegativeButton("Bỏ qua", (dialog, which) -> showDefaultAutofillDialog())
            .show();
    }

    private void showDefaultAutofillDialog() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AutofillManager afm = requireContext().getSystemService(AutofillManager.class);
            if (afm != null && !afm.hasEnabledAutofillServices()) {
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Bước 3: Chọn LifeHub làm mặc định")
                    .setMessage("Chọn LifeHub làm 'Dịch vụ tự động điền' mặc định của hệ thống.")
                    .setPositiveButton("Thiết lập", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Xong", null)
                    .show();
            } else {
                Toast.makeText(getContext(), "Thiết lập hoàn tất!", Toast.LENGTH_SHORT).show();
            }
        }
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

            // Thành công -> BƯỚC TIẾP THEO: Yêu cầu nhập mật khẩu để lưu an toàn
            if (pendingBiometricState) {
                showPasswordConfirmationDialog();
            } else {
                // Nếu tắt vân tay -> Xóa pass đã lưu và tắt flag
                sessionManager.setBiometricEnabled(false);
                sessionManager.clearEncryptionPassword();
                
                // Tắt autofill
                isProgrammaticChange = true;
                switchAutofill.setChecked(false);
                switchAutofill.setEnabled(false);
                tvAutofillHint.setVisibility(android.view.View.VISIBLE);
                isProgrammaticChange = false;
                sessionManager.setAutofillEnabled(false);

                Toast.makeText(getContext(), "Đã tắt xác thực vân tay", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Dialog yêu cầu nhập mật khẩu để lưu vào Keystore (bắt buộc khi bật vân tay)
     */
    private void showPasswordConfirmationDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_confirm, null);
        com.google.android.material.textfield.TextInputEditText etPassword = view.findViewById(R.id.et_password);
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Bắt buộc: Xác nhận mật khẩu")
            .setMessage("Để dùng vân tay mở khóa, bạn cần nhập MẬT KHẨU ĐĂNG NHẬP (Firebase Password) 1 lần duy nhất để lưu an toàn vào hệ thống.")
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("Xác nhận", (dialog, which) -> {
                String password = etPassword.getText().toString();
                if (password.length() >= 6) {
                    // Lưu pass và bật vân tay
                    sessionManager.setBiometricEnabled(true);
                    sessionManager.saveEncryptionPassword(password);
                    
                    // Enable Autofill switch
                    switchAutofill.setEnabled(true);
                    tvAutofillHint.setVisibility(android.view.View.GONE);
                    
                    // Hiển thị thông báo bắt buộc khởi động lại
                    showRestartAppDialog();
                } else {
                    Toast.makeText(getContext(), "Mật khẩu không hợp lệ", Toast.LENGTH_SHORT).show();
                    revertSwitchState(false);
                }
            })
            .setNegativeButton("Hủy", (dialog, which) -> revertSwitchState(false))
            .show();
    }

    private void showRestartAppDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cần khởi động lại")
            .setMessage("Thiết lập sinh trắc học đã hoàn tất. Ứng dụng cần được khởi động lại để áp dụng các thay đổi bảo mật.")
            .setPositiveButton("Khởi động lại ngay", (dialog, which) -> {
                requireActivity().finishAffinity();
                System.exit(0);
            })
            .setCancelable(false)
            .show();
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

    private void handleDataMigration() {
        if (!encryptionManager.isUnlocked()) {
            Toast.makeText(getContext(), "Vui lòng mở khóa ứng dụng trước khi đồng bộ", Toast.LENGTH_LONG).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đồng bộ dữ liệu Web")
            .setMessage("Ứng dụng sẽ chuyển đổi toàn bộ mật khẩu cũ sang chuẩn mới để bạn có thể xem trên trình duyệt Web. Quá trình này có thể mất vài giây.")
            .setPositiveButton("Bắt đầu", (dialog, which) -> {
                performMigration();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void performMigration() {
        android.app.ProgressDialog pd = new android.app.ProgressDialog(requireContext());
        pd.setMessage("Đang đồng bộ dữ liệu...");
        pd.setCancelable(false);
        pd.show();

        accountRepository.migrateEncryption(encryptionManager, new AccountRepository.MigrationCallback() {
            @Override
            public void onProgress(int current, int total) {
                getActivity().runOnUiThread(() -> pd.setMessage("Đang xử lý: " + current + "/" + total));
            }

            @Override
            public void onComplete(int successCount, int failedCount) {
                getActivity().runOnUiThread(() -> {
                    pd.dismiss();
                    new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Đồng bộ hoàn tất")
                        .setMessage("Đã nâng cấp mã hóa cho " + successCount + " tài khoản. " + 
                                (failedCount > 0 ? "\n Thất bại: " + failedCount : ""))
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        });
    }

    private void openChangePasswordActivity() {
        Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
        startActivity(intent);
    }
}