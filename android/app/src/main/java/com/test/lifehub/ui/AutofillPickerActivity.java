package com.test.lifehub.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.core.services.LifeHubAutofillService;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import android.view.WindowManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * AutofillPickerActivity - Màn hình chọn tài khoản sau khi xác thực biometric
 * 
 * Flow:
 * 1. User tap vào "Tự động điền với LifeHub" trong popup
 * 2. Activity này mở và hiển thị biometric prompt
 * 3. Sau biometric thành công → hiển thị danh sách accounts
 * 4. User chọn account → điền và đóng
 */
@AndroidEntryPoint
@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillPickerActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "AutofillPicker";
    
    // Intent extras
    public static final String EXTRA_USERNAME_AUTOFILL_ID = "username_autofill_id";
    public static final String EXTRA_PASSWORD_AUTOFILL_ID = "password_autofill_id";
    
    // Views
    private RecyclerView rvAccounts;
    private ImageView btnClose;
    
    // Data
    private AutofillId usernameAutofillId;
    private AutofillId passwordAutofillId;
    private String selectedAccountId;
    private List<AccountEntry> accounts;
    
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
        setContentView(R.layout.activity_autofill_picker);
        
        parseIntent();
        initViews();
        
        // Lấy accounts từ service cache
        accounts = LifeHubAutofillService.getCachedAccounts();
        
        if (accounts == null || accounts.isEmpty()) {
            Toast.makeText(this, "Không có tài khoản nào", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Bắt đầu biometric ngay
        startBiometric();
    }
    
    private void parseIntent() {
        Intent intent = getIntent();
        usernameAutofillId = intent.getParcelableExtra(EXTRA_USERNAME_AUTOFILL_ID);
        passwordAutofillId = intent.getParcelableExtra(EXTRA_PASSWORD_AUTOFILL_ID);
        selectedAccountId = intent.getStringExtra("ACCOUNT_ID");
        Log.d(TAG, "Parsed intent: usernameId=" + (usernameAutofillId!=null) + 
                   ", passwordId=" + (passwordAutofillId!=null) + 
                   ", selectedAccountId=" + selectedAccountId);
    }
    
    private void initViews() {
        rvAccounts = findViewById(R.id.rv_accounts);
        btnClose = findViewById(R.id.btn_close);
        
        btnClose.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
        
        // Ẩn list cho đến khi biometric thành công
        rvAccounts.setVisibility(View.GONE);
    }
    
    private void startBiometric() {
        if (!BiometricHelper.isBiometricAvailable(this)) {
            // Nếu không có biometric, hiện list luôn
            showAccountList();
            return;
        }
        
        BiometricHelper.showBiometricPrompt(this, this);
    }
    
    private void showAccountList() {
        rvAccounts.setVisibility(View.VISIBLE);
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(new AccountPickerAdapter(accounts, this::onAccountSelected));
    }
    
    private void onAccountSelected(AccountEntry account) {
        Log.d(TAG, "Selected account: " + account.serviceName);
        
        // Hiện dialog chọn điền gì
        showFillOptionsDialog(account);
    }
    
    private void showFillOptionsDialog(AccountEntry account) {
        // Giải mã mật khẩu bằng biến tạm để đảm bảo effectively final cho lambda
        String tempPassword = "";
        try {
            if (account.password != null) {
                // Sử dụng EncryptionManager để hỗ trợ cả chuẩn cũ và mới
                tempPassword = encryptionManager.decrypt(account.password);
                Log.d(TAG, "Password decrypted successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting password", e);
            tempPassword = account.password != null ? account.password : "";
        }
        final String password = tempPassword; 
        
        // Tạo bottom sheet dialog
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_fill_options, null);
        dialog.setContentView(sheetView);
        
        // Set account info
        TextView tvServiceName = sheetView.findViewById(R.id.tv_dialog_service_name);
        TextView tvUsername = sheetView.findViewById(R.id.tv_dialog_username);
        TextView tvPasswordMask = sheetView.findViewById(R.id.tv_dialog_password_mask);
        
        tvServiceName.setText(account.serviceName);
        tvUsername.setText(account.username);
        tvPasswordMask.setText("••••••••");
        
        // Button listeners
        sheetView.findViewById(R.id.btn_fill_all).setOnClickListener(v -> {
            dialog.dismiss();
            fillWithOption(account, password, true, true);
        });
        
        sheetView.findViewById(R.id.btn_fill_username).setOnClickListener(v -> {
            dialog.dismiss();
            fillWithOption(account, password, true, false);
        });
        
        sheetView.findViewById(R.id.btn_fill_password).setOnClickListener(v -> {
            dialog.dismiss();
            fillWithOption(account, password, false, true);
        });
        
        sheetView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void fillWithOption(AccountEntry account, String password, boolean fillUsername, boolean fillPassword) {
        try {
            Intent resultIntent = new Intent();
            
            RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_item);
            presentation.setTextViewText(R.id.autofill_service_name, account.serviceName);
            presentation.setTextViewText(R.id.autofill_username, account.username);
            
            Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
            
            if (fillUsername && usernameAutofillId != null) {
                datasetBuilder.setValue(usernameAutofillId, AutofillValue.forText(account.username));
            }
            if (fillPassword && passwordAutofillId != null) {
                datasetBuilder.setValue(passwordAutofillId, AutofillValue.forText(password));
            }
            
            Dataset dataset = datasetBuilder.build();
            resultIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset);
            
            // --- NEW: Kích hoạt dán ngầm qua Accessibility Service (Double-tap) ---
            try {
                com.test.lifehub.core.services.LifeHubAccessibilityService accessibilityService = 
                        com.test.lifehub.core.services.LifeHubAccessibilityService.getInstance();
                if (accessibilityService != null) {
                    Log.d(TAG, "Triggering direct fill via Accessibility Service");
                    accessibilityService.performDirectFill(
                            fillUsername ? account.username : null, 
                            fillPassword ? password : null
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error triggering accessibility fill", e);
            }
            // -------------------------------------------------------------------

            setResult(Activity.RESULT_OK, resultIntent);
            finish();
            
            Log.d(TAG, "Autofill completed for: " + account.serviceName + 
                      " (username: " + fillUsername + ", password: " + fillPassword + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error building autofill result", e);
            Toast.makeText(this, R.string.error_unknown, Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
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
                        proceedAfterAuth();
                    } else {
                        Log.e(TAG, "Failed to auto-unlock encryption - password might have changed");
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
        
        proceedAfterAuth();
    }
    
    private void proceedAfterAuth() {
        Log.d(TAG, "Proceeding after successful authentication. Searching for account ID: " + selectedAccountId);
        if (selectedAccountId != null && accounts != null) {
            for (AccountEntry account : accounts) {
                if (selectedAccountId.equals(account.documentId)) {
                    Log.d(TAG, "Found target account from ID, jumping to fill options");
                    showFillOptionsDialog(account);
                    return;
                }
            }
            Log.w(TAG, "Account with ID " + selectedAccountId + " not found in cache");
        }
        showAccountList();
    }
    
    @Override
    public void onBiometricAuthError(String errorMessage) {
        Log.e(TAG, "Biometric error: " + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
    
    @Override
    public void onBiometricAuthFailed() {
        Log.w(TAG, "Biometric failed");
        // User có thể thử lại
    }
    
    // ===== Inner Adapter =====
    
    private static class AccountPickerAdapter extends RecyclerView.Adapter<AccountPickerAdapter.ViewHolder> {
        
        private final List<AccountEntry> accounts;
        private final OnAccountClickListener listener;
        
        interface OnAccountClickListener {
            void onAccountClick(AccountEntry account);
        }
        
        AccountPickerAdapter(List<AccountEntry> accounts, OnAccountClickListener listener) {
            this.accounts = accounts;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.autofill_item, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AccountEntry account = accounts.get(position);
            holder.tvServiceName.setText(account.serviceName);
            holder.tvUsername.setText(account.username);
            holder.tvPasswordHint.setText("••••••••");
            
            holder.itemView.setOnClickListener(v -> listener.onAccountClick(account));
        }
        
        @Override
        public int getItemCount() {
            return accounts != null ? accounts.size() : 0;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvServiceName;
            TextView tvUsername;
            TextView tvPasswordHint;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvServiceName = itemView.findViewById(R.id.autofill_service_name);
                tvUsername = itemView.findViewById(R.id.autofill_username);
                tvPasswordHint = itemView.findViewById(R.id.autofill_password_hint);
            }
        }
    }
}
