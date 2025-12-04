package com.test.lifehub.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
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
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.features.three_settings.ui.ChangePasswordActivity;

import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_WEAK_PASSWORD_CHECKED = "weak_password_checked_";
    
    // Password validation patterns
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    private Toolbar mToolbar;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private ProgressBar pbLogin;
    private LinearLayout mLayoutBiometric;
    private LoginViewModel mViewModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- BẢO MẬT ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        // ----------------

        setContentView(R.layout.activity_login);

        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        findViews();
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.setNavigationOnClickListener(v -> finish());

        setupListeners();
        observeLoginState();
        observeInitialCheck();

        if (savedInstanceState == null) {
            setLoading(true);
            mViewModel.performInitialCheck();
        }
    }

    // (Giữ nguyên phần code còn lại của LoginActivity như file gốc bạn gửi)
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
        btnLogin.setOnClickListener(v -> mViewModel.attemptManualLogin(etEmail.getText().toString().trim(), etPassword.getText().toString().trim()));
        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterEmailActivity.class)));
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        mLayoutBiometric.setOnClickListener(v -> { setLoading(true); BiometricHelper.showBiometricPrompt(this, this); });
    }

    private void observeInitialCheck() {
        mViewModel.initialState.observe(this, state -> {
            if(state==null) return;
            setLoading(false);
            if(state == LoginViewModel.InitialCheckState.SHOW_BIOMETRIC_PROMPT) mLayoutBiometric.setVisibility(View.VISIBLE);
            else if(state == LoginViewModel.InitialCheckState.NAVIGATE_TO_MAIN) navigateToMain();
        });
    }

    private void observeLoginState() {
        mViewModel.loginState.observe(this, state -> {
            if(state==null) return;
            if(state == LoginViewModel.LoginState.LOADING) setLoading(true);
            else if(state == LoginViewModel.LoginState.SUCCESS) { 
                setLoading(false); 
                checkPasswordStrengthBeforeNavigate();
            }
            else { setLoading(false); Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show(); }
        });
    }
    
    private void checkPasswordStrengthBeforeNavigate() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToMain();
            return;
        }
        
        // Check if already prompted for this user
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String key = KEY_WEAK_PASSWORD_CHECKED + user.getUid();
        
        if (prefs.getBoolean(key, false)) {
            // Already checked, proceed
            navigateToMain();
            return;
        }
        
        // Get entered password to validate
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        
        if (isPasswordStrong(password)) {
            // Strong password, mark as checked and proceed
            prefs.edit().putBoolean(key, true).apply();
            navigateToMain();
        } else {
            // Weak password detected
            showWeakPasswordDialog();
        }
    }
    
    private boolean isPasswordStrong(String password) {
        if (TextUtils.isEmpty(password) || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasNumber = NUMBER_PATTERN.matcher(password).find();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).find();
        
        return hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
    }
    
    private void showWeakPasswordDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.title_weak_password_detected)
            .setMessage(R.string.msg_weak_password_detected)
            .setCancelable(false)
            .setPositiveButton(R.string.btn_change_password_now, (dialog, which) -> {
                // Navigate to ChangePasswordActivity
                Intent intent = new Intent(this, ChangePasswordActivity.class);
                startActivity(intent);
                finish();
            })
            .setNegativeButton(R.string.btn_remind_later, (dialog, which) -> {
                // Mark as checked to avoid repeated prompts this session
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    prefs.edit().putBoolean(KEY_WEAK_PASSWORD_CHECKED + user.getUid(), true).apply();
                }
                navigateToMain();
            })
            .show();
    }

    private void showForgotPasswordDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        final TextInputEditText etEmailInput = dialogView.findViewById(R.id.et_dialog_email);

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_password_recovery)
                .setMessage(R.string.msg_password_recovery)
                .setView(dialogView)
                .setPositiveButton(R.string.send, (dialog, which) -> {
                    String email = etEmailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, R.string.error_email_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, R.string.error_email_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Send password reset email (sẽ redirect về ResetPasswordActivity)
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(aVoid -> {
                                new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle(R.string.title_email_sent)
                                    .setMessage(getString(R.string.msg_email_sent_check))
                                    .setPositiveButton("OK", null)
                                    .show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error_with_message, e.getMessage()), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        pbLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }

    @Override
    public void onBiometricAuthSuccess() { navigateToMain(); }
    @Override
    public void onBiometricAuthError(String errorMessage) { setLoading(false); Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show(); }
    @Override
    public void onBiometricAuthFailed() { }
}