package com.test.lifehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout; // <-- Thêm import
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // <-- Thêm import

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.SessionManager;

/**
 * Activity xử lý Đăng nhập (Firebase) và Mở khóa (Vân tay).
 * (Phiên bản đã cập nhật giao diện mới)
 */
public class LoginActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "LoginActivity";

    // --- Views ---
    private Toolbar mToolbar;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private ProgressBar pbLogin;
    private LinearLayout mLayoutBiometric; // <-- View mới

    // --- Logic ---
    private FirebaseAuth mAuth;
    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo
        mAuth = FirebaseAuth.getInstance();
        mSessionManager = new SessionManager(this);

        // Ánh xạ (Find) Views
        findViews();

        // Cài đặt Toolbar
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(v -> finish()); // Nút Back

        // Thiết lập Listeners
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // ----- LUỒNG LOGIC TỰ ĐỘNG ĐĂNG NHẬP / MỞ KHÓA -----

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null && currentUser.isEmailVerified()) {
            // 1. Người dùng ĐÃ đăng nhập VÀ đã xác thực
            mSessionManager.createLoginSession(currentUser.getUid());

            // 2. Kiểm tra xem họ có muốn dùng VÂN TAY không
            if (mSessionManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(this)) {
                // SỬA LỖI LOGIC:
                // Thay vì tự động bật vân tay, chúng ta hiển thị nút
                mLayoutBiometric.setVisibility(View.VISIBLE);
                setLoading(false); // Đảm bảo form đăng nhập vẫn hiện
            } else {
                // Không bật vân tay -> Tự động đăng nhập
                navigateToMain();
            }

        } else if (currentUser != null && !currentUser.isEmailVerified()) {
            // 3. Đã đăng nhập, nhưng CHƯA xác thực
            Toast.makeText(this, "Vui lòng xác thực email của bạn.", Toast.LENGTH_LONG).show();
            mAuth.signOut();
            mLayoutBiometric.setVisibility(View.GONE);
            setLoading(false);
        } else {
            // 4. Người dùng CHƯA đăng nhập
            Log.d(TAG, "Không có người dùng nào đăng nhập, hiển thị form.");
            mLayoutBiometric.setVisibility(View.GONE);
            setLoading(false);
        }
    }

    private void findViews() {
        mToolbar = findViewById(R.id.toolbar_login); // <-- View mới
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        pbLogin = findViewById(R.id.pb_login);
        mLayoutBiometric = findViewById(R.id.layout_biometric_login); // <-- View mới
    }

    private void setupListeners() {
        // Nút Đăng nhập
        btnLogin.setOnClickListener(v -> attemptManualLogin());

        // Nút chuyển sang Đăng ký
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterEmailActivity.class);
            startActivity(intent);
        });

        // Nút Quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // SỬA LỖI LOGIC: Nút Đăng nhập Vân tay
        mLayoutBiometric.setOnClickListener(v -> {
            // Chỉ khi nhấn nút này, hộp thoại vân tay mới hiện lên
            setLoading(true); // Ẩn form
            BiometricHelper.showBiometricPrompt(this, this);
        });
    }

    /**
     * Xử lý khi người dùng nhấn nút "Đăng nhập" thủ công.
     */
    private void attemptManualLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true); // Bật ProgressBar

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        if (user.isEmailVerified()) {
                            mSessionManager.createLoginSession(user.getUid());
                            navigateToMain();
                        } else {
                            setLoading(false);
                            Toast.makeText(this, "Email này chưa được xác thực. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.w(TAG, "signInWithEmail:failure", e);
                    Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * (Chức năng thêm) Hiển thị hộp thoại để gửi email Quên mật khẩu.
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


    // ----- CÁC HÀM GỌI LẠI (CALLBACK) TỪ BIOMETRICHELPER -----

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