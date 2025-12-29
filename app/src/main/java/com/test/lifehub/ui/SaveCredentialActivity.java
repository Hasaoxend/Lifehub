package com.test.lifehub.ui;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import android.view.WindowManager;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * SaveCredentialActivity - Màn hình xác nhận lưu password mới
 * 
 * === WORKFLOW ===
 * 1. Nhận username/password từ onSaveRequest
 * 2. Hiển thị form cho user review/edit
 * 3. Yêu cầu biometric xác thực
 * 4. Encrypt và lưu vào Firestore
 */
@AndroidEntryPoint
@RequiresApi(api = Build.VERSION_CODES.O)
public class SaveCredentialActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "SaveCredential";
    
    // Intent extras
    public static final String EXTRA_SAVE_MODE = "SAVE_MODE";
    public static final String EXTRA_USERNAME = "SAVE_USERNAME";
    public static final String EXTRA_PASSWORD = "SAVE_PASSWORD";
    public static final String EXTRA_PACKAGE = "SAVE_PACKAGE";
    
    // Views
    private TextView tvAppName;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextInputEditText etServiceName;
    private Button btnSave;
    private Button btnCancel;
    private ImageView btnClose;
    
    // Data
    private String receivedUsername;
    private String receivedPassword;
    private String receivedPackage;
    
    // Logic cho Update
    private AccountEntry existingAccount;
    private boolean isUpdateMode = false;
    private TextView tvTitle;
    
    @Inject
    AccountRepository accountRepository;
    
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
        setContentView(R.layout.activity_save_credential);
        
        parseIntent();
        initViews();
        setupUI();
        setupListeners();
    }
    
    private void parseIntent() {
        receivedUsername = getIntent().getStringExtra(EXTRA_USERNAME);
        receivedPassword = getIntent().getStringExtra(EXTRA_PASSWORD);
        receivedPackage = getIntent().getStringExtra(EXTRA_PACKAGE);
        
        Log.d(TAG, "Received save request for package: " + receivedPackage);
    }
    
    private void initViews() {
        tvAppName = findViewById(R.id.tv_app_name);
        tvTitle = findViewById(R.id.tv_save_title);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etServiceName = findViewById(R.id.et_service_name);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        btnClose = findViewById(R.id.btn_close);
    }
    
    private void setupUI() {
        // Set app name từ package/domain
        String appName = isWebDomain(receivedPackage) ? receivedPackage : getAppNameFromPackage(receivedPackage);
        tvAppName.setText(appName);
        
        Log.d(TAG, "Setting up UI for: " + (isWebDomain(receivedPackage) ? "Web" : "Native") + " - " + appName);
        
        // Fill form với data nhận được
        if (receivedUsername != null) {
            etUsername.setText(receivedUsername);
        }
        if (receivedPassword != null) {
            etPassword.setText(receivedPassword);
        }
        
        // Suggest service name từ package/domain
        etServiceName.setText(appName);
        
        // Kiểm tra xem tài khoản đã tồn tại chưa
        checkForExistingAccount();
    }
    
    private boolean isWebDomain(String input) {
        if (input == null) return false;
        return input.contains(".") && !input.contains("com.") && !input.contains("android."); // Heuristic đơn giản
    }
    
    private void checkForExistingAccount() {
        if (receivedUsername == null || receivedPackage == null) return;
        
        List<AccountEntry> allAccounts = accountRepository.getAllAccounts().getValue();
        if (allAccounts == null) {
            Log.d(TAG, "No accounts in repository to match against");
            return;
        }
        
        for (AccountEntry acc : allAccounts) {
            boolean usernameMatch = receivedUsername.equalsIgnoreCase(acc.username);
            
            // Match bằng package name HOẶC websiteUrl (chứa domain)
            boolean targetMatch = receivedPackage.equalsIgnoreCase(acc.websiteUrl) || 
                                 (acc.websiteUrl != null && acc.websiteUrl.contains(receivedPackage)) ||
                                 (receivedPackage.contains(".") && acc.websiteUrl != null && receivedPackage.contains(acc.websiteUrl));
            
            if (usernameMatch && targetMatch) {
                existingAccount = acc;
                isUpdateMode = true;
                break;
            }
        }
        
        if (isUpdateMode) {
            Log.d(TAG, "Found existing account for update: " + existingAccount.serviceName);
            if (tvTitle != null) tvTitle.setText(R.string.update_account_title);
            btnSave.setText(R.string.update_button);
            if (existingAccount.serviceName != null) {
                etServiceName.setText(existingAccount.serviceName);
            }
        } else {
            Log.d(TAG, "No existing account found. Mode: SAVE NEW");
        }
    }
    
    private void setupListeners() {
        btnSave.setOnClickListener(v -> startBiometricAuth());
        btnCancel.setOnClickListener(v -> finish());
        btnClose.setOnClickListener(v -> finish());
    }
    
    private String getAppNameFromPackage(String packageName) {
        if (packageName == null) return "Unknown";
        
        try {
            return getPackageManager()
                    .getApplicationLabel(
                            getPackageManager().getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            // Fallback: extract from package name
            String[] parts = packageName.split("\\.");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                return lastPart.substring(0, 1).toUpperCase() + lastPart.substring(1);
            }
            return packageName;
        }
    }
    
    private void startBiometricAuth() {
        if (!BiometricHelper.isBiometricAvailable(this)) {
            // Fallback nếu không có biometric
            saveCredential();
            return;
        }
        
        BiometricHelper.showBiometricPrompt(this, this);
    }
    
    private void saveCredential() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String serviceName = etServiceName.getText() != null ? etServiceName.getText().toString().trim() : "";
        
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (serviceName.isEmpty()) {
            serviceName = getAppNameFromPackage(receivedPackage);
        }
        
        // Encrypt password using EncryptionManager
        String encryptedPassword = encryptionManager.encrypt(password);
        
        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");
        
        if (isUpdateMode && existingAccount != null) {
            // Flow Cập nhật
            existingAccount.password = encryptedPassword;
            existingAccount.serviceName = serviceName;
            existingAccount.username = username;
            
            Log.d(TAG, "Updating account in Firestore...");
            accountRepository.update(existingAccount);
            // Vì repository dùng async Firestore nhưng không trả về task trực tiếp cho Activity,
            // Ta giả định thành công và báo Toast trước, nhưng thêm log để track.
            Toast.makeText(this, "Đã cập nhật mật khẩu thành công!", Toast.LENGTH_SHORT).show();
        } else {
            // Flow Lưu mới
            AccountEntry entry = new AccountEntry();
            entry.serviceName = serviceName;
            entry.username = username;
            entry.password = encryptedPassword;
            entry.websiteUrl = receivedPackage; 
            entry.notes = "Auto-saved from " + receivedPackage;
            
            Log.d(TAG, "Inserting new account to Firestore... Target: " + receivedPackage);
            accountRepository.insert(entry);
            Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
        }
        
        // Delay nhẹ để user thấy text "Đang lưu" rồi mới đóng
        new android.os.Handler().postDelayed(this::finish, 500);
    }
    
    // BiometricAuthListener callbacks
    
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
                        saveCredential();
                    } else {
                        Log.e(TAG, "Failed to auto-unlock encryption");
                        Toast.makeText(this, "Không thể mở khóa dữ liệu. Vui lòng đăng nhập lại vào LifeHub.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
                return;
            } else {
                Log.w(TAG, "Encryption is locked but no saved password found");
                Toast.makeText(this, "Vui lòng mở ứng dụng LifeHub để kích hoạt lại vân tay.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        
        saveCredential();
    }
    
    @Override
    public void onBiometricAuthError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onBiometricAuthFailed() {
        // User can retry
    }
}
