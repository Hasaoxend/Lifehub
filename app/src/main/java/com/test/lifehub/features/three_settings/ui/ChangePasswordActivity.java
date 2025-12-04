package com.test.lifehub.features.three_settings.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;
import com.test.lifehub.features.four_calendar.repository.CalendarRepository;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;
import com.test.lifehub.ui.LoginActivity;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChangePasswordActivity extends AppCompatActivity {

    // Step 1 views
    private View step1Layout;
    private TextInputEditText etCurrentPassword;
    private TextInputLayout layoutCurrentPassword;
    private Button btnVerifyPassword;
    private TextView tvForgotPassword;
    private ProgressBar pbLoadingStep1;

    // Step 2 views
    private View step2Layout;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private TextInputLayout layoutNewPassword, layoutConfirmPassword;
    private Button btnUpdatePassword;
    private ProgressBar pbLoadingStep2;
    private TextView tvReqLength, tvReqUppercase, tvReqLowercase, tvReqDigit, tvReqSpecial;

    private FirebaseAuth mAuth;
    private String verifiedOldPassword;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bảo mật màn hình
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mAuth = FirebaseAuth.getInstance();

        // Start with Step 1
        setContentView(R.layout.activity_change_password_step1);
        initStep1Views();
        setupStep1Listeners();
    }

    private void initStep1Views() {
        Toolbar toolbar = findViewById(R.id.toolbar_change_pass_step1);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etCurrentPassword = findViewById(R.id.et_current_password);
        layoutCurrentPassword = findViewById(R.id.layout_current_password);
        btnVerifyPassword = findViewById(R.id.btn_verify_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        pbLoadingStep1 = findViewById(R.id.pb_loading);
    }

    private void setupStep1Listeners() {
        btnVerifyPassword.setOnClickListener(v -> verifyCurrentPassword());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void verifyCurrentPassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();

        layoutCurrentPassword.setError(null);

        if (TextUtils.isEmpty(currentPass)) {
            layoutCurrentPassword.setError(getString(R.string.change_password_current_required));
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, R.string.change_password_user_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingStep1(true);

        // Re-authenticate to verify current password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            setLoadingStep1(false);
            if (task.isSuccessful()) {
                // Password verified, save it and move to step 2
                verifiedOldPassword = currentPass;
                moveToStep2();
            } else {
                layoutCurrentPassword.setError(getString(R.string.change_password_current_incorrect));
                Toast.makeText(ChangePasswordActivity.this, R.string.change_password_auth_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showForgotPasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, R.string.change_password_email_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = user.getEmail();

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_forgot_password)
            .setMessage(getString(R.string.msg_forgot_password, userEmail))
            .setPositiveButton("Gửi email", (dialog, which) -> sendPasswordResetEmail(userEmail))
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void sendPasswordResetEmail(String email) {
        setLoadingStep1(true);

        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                setLoadingStep1(false);
                if (task.isSuccessful()) {
                    new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.title_email_sent)
                        .setMessage(R.string.msg_email_sent_check)
                        .setPositiveButton("Đăng xuất ngay", (d, w) -> logoutAndRedirect())
                        .setNegativeButton("Để sau", (d, w) -> finish())
                        .setCancelable(false)
                        .show();
                } else {
                    Toast.makeText(this, getString(R.string.change_password_email_error, task.getException().getMessage()), Toast.LENGTH_LONG).show();
                }
            });
    }

    private void moveToStep2() {
        setContentView(R.layout.activity_change_password_step2);
        initStep2Views();
        setupStep2Listeners();
    }

    private void initStep2Views() {
        Toolbar toolbar = findViewById(R.id.toolbar_change_pass_step2);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            // Go back to step 1
            setContentView(R.layout.activity_change_password_step1);
            initStep1Views();
            setupStep1Listeners();
        });

        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        layoutNewPassword = findViewById(R.id.layout_new_password);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        btnUpdatePassword = findViewById(R.id.btn_update_password);
        pbLoadingStep2 = findViewById(R.id.pb_loading);

        tvReqLength = findViewById(R.id.tv_requirement_length);
        tvReqUppercase = findViewById(R.id.tv_requirement_uppercase);
        tvReqLowercase = findViewById(R.id.tv_requirement_lowercase);
        tvReqDigit = findViewById(R.id.tv_requirement_digit);
        tvReqSpecial = findViewById(R.id.tv_requirement_special);
    }

    private void setupStep2Listeners() {
        btnUpdatePassword.setOnClickListener(v -> validateAndChangePassword());

        // Real-time password validation
        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordRequirements(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordRequirements(String password) {
        // Length
        if (password.length() >= 8) {
            tvReqLength.setTextColor(getColor(R.color.status_success));
            tvReqLength.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        } else {
            tvReqLength.setTextColor(getColor(R.color.text_secondary));
            tvReqLength.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        // Uppercase
        if (password.matches(".*[A-Z].*")) {
            tvReqUppercase.setTextColor(getColor(R.color.status_success));
            tvReqUppercase.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        } else {
            tvReqUppercase.setTextColor(getColor(R.color.text_secondary));
            tvReqUppercase.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        // Lowercase
        if (password.matches(".*[a-z].*")) {
            tvReqLowercase.setTextColor(getColor(R.color.status_success));
            tvReqLowercase.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        } else {
            tvReqLowercase.setTextColor(getColor(R.color.text_secondary));
            tvReqLowercase.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        // Digit
        if (password.matches(".*\\d.*")) {
            tvReqDigit.setTextColor(getColor(R.color.status_success));
            tvReqDigit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        } else {
            tvReqDigit.setTextColor(getColor(R.color.text_secondary));
            tvReqDigit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        // Special character
        if (password.matches(".*[@#$%^&+=!].*")) {
            tvReqSpecial.setTextColor(getColor(R.color.status_success));
            tvReqSpecial.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_circle, 0, 0, 0);
        } else {
            tvReqSpecial.setTextColor(getColor(R.color.text_secondary));
            tvReqSpecial.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void validateAndChangePassword() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        // Reset errors
        layoutNewPassword.setError(null);
        layoutConfirmPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(newPass)) {
            layoutNewPassword.setError(getString(R.string.change_password_new_required));
            isValid = false;
        } else if (newPass.length() < 8) {
            layoutNewPassword.setError(getString(R.string.change_password_min_length));
            isValid = false;
        } else if (!newPass.matches(".*[A-Z].*")) {
            layoutNewPassword.setError(getString(R.string.change_password_need_uppercase));
            isValid = false;
        } else if (!newPass.matches(".*[a-z].*")) {
            layoutNewPassword.setError(getString(R.string.change_password_need_lowercase));
            isValid = false;
        } else if (!newPass.matches(".*\\d.*")) {
            layoutNewPassword.setError(getString(R.string.change_password_need_number));
            isValid = false;
        } else if (!newPass.matches(".*[@#$%^&+=!].*")) {
            layoutNewPassword.setError(getString(R.string.change_password_need_special));
            isValid = false;
        } else if (newPass.equals(verifiedOldPassword)) {
            layoutNewPassword.setError(getString(R.string.change_password_same_as_old));
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPass)) {
            layoutConfirmPassword.setError(getString(R.string.change_password_confirm_required));
            isValid = false;
        } else if (!newPass.equals(confirmPass)) {
            layoutConfirmPassword.setError(getString(R.string.change_password_confirm_mismatch));
            isValid = false;
        }

        if (isValid) {
            updatePasswordFirebase(newPass);
        }
    }

    private void updatePasswordFirebase(String newPass) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        setLoadingStep2(true);

        user.updatePassword(newPass).addOnCompleteListener(task -> {
            setLoadingStep2(false);
            if (task.isSuccessful()) {
                new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.title_success)
                    .setMessage(R.string.msg_password_updated)
                    .setPositiveButton("Đăng nhập lại", (d, w) -> logoutAndRedirect())
                    .setCancelable(false)
                    .show();
            } else {
                Toast.makeText(ChangePasswordActivity.this, getString(R.string.change_password_update_error, task.getException().getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logoutAndRedirect() {
        // ✅ BẢO MẬT: Stop ALL Firestore listeners để tránh data leak
        totpRepository.stopListening();
        accountRepository.stopListening();
        calendarRepository.stopListening();
        productivityRepository.stopListening();
        
        sessionManager.logoutUser();
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoadingStep1(boolean isLoading) {
        pbLoadingStep1.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnVerifyPassword.setEnabled(!isLoading);
        etCurrentPassword.setEnabled(!isLoading);
        tvForgotPassword.setEnabled(!isLoading);
    }

    private void setLoadingStep2(boolean isLoading) {
        pbLoadingStep2.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUpdatePassword.setEnabled(!isLoading);
        etNewPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }
}