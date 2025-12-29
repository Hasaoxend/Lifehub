package com.test.lifehub.features.masterpassword;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.test.lifehub.ui.MainActivity;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.security.PasscodeRateLimiter;
import com.test.lifehub.core.util.SessionManager;

import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MasterPasswordActivity extends AppCompatActivity {

    private static final String TAG = "MasterPasswordActivity";

    @Inject
    EncryptionManager encryptionManager;
    
    @Inject
    SessionManager sessionManager;

    @Inject
    PasscodeRateLimiter rateLimiter;
    
    private TextInputLayout tilMasterPassword;
    private TextInputEditText etMasterPassword;
    private MaterialButton btnUnlock;
    private View progressBar;

    // Lockout UI
    private View layoutLockout;
    private TextView tvLockoutMessage;
    private TextView tvCountdown;
    private CountDownTimer lockoutTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_password);
        
        initViews();
        setupUI();
        checkLockoutStatus();
    }

    private void initViews() {
        tilMasterPassword = findViewById(R.id.tilMasterPassword);
        etMasterPassword = findViewById(R.id.etMasterPassword);
        btnUnlock = findViewById(R.id.btnUnlock);
        progressBar = findViewById(R.id.progressBar);
        
        layoutLockout = findViewById(R.id.layout_lockout);
        tvLockoutMessage = findViewById(R.id.tvLockoutMessage);
        tvCountdown = findViewById(R.id.tvCountdown);
    }

    private void setupUI() {
        btnUnlock.setOnClickListener(v -> handleUnlock());
        
        etMasterPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilMasterPassword.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkLockoutStatus() {
        if (rateLimiter.isLocked()) {
            startLockoutCountdown(rateLimiter.getRemainingLockTimeSeconds());
        } else {
            layoutLockout.setVisibility(View.GONE);
            enableInputs(true);
        }
    }

    private void startLockoutCountdown(long seconds) {
        layoutLockout.setVisibility(View.VISIBLE);
        enableInputs(false);
        
        if (lockoutTimer != null) lockoutTimer.cancel();
        
        lockoutTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000;
                String time = String.format(Locale.getDefault(), "%02d:%02d", secs / 60, secs % 60);
                tvCountdown.setText(getString(R.string.lockout_countdown_format, time));
            }

            @Override
            public void onFinish() {
                layoutLockout.setVisibility(View.GONE);
                enableInputs(true);
            }
        }.start();
    }

    private void enableInputs(boolean enabled) {
        tilMasterPassword.setEnabled(enabled);
        btnUnlock.setEnabled(enabled);
    }


    private void handleUnlock() {
        if (rateLimiter.isLocked()) return;
        
        String pin = etMasterPassword.getText() != null ? etMasterPassword.getText().toString() : "";
        if (pin.length() != 6) {
            tilMasterPassword.setError(getString(R.string.error_pin_length));
            return;
        }

        processUnlock(pin);
    }

    private void processUnlock(String pin) {
        showLoading(true);
        encryptionManager.initialize(pin, result -> {
            showLoading(false);
            if (result == EncryptionManager.InitResult.SUCCESS) {
                rateLimiter.resetAttempts();
                if (sessionManager.isBiometricEnabled()) {
                    sessionManager.saveEncryptionPassword(pin);
                }
                navigateToMain();
            } else {
                rateLimiter.recordFailedAttempt();
                int remaining = rateLimiter.getRemainingAttempts();
                
                if (rateLimiter.isLocked()) {
                    startLockoutCountdown(rateLimiter.getRemainingLockTimeSeconds());
                } else {
                    tilMasterPassword.setError(getString(R.string.lockout_remaining_attempts, remaining));
                    etMasterPassword.setText("");
                }
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnUnlock.setEnabled(!show && !rateLimiter.isLocked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lockoutTimer != null) lockoutTimer.cancel();
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
