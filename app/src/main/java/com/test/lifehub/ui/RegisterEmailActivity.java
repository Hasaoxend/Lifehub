package com.test.lifehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.test.lifehub.R;

/**
 * Activity Bước 1: Nhập Email
 * Kiểm tra email hợp lệ và chưa được sử dụng
 */
public class RegisterEmailActivity extends AppCompatActivity {

    private static final String TAG = "RegisterEmailActivity";

    // --- Views ---
    private TextInputLayout layoutEmail;
    private TextInputEditText etEmail;
    private Button btnContinue;
    private TextView tvGoToLogin;
    private ProgressBar pbLoading;

    // --- Firebase ---
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_email_step1);

        mAuth = FirebaseAuth.getInstance();

        findViews();
        setupListeners();
    }

    private void findViews() {
        layoutEmail = findViewById(R.id.layout_email);
        etEmail = findViewById(R.id.et_email);
        btnContinue = findViewById(R.id.btn_continue);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);
        pbLoading = findViewById(R.id.pb_loading);
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> validateAndContinue());

        tvGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterEmailActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void validateAndContinue() {
        String email = etEmail.getText().toString().trim();

        // Xóa lỗi cũ
        layoutEmail.setError(null);

        // 1. Kiểm tra email trống
        if (TextUtils.isEmpty(email)) {
            layoutEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        // 2. Kiểm tra định dạng email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Email không hợp lệ");
            etEmail.requestFocus();
            return;
        }

        // 3. Kiểm tra email đã tồn tại chưa
        checkEmailAvailability(email);
    }

    private void checkEmailAvailability(String email) {
        setLoading(true);

        // Sử dụng fetchSignInMethodsForEmail để kiểm tra email đã tồn tại
        mAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    setLoading(false);

                    if (result.getSignInMethods() != null && !result.getSignInMethods().isEmpty()) {
                        // Email đã tồn tại
                        layoutEmail.setError("Email này đã được sử dụng");
                        etEmail.requestFocus();
                    } else {
                        // Email chưa tồn tại, chuyển sang bước 2
                        Intent intent = new Intent(RegisterEmailActivity.this, RegisterPasswordActivity.class);
                        intent.putExtra("EMAIL", email);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    layoutEmail.setError("Lỗi kiểm tra email: " + e.getMessage());
                });
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            pbLoading.setVisibility(View.VISIBLE);
            btnContinue.setEnabled(false);
            etEmail.setEnabled(false);
        } else {
            pbLoading.setVisibility(View.GONE);
            btnContinue.setEnabled(true);
            etEmail.setEnabled(true);
        }
    }
}