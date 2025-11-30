package com.test.lifehub.features.authenticator.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.core.util.TotpManager;
import com.test.lifehub.features.authenticator.data.TotpAccount;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.authenticator.viewmodel.AuthenticatorViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity thêm tài khoản TOTP mới
 * Có 2 cách: Quét QR code hoặc nhập thủ công
 * Lưu lên Firestore thay vì chỉ local
 */
@AndroidEntryPoint
public class AddTotpAccountActivity extends AppCompatActivity {

    private static final String TAG = "AddTotpAccountActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1002;
    private static final int REQUEST_SCAN_QR = 1003;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    
    // Manual input views
    private View layoutManual;
    private TextInputEditText etAccountName;
    private TextInputEditText etIssuer;
    private TextInputEditText etSecret;
    private Button btnAddManual;
    
    // Scan QR views
    private View layoutScan;
    private Button btnScanQR;

    private AuthenticatorViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_totp);

        viewModel = new ViewModelProvider(this).get(AuthenticatorViewModel.class);

        findViews();
        setupToolbar();
        setupTabs();
        setupListeners();
        
        // Check if should show manual tab directly
        boolean showManualTab = getIntent().getBooleanExtra("SHOW_MANUAL_TAB", false);
        if (showManualTab) {
            tabLayout.selectTab(tabLayout.getTabAt(1));
        }
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar_add_totp);
        tabLayout = findViewById(R.id.tab_layout_add_method);
        
        layoutManual = findViewById(R.id.layout_manual_input);
        etAccountName = findViewById(R.id.et_account_name);
        etIssuer = findViewById(R.id.et_issuer);
        etSecret = findViewById(R.id.et_secret);
        btnAddManual = findViewById(R.id.btn_add_manual);
        
        layoutScan = findViewById(R.id.layout_scan_qr);
        btnScanQR = findViewById(R.id.btn_scan_qr);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.add_account);
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.scan_qr_code));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.manual_entry));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutScan.setVisibility(View.VISIBLE);
                    layoutManual.setVisibility(View.GONE);
                } else {
                    layoutScan.setVisibility(View.GONE);
                    layoutManual.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // Default: show scan QR
        layoutScan.setVisibility(View.VISIBLE);
        layoutManual.setVisibility(View.GONE);
    }

    private void setupListeners() {
        btnScanQR.setOnClickListener(v -> checkCameraPermissionAndScan());
        btnAddManual.setOnClickListener(v -> addAccountManually());
    }

    private void checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                REQUEST_CAMERA_PERMISSION);
        } else {
            startQRScanner();
        }
    }

    private void startQRScanner() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_QR);
    }

    private void addAccountManually() {
        String accountName = etAccountName.getText().toString().trim();
        String issuer = etIssuer.getText().toString().trim();
        String secret = etSecret.getText().toString().trim().toUpperCase().replaceAll(" ", "");

        if (TextUtils.isEmpty(accountName)) {
            etAccountName.setError(getString(R.string.please_enter_account_name));
            return;
        }

        if (TextUtils.isEmpty(secret)) {
            etSecret.setError(getString(R.string.please_enter_secret));
            return;
        }

        // Validate secret (must be Base32)
        if (!secret.matches("[A-Z2-7]+")) {
            etSecret.setError(getString(R.string.invalid_secret_format));
            return;
        }

        if (TextUtils.isEmpty(issuer)) {
            issuer = "LifeHub";
        }

        addAccount(accountName, issuer, secret);
    }

    private void addAccount(String accountName, String issuer, String secret) {
        // Tạo TotpAccount object
        TotpAccount account = new TotpAccount(accountName, issuer, secret);
        
        // Lưu lên Firestore
        viewModel.insert(account, new TotpRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String documentId) {
                Toast.makeText(AddTotpAccountActivity.this, 
                    R.string.account_added_success, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(AddTotpAccountActivity.this, 
                    getString(R.string.error) + ": " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, R.string.error_camera_permission, 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN_QR && resultCode == RESULT_OK && data != null) {
            String qrContent = data.getStringExtra("QR_CONTENT");
            if (qrContent != null) {
                TotpManager.TotpAccount account = TotpManager.parseOtpAuthUri(qrContent);
                if (account != null) {
                    // Hiển thị thông tin: ưu tiên Issuer, fallback là AccountName
                    String displayName;
                    if (account.getIssuer() != null && !account.getIssuer().isEmpty()) {
                        if (account.getAccountName() != null && !account.getAccountName().isEmpty() 
                            && !account.getIssuer().equals(account.getAccountName())) {
                            displayName = account.getIssuer() + " (" + account.getAccountName() + ")";
                        } else {
                            displayName = account.getIssuer();
                        }
                    } else {
                        displayName = account.getAccountName();
                    }
                    
                    Toast.makeText(this, getString(R.string.adding_account, displayName), Toast.LENGTH_LONG).show();
                    
                    addAccount(account.getAccountName(), account.getIssuer(), account.getSecret());
                } else {
                    Toast.makeText(this, R.string.invalid_qr_code, Toast.LENGTH_SHORT).show();
                }
            }
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
}
