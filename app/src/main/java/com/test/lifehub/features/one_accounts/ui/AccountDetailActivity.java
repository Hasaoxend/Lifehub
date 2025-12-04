package com.test.lifehub.features.one_accounts.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.Map;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AccountDetailActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    public static final String EXTRA_ACCOUNT_ID = "ACCOUNT_ID";

    @Inject
    EncryptionHelper encryptionHelper;

    @Inject
    SessionManager sessionManager;

    private String mAccountId;
    private AccountEntry mAccount;

    // Views
    private TextView tvServiceName, tvUsername, tvPassword, tvWebsite, tvNotes;
    private ImageButton btnCopyUsername, btnCopyPassword, btnTogglePassword;
    private LinearLayout layoutCustomFields;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- BẢO MẬT: Ngăn chặn chụp màn hình & Recent Apps ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_account_detail);

        Toolbar toolbar = findViewById(R.id.toolbar_account_detail);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();

        mAccountId = getIntent().getStringExtra(EXTRA_ACCOUNT_ID);

        AddEditAccountViewModel dataViewModel = new ViewModelProvider(this).get(AddEditAccountViewModel.class);

        if (mAccountId != null) {
            dataViewModel.getAccountById(mAccountId).observe(this, account -> {
                if (account != null) {
                    mAccount = account;
                    displayData(account);
                }
            });
        }
    }

    private void initViews() {
        tvServiceName = findViewById(R.id.tv_detail_service_name);
        tvUsername = findViewById(R.id.tv_detail_username);
        tvPassword = findViewById(R.id.tv_detail_password);
        tvWebsite = findViewById(R.id.tv_detail_website);
        tvNotes = findViewById(R.id.tv_detail_notes);

        layoutCustomFields = findViewById(R.id.layout_detail_custom_fields);

        btnCopyUsername = findViewById(R.id.btn_copy_username);
        btnCopyPassword = findViewById(R.id.btn_copy_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);

        btnTogglePassword.setOnClickListener(v -> togglePassword());
        btnCopyUsername.setOnClickListener(v -> copyToClipboard("Tên đăng nhập", tvUsername.getText().toString(), false));

        btnCopyPassword.setOnClickListener(v -> {
            if (mAccount != null) {
                copyToClipboard("Mật khẩu", mAccount.password, true);
            }
        });
    }

    private void displayData(AccountEntry account) {
        tvServiceName.setText(account.serviceName != null ? account.serviceName : "");
        tvUsername.setText(account.username != null ? account.username : "");
        tvWebsite.setText(account.websiteUrl != null ? account.websiteUrl : "");
        tvNotes.setText(account.notes != null ? account.notes : "");

        tvPassword.setText("••••••••");
        isPasswordVisible = false;
        btnTogglePassword.setImageResource(R.drawable.ic_visibility);

        // --- XỬ LÝ CÁC TRƯỜNG TÙY CHỈNH ---
        layoutCustomFields.removeAllViews();
        if (account.customFields != null && !account.customFields.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);

            for (Map.Entry<String, Object> entry : account.customFields.entrySet()) {
                String label = entry.getKey();
                Object valueObj = entry.getValue();

                if (valueObj instanceof Map) {
                    Map<String, Object> fieldData = (Map<String, Object>) valueObj;
                    String value = (String) fieldData.get("value");

                    // Kiểm tra kiểu dữ liệu (nếu cần xử lý ẩn/hiện sau này)
                    int type = AccountEntry.FIELD_TYPE_TEXT;
                    if (fieldData.get("type") instanceof Long) {
                        type = ((Long) fieldData.get("type")).intValue();
                    } else if (fieldData.get("type") instanceof Integer) {
                        type = (Integer) fieldData.get("type");
                    }

                    // Inflate layout item_custom_field_detail.xml
                    View customView = inflater.inflate(R.layout.item_custom_field_detail, layoutCustomFields, false);

                    // --- SỬA LỖI Ở ĐÂY: Dùng đúng ID trong file XML bạn gửi ---
                    TextView tvLabel = customView.findViewById(R.id.tv_field_label);
                    TextView tvValue = customView.findViewById(R.id.tv_field_value);
                    ImageButton btnCopy = customView.findViewById(R.id.btn_copy_field);

                    if (tvLabel != null && tvValue != null) {
                        tvLabel.setText(label);

                        // Nếu là mật khẩu tùy chỉnh, hiển thị dấu chấm tròn
                        if (type == AccountEntry.FIELD_TYPE_PASSWORD) {
                            tvValue.setText("••••••••");
                        } else {
                            tvValue.setText(value);
                        }

                        // Xử lý nút Copy cho trường tùy chỉnh
                        final String finalValue = value;
                        final int finalType = type;
                        btnCopy.setOnClickListener(v -> {
                            // Nếu là password, copy giá trị thật (finalValue), coi là nhạy cảm
                            copyToClipboard(label, finalValue, finalType == AccountEntry.FIELD_TYPE_PASSWORD);
                        });

                        layoutCustomFields.addView(customView);
                    }
                }
            }
        }
    }

    private void togglePassword() {
        if (mAccount == null) return;
        
        // Nếu đang hiển thị mật khẩu → Ẩn mật khẩu (không cần xác thực)
        if (isPasswordVisible) {
            isPasswordVisible = false;
            tvPassword.setText("••••••••");
            btnTogglePassword.setImageResource(R.drawable.ic_visibility);
            return;
        }
        
        // Nếu đang ẩn → Muốn hiển thị mật khẩu
        // Kiểm tra xem user có bật biometric trong Settings không
        if (sessionManager.isBiometricEnabled()) {
            // Đã bật biometric → Kiểm tra thiết bị có hỗ trợ không
            if (BiometricHelper.isBiometricAvailable(this)) {
                // Thiết bị hỗ trợ → Yêu cầu xác thực
                BiometricHelper.showBiometricPrompt(this, this);
            } else {
                // Thiết bị KHÔNG hỗ trợ nhưng setting đã bật
                // → Tự động tắt setting biometric và cho phép xem
                sessionManager.setBiometricEnabled(false);
                Toast.makeText(this, 
                    "Thiết bị không hỗ trợ sinh trắc học. Đã tự động tắt xác thực vân tay.", 
                    Toast.LENGTH_LONG).show();
                showPassword();
            }
        } else {
            // Chưa bật biometric → Hiển thị trực tiếp
            showPassword();
        }
    }
    
    private void showPassword() {
        if (mAccount == null) return;
        isPasswordVisible = true;
        String plain = encryptionHelper.decrypt(mAccount.password);
        tvPassword.setText(plain);
        btnTogglePassword.setImageResource(R.drawable.ic_visibility_off);
    }

    // --- BIOMETRIC CALLBACKS ---
    @Override
    public void onBiometricAuthSuccess() {
        // Xác thực thành công → Hiển thị mật khẩu
        showPassword();
    }

    @Override
    public void onBiometricAuthError(String errorMessage) {
        // Xác thực thất bại/hủy → Không làm gì
        Toast.makeText(this, getString(R.string.account_auth_failed, errorMessage), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBiometricAuthFailed() {
        // Vân tay không khớp → Hệ thống cho phép thử lại
    }

    private void copyToClipboard(String label, String text, boolean isSensitive) {
        if (text == null) return;
        String contentToCopy = text;

        if (isSensitive) {
            // Giải mã nếu chuỗi đang bị mã hóa (dành cho mật khẩu chính)
            // Lưu ý: Trường tùy chỉnh hiện tại chưa được mã hóa trong DB nên text đã là plain text.
            // Hàm này check nếu là mật khẩu chính thì cần decrypt, còn custom field thì dùng trực tiếp.
            if (label.equals("Mật khẩu")) {
                contentToCopy = encryptionHelper.decrypt(text);
            }
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, contentToCopy);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, getString(R.string.account_copied, label), Toast.LENGTH_SHORT).show();

        if (isSensitive) {
            // Tự động xóa clipboard sau 30 giây nếu là dữ liệu nhạy cảm
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    ClipData emptyClip = ClipData.newPlainText("", "");
                    clipboard.setPrimaryClip(emptyClip);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 30000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Intent intent = new Intent(this, AddEditAccountActivity.class);
            intent.putExtra(AddEditAccountActivity.EXTRA_ACCOUNT_ID, mAccountId);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}