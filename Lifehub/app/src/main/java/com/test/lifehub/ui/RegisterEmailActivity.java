package com.test.lifehub.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;

/**
 * Activity xử lý việc Đăng ký (Register) một tài khoản mới bằng Email và Mật khẩu
 * sử dụng Firebase Authentication.
 */
public class RegisterEmailActivity extends AppCompatActivity {

    private static final String TAG = "RegisterEmailActivity";

    // --- Views ---
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar pbRegister;

    // --- Firebase ---
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_email);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ (Find) Views
        findViews();

        // Thiết lập Listeners
        setupListeners();
    }

    private void findViews() {
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);
        pbRegister = findViewById(R.id.pb_register);
    }

    private void setupListeners() {
        // Nút Đăng ký
        btnRegister.setOnClickListener(v -> attemptRegistration());

        // Nút Quay lại Đăng nhập
        tvGoToLogin.setOnClickListener(v -> {
            // Chỉ cần kết thúc (finish) Activity này
            // LoginActivity sẽ tự động xuất hiện (vì nó nằm dưới)
            finish();
        });
    }

    /**
     * Xử lý khi người dùng nhấn nút "Tạo tài khoản".
     */
    private void attemptRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // --- 1. Kiểm tra (Validate) dữ liệu đầu vào ---
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email không được để trống.");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ.");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mật khẩu không được để trống.");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự.");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu không khớp.");
            etConfirmPassword.requestFocus();
            return;
        }

        // --- 2. Gọi API Firebase ---
        setLoading(true); // Bật ProgressBar

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Đăng ký tài khoản THÀNH CÔNG
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        // Gửi email xác thực
                        sendVerificationEmail(user);
                    } else {
                        // Trường hợp hiếm gặp
                        setLoading(false);
                        Toast.makeText(this, "Tạo tài khoản thất bại (user null).", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Đăng ký tài khoản THẤT BẠI
                    setLoading(false);
                    Log.w(TAG, "createUserWithEmail:failure", e);
                    // Hiển thị lỗi (ví dụ: email đã tồn tại)
                    Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Gửi email xác thực đến người dùng.
     */
    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnSuccessListener(aVoid -> {
                    // Gửi email THÀNH CÔNG
                    setLoading(false);
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    // Gửi email THẤT BẠI
                    setLoading(false);
                    Log.w(TAG, "sendEmailVerification:failure", e);
                    Toast.makeText(this, "Đăng ký thành công, nhưng gửi email xác thực thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Hiển thị hộp thoại thông báo thành công và hướng dẫn người dùng.
     */
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng ký Thành công!")
                .setMessage("Một email xác thực đã được gửi đến địa chỉ email của bạn. Vui lòng kiểm tra hộp thư (kể cả spam) và xác thực tài khoản trước khi đăng nhập.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Đóng Activity Đăng ký và quay lại Đăng nhập
                    finish();
                })
                .setCancelable(false) // Không cho đóng dialog bằng cách nhấn bên ngoài
                .show();
    }

    /**
     * Bật/Tắt ProgressBar và ẩn/hiện form đăng ký.
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            pbRegister.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            etEmail.setEnabled(false);
            etPassword.setEnabled(false);
            etConfirmPassword.setEnabled(false);
        } else {
            pbRegister.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            etEmail.setEnabled(true);
            etPassword.setEnabled(true);
            etConfirmPassword.setEnabled(true);
        }
    }
}