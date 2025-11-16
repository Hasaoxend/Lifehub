package com.test.lifehub.features.one_accounts.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.test.lifehub.R;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AccountDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ACCOUNT_ID = "ACCOUNT_ID";

    private Toolbar mToolbar;
    private TextView tvServiceName, tvUsername, tvPassword, tvWebsite, tvNotes;
    private ImageButton btnCopyUsername, btnCopyPassword, btnTogglePassword;
    private LinearLayout layoutCustomFields;

    private AddEditAccountViewModel mViewModel;
    private AccountEntry mCurrentAccount;
    private String mAccountId;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_detail);

        mToolbar = findViewById(R.id.toolbar_account_detail);
        tvServiceName = findViewById(R.id.tv_detail_service_name);
        tvUsername = findViewById(R.id.tv_detail_username);
        tvPassword = findViewById(R.id.tv_detail_password);
        tvWebsite = findViewById(R.id.tv_detail_website);
        tvNotes = findViewById(R.id.tv_detail_notes);
        btnCopyUsername = findViewById(R.id.btn_copy_username);
        btnCopyPassword = findViewById(R.id.btn_copy_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        layoutCustomFields = findViewById(R.id.layout_detail_custom_fields);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(AddEditAccountViewModel.class);

        mAccountId = getIntent().getStringExtra(EXTRA_ACCOUNT_ID);
        if (mAccountId != null) {
            loadAccountData();
        }

        setupClickListeners();
    }

    private void loadAccountData() {
        mViewModel.getAccountById(mAccountId).observe(this, account -> {
            if (account != null) {
                mCurrentAccount = account;
                displayAccount(account);
            }
        });
    }

    private void displayAccount(AccountEntry account) {
        getSupportActionBar().setTitle(account.serviceName);

        tvServiceName.setText(account.serviceName);
        tvUsername.setText(account.username);
        tvPassword.setText(maskPassword(account.password));

        if (account.websiteUrl != null && !account.websiteUrl.isEmpty()) {
            tvWebsite.setText(account.websiteUrl);
            tvWebsite.setVisibility(View.VISIBLE);
        } else {
            tvWebsite.setVisibility(View.GONE);
        }

        if (account.notes != null && !account.notes.isEmpty()) {
            tvNotes.setText(account.notes);
            tvNotes.setVisibility(View.VISIBLE);
        } else {
            tvNotes.setVisibility(View.GONE);
        }

        // Display custom fields
        layoutCustomFields.removeAllViews();
        if (account.customFields != null && !account.customFields.isEmpty()) {
            for (Map.Entry<String, Object> entry : account.customFields.entrySet()) {
                addCustomFieldView(entry.getKey(), entry.getValue());
            }
        }
    }

    private void addCustomFieldView(String label, Object fieldData) {
        View fieldView = getLayoutInflater().inflate(R.layout.item_custom_field_detail, layoutCustomFields, false);

        TextView tvLabel = fieldView.findViewById(R.id.tv_field_label);
        TextView tvValue = fieldView.findViewById(R.id.tv_field_value);
        ImageButton btnCopy = fieldView.findViewById(R.id.btn_copy_field);

        tvLabel.setText(label);

        if (fieldData instanceof Map) {
            Map<String, Object> data = (Map<String, Object>) fieldData;
            String value = (String) data.get("value");
            int type = AccountEntry.FIELD_TYPE_TEXT;
            if (data.get("type") instanceof Long) {
                type = ((Long) data.get("type")).intValue();
            }

            if (type == AccountEntry.FIELD_TYPE_PASSWORD) {
                tvValue.setText(maskPassword(value));
            } else {
                tvValue.setText(value);
            }

            btnCopy.setOnClickListener(v -> copyToClipboard(label, value));
        }

        layoutCustomFields.addView(fieldView);
    }

    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) return "";
        return "●".repeat(password.length());
    }

    private void setupClickListeners() {
        btnCopyUsername.setOnClickListener(v -> {
            if (mCurrentAccount != null) {
                copyToClipboard("Tên đăng nhập", mCurrentAccount.username);
            }
        });

        btnCopyPassword.setOnClickListener(v -> {
            if (mCurrentAccount != null) {
                copyToClipboard("Mật khẩu", mCurrentAccount.password);
            }
        });

        btnTogglePassword.setOnClickListener(v -> {
            if (mCurrentAccount != null) {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    tvPassword.setText(mCurrentAccount.password);
                    btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                } else {
                    tvPassword.setText(maskPassword(mCurrentAccount.password));
                    btnTogglePassword.setImageResource(R.drawable.ic_visibility);
                }
            }
        });
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép " + label, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_edit) {
            Intent intent = new Intent(this, AddEditAccountActivity.class);
            intent.putExtra(AddEditAccountActivity.EXTRA_ACCOUNT_ID, mAccountId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        if (mCurrentAccount == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tài khoản này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    mViewModel.delete(mCurrentAccount);
                    Toast.makeText(this, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }
}