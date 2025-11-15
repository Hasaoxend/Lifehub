package com.test.lifehub.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.test.lifehub.R;

import java.util.regex.Pattern;

public class CreatePasswordActivity extends AppCompatActivity {

    private static final String TAG = "CreatePasswordActivity";

    // --- Views ---
    private TextView tvTitle, tvEmailDisplay, tvPasswordStrength; // tvTitle chính là tv_title_create_password
    private TextInputEditText etPassword, etConfirmPassword;
    private ProgressBar progressPasswordStrength, loadingProgressBar;
    private Button btnCreate;

    // --- Firebase & Logic ---
    private FirebaseAuth mAuth;
    private String mActionCode; // Token (oobCode) từ email link
    private int mPasswordScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password); // Tải file XML

        mAuth = FirebaseAuth.getInstance();

        // 1. Ánh xạ (Find) Views
        findViews(); // Gọi hàm này

        // 2. Xử lý Deep Link đến
        handleDeepLink(getIntent());

        // 3. Thiết lập Listeners
        setupListeners();
    }

    private void findViews() {
        // Dòng này tìm ID "tv_title_create_password" trong XML
        tvTitle = findViewById(R.id.tv_title_create_password);

        // Các ID khác
        tvEmailDisplay = findViewById(R.id.tv_email_display);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressPasswordStrength = findViewById(R.id.progressPasswordStrength);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        btnCreate = findViewById(R.id.btnCreate);
    }

    /**
     * Lấy và xác thực 'oobCode' (token) từ Deep Link.
     */
    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            invalidLinkError();
            return;
        }

        Uri data = intent.getData();
        mActionCode = data.getQueryParameter("oobCode");

        if (mActionCode == null || mActionCode.isEmpty()) {
            invalidLinkError();
            return;
        }

        setLoading(true);
        mAuth.verifyPasswordResetCode(mActionCode)
                .addOnSuccessListener(email -> {
                    setLoading(false);
                    tvEmailDisplay.setText("Email: " + email);
                    tvTitle.setText("Tạo Mật khẩu");
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    invalidLinkError(e.getMessage());
                });
    }

    /**
     * Thiết lập các sự kiện lắng nghe (TextWatcher, OnClick)
     */
    private void setupListeners() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordStrength(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCreate.setOnClickListener(v -> attemptPasswordCreation());
    }

    /**
     * Kiểm tra độ mạnh của mật khẩu và cập nhật UI.
     */
    private void checkPasswordStrength(String password) {
        mPasswordScore = 0;

        if (password.length() >= 8) mPasswordScore++;
        if (Pattern.compile("[a-z]").matcher(password).find()) mPasswordScore++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) mPasswordScore++;
        if (Pattern.compile("[0-9]").matcher(password).find()) mPasswordScore++;
        if (Pattern.compile("[!@#$%^&*()_+\\-=|?.,]").matcher(password).find()) mPasswordScore++;

        progressPasswordStrength.setProgress(mPasswordScore);

        switch (mPasswordScore) {
            case 0:
            case 1:
            case 2:
                tvPasswordStrength.setText("Độ mạnh: Yếu");
                tvPasswordStrength.setTextColor(Color.RED);
                btnCreate.setEnabled(false);
                break;
            case 3:
                tvPasswordStrength.setText("Độ mạnh: Trung bình");
                tvPasswordStrength.setTextColor(Color.BLUE);
                btnCreate.setEnabled(true);
                break;
            case 4:
            case 5:
                tvPasswordStrength.setText("Độ mạnh: Mạnh");
                tvPasswordStrength.setTextColor(Color.GREEN);
                btnCreate.setEnabled(true);
                break;
        }
    }

    /**
     * Xử lý khi nhấn nút "Hoàn tất".
     */
    private void attemptPasswordCreation() {
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (mActionCode == null) {
            invalidLinkError("Không tìm thấy mã xác thực.");
            return;
        }
        if (mPasswordScore < 3) {
            Toast.makeText(this, "Mật khẩu quá yếu!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu không khớp!");
            return;
        }

        setLoading(true);
        mAuth.confirmPasswordReset(mActionCode, password)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(CreatePasswordActivity.this,
                            "Tạo tài khoản thành công! Vui lòng đăng nhập.",
                            Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(CreatePasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "confirmPasswordReset:failure", e);
                    Toast.makeText(CreatePasswordActivity.this,
                            "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void invalidLinkError(String... message) {
        String error = (message.length > 0) ? message[0] : "Đường link không hợp lệ, đã hết hạn, hoặc đã được sử dụng.";
        tvTitle.setText("Lỗi Liên kết");
        tvEmailDisplay.setText(error);
        tvEmailDisplay.setTextColor(Color.RED);
        etPassword.setEnabled(false);
        etConfirmPassword.setEnabled(false);
        btnCreate.setEnabled(false);
    }

    private void setLoading(boolean isLoading) {
        loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreate.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }
}