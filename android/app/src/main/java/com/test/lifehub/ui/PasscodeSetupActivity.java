package com.test.lifehub.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.Tasks;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.test.lifehub.R;
import com.test.lifehub.core.security.CrossPlatformEncryptionHelper;
import com.test.lifehub.core.security.EncryptionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PasscodeSetupActivity extends AppCompatActivity {

    private static final String TAG = "PasscodeSetupActivity";

    private View layoutStepPin, layoutStepRecovery;
    private TextInputEditText etPin, etConfirmPin;
    private TextInputLayout tilPin, tilConfirmPin;
    private Button btnNextToRecovery, btnCopyRecovery, btnFinishSetup;
    private TextView tvRecoveryCode;
    private ProgressBar pbSetup;
    private Toolbar toolbar;

    private String generatedRecoveryCode;
    private String validatedPin;

    @Inject
    EncryptionManager encryptionManager;
    
    @Inject
    CrossPlatformEncryptionHelper crossPlatformHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_passcode_setup);

        findViews();
        setupToolbar();
        setupListeners();
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar_passcode_setup);
        layoutStepPin = findViewById(R.id.layout_step_pin);
        layoutStepRecovery = findViewById(R.id.layout_step_recovery);
        etPin = findViewById(R.id.et_pin);
        etConfirmPin = findViewById(R.id.et_confirm_pin);
        tilPin = findViewById(R.id.layout_pin);
        tilConfirmPin = findViewById(R.id.layout_confirm_pin);
        btnNextToRecovery = findViewById(R.id.btn_next_to_recovery);
        btnCopyRecovery = findViewById(R.id.btn_copy_recovery);
        btnFinishSetup = findViewById(R.id.btn_finish_setup);
        tvRecoveryCode = findViewById(R.id.tv_recovery_code);
        pbSetup = findViewById(R.id.pb_setup);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Force setup, no back
        }
    }

    private void setupListeners() {
        btnNextToRecovery.setOnClickListener(v -> handlePinSubmit());
        btnCopyRecovery.setOnClickListener(v -> copyRecoveryCode());
        btnFinishSetup.setOnClickListener(v -> finishSetup());
    }

    private void handlePinSubmit() {
        String pin = etPin.getText().toString().trim();
        String confirmPin = etConfirmPin.getText().toString().trim();

        tilPin.setError(null);
        tilConfirmPin.setError(null);

        if (pin.length() != 6) {
            tilPin.setError(getString(R.string.error_pin_length));
            return;
        }

        if (!pin.equals(confirmPin)) {
            tilConfirmPin.setError(getString(R.string.error_pin_mismatch));
            return;
        }

        this.validatedPin = pin;
        startSetupFlow(pin);
    }

    private void startSetupFlow(String pin) {
        setLoading(true);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Initialize encryption with the new PIN
                boolean success = crossPlatformHelper.initializeWithMasterPassword(pin, true);
                if (!success) throw new Exception("Failed to initialize encryption");

                // 2. Generate recovery code
                generatedRecoveryCode = crossPlatformHelper.generateRecoveryCode();
                
                // 3. Prepare data for Firestore
                String salt = crossPlatformHelper.getSaltBase64();
                String verification = crossPlatformHelper.encrypt("LIFEHUB_VERIFY");
                
                // We store the first 8 chars of recovery code as a hint (same as web)
                String recoveryHint = generatedRecoveryCode.substring(0, 8);

                Map<String, Object> data = new HashMap<>();
                data.put("encryptionSalt", salt);
                data.put("encryptionVerification", verification);
                data.put("recoveryCodePreview", recoveryHint); // Matching web logic
                data.put("encryptionVersion", 2);

                Tasks.await(FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .set(data, SetOptions.merge()));

                runOnUiThread(() -> {
                    setLoading(false);
                    showRecoveryStep();
                });

            } catch (Exception e) {
                Log.e(TAG, "Setup failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Setup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showRecoveryStep() {
        layoutStepPin.setVisibility(View.GONE);
        layoutStepRecovery.setVisibility(View.VISIBLE);
        tvRecoveryCode.setText(generatedRecoveryCode);
    }

    private void copyRecoveryCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("LifeHub Recovery Code", generatedRecoveryCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getString(R.string.clipboard_copy_success), Toast.LENGTH_SHORT).show();
    }

    private void finishSetup() {
        // Double check setup is valid
        if (encryptionManager.isUnlocked()) {
            navigateToMain();
        } else {
            // Re-initialize manager if needed
            encryptionManager.initialize(validatedPin, result -> {
                if (result == EncryptionManager.InitResult.SUCCESS) {
                    navigateToMain();
                } else {
                    Toast.makeText(this, "Encryption failed to initialize", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void navigateToMain() {
        Toast.makeText(this, getString(R.string.passcode_setup_success), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        pbSetup.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnNextToRecovery.setEnabled(!isLoading);
        etPin.setEnabled(!isLoading);
        etConfirmPin.setEnabled(!isLoading);
    }
}
