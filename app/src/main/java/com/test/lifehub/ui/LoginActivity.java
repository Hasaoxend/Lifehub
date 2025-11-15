package com.test.lifehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider; // <-- THÊM IMPORT NÀY

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth; // <-- SẼ CẦN CHO VIỆC RESET PASS
import com.google.firebase.auth.FirebaseUser; // <-- XÓA IMPORT NÀY
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.SessionManager; // <-- XÓA IMPORT NÀY

import dagger.hilt.android.AndroidEntryPoint; // <-- THÊM IMPORT NÀY

/**
 * Activity xử lý Đăng nhập (Firebase) và Mở khóa (Vân tay).
 * (Phiên bản đã refactor hoàn toàn sang Hilt và MVVM)
 */
@AndroidEntryPoint // <-- THÊM CHÚ THÍCH NÀY
public class LoginActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "LoginActivity";

    // --- Views ---
    private Toolbar mToolbar;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private ProgressBar pbLogin;
    private LinearLayout mLayoutBiometric;

    // --- Logic ---
    private LoginViewModel mViewModel; // <-- SỬA LẠI: "BỘ NÃO" MỚI
    private FirebaseAuth mAuth; // <-- Vẫn giữ lại cho chức năng Reset Password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo
        // mAuth và mSessionManager SẼ ĐƯỢC HILT TIÊM VÀO VIEWMODEL
        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        mAuth = FirebaseAuth.getInstance(); // Vẫn cần cho dialog Quên mật khẩu

        // Ánh xạ (Find) Views
        findViews();

        // Cài đặt Toolbar
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(v -> finish()); // Nút Back

        // Thiết lập Listeners
        setupListeners();

        // Thiết lập Observers (Lắng nghe ViewModel)
        observeInitialCheck();
        observeLoginState();

        // Chỉ gọi kiểm tra ban đầu MỘT LẦN khi Activity mới được tạo
        if (savedInstanceState == null) {
            setLoading(true); // Hiển thị loading trong khi kiểm tra
            mViewModel.performInitialCheck();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // XÓA HẾT MỌI LOGIC TRONG NÀY.
        // ViewModel đã xử lý ở onCreate.
    }

    private void findViews() {
        mToolbar = findViewById(R.id.toolbar_login);
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        pbLogin = findViewById(R.id.pb_login);
        mLayoutBiometric = findViewById(R.id.layout_biometric_login);
    }

    private void setupListeners() {
        // Nút Đăng nhập
        btnLogin.setOnClickListener(v -> {
            // SỬA LẠI: Giao việc cho ViewModel
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            mViewModel.attemptManualLogin(email, password);
        });

        // Nút chuyển sang Đăng ký
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterEmailActivity.class);
            startActivity(intent);
        });

        // Nút Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Nút Đăng nhập Vân tay
        mLayoutBiometric.setOnClickListener(v -> {
            setLoading(true); // Ẩn form
            BiometricHelper.showBiometricPrompt(this, this);
        });
    }

    /**
     * HÀM MỚI:
     * Lắng nghe trạng thái khởi động (thay cho onStart).
     */
    private void observeInitialCheck() {
        mViewModel.initialState.observe(this, state -> {
            if (state == null) return;
            Log.d(TAG, "InitialCheckState: " + state.name());

            setLoading(false); // Tắt loading (vì performInitialCheck đã xong)

            switch (state) {
                case SHOW_LOGIN_FORM:
                    mLayoutBiometric.setVisibility(View.GONE);
                    break;
                case SHOW_BIOMETRIC_PROMPT:
                    mLayoutBiometric.setVisibility(View.VISIBLE);
                    break;
                case NAVIGATE_TO_MAIN:
                    navigateToMain(); // Vào thẳng
                    break;
                case IDLE:
                    // Đang chờ... (setLoading(true) ở onCreate đã xử lý)
                    break;
            }
        });
    }

    /**
     * HÀM MỚI:
     * Lắng nghe trạng thái của nút bấm Đăng nhập.
     */
    private void observeLoginState() {
        mViewModel.loginState.observe(this, state -> {
            if (state == null) return;
            Log.d(TAG, "LoginState: " + state.name());

            switch (state) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    navigateToMain();
                    break;
                case ERROR_EMAIL_UNVERIFIED:
                    setLoading(false);
                    Toast.makeText(this, "Email này chưa được xác thực. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_BAD_CREDENTIALS:
                    setLoading(false);
                    Toast.makeText(this, "Đăng nhập thất bại: Sai email hoặc mật khẩu.", Toast.LENGTH_LONG).show();
                    break;
                case ERROR_EMPTY_FIELDS:
                    setLoading(false);
                    Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
                    break;
                case IDLE:
                    setLoading(false);
                    break;
            }
        });
    }

    /**
     * Xử lý khi người dùng nhấn nút "Đăng nhập" thủ công.
     * (HÀM NÀY ĐÃ BỊ XÓA - LOGIC ĐÃ CHUYỂN SANG VIEWMODEL)
     */
    // private void attemptManualLogin() { ... }

    /**
     * (Chức năng thêm) Hiển thị hộp thoại để gửi email Quên mật khẩu.
     * (Hàm này giữ nguyên)
     */
    private void showForgotPasswordDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        final TextInputEditText etEmailInput = dialogView.findViewById(R.id.et_dialog_email);

        new AlertDialog.Builder(this)
                .setTitle("Khôi phục Mật khẩu")
                .setMessage("Nhập email của bạn để nhận link khôi phục:")
                .setView(dialogView)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String email = etEmailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Email không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã gửi link khôi phục, vui lòng kiểm tra email.", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * (Hàm này giữ nguyên)
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * (Hàm này giữ nguyên)
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            pbLogin.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
            etEmail.setEnabled(false);
            etPassword.setEnabled(false);
        } else {
            pbLogin.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            etEmail.setEnabled(true);
            etPassword.setEnabled(true);
        }
    }


    // ----- CÁC HÀM GỌI LẠI (CALLBACK) TỪ BIOMETRICHELPER -----
    // (Giữ nguyên toàn bộ)

    @Override
    public void onBiometricAuthSuccess() {
        Log.d(TAG, "Mở khóa bằng vân tay thành công.");
        navigateToMain();
    }

    @Override
    public void onBiometricAuthError(String errorMessage) {
        Log.e(TAG, "Lỗi vân tay: " + errorMessage);
        setLoading(false); // Hiển thị lại form đăng nhập
        Toast.makeText(this, "Xác thực vân tay bị hủy.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBiometricAuthFailed() {
        Log.w(TAG, "Vân tay không khớp.");
    }
}