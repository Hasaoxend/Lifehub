package com.test.lifehub.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
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
public class ChangePasscodeActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPin, etNewPin, etConfirmNewPin;
    private TextInputLayout tilCurrentPin, tilNewPin, tilConfirmNewPin;
    private Button btnChangePin;
    private ProgressBar pbChangePin;
    private Toolbar toolbar;

    @Inject
    EncryptionManager encryptionManager;

    @Inject
    CrossPlatformEncryptionHelper crossPlatformHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_change_passcode);

        findViews();
        setupToolbar();
        setupListeners();
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar_change_passcode);
        etCurrentPin = findViewById(R.id.et_current_pin);
        etNewPin = findViewById(R.id.et_new_pin);
        etConfirmNewPin = findViewById(R.id.et_confirm_new_pin);
        tilCurrentPin = findViewById(R.id.layout_current_pin);
        tilNewPin = findViewById(R.id.layout_new_pin);
        tilConfirmNewPin = findViewById(R.id.layout_confirm_new_pin);
        btnChangePin = findViewById(R.id.btn_change_pin);
        pbChangePin = findViewById(R.id.pb_change_pin);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        btnChangePin.setOnClickListener(v -> handleChangePin());
    }

    private void handleChangePin() {
        String currentPin = etCurrentPin.getText().toString().trim();
        String newPin = etNewPin.getText().toString().trim();
        String confirmPin = etConfirmNewPin.getText().toString().trim();

        tilCurrentPin.setError(null);
        tilNewPin.setError(null);
        tilConfirmNewPin.setError(null);

        if (currentPin.length() != 6) {
            tilCurrentPin.setError(getString(R.string.error_pin_length));
            return;
        }

        if (newPin.length() != 6) {
            tilNewPin.setError(getString(R.string.error_pin_length));
            return;
        }

        if (!newPin.equals(confirmPin)) {
            tilConfirmNewPin.setError(getString(R.string.error_pin_mismatch));
            return;
        }

        performChange(currentPin, newPin);
    }

    private void performChange(String currentPin, String newPin) {
        setLoading(true);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // 1. Verify current PIN using initialize call (synchronously here)
                // We use crossPlatformHelper directly to verify against current Firestore state
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                com.google.firebase.firestore.DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(user.getUid()).get());
                
                String currentVerification = snapshot.getString("encryptionVerification");
                String currentSalt = snapshot.getString("encryptionSalt");

                if (currentSalt == null || currentVerification == null) {
                    throw new Exception("Encryption not set up");
                }

                crossPlatformHelper.setSaltFromBase64(currentSalt);
                boolean isValid = crossPlatformHelper.verifyMasterPassword(currentPin, currentVerification);

                if (!isValid) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        tilCurrentPin.setError(getString(R.string.error_passcode_incorrect));
                    });
                    return;
                }

                // 2. Generate new salt and initialize for new PIN
                boolean success = crossPlatformHelper.initializeWithMasterPassword(newPin, true);
                if (!success) throw new Exception("Failed to re-initialize encryption");

                // 3. Prepare data for Firestore update
                String newSalt = crossPlatformHelper.getSaltBase64();
                String newVerification = crossPlatformHelper.encrypt("LIFEHUB_VERIFY");

                Map<String, Object> data = new HashMap<>();
                data.put("encryptionSalt", newSalt);
                data.put("encryptionVerification", newVerification);
                data.put("encryptionVersion", 2);

                Tasks.await(db.collection("users").document(user.getUid()).set(data, SetOptions.merge()));

                // 4. Update core EncryptionManager with new state
                encryptionManager.initialize(newPin, result -> {
                    runOnUiThread(() -> {
                        setLoading(false);
                        if (result == EncryptionManager.InitResult.SUCCESS) {
                            Toast.makeText(this, getString(R.string.msg_passcode_changed_success), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Changed in cloud but local update failed", Toast.LENGTH_LONG).show();
                        }
                    });
                });

            } catch (Exception e) {
                android.util.Log.e("ChangePasscode", "Failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        runOnUiThread(() -> {
            pbChangePin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnChangePin.setEnabled(!isLoading);
            etCurrentPin.setEnabled(!isLoading);
            etNewPin.setEnabled(!isLoading);
            etConfirmNewPin.setEnabled(!isLoading);
        });
    }
}
