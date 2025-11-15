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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.SessionManager; // (Cần cho hàm cũ)

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity xử lý Đăng nhập (Firebase) và Mở khóa (Vân tay).
 * (Phiên bản đã sửa lỗi Tiêu đề Toolbar)
 */
@AndroidEntryPoint
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
    private LoginViewModel mViewModel;
    private FirebaseAuth mAuth; // Cần cho Reset Password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        findViews();

        setSupportActionBar(mToolbar);
        // ✅ SỬA LỖI 4: Ẩn tiêu đề "LifeHub"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mToolbar.setNavigationOnClickListener(v -> finish()); // Nút Back

        setupListeners();

        observeLoginState();
        observeInitialCheck();

        if (savedInstanceState == null) {
            setLoading(true);
            mViewModel.performInitialCheck();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Logic đã chuyển sang ViewModel
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
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            mViewModel.attemptManualLogin(email, password);
        });

        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterEmailActivity.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        mLayoutBiometric.setOnClickListener(v -> {
            setLoading(true);
            BiometricHelper.showBiometricPrompt(this, this);
        });
    }

    private void observeInitialCheck() {
        mViewModel.initialState.observe(this, state -> {
            if (state == null) return;
            Log.d(TAG, "InitialCheckState: " + state.name());

            setLoading(false);

            switch (state) {
                case SHOW_LOGIN_FORM:
                    mLayoutBiometric.setVisibility(View.GONE);
                    break;
                case SHOW_BIOMETRIC_PROMPT:
                    mLayoutBiometric.setVisibility(View.VISIBLE);
                    break;
                case NAVIGATE_TO_MAIN:
                    navigateToMain();
                    break;
                case IDLE:
                    break;
            }
        });
    }

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

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

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

    // --- CÁC HÀM GỌI LẠI (CALLBACK) TỪ BIOMETRICHELPER ---

    @Override
    public void onBiometricAuthSuccess() {
        Log.d(TAG, "Mở khóa bằng vân tay thành công.");
        navigateToMain();
    }

    @Override
    public void onBiometricAuthError(String errorMessage) {
        Log.e(TAG, "Lỗi vân tay: " + errorMessage);
        setLoading(false);
        Toast.makeText(this, "Xác thực vân tay bị hủy.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBiometricAuthFailed() {
        Log.w(TAG, "Vân tay không khớp.");
    }
}