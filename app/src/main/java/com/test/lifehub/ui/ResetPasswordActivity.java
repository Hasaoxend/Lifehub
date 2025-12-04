package com.test.lifehub.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthActionCodeException;
import com.test.lifehub.R;

import java.util.regex.Pattern;

/**
 * Custom Password Reset Activity
 * Thay thế Firebase default web page với validation trong app
 */
public class ResetPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ResetPasswordActivity";

    // Validation patterns
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    // Views
    private Toolbar toolbar;
    private TextView tvEmailDisplay;
    private TextInputLayout layoutNewPassword, layoutConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private ImageView iconLength, iconUppercase, iconLowercase, iconNumber, iconSpecial;
    private Button btnResetPassword;
    private ProgressBar pbLoading;

    // Data
    private String mEmail;
    private String mOobCode; // Action code từ reset email link
    private FirebaseAuth mAuth;

    // Password validation flags
    private boolean hasMinLength = false;
    private boolean hasUppercase = false;
    private boolean hasLowercase = false;
    private boolean hasNumber = false;
    private boolean hasSpecialChar = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        // Handle deep link từ Firebase reset email
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null) {
            // Parse query parameters từ Firebase link
            // Format: https://lifehub-app.firebaseapp.com/__/auth/action?mode=resetPassword&oobCode=ABC123&apiKey=...
            String mode = data.getQueryParameter("mode");
            mOobCode = data.getQueryParameter("oobCode");
            
            if (!"resetPassword".equals(mode) || TextUtils.isEmpty(mOobCode)) {
                Toast.makeText(this, R.string.error_invalid_reset_link, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            // Verify OOB code and get email
            mAuth.verifyPasswordResetCode(mOobCode)
                .addOnSuccessListener(email -> {
                    mEmail = email;
                    if (!TextUtils.isEmpty(mEmail) && tvEmailDisplay != null) {
                        tvEmailDisplay.setText(mEmail);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.error_reset_link_expired, Toast.LENGTH_SHORT).show();
                    finish();
                });
        } else {
            // Fallback: Có thể được gọi trực tiếp với extras
            mEmail = intent.getStringExtra("EMAIL");
            mOobCode = intent.getStringExtra("OOB_CODE");
            
            if (TextUtils.isEmpty(mOobCode)) {
                Toast.makeText(this, R.string.error_invalid_reset_link, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        findViews();
        setupToolbar();
        setupListeners();

        if (!TextUtils.isEmpty(mEmail) && tvEmailDisplay != null) {
            tvEmailDisplay.setText(mEmail);
        }
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar_reset_password);
        tvEmailDisplay = findViewById(R.id.tv_email_display);
        layoutNewPassword = findViewById(R.id.layout_new_password);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        iconLength = findViewById(R.id.icon_length);
        iconUppercase = findViewById(R.id.icon_uppercase);
        iconLowercase = findViewById(R.id.icon_lowercase);
        iconNumber = findViewById(R.id.icon_number);
        iconSpecial = findViewById(R.id.icon_special);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        pbLoading = findViewById(R.id.pb_loading);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
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

        // Confirm password validation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = getText(etNewPassword);
                String confirm = s.toString();
                
                if (!TextUtils.isEmpty(confirm)) {
                    if (!confirm.equals(password)) {
                        layoutConfirmPassword.setError(getString(R.string.error_password_mismatch));
                    } else {
                        layoutConfirmPassword.setError(null);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnResetPassword.setOnClickListener(v -> attemptPasswordReset());
    }

    private void updatePasswordRequirements(String password) {
        layoutNewPassword.setError(null);

        // Check min length
        hasMinLength = password.length() >= MIN_PASSWORD_LENGTH;
        updateIndicator(iconLength, hasMinLength);

        // Check uppercase
        hasUppercase = UPPERCASE_PATTERN.matcher(password).find();
        updateIndicator(iconUppercase, hasUppercase);

        // Check lowercase
        hasLowercase = LOWERCASE_PATTERN.matcher(password).find();
        updateIndicator(iconLowercase, hasLowercase);

        // Check number
        hasNumber = NUMBER_PATTERN.matcher(password).find();
        updateIndicator(iconNumber, hasNumber);

        // Check special character
        hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).find();
        updateIndicator(iconSpecial, hasSpecialChar);

        // Enable button if all requirements met
        boolean allRequirementsMet = hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
        btnResetPassword.setEnabled(allRequirementsMet);
    }

    private void updateIndicator(ImageView icon, boolean isValid) {
        int color = isValid ? 
            ContextCompat.getColor(this, R.color.success_green) : 
            ContextCompat.getColor(this, R.color.text_disabled);
        icon.setColorFilter(color);
    }

    private void attemptPasswordReset() {
        String newPassword = getText(etNewPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Final validation
        if (!isPasswordValid(newPassword)) {
            Toast.makeText(this, R.string.error_password_not_strong, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            layoutConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return;
        }

        if (WHITESPACE_PATTERN.matcher(newPassword).find()) {
            layoutNewPassword.setError(getString(R.string.error_password_no_spaces));
            return;
        }

        setLoading(true);

        // Confirm password reset với oobCode
        mAuth.confirmPasswordReset(mOobCode, newPassword)
            .addOnSuccessListener(aVoid -> {
                setLoading(false);
                showSuccessDialog();
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                Log.e(TAG, "Password reset failed", e);
                
                String errorMessage;
                if (e instanceof FirebaseAuthActionCodeException) {
                    errorMessage = getString(R.string.error_reset_link_expired);
                } else {
                    errorMessage = getString(R.string.error_with_message, e.getMessage());
                }
                
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            });
    }

    private boolean isPasswordValid(String password) {
        return !TextUtils.isEmpty(password)
                && hasMinLength
                && hasUppercase
                && hasLowercase
                && hasNumber
                && hasSpecialChar
                && !WHITESPACE_PATTERN.matcher(password).find();
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.reset_password_success_title)
            .setMessage(R.string.reset_password_success_message)
            .setPositiveButton("OK", (dialog, which) -> navigateToLogin())
            .setCancelable(false)
            .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (!TextUtils.isEmpty(mEmail)) {
            intent.putExtra("EMAIL", mEmail);
        }
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!isLoading);
        etNewPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
