package com.test.lifehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.test.lifehub.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Activity Bước 2: Tạo Mật khẩu
 * Version: 2.0 - Improved with better validation and error handling
 */
public class RegisterPasswordActivity extends AppCompatActivity {

    private static final String TAG = "RegisterPasswordActivity";
    private static final int MIN_PASSWORD_LENGTH = 7;

    // Regex patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    // --- Views ---
    private Toolbar toolbar;
    private TextView tvEmailDisplay;
    private TextInputLayout layoutPassword, layoutConfirmPassword;
    private TextInputEditText etPassword, etConfirmPassword;
    private ProgressBar progressPasswordStrength, pbLoading;
    private TextView tvPasswordStrength;
    private Button btnCreateAccount;

    // Password requirement indicators
    private ImageView iconLength, iconUppercase, iconLowercase, iconNumber;

    // --- Data ---
    private String mEmail;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;

    // Password strength tracking
    private boolean hasMinLength = false;
    private boolean hasUppercase = false;
    private boolean hasLowercase = false;
    private boolean hasNumber = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_password_step2);

        // Lấy email từ Intent
        mEmail = getIntent().getStringExtra("EMAIL");
        if (TextUtils.isEmpty(mEmail)) {
            Log.e(TAG, "Email is null or empty!");
            Toast.makeText(this, "Lỗi: Không tìm thấy email", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

        findViews();
        setupToolbar();
        setupListeners();

        tvEmailDisplay.setText("Email: " + mEmail);
    }

    private void findViews() {
        toolbar = findViewById(R.id.toolbar);
        tvEmailDisplay = findViewById(R.id.tv_email_display);
        layoutPassword = findViewById(R.id.layout_password);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        progressPasswordStrength = findViewById(R.id.progress_password_strength);
        tvPasswordStrength = findViewById(R.id.tv_password_strength);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        pbLoading = findViewById(R.id.pb_loading);

        iconLength = findViewById(R.id.icon_length);
        iconUppercase = findViewById(R.id.icon_uppercase);
        iconLowercase = findViewById(R.id.icon_lowercase);
        iconNumber = findViewById(R.id.icon_number);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        // Kiểm tra độ mạnh mật khẩu khi người dùng gõ
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Kiểm tra khớp mật khẩu khi người dùng gõ xác nhận
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordsMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCreateAccount.setOnClickListener(v -> attemptRegistration());
    }

    /**
     * Kiểm tra độ mạnh mật khẩu theo 4 tiêu chí
     */
    private void checkPasswordStrength(String password) {
        layoutPassword.setError(null);

        if (TextUtils.isEmpty(password)) {
            resetPasswordStrengthUI();
            return;
        }

        // Kiểm tra khoảng trắng
        if (WHITESPACE_PATTERN.matcher(password).find()) {
            layoutPassword.setError("Mật khẩu không được chứa khoảng trắng");
            resetPasswordStrengthUI();
            return;
        }

        // 1. Kiểm tra độ dài tối thiểu
        hasMinLength = password.length() >= MIN_PASSWORD_LENGTH;
        updateRequirementIcon(iconLength, hasMinLength);

        // 2. Kiểm tra chữ in hoa
        hasUppercase = UPPERCASE_PATTERN.matcher(password).find();
        updateRequirementIcon(iconUppercase, hasUppercase);

        // 3. Kiểm tra chữ thường
        hasLowercase = LOWERCASE_PATTERN.matcher(password).find();
        updateRequirementIcon(iconLowercase, hasLowercase);

        // 4. Kiểm tra chữ số
        hasNumber = NUMBER_PATTERN.matcher(password).find();
        updateRequirementIcon(iconNumber, hasNumber);

        // Tính điểm độ mạnh
        int score = calculatePasswordScore();
        updatePasswordStrengthUI(score);

        // Kiểm tra có thể enable button không
        checkPasswordsMatch();
    }

    private int calculatePasswordScore() {
        int score = 0;
        if (hasMinLength) score++;
        if (hasUppercase) score++;
        if (hasLowercase) score++;
        if (hasNumber) score++;
        return score;
    }

    private void updatePasswordStrengthUI(int score) {
        progressPasswordStrength.setProgress(score);

        switch (score) {
            case 0:
            case 1:
                tvPasswordStrength.setText("Yếu");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.password_strength_weak));
                progressPasswordStrength.setProgressTintList(
                        ContextCompat.getColorStateList(this, R.color.password_strength_weak_tint));
                break;
            case 2:
                tvPasswordStrength.setText("Trung bình");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.password_strength_medium));
                progressPasswordStrength.setProgressTintList(
                        ContextCompat.getColorStateList(this, R.color.password_strength_medium_tint));
                break;
            case 3:
                tvPasswordStrength.setText("Khá");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.password_strength_good));
                progressPasswordStrength.setProgressTintList(
                        ContextCompat.getColorStateList(this, R.color.password_strength_good_tint));
                break;
            case 4:
                tvPasswordStrength.setText("Mạnh");
                tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.password_strength_strong));
                progressPasswordStrength.setProgressTintList(
                        ContextCompat.getColorStateList(this, R.color.password_strength_strong_tint));
                break;
        }
    }

    private void resetPasswordStrengthUI() {
        tvPasswordStrength.setText("Chưa nhập");
        tvPasswordStrength.setTextColor(ContextCompat.getColor(this, R.color.password_strength_default));
        progressPasswordStrength.setProgress(0);
        resetRequirementIcons();
        updateButtonState(false);
    }

    /**
     * Cập nhật icon yêu cầu (tick xanh nếu đạt, vòng tròn xám nếu chưa)
     */
    private void updateRequirementIcon(ImageView icon, boolean isMet) {
        if (isMet) {
            icon.setImageResource(R.drawable.ic_check_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.password_strength_strong));
        } else {
            icon.setImageResource(R.drawable.ic_circle);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.password_strength_default));
        }
    }

    private void resetRequirementIcons() {
        hasMinLength = false;
        hasUppercase = false;
        hasLowercase = false;
        hasNumber = false;

        updateRequirementIcon(iconLength, false);
        updateRequirementIcon(iconUppercase, false);
        updateRequirementIcon(iconLowercase, false);
        updateRequirementIcon(iconNumber, false);
    }

    private void checkPasswordsMatch() {
        String password = getTextSafely(etPassword);
        String confirmPassword = getTextSafely(etConfirmPassword);

        if (TextUtils.isEmpty(confirmPassword)) {
            layoutConfirmPassword.setError(null);
            updateButtonState(false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Mật khẩu không khớp");
            updateButtonState(false);
        } else {
            layoutConfirmPassword.setError(null);
            // Chỉ bật nút nếu mật khẩu đủ mạnh (score = 4)
            boolean isPasswordStrong = hasMinLength && hasUppercase && hasLowercase && hasNumber;
            updateButtonState(isPasswordStrong);
        }
    }

    private void updateButtonState(boolean enabled) {
        btnCreateAccount.setEnabled(enabled);
        btnCreateAccount.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void attemptRegistration() {
        String password = getTextSafely(etPassword);
        String confirmPassword = getTextSafely(etConfirmPassword);

        // Kiểm tra lần cuối
        if (!isPasswordValid(password)) {
            Toast.makeText(this, "Mật khẩu chưa đủ mạnh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            layoutConfirmPassword.setError("Mật khẩu không khớp");
            return;
        }

        if (WHITESPACE_PATTERN.matcher(password).find()) {
            layoutPassword.setError("Mật khẩu không được chứa khoảng trắng");
            return;
        }

        setLoading(true);

        // Tạo tài khoản Firebase
        mAuth.createUserWithEmailAndPassword(mEmail, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        createUserDocumentInFirestore(user);
                    } else {
                        setLoading(false);
                        showError("Tạo tài khoản thất bại");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.w(TAG, "createUserWithEmail:failure", e);
                    String errorMessage = parseFirebaseError(e.getMessage());
                    showError("Đăng ký thất bại: " + errorMessage);
                });
    }

    private boolean isPasswordValid(String password) {
        return !TextUtils.isEmpty(password)
                && hasMinLength
                && hasUppercase
                && hasLowercase
                && hasNumber
                && !WHITESPACE_PATTERN.matcher(password).find();
    }

    private String parseFirebaseError(String error) {
        if (error == null) return "Lỗi không xác định";

        if (error.contains("email address is already in use")) {
            return "Email đã được sử dụng";
        } else if (error.contains("network error")) {
            return "Lỗi kết nối mạng";
        } else if (error.contains("weak password")) {
            return "Mật khẩu quá yếu";
        }
        return error;
    }

    private void createUserDocumentInFirestore(FirebaseUser user) {
        String uid = user.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", mEmail);
        userData.put("uid", uid);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("emailVerified", false);

        mDb.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Tạo User Document thành công!");
                    sendVerificationEmail(user);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.e(TAG, "Lỗi khi tạo User Document", e);
                    showError("Đăng ký thành công, nhưng không thể tạo CSDL người dùng.");
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Log.w(TAG, "sendEmailVerification:failure", e);
                    showError("Đăng ký thành công, nhưng gửi email xác thực thất bại: " + e.getMessage());
                });
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng ký Thành công!")
                .setMessage("Một email xác thực đã được gửi đến " + mEmail +
                        ".\n\nVui lòng kiểm tra hộp thư (kể cả spam) và xác thực tài khoản trước khi đăng nhập.")
                .setPositiveButton("OK", (dialog, which) -> navigateToLogin())
                .setCancelable(false)
                .show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterPasswordActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EMAIL", mEmail); // Pass email to login screen
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean isLoading) {
        pbLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateAccount.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);

        // Disable toolbar navigation during loading
        toolbar.setNavigationOnClickListener(isLoading ? null : v -> onBackPressed());
    }

    private String getTextSafely(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    @Override
    public void onBackPressed() {
        if (pbLoading.getVisibility() == View.VISIBLE) {
            // Prevent back press during loading
            Toast.makeText(this, "Vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }
}