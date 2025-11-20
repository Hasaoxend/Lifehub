package com.test.lifehub.features.three_settings.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.ui.LoginActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChangePasswordActivity extends AppCompatActivity {

    private TextInputEditText etOldPass, etNewPass, etConfirmPass;
    private TextInputLayout layoutOldPass, layoutNewPass, layoutConfirmPass;
    private Button btnUpdate;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Inject
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bảo mật màn hình
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_change_pass);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etOldPass = findViewById(R.id.et_old_pass);
        etNewPass = findViewById(R.id.et_new_pass);
        etConfirmPass = findViewById(R.id.et_confirm_pass);

        layoutOldPass = findViewById(R.id.layout_old_pass);
        layoutNewPass = findViewById(R.id.layout_new_pass);
        layoutConfirmPass = findViewById(R.id.layout_confirm_pass);

        btnUpdate = findViewById(R.id.btn_update_password);
        progressBar = findViewById(R.id.pb_loading);
    }

    private void setupListeners() {
        btnUpdate.setOnClickListener(v -> validateAndChangePassword());
    }

    private void validateAndChangePassword() {
        String oldPass = etOldPass.getText().toString().trim();
        String newPass = etNewPass.getText().toString().trim();
        String confirmPass = etConfirmPass.getText().toString().trim();

        // Reset lỗi
        layoutOldPass.setError(null);
        layoutNewPass.setError(null);
        layoutConfirmPass.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(oldPass)) {
            layoutOldPass.setError("Vui lòng nhập mật khẩu cũ");
            isValid = false;
        }

        if (TextUtils.isEmpty(newPass)) {
            layoutNewPass.setError("Vui lòng nhập mật khẩu mới");
            isValid = false;
        } else if (newPass.length() < 6) {
            layoutNewPass.setError("Mật khẩu phải từ 6 ký tự trở lên");
            isValid = false;
        } else if (newPass.equals(oldPass)) {
            layoutNewPass.setError("Mật khẩu mới không được trùng với mật khẩu cũ");
            isValid = false;
        }

        if (!newPass.equals(confirmPass)) {
            layoutConfirmPass.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        if (isValid) {
            updatePasswordFirebase(oldPass, newPass);
        }
    }

    private void updatePasswordFirebase(String oldPass, String newPass) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        setLoading(true);

        // BƯỚC 1: Xác thực lại (Re-authenticate) để đảm bảo Old Password đúng
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // BƯỚC 2: Nếu mật khẩu cũ đúng -> Tiến hành đổi
                user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    setLoading(false);
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
                        logoutAndRedirect();
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, "Lỗi đổi mật khẩu: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                setLoading(false);
                layoutOldPass.setError("Mật khẩu cũ không chính xác");
                Toast.makeText(ChangePasswordActivity.this, "Xác thực thất bại. Kiểm tra lại mật khẩu cũ.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutAndRedirect() {
        sessionManager.logoutUser();
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUpdate.setEnabled(!isLoading);
        etOldPass.setEnabled(!isLoading);
        etNewPass.setEnabled(!isLoading);
        etConfirmPass.setEnabled(!isLoading);
    }
}