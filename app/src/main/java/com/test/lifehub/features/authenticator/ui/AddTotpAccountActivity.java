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

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.core.util.TotpManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Activity thêm tài khoản TOTP mới
 * Có 2 cách: Quét QR code hoặc nhập thủ công
 */
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

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_totp);

        sessionManager = new SessionManager(this);

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
            getSupportActionBar().setTitle("Thêm tài khoản");
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Quét QR"));
        tabLayout.addTab(tabLayout.newTab().setText("Nhập thủ công"));
        
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
            etAccountName.setError("Vui lòng nhập tên tài khoản");
            return;
        }

        if (TextUtils.isEmpty(secret)) {
            etSecret.setError("Vui lòng nhập secret key");
            return;
        }

        // Validate secret (must be Base32)
        if (!secret.matches("[A-Z2-7]+")) {
            etSecret.setError("Secret key không hợp lệ (chỉ chứa A-Z và 2-7)");
            return;
        }

        if (TextUtils.isEmpty(issuer)) {
            issuer = "LifeHub";
        }

        addAccount(accountName, issuer, secret);
    }

    private void addAccount(String accountName, String issuer, String secret) {
        try {
            // Load existing accounts
            String accountsJson = sessionManager.getTotpAccounts();
            JSONArray jsonArray = new JSONArray(accountsJson);
            
            // Check if account already exists
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                if (obj.getString("accountName").equals(accountName) && 
                    obj.getString("issuer").equals(issuer)) {
                    Toast.makeText(this, "Tài khoản này đã tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            
            // Add new account
            JSONObject newAccount = new JSONObject();
            newAccount.put("accountName", accountName);
            newAccount.put("issuer", issuer);
            newAccount.put("secret", secret);
            jsonArray.put(newAccount);
            
            // Save
            sessionManager.saveTotpAccounts(jsonArray.toString());
            
            Toast.makeText(this, "Đã thêm tài khoản thành công", Toast.LENGTH_SHORT).show();
            
            setResult(RESULT_OK);
            finish();
            
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi thêm tài khoản", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQRScanner();
            } else {
                Toast.makeText(this, "Cần quyền camera để quét QR code", 
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
                    addAccount(account.getAccountName(), account.getIssuer(), account.getSecret());
                } else {
                    Toast.makeText(this, "QR code không hợp lệ", Toast.LENGTH_SHORT).show();
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
