package com.test.lifehub.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.services.LifeHubAccessibilityService;
import com.test.lifehub.core.services.LifeHubAutofillService;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * AccessibilityAutofillActivity - Màn hình chọn account cho Browser Autofill
 * 
 * Được mở từ floating button của AccessibilityService.
 * 
 * Flow:
 * 1. Hiển thị biometric prompt
 * 2. Sau khi xác thực → hiện danh sách accounts
 * 3. User chọn account → copy password vào clipboard (vì không thể inject trực tiếp)
 */
@AndroidEntryPoint
@RequiresApi(api = Build.VERSION_CODES.O)
public class AccessibilityAutofillActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "AccessibilityAutofill";
    
    private RecyclerView rvAccounts;
    private ImageView btnClose;
    private TextView tvTitle;
    
    private List<AccountEntry> accounts;
    private String packageName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofill_picker);
        
        parseIntent();
        initViews();
        
        // Get cached accounts
        accounts = LifeHubAutofillService.getCachedAccounts();
        
        if (accounts == null || accounts.isEmpty()) {
            Toast.makeText(this, "Không có tài khoản nào", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Start biometric
        startBiometric();
    }
    
    private void parseIntent() {
        Intent intent = getIntent();
        packageName = intent.getStringExtra("PACKAGE_NAME");
        Log.d(TAG, "Opened for package: " + packageName);
    }
    
    private void initViews() {
        rvAccounts = findViewById(R.id.rv_accounts);
        btnClose = findViewById(R.id.btn_close);
        
        btnClose.setOnClickListener(v -> finish());
        
        // Hide list until biometric success
        rvAccounts.setVisibility(View.GONE);
    }
    
    private void startBiometric() {
        if (!BiometricHelper.isBiometricAvailable(this)) {
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
        
        // Show options dialog
        showFillOptionsDialog(account);
    }
    
    private void showFillOptionsDialog(AccountEntry account) {
        String password = account.password != null ? account.password : "";
        
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_fill_options, null);
        dialog.setContentView(sheetView);
        
        // Set account info
        TextView tvServiceName = sheetView.findViewById(R.id.tv_dialog_service_name);
        TextView tvUsername = sheetView.findViewById(R.id.tv_dialog_username);
        TextView tvPasswordMask = sheetView.findViewById(R.id.tv_dialog_password_mask);
        
        tvServiceName.setText(account.serviceName);
        tvUsername.setText(account.username);
        tvPasswordMask.setText("••••••••");
        
        // Button listeners - copy to clipboard
        sheetView.findViewById(R.id.btn_fill_all).setOnClickListener(v -> {
            dialog.dismiss();
            copyBothToClipboard(account, password);
        });
        
        sheetView.findViewById(R.id.btn_fill_username).setOnClickListener(v -> {
            dialog.dismiss();
            copyToClipboard("Username", account.username);
        });
        
        sheetView.findViewById(R.id.btn_fill_password).setOnClickListener(v -> {
            dialog.dismiss();
            copyToClipboard("Password", password);
        });
        
        sheetView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void copyToClipboard(String label, String text) {
        // Direct Injection via A11y Service
        LifeHubAccessibilityService service = LifeHubAccessibilityService.getInstance();
        boolean serviceFound = (service != null);
        if (serviceFound) {
            Log.d(TAG, "Found A11y service instance, attempting direct fill");
            if ("Username".equals(label)) {
                service.performDirectFill(text, null);
            } else {
                service.performDirectFill(null, text);
            }
        } else {
            Log.w(TAG, "A11y service instance not found or connected");
        }

        android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        
        String msg = serviceFound ? label + " đã được điền tự động!" : label + " đã được copy (Bật Accessibility để tự điền)";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void copyBothToClipboard(AccountEntry account, String password) {
        // Direct Injection via A11y Service
        LifeHubAccessibilityService service = LifeHubAccessibilityService.getInstance();
        boolean serviceFound = (service != null);
        if (serviceFound) {
            Log.d(TAG, "Found A11y service instance, attempting direct fill of both fields");
            service.performDirectFill(account.username, password);
        } else {
            Log.w(TAG, "A11y service instance not found or connected");
        }

        android.content.ClipboardManager clipboard = 
                (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Credentials", password);
        clipboard.setPrimaryClip(clip);
        
        String msg = serviceFound ? "Đang tự động điền tài khoản & mật khẩu..." : "Đã copy mật khẩu (Bật Accessibility để tự điền)";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    // BiometricAuthListener callbacks
    
    @Override
    public void onBiometricAuthSuccess() {
        Log.d(TAG, "Biometric success");
        showAccountList();
    }
    
    @Override
    public void onBiometricAuthError(String errorMessage) {
        Log.e(TAG, "Biometric error: " + errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        finish();
    }
    
    @Override
    public void onBiometricAuthFailed() {
        Log.w(TAG, "Biometric failed");
        // User can retry
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
