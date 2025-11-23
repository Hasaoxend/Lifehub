package com.test.lifehub.features.authenticator.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.core.util.TotpManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity hiển thị danh sách tài khoản TOTP
 * Tương tự như Google Authenticator
 */
public class AuthenticatorActivity extends AppCompatActivity {

    private static final String TAG = "AuthenticatorActivity";
    private static final int REQUEST_ADD_ACCOUNT = 1001;

    private RecyclerView rvAccounts;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private Toolbar toolbar;

    private TotpAccountsAdapter adapter;
    private List<TotpAccountItem> accounts;
    private SessionManager sessionManager;

    private Handler handler;
    private Runnable updateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);

        sessionManager = new SessionManager(this);
        accounts = new ArrayList<>();

        findViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        
        loadAccounts();
        startAutoUpdate();
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

    private void loadAccounts() {
        accounts.clear();
        try {
            String accountsJson = sessionManager.getTotpAccounts();
            JSONArray jsonArray = new JSONArray(accountsJson);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String accountName = obj.getString("accountName");
                String issuer = obj.getString("issuer");
                String secret = obj.getString("secret");
                
                accounts.add(new TotpAccountItem(accountName, issuer, secret));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        updateEmptyView();
        adapter.notifyDataSetChanged();
    }

    private void saveAccounts() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (TotpAccountItem account : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("accountName", account.getAccountName());
                obj.put("issuer", account.getIssuer());
                obj.put("secret", account.getSecret());
                jsonArray.put(obj);
            }
            sessionManager.saveTotpAccounts(jsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateEmptyView() {
        if (accounts.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvAccounts.setVisibility(View.GONE);
        } else {
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
            .setMessage("Bạn có chắc muốn xóa tài khoản \"" + account.getAccountName() + "\"?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                accounts.remove(account);
                saveAccounts();
                updateEmptyView();
                adapter.notifyDataSetChanged();
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
            loadAccounts(); // Reload accounts after adding new one
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
        private String accountName;
        private String issuer;
        private String secret;

        public TotpAccountItem(String accountName, String issuer, String secret) {
            this.accountName = accountName;
            this.issuer = issuer;
            this.secret = secret;
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

        public String getCurrentCode() {
            return TotpManager.getCurrentCode(secret);
        }

        public int getTimeRemaining() {
            return TotpManager.getTimeRemaining();
        }
    }
}
