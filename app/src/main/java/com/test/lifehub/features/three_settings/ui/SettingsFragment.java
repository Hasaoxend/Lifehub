package com.test.lifehub.features.three_settings.ui;

import android.content.Intent;
import android.os.Bundle;
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

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.ui.LoginActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment implements BiometricHelper.BiometricAuthListener {

    private TextView tvUserEmail;
    private LinearLayout btnChangePassword;
    private MaterialSwitch switchBiometric;
    private View btnLogout;

    @Inject
    SessionManager sessionManager;

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
                Toast.makeText(getContext(), "Thiết bị không hỗ trợ hoặc chưa cài đặt vân tay", Toast.LENGTH_SHORT).show();
                // Không hỗ trợ thì trả về trạng thái cũ
                revertSwitchState(!isChecked);
            }
        });

        // 3. Xử lý Đổi Mật Khẩu
        btnChangePassword.setOnClickListener(v -> {
            if (BiometricHelper.isBiometricAvailable(requireContext())) {
                isChangePasswordRequest = true;
                if (getActivity() instanceof AppCompatActivity) {
                    BiometricHelper.showBiometricPrompt((AppCompatActivity) getActivity(), this);
                }
            } else {
                openChangePasswordActivity();
            }
        });
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
            Toast.makeText(getContext(), "Đăng nhập bằng vân tay đã " + status, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), "Xác thực thất bại: " + msg, Toast.LENGTH_SHORT).show();

        } else if (isBiometricToggleRequest) {
            isBiometricToggleRequest = false;

            // Thất bại -> Trả lại trạng thái Switch cũ
            revertSwitchState(!pendingBiometricState);
            Toast.makeText(getContext(), "Không thể thay đổi cài đặt: " + msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void openChangePasswordActivity() {
        Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
        startActivity(intent);
    }
}