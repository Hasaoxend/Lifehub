package com.test.lifehub.features.authenticator.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.core.util.TotpManager;
import com.test.lifehub.features.authenticator.data.TotpAccount;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.authenticator.viewmodel.AuthenticatorViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity hiển thị danh sách tài khoản TOTP
 * Tương tự như Google Authenticator
 * Load dữ liệu từ Firestore
 */
@AndroidEntryPoint
public class AuthenticatorActivity extends AppCompatActivity {

    private static final String TAG = "AuthenticatorActivity";
    private static final int REQUEST_ADD_ACCOUNT = 1001;

    private RecyclerView rvAccounts;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private Toolbar toolbar;

    private TotpAccountsAdapter adapter;
    private List<TotpAccountItem> accounts;
    private AuthenticatorViewModel viewModel;

    private Handler handler;
    private Runnable updateRunnable;

    @javax.inject.Inject
    EncryptionHelper encryptionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "==================== onCreate START ====================");
        Log.d(TAG, "Activity instance: " + this.hashCode());
        
        setContentView(R.layout.activity_authenticator);
        Log.d(TAG, "setContentView done");

        viewModel = new ViewModelProvider(this).get(AuthenticatorViewModel.class);
        Log.d(TAG, "ViewModel created: " + viewModel.hashCode());
        
        accounts = new ArrayList<>();

        findViews();
        Log.d(TAG, "findViews done");
        
        setupToolbar();
        Log.d(TAG, "setupToolbar done");
        
        setupRecyclerView();
        Log.d(TAG, "setupRecyclerView done");
        
        setupListeners();
        Log.d(TAG, "setupListeners done");
        
        // Observe accounts from Firestore
        observeAccounts();
        Log.d(TAG, "observeAccounts done");
        
        startAutoUpdate();
        Log.d(TAG, "==================== onCreate END ====================");
    }

    private void observeAccounts() {
        Log.d(TAG, "Setting up Firestore observer... Activity instance: " + this.hashCode());
        viewModel.getAllAccounts().observe(this, totpAccounts -> {
            Log.d(TAG, "[Activity " + this.hashCode() + "] Observer triggered with " + (totpAccounts != null ? totpAccounts.size() : 0) + " accounts");
            
            if (totpAccounts != null) {
                accounts.clear();
                
                // Kiểm tra EncryptionHelper
                if (encryptionHelper == null) {
                    Log.e(TAG, "EncryptionHelper is NULL! Cannot decrypt secrets.");
                    updateEmptyView();
                    return;
                }
                
                // Convert TotpAccount to TotpAccountItem
                for (TotpAccount account : totpAccounts) {
                    try {
                        Log.d(TAG, "Processing account: " + account.getIssuer() + " / " + account.getAccountName());
                        
                        // Giải mã secret key
                        String decryptedSecret = encryptionHelper.decrypt(account.getSecretKey());
                        
                        if (decryptedSecret == null || decryptedSecret.isEmpty()) {
                            Log.e(TAG, "Failed to decrypt secret for: " + account.getIssuer());
                            continue;
                        }
                        
                        accounts.add(new TotpAccountItem(
                            account.getDocumentId(),
                            account.getAccountName(),
                            account.getIssuer(),
                            decryptedSecret
                        ));
                        
                        Log.d(TAG, "Successfully added account: " + account.getIssuer());
                    } catch (Exception e) {
                        Log.e(TAG, "Error decrypting secret for account: " + account.getIssuer(), e);
                    }
                }
                
                Log.d(TAG, "Calling updateEmptyView() and notifyDataSetChanged()");
                updateEmptyView();
                adapter.notifyDataSetChanged();
                Log.d(TAG, "Final account count in UI: " + accounts.size());
                Log.d(TAG, "RecyclerView visibility: " + (rvAccounts.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
                Log.d(TAG, "Empty view visibility: " + (tvEmpty.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
            } else {
                Log.w(TAG, "totpAccounts is NULL");
            }
        });
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar_authenticator);
        rvAccounts = findViewById(R.id.rv_totp_accounts);
        tvEmpty = findViewById(R.id.tv_empty_accounts);
        fabAdd = findViewById(R.id.fab_add_account);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Authenticator");
        }
    }

    private void setupRecyclerView() {
        adapter = new TotpAccountsAdapter(accounts, new TotpAccountsAdapter.OnAccountActionListener() {
            @Override
            public void onCopyCode(TotpAccountItem account) {
                copyToClipboard(account.getCurrentCode());
            }

            @Override
            public void onDeleteAccount(TotpAccountItem account) {
                showDeleteConfirmDialog(account);
            }
        });
        
        rvAccounts.setLayoutManager(new LinearLayoutManager(this));
        rvAccounts.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTotpAccountActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ACCOUNT);
        });
    }

    private void updateEmptyView() {
        if (accounts.isEmpty()) {
            Log.d(TAG, "updateEmptyView: Showing empty view");
            tvEmpty.setVisibility(View.VISIBLE);
            rvAccounts.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "updateEmptyView: Showing RecyclerView with " + accounts.size() + " items");
            tvEmpty.setVisibility(View.GONE);
            rvAccounts.setVisibility(View.VISIBLE);
        }
    }

    private void startAutoUpdate() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                adapter.updateCodes();
                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(updateRunnable);
    }

    private void showDeleteConfirmDialog(TotpAccountItem account) {
        new AlertDialog.Builder(this)
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc muốn xóa tài khoản \"" + account.getDisplayName() + "\"?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                // Xóa từ Firestore
                viewModel.delete(account.getDocumentId(), new TotpRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess(String documentId) {
                        android.widget.Toast.makeText(AuthenticatorActivity.this, 
                            "Đã xóa tài khoản", android.widget.Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        android.widget.Toast.makeText(AuthenticatorActivity.this, 
                            "Lỗi: " + error, android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) 
            getSystemService(CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("TOTP Code", text);
        clipboard.setPrimaryClip(clip);
        
        android.widget.Toast.makeText(this, "Đã sao chép mã: " + text, 
            android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_ACCOUNT && resultCode == RESULT_OK) {
            // Firestore sẽ tự động update qua LiveData observer
            Log.d(TAG, "Account added successfully");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Class đại diện cho một item tài khoản TOTP trong danh sách
     */
    public static class TotpAccountItem {
        private String documentId;  // ID từ Firestore
        private String accountName;
        private String issuer;
        private String secret;

        public TotpAccountItem(String documentId, String accountName, String issuer, String secret) {
            this.documentId = documentId;
            this.accountName = accountName;
            this.issuer = issuer;
            this.secret = secret;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getSecret() {
            return secret;
        }

        public String getDisplayName() {
            if (issuer != null && !issuer.isEmpty()) {
                return issuer + " (" + accountName + ")";
            }
            return accountName;
        }

        public String getCurrentCode() {
            return TotpManager.getCurrentCode(secret);
        }

        public int getTimeRemaining() {
            return TotpManager.getTimeRemaining();
        }
    }
}
