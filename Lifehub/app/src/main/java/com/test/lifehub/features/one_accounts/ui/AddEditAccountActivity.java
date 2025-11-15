package com.test.lifehub.features.one_accounts.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity để Thêm/Sửa một tài khoản.
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore, Bỏ mã hóa)
 */
public class AddEditAccountActivity extends AppCompatActivity implements PasswordGeneratorDialog.PasswordGeneratedListener {

    private static final String TAG = "AddEditAccountActivity";

    // ----- SỬA LỖI: THÊM HẰNG SỐ (KEY) BỊ THIẾU -----
    /**
     * "Chìa khóa" (Key) để nhận Document ID từ Intent.
     * Phải là "public static final" để AccountAdapter có thể thấy.
     */
    public static final String EXTRA_ACCOUNT_ID = "ACCOUNT_ID";
    // ---------------------------------------------

    // --- Các biến UI (Cố định) ---
    private Toolbar mToolbar;
    private TextInputEditText etServiceName, etUsername, etPassword, etWebsiteUrl, etNotes;
    private Button btnGeneratePassword, btnAddField, btnRemoveField;

    // --- Các biến Logic ---
    private AddEditAccountViewModel mViewModel;
    private AccountEntry mCurrentAccount; // Lưu tài khoản hiện tại
    private String mAccountDocumentId = null; // (Dùng String ID)
    private boolean mIsDataLoaded = false;

    // --- Biến cho Trường Tùy chỉnh ---
    private final List<CustomFieldViewHolder> mCustomFieldViews = new ArrayList<>();
    private int mCurrentCustomFieldCount = 0;
    private static final int MAX_CUSTOM_FIELDS = 5;

    /**
     * Lớp ViewHolder nội bộ để quản lý 1 khối (block) trường tùy chỉnh
     */
    private static class CustomFieldViewHolder {
        // ... (Nội dung lớp ViewHolder giữ nguyên)
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

        void clear() {
            etLabel.setText("");
            etValue.setText("");
            fieldType = AccountEntry.FIELD_TYPE_TEXT;
            updateFieldTypeUI(this, false);
        }

        void updateFieldTypeUI(CustomFieldViewHolder vh, boolean showToast) {
            Context context = vh.block.getContext();
            if (vh.fieldType == AccountEntry.FIELD_TYPE_PASSWORD) {
                vh.etValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                vh.btnToggleType.setImageResource(R.drawable.ic_visibility);
                if (showToast) Toast.makeText(context, "Đã đổi thành kiểu Mật khẩu (Ẩn)", Toast.LENGTH_SHORT).show();
            } else {
                vh.etValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                vh.btnToggleType.setImageResource(R.drawable.ic_visibility_off);
                if (showToast) Toast.makeText(context, "Đã đổi thành kiểu Văn bản (Hiện)", Toast.LENGTH_SHORT).show();
            }
            vh.etValue.setSelection(vh.etValue.getText() != null ? vh.etValue.getText().length() : 0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_account);

        // (Đã xóa EncryptionHelper)

        setupViews();
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(AddEditAccountViewModel.class);

        // SỬA LỖI: Sử dụng hằng số vừa định nghĩa
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

    //
    // ----- (TẤT CẢ CÁC HÀM CÒN LẠI GIỮ NGUYÊN) -----
    // (setupViews, loadAccountData, populateUi, populateCustomFieldUI,
    //  setupListeners, updateFieldButtons, onPasswordGenerated,
    //  onCreateOptionsMenu, onOptionsItemSelected, saveAccount, confirmDelete)
    //

    // ... (Toàn bộ các hàm còn lại của bạn) ...
    // ... (Copy/paste chúng từ file AddEditAccountActivity (phiên bản Firestore) trước đó) ...
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
        etPassword.setText(account.password);
        etNotes.setText(account.notes);
        mCurrentCustomFieldCount = 0;
        if (account.customFields != null) {
            int i = 0;
            for (Map.Entry<String, Object> entry : account.customFields.entrySet()) {
                if (i >= MAX_CUSTOM_FIELDS) break;
                String label = entry.getKey();
                Map<String, Object> fieldData = (Map<String, Object>) entry.getValue();
                String value = (String) fieldData.get("value");
                int type = ((Long) fieldData.get("type")).intValue();
                populateCustomFieldUI(i, label, value, type);
                i++;
            }
        }
        updateFieldButtons();
        mIsDataLoaded = true;
    }

    private void populateCustomFieldUI(int index, String label, String value, int type) {
        if (label != null && !label.isEmpty()) {
            CustomFieldViewHolder vh = mCustomFieldViews.get(index);
            vh.block.setVisibility(View.VISIBLE);
            vh.etLabel.setText(label);
            vh.etValue.setText(value);
            vh.fieldType = type;
            vh.updateFieldTypeUI(vh, false);
            mCurrentCustomFieldCount++;
        }
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
                vh.block.setVisibility(View.GONE);
                vh.clear();
                updateFieldButtons();
            }
        });
        for(CustomFieldViewHolder vh : mCustomFieldViews) {
            vh.btnToggleType.setOnClickListener(v -> {
                vh.fieldType = (vh.fieldType == AccountEntry.FIELD_TYPE_TEXT)
                        ? AccountEntry.FIELD_TYPE_PASSWORD
                        : AccountEntry.FIELD_TYPE_TEXT;
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
        Toast.makeText(this, "Đã áp dụng mật khẩu!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        if (mAccountDocumentId == null) {
            MenuItem deleteItem = menu.findItem(R.id.menu_delete);
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            saveAccount();
            return true;
        } else if (id == R.id.menu_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAccount() {
        if (!mIsDataLoaded) {
            Toast.makeText(this, "Dữ liệu đang tải...", Toast.LENGTH_SHORT).show();
            return;
        }
        String serviceName = etServiceName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String websiteUrl = etWebsiteUrl.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        if (serviceName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Tên dịch vụ, Tên đăng nhập và Mật khẩu", Toast.LENGTH_LONG).show();
            return;
        }

        AccountEntry accountToSave;
        if(mCurrentAccount != null) {
            accountToSave = mCurrentAccount;
        } else {
            accountToSave = new AccountEntry();
        }

        accountToSave.serviceName = serviceName;
        accountToSave.username = username;
        accountToSave.websiteUrl = websiteUrl;
        accountToSave.password = password;
        accountToSave.notes = notes;

        Map<String, Object> customFieldsMap = new HashMap<>();
        for (int i = 0; i < mCurrentCustomFieldCount; i++) {
            CustomFieldViewHolder vh = mCustomFieldViews.get(i);
            String label = vh.etLabel.getText().toString().trim();
            String value = vh.etValue.getText().toString().trim();
            int type = vh.fieldType;

            if (!label.isEmpty()) {
                Map<String, Object> fieldData = new HashMap<>();
                fieldData.put("value", value);
                fieldData.put("type", type);
                customFieldsMap.put(label, fieldData);
            }
        }
        accountToSave.customFields = customFieldsMap;

        if (mAccountDocumentId == null) {
            mViewModel.insert(accountToSave);
        } else {
            mViewModel.update(accountToSave);
        }

        Toast.makeText(this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
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