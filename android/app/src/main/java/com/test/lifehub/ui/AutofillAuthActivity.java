package com.test.lifehub.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.util.SessionManager;
import android.view.WindowManager;

import java.util.concurrent.Executor;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * AutofillAuthActivity - Màn hình xác thực trước khi Autofill
 * 
 * === MỤC ĐÍCH ===
 * Hiển thị thông tin account và cho phép user chọn:
 * - Điền chỉ Username
 * - Điền chỉ Password
 * - Điền tất cả
 * 
 * === WORKFLOW ===
 * 1. Nhận thông tin account từ Intent
 * 2. Hiển thị UI với account info
 * 3. User chọn option (username/password/all)
 * 4. Yêu cầu Biometric authentication
 * 5. Nếu thành công → Return autofill data
 * 
 * === GIỐNG iOS ===
 * UI tương tự iOS Keychain autofill popup
 */
@AndroidEntryPoint
@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillAuthActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "AutofillAuth";
    
    // Intent Extras
    public static final String EXTRA_ACCOUNT_ID = "account_id";
    public static final String EXTRA_SERVICE_NAME = "service_name";
    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_PASSWORD_ENCRYPTED = "password_encrypted";
    public static final String EXTRA_USERNAME_AUTOFILL_ID = "username_autofill_id";
    public static final String EXTRA_PASSWORD_AUTOFILL_ID = "password_autofill_id";
    
    // Fill Mode
    public static final int FILL_MODE_ALL = 0;
    public static final int FILL_MODE_USERNAME_ONLY = 1;
    public static final int FILL_MODE_PASSWORD_ONLY = 2;
    
    // Views
    private TextView tvServiceName;
    private TextView tvUsername;
    private TextView tvPasswordMask;
    private Button btnFillAll;
    private Button btnFillUsername;
    private Button btnFillPassword;
    private ImageView btnClose;
    
    // Data
    private String accountId;
    private String serviceName;
    private String username;
    private String passwordEncrypted;
    private AutofillId usernameAutofillId;
    private AutofillId passwordAutofillId;
    private int selectedFillMode = FILL_MODE_ALL;
    
    @Inject
    EncryptionManager encryptionManager;
    
    @Inject
    SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- BẢO MẬT: Chống chụp màn hình ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        // ---------------------------------
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofill_auth);
        
        // Parse intent
        parseIntent();
        
        // Init views
        initViews();
        
        // Setup UI
        setupUI();
        
        // Setup listeners
        setupListeners();
    }
    
    private void parseIntent() {
        Intent intent = getIntent();
        accountId = intent.getStringExtra(EXTRA_ACCOUNT_ID);
        serviceName = intent.getStringExtra(EXTRA_SERVICE_NAME);
        username = intent.getStringExtra(EXTRA_USERNAME);
        passwordEncrypted = intent.getStringExtra(EXTRA_PASSWORD_ENCRYPTED);
        usernameAutofillId = intent.getParcelableExtra(EXTRA_USERNAME_AUTOFILL_ID);
        passwordAutofillId = intent.getParcelableExtra(EXTRA_PASSWORD_AUTOFILL_ID);
        
        Log.d(TAG, "Account: " + serviceName + " / " + username);
    }
    
    private void initViews() {
        tvServiceName = findViewById(R.id.tv_service_name);
        tvUsername = findViewById(R.id.tv_username);
        tvPasswordMask = findViewById(R.id.tv_password_mask);
        btnFillAll = findViewById(R.id.btn_fill_all);
        btnFillUsername = findViewById(R.id.btn_fill_username);
        btnFillPassword = findViewById(R.id.btn_fill_password);
        btnClose = findViewById(R.id.btn_close);
    }
    
    private void setupUI() {
        tvServiceName.setText(serviceName != null ? serviceName : "Unknown");
        tvUsername.setText(username != null ? username : "");
        tvPasswordMask.setText("••••••••");
        
        // Disable buttons nếu không có autofill ID tương ứng
        btnFillUsername.setEnabled(usernameAutofillId != null);
        btnFillPassword.setEnabled(passwordAutofillId != null);
        btnFillAll.setEnabled(usernameAutofillId != null || passwordAutofillId != null);
    }
    
    private void setupListeners() {
        btnFillAll.setOnClickListener(v -> {
            selectedFillMode = FILL_MODE_ALL;
            startBiometricAuth();
        });
        
        btnFillUsername.setOnClickListener(v -> {
            selectedFillMode = FILL_MODE_USERNAME_ONLY;
            startBiometricAuth();
        });
        
        btnFillPassword.setOnClickListener(v -> {
            selectedFillMode = FILL_MODE_PASSWORD_ONLY;
            startBiometricAuth();
        });
        
        btnClose.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }
    
    private void startBiometricAuth() {
        if (BiometricHelper.isBiometricAvailable(this)) {
            BiometricHelper.showBiometricPrompt(this, this);
        } else {
            // Fallback: Nếu không có biometric, cho phép luôn
            Log.w(TAG, "Biometric not available, proceeding without auth");
            onBiometricAuthSuccess();
        }
    }
    
    @Override
    public void onBiometricAuthSuccess() {
        Log.d(TAG, "Biometric success. Checking encryption status...");
        
        // --- Tự động mở khóa Encryption nếu đang bị khóa ---
        if (!encryptionManager.isUnlocked()) {
            String savedPassword = sessionManager.getEncryptionPassword();
            if (savedPassword != null && !savedPassword.isEmpty()) {
                Log.d(TAG, "Encryption is locked, attempting auto-unlock with saved password");
                encryptionManager.initializeWithLoginPassword(savedPassword, false, result -> {
                    if (result == EncryptionManager.InitResult.SUCCESS) {
                        Log.d(TAG, "Encryption auto-unlocked successfully");
                        proceedWithAutofill();
                    } else {
                        Log.e(TAG, "Failed to auto-unlock encryption");
                        Toast.makeText(this, "Không thể mở khóa dữ liệu. Vui lòng đăng nhập lại vào LifeHub.", Toast.LENGTH_LONG).show();
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                });
                return;
            } else {
                Log.w(TAG, "Encryption is locked but no saved password found");
                Toast.makeText(this, "Vui lòng mở ứng dụng LifeHub để kích hoạt lại vân tay.", Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_CANCELED);
                finish();
                return;
            }
        }
        
        proceedWithAutofill();
    }
    
    private void proceedWithAutofill() {
        Log.d(TAG, "Biometric success, filling with mode: " + selectedFillMode);
        
        // Decrypt password
        String decryptedPassword = "";
        if (passwordEncrypted != null && !passwordEncrypted.isEmpty()) {
            try {
                decryptedPassword = encryptionManager.decrypt(passwordEncrypted);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt password", e);
                Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        
        // Build autofill response
        try {
            Intent resultIntent = new Intent();
            
            // Create dataset with actual values
            RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_item);
            presentation.setTextViewText(R.id.autofill_service_name, serviceName);
            presentation.setTextViewText(R.id.autofill_username, username);
            
            Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
            
            switch (selectedFillMode) {
                case FILL_MODE_ALL:
                    if (usernameAutofillId != null) {
                        datasetBuilder.setValue(usernameAutofillId, AutofillValue.forText(username));
                    }
                    if (passwordAutofillId != null) {
                        datasetBuilder.setValue(passwordAutofillId, AutofillValue.forText(decryptedPassword));
                    }
                    break;
                    
                case FILL_MODE_USERNAME_ONLY:
                    if (usernameAutofillId != null) {
                        datasetBuilder.setValue(usernameAutofillId, AutofillValue.forText(username));
                    }
                    break;
                    
                case FILL_MODE_PASSWORD_ONLY:
                    if (passwordAutofillId != null) {
                        datasetBuilder.setValue(passwordAutofillId, AutofillValue.forText(decryptedPassword));
                    }
                    break;
            }
            
            Dataset dataset = datasetBuilder.build();
            resultIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset);
            
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            
            Log.d(TAG, "Autofill completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error building autofill result", e);
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }
    
    @Override
    public void onBiometricAuthError(String errorMessage) {
        Log.e(TAG, "Biometric error: " + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBiometricAuthFailed() {
        Log.w(TAG, "Biometric failed");
        Toast.makeText(this, R.string.biometric_error_auth, Toast.LENGTH_SHORT).show();
    }
}
