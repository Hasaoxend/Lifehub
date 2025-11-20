package com.test.lifehub.features.one_accounts.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.test.lifehub.R;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditAccountActivity extends AppCompatActivity implements PasswordGeneratorDialog.PasswordGeneratedListener {

    public static final String EXTRA_ACCOUNT_ID = "ACCOUNT_ID";

    // Inject Helper để GIẢI MÃ khi load dữ liệu lên màn hình
    @Inject
    EncryptionHelper encryptionHelper;

    private Toolbar mToolbar;
    private TextInputEditText etServiceName, etUsername, etPassword, etWebsiteUrl, etNotes;
    private Button btnGeneratePassword, btnAddField, btnRemoveField;

    private AddEditAccountViewModel mViewModel;
    private AccountEntry mCurrentAccount;
    private String mAccountDocumentId = null;
    private boolean mIsDataLoaded = false;

    private final List<CustomFieldViewHolder> mCustomFieldViews = new ArrayList<>();
    private int mCurrentCustomFieldCount = 0;
    private static final int MAX_CUSTOM_FIELDS = 5;

    private static class CustomFieldViewHolder {
        final View block;
        final TextInputEditText etLabel;
        final TextInputLayout layoutValue;
        final TextInputEditText etValue;
        final ImageButton btnToggleType;
        int fieldType = AccountEntry.FIELD_TYPE_TEXT;

        CustomFieldViewHolder(View block) {
            this.block = block;
            this.etLabel = block.findViewById(R.id.et_custom_label);
            this.layoutValue = block.findViewById(R.id.layout_custom_value);
            this.etValue = block.findViewById(R.id.et_custom_value);
            this.btnToggleType = block.findViewById(R.id.btn_toggle_field_type);
        }
        // (Các hàm clear/updateUI giữ nguyên như cũ, tôi lược bớt để code ngắn gọn)
        void clear() {
            etLabel.setText(""); etValue.setText(""); fieldType = AccountEntry.FIELD_TYPE_TEXT; updateFieldTypeUI(this, false);
        }
        void updateFieldTypeUI(CustomFieldViewHolder vh, boolean showToast) {
            Context context = vh.block.getContext();
            if (vh.fieldType == AccountEntry.FIELD_TYPE_PASSWORD) {
                vh.etValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                vh.btnToggleType.setImageResource(R.drawable.ic_visibility);
            } else {
                vh.etValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                vh.btnToggleType.setImageResource(R.drawable.ic_visibility_off);
            }
            vh.etValue.setSelection(vh.etValue.getText() != null ? vh.etValue.getText().length() : 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- BẢO MẬT: Ngăn chặn chụp màn hình & Recent Apps ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        // ------------------------------------------------------

        setContentView(R.layout.activity_add_edit_account);

        setupViews();
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(AddEditAccountViewModel.class);

        if (getIntent().hasExtra(EXTRA_ACCOUNT_ID)) {
            mAccountDocumentId = getIntent().getStringExtra(EXTRA_ACCOUNT_ID);
        }

        if (mAccountDocumentId == null) {
            getSupportActionBar().setTitle("Thêm tài khoản mới");
            mIsDataLoaded = true;
            updateFieldButtons();
        } else {
            getSupportActionBar().setTitle("Sửa tài khoản");
            loadAccountData(mAccountDocumentId);
        }
        setupListeners();
    }

    // (Giữ nguyên findViews)
    private void setupViews() {
        mToolbar = findViewById(R.id.toolbar);
        etServiceName = findViewById(R.id.et_service_name);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        etWebsiteUrl = findViewById(R.id.et_website_url);
        etNotes = findViewById(R.id.et_notes);
        btnGeneratePassword = findViewById(R.id.btn_generate_password);
        btnAddField = findViewById(R.id.btn_add_field);
        btnRemoveField = findViewById(R.id.btn_remove_field);

        mCustomFieldViews.add(new CustomFieldViewHolder(findViewById(R.id.field_block_1)));
        mCustomFieldViews.add(new CustomFieldViewHolder(findViewById(R.id.field_block_2)));
        mCustomFieldViews.add(new CustomFieldViewHolder(findViewById(R.id.field_block_3)));
        mCustomFieldViews.add(new CustomFieldViewHolder(findViewById(R.id.field_block_4)));
        mCustomFieldViews.add(new CustomFieldViewHolder(findViewById(R.id.field_block_5)));
    }

    private void loadAccountData(String id) {
        mViewModel.getAccountById(id).observe(this, account -> {
            if (account != null && !mIsDataLoaded) {
                mCurrentAccount = account;
                populateUi(account);
            }
        });
    }

    private void populateUi(AccountEntry account) {
        etServiceName.setText(account.serviceName);
        etUsername.setText(account.username);
        etWebsiteUrl.setText(account.websiteUrl);
        etNotes.setText(account.notes);

        // --- GIẢI MÃ MẬT KHẨU ĐỂ HIỂN THỊ ---
        String decryptedPass = encryptionHelper.decrypt(account.password);
        etPassword.setText(decryptedPass);
        // ------------------------------------

        mCurrentCustomFieldCount = 0;
        // (Logic populate custom field giữ nguyên)
        for (CustomFieldViewHolder vh : mCustomFieldViews) { vh.block.setVisibility(View.GONE); vh.clear(); }
        if (account.customFields != null) {
            int i = 0;
            for (Map.Entry<String, Object> entry : account.customFields.entrySet()) {
                if (i >= MAX_CUSTOM_FIELDS) break;
                Object val = entry.getValue();
                if (val instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) val;
                    String v = (String) map.get("value");
                    // Nếu là field password tùy chỉnh, cũng nên giải mã (tùy bạn, ở đây tôi giữ nguyên text)
                    int t = AccountEntry.FIELD_TYPE_TEXT;
                    if (map.get("type") instanceof Long) t = ((Long)map.get("type")).intValue();
                    populateCustomFieldUI(i, entry.getKey(), v, t);
                    i++;
                }
            }
        }
        updateFieldButtons();
        mIsDataLoaded = true;
    }

    private void populateCustomFieldUI(int index, String label, String value, int type) {
        CustomFieldViewHolder vh = mCustomFieldViews.get(index);
        vh.block.setVisibility(View.VISIBLE);
        vh.etLabel.setText(label);
        vh.etValue.setText(value);
        vh.fieldType = type;
        vh.updateFieldTypeUI(vh, false);
        mCurrentCustomFieldCount++;
    }

    private void setupListeners() {
        btnGeneratePassword.setOnClickListener(v -> {
            PasswordGeneratorDialog dialog = new PasswordGeneratorDialog();
            dialog.setListener(this);
            dialog.show(getSupportFragmentManager(), "PasswordGeneratorDialog");
        });
        btnAddField.setOnClickListener(v -> {
            if (mCurrentCustomFieldCount < MAX_CUSTOM_FIELDS) {
                mCustomFieldViews.get(mCurrentCustomFieldCount).block.setVisibility(View.VISIBLE);
                mCurrentCustomFieldCount++;
                updateFieldButtons();
            }
        });
        btnRemoveField.setOnClickListener(v -> {
            if (mCurrentCustomFieldCount > 0) {
                mCurrentCustomFieldCount--;
                CustomFieldViewHolder vh = mCustomFieldViews.get(mCurrentCustomFieldCount);
                vh.block.setVisibility(View.GONE); vh.clear();
                updateFieldButtons();
            }
        });
        for(CustomFieldViewHolder vh : mCustomFieldViews) {
            vh.btnToggleType.setOnClickListener(v -> {
                vh.fieldType = (vh.fieldType == AccountEntry.FIELD_TYPE_TEXT) ? AccountEntry.FIELD_TYPE_PASSWORD : AccountEntry.FIELD_TYPE_TEXT;
                vh.updateFieldTypeUI(vh, true);
            });
        }
    }

    private void updateFieldButtons() {
        btnAddField.setVisibility(mCurrentCustomFieldCount < MAX_CUSTOM_FIELDS ? View.VISIBLE : View.GONE);
        btnRemoveField.setVisibility(mCurrentCustomFieldCount > 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPasswordGenerated(String password) {
        etPassword.setText(password);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        if (mAccountDocumentId == null) {
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            if (deleteItem != null) deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) { saveAccount(); return true; }
        else if (id == R.id.action_delete) { confirmDelete(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void saveAccount() {
        if (!mIsDataLoaded) return;
        String serviceName = etServiceName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim(); // Đây là Plain Text

        if (serviceName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountEntry account = (mCurrentAccount != null) ? mCurrentAccount : new AccountEntry();
        account.serviceName = serviceName;
        account.username = username;
        account.password = password; // Gán Plain Text vào, ViewModel sẽ Mã Hóa sau
        account.websiteUrl = etWebsiteUrl.getText().toString().trim();
        account.notes = etNotes.getText().toString().trim();

        // (Logic save custom field giữ nguyên)
        Map<String, Object> customMap = new HashMap<>();
        for (int i = 0; i < mCurrentCustomFieldCount; i++) {
            CustomFieldViewHolder vh = mCustomFieldViews.get(i);
            String l = vh.etLabel.getText().toString().trim();
            if(!l.isEmpty()){
                Map<String, Object> d = new HashMap<>();
                d.put("value", vh.etValue.getText().toString().trim());
                d.put("type", vh.fieldType);
                customMap.put(l, d);
            }
        }
        account.customFields = customMap;

        if (mAccountDocumentId == null) mViewModel.insert(account);
        else { account.documentId = mAccountDocumentId; mViewModel.update(account); }

        Toast.makeText(this, "Đã lưu (Bảo mật)", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle("Xóa").setMessage("Xóa tài khoản này?")
                .setPositiveButton("Xóa", (d,w) -> { mViewModel.delete(mCurrentAccount); finish(); })
                .setNegativeButton("Hủy", null).show();
    }
}