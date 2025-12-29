package com.test.lifehub.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.three_settings.ui.ChangePasswordActivity;

import javax.inject.Inject;

import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * LoginActivity - Màn hình đăng nhập
 * 
 * === MỤC ĐÍCH ===
 * Activity chính cho authentication với 2 phương thức:
 * 1. Email/Password Login (Firebase Authentication)
 * 2. Biometric Login (Fingerprint/Face ID)
 * 
 * === TÍNH NĂNG NỔI BẬT ===
 * 
 * 1. PASSWORD STRENGTH VALIDATION:
 *    - Minimum 8 characters
 *    - Phải có: uppercase, lowercase, number, special char
 *    - Kiểm tra khi login lần đầu
 *    - Gợi ý đổi password nếu yếu
 * 
 * 2. BIOMETRIC AUTHENTICATION:
 *    - Hiển thị option nếu thiết bị hỗ trợ
 *    - Sử dụng BiometricHelper
 *    - Auto-login sau khi verify thành công
 * 
 * 3. WEAK PASSWORD DETECTION:
 *    - Kiểm tra lần đầu login
 *    - Lưu flag trong SharedPreferences (per user)
 *    - Hiển thị dialog gợi ý đổi password
 *    - Link trực tiếp đến ChangePasswordActivity
 * 
 * === SECURITY FEATURES ===
 * 
 * 1. SCREEN SECURITY:
 *    ```java
 *    getWindow().setFlags(
 *        WindowManager.LayoutParams.FLAG_SECURE,
 *        WindowManager.LayoutParams.FLAG_SECURE
 *    );
 *    ```
 *    - Chặn screenshot
 *    - Chặn screen recording
 *    - Che màn hình trong Recent Apps
 * 
 * 2. PASSWORD PATTERNS:
 *    ```java
 *    MIN_PASSWORD_LENGTH = 8
 *    UPPERCASE_PATTERN = [A-Z]
 *    LOWERCASE_PATTERN = [a-z]
 *    NUMBER_PATTERN = [0-9]
 *    SPECIAL_CHAR_PATTERN = [!@#$%^&*(),.?\":{}|<>]
 *    ```
 * 
 * === FLOW DIAGRAM ===
 * ```
 * onCreate()
 *    |
 *    v
 * performInitialCheck() <- LoginViewModel
 *    |
 *    ├─> User logged in + Biometric enabled -> Auto-login
 *    ├─> User logged in, no biometric -> MainActivity
 *    └─> Not logged in -> Show login form
 * 
 * User nhập email/password
 *    |
 *    v
 * login() <- LoginViewModel
 *    |
 *    ├─> Success -> Check password strength
 *    │              |
 *    │              ├─> Strong -> MainActivity
 *    │              └─> Weak -> Show dialog suggest change
 *    │
 *    └─> Failure -> Show error message
 * ```
 * 
 * === SHARED PREFERENCES ===
 * Key format: "weak_password_checked_{userId}"
 * - true: Đã check weak password rồi, không check nữa
 * - false/null: Chưa check, cần kiểm tra
 * 
 * === BIOMETRIC IMPLEMENTATION ===
 * ```java
 * // Kiểm tra khả dụng
 * if (BiometricHelper.isBiometricAvailable(this)) {
 *     mLayoutBiometric.setVisibility(View.VISIBLE);
 * }
 * 
 * // Show prompt
 * BiometricHelper.showBiometricPrompt(this, new BiometricAuthListener() {
 *     public void onBiometricAuthSuccess() {
 *         // Auto-login
 *         navigateToMainActivity();
 *     }
 * });
 * ```
 * 
 * === PASSWORD VALIDATION ===
 * ```java
 * private boolean isPasswordWeak(String password) {
 *     return password.length() < MIN_PASSWORD_LENGTH
 *         || !UPPERCASE_PATTERN.matcher(password).find()
 *         || !LOWERCASE_PATTERN.matcher(password).find()
 *         || !NUMBER_PATTERN.matcher(password).find()
 *         || !SPECIAL_CHAR_PATTERN.matcher(password).find();
 * }
 * ```
 * 
 * === MVVM PATTERN ===
 * LoginActivity -> LoginViewModel -> FirebaseAuth
 * - Activity chỉ handle UI
 * - ViewModel handle business logic
 * - LiveData cho login state changes
 * 
 * === LIFECYCLE ===
 * 1. onCreate(): Setup UI, inject dependencies
 * 2. performInitialCheck(): Kiểm tra user đã login chưa
 * 3. observeLoginState(): Observe login success/failure
 * 4. onBiometricAuthSuccess(): Auto-login khi biometric OK
 * 
 * === ERROR HANDLING ===
 * - Email invalid: "Email không hợp lệ"
 * - Password wrong: "Sai email hoặc mật khẩu"
 * - Network error: "Lỗi kết nối"
 * - Biometric failed: "Xác thực thất bại"
 * 
 * === NAVIGATION ===
 * - Login success -> MainActivity
 * - "Đăng ký" -> RegisterEmailActivity
 * - "Quên mật khẩu" -> ResetPasswordActivity
 * - Weak password -> ChangePasswordActivity (optional)
 * 
 * === LƯU Ý QUAN TRỌNG ===
 * 1. FLAG_SECURE phải set TRƯỚC setContentView()
 * 2. Weak password check CHỈ 1 LẦN per user
 * 3. Biometric option CHỈ hiển thị nếu device support
 * 4. Clear password field sau login failed
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm "Remember me" option
 * TODO: Implement login rate limiting (chống brute force)
 * TODO: Thêm multi-factor authentication (SMS OTP)
 * TODO: Social login (Google, Facebook)
 * FIXME: Handle biometric lockout (quá nhiều lần thử sai)
 * 
 * @see LoginViewModel Handle login business logic
 * @see BiometricHelper Biometric authentication
 * @see MainActivity Destination sau login
 */
@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity implements BiometricHelper.BiometricAuthListener {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_WEAK_PASSWORD_CHECKED = "weak_password_checked_";
    
    // Password validation patterns
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    private Toolbar mToolbar;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private ProgressBar pbLogin;
    private View mLayoutBiometric;
    private View mGroupEmail, mGroupPassword;
    private Button btnNext;
    private LoginViewModel mViewModel;
    private FirebaseAuth mAuth;
    @Inject
    SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- BẢO MẬT ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        // ----------------

        setContentView(R.layout.activity_login);

        mViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        findViews();
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.setNavigationOnClickListener(v -> finish());

        setupListeners();
        observeLoginState();
        // ===== BƯỚC 3: KIỂM TRA EMAIL TỪ INTENT (SAU KHI ĐĂNG KÝ) =====
        String emailIntent = getIntent().getStringExtra("EMAIL");
        if (emailIntent != null) {
            etEmail.setText(emailIntent);
        } else {
            // Thử lấy email cuối cùng đăng nhập
            String lastEmail = mViewModel.getLastEmail();
            if (lastEmail != null) {
                etEmail.setText(lastEmail);
            }
        }

        observeInitialCheck();

        if (savedInstanceState == null) {
            setLoading(true);
            mViewModel.performInitialCheck();
        }
    }

    // (Giữ nguyên phần code còn lại của LoginActivity như file gốc bạn gửi)
    private void findViews() {
        mToolbar = findViewById(R.id.toolbar_login);
        etEmail = findViewById(R.id.et_login_email);
        etPassword = findViewById(R.id.et_login_password);
        btnLogin = findViewById(R.id.btn_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        pbLogin = findViewById(R.id.pb_login);
        mLayoutBiometric = findViewById(R.id.layout_biometric_login);
        mGroupEmail = findViewById(R.id.group_email_stage);
        mGroupPassword = findViewById(R.id.group_password_stage);
        btnNext = findViewById(R.id.btn_next);
    }

    private void setupListeners() {
        btnNext.setOnClickListener(v -> handleNextStep());
        btnLogin.setOnClickListener(v -> mViewModel.attemptManualLogin(etEmail.getText().toString().trim(), etPassword.getText().toString().trim()));
        tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterEmailActivity.class)));
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        mLayoutBiometric.setOnClickListener(v -> handleBiometricLogin());
    }

    private void handleNextStep() {
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Luôn chuyển sang bước nhập mật khẩu. 
        // Firebase mặc định bật "Email enumeration protection" nên fetchSignInMethodsForEmail sẽ trả về rỗng.
        showPasswordStage();
    }

    private void showPasswordStage() {
        mGroupEmail.setVisibility(View.GONE);
        mGroupPassword.setVisibility(View.VISIBLE);
        etPassword.requestFocus();
    }

    private void handleBiometricLogin() {
        if (BiometricHelper.isBiometricAvailable(this)) {
            setLoading(true);
            BiometricHelper.showBiometricPrompt(this, false, this);
        }
    }

    private void observeInitialCheck() {
        mViewModel.initialState.observe(this, state -> {
            if(state==null) return;
            setLoading(false);
            if(state == LoginViewModel.InitialCheckState.SHOW_BIOMETRIC_PROMPT) {
                mLayoutBiometric.setVisibility(View.VISIBLE);
                // TỰ ĐỘNG: Hiện vân tay luôn mà không chờ user bấm (cho phép dùng PIN thiết bị nếu vân tay lỗi)
                BiometricHelper.showBiometricPrompt(this, false, this);
            }
            else if(state == LoginViewModel.InitialCheckState.NAVIGATE_TO_MAIN) navigateToMain();
        });
    }

    private void observeLoginState() {
        mViewModel.loginState.observe(this, state -> {
            if(state==null) return;
            
            if(state == LoginViewModel.LoginState.LOADING) {
                setLoading(true);
            } else if(state == LoginViewModel.LoginState.SUCCESS) { 
                setLoading(false); 
                
                // Lấy mật khẩu vừa dùng để đăng nhập (thủ công hoặc vân tay)
                String pwd = etPassword.getText().toString();
                if (pwd.isEmpty()) {
                    pwd = mViewModel.getSavedPassword();
                }
                
                checkPasswordStrengthBeforeNavigate(pwd);
            } else if(state == LoginViewModel.LoginState.ERROR_RATE_LIMITED) {
                setLoading(false);
                showRateLimitedError();
            } else if(state == LoginViewModel.LoginState.ERROR_BAD_CREDENTIALS) {
                setLoading(false);
                // Hiển thị cảnh báo nếu còn ít lần thử
                if (mViewModel.shouldShowWarning()) {
                    int remaining = mViewModel.getRemainingAttempts();
                    Toast.makeText(this, 
                        getString(R.string.error_login_failed) + "\n" + 
                        getString(R.string.warning_remaining_attempts, remaining), 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show();
                }
            } else if (state == LoginViewModel.LoginState.ERROR_ENCRYPTION_FAILED) {
                setLoading(false);
                Toast.makeText(this, "Lỗi xác thực khóa mã hóa. Vui lòng kiểm tra lại mật khẩu.", Toast.LENGTH_LONG).show();
            } else if (state == LoginViewModel.LoginState.ERROR_NEEDS_PIN_SETUP) {
                setLoading(false);
                // Chuyển sang màn hình Setup PIN
                Intent intent = new Intent(this, PasscodeSetupActivity.class);
                startActivity(intent);
                finish();
            } else { 
                setLoading(false); 
                Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show(); 
            }
        });
    }
    
    private void showRateLimitedError() {
        long remainingSeconds = mViewModel.getRemainingLockTimeSeconds();
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;
        
        String message;
        if (minutes > 0) {
            message = getString(R.string.error_rate_limited_minutes, minutes, seconds);
        } else {
            message = getString(R.string.error_rate_limited_seconds, seconds);
        }
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.title_account_locked)
            .setMessage("Tài khoản của bạn tạm thời bị khóa do nhập sai nhiều lần. Vui lòng thử lại sau " + mViewModel.getRemainingLockTimeSeconds() + " giây.")
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void checkPasswordStrengthBeforeNavigate(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToMain();
            return;
        }
        
        if (isPasswordStrong(password)) {
            // Strong password, proceed to main
            navigateToMain();
        } else {
            // Weak password detected - MUST change password (mandatory)
            showWeakPasswordDialog();
        }
    }
    
    private boolean isPasswordStrong(String password) {
        if (TextUtils.isEmpty(password) || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).find();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).find();
        boolean hasNumber = NUMBER_PATTERN.matcher(password).find();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).find();
        
        return hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
    }
    
    private void showWeakPasswordDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.title_weak_password_detected)
            .setMessage(R.string.msg_weak_password_detected)
            .setCancelable(false)
            .setPositiveButton(R.string.btn_change_password_now, (dialog, which) -> {
                // Navigate to ChangePasswordActivity (MANDATORY)
                Intent intent = new Intent(this, ChangePasswordActivity.class);
                startActivity(intent);
                finish();
            })
            .show();
    }

    private void showForgotPasswordDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        final TextInputEditText etEmailInput = dialogView.findViewById(R.id.et_dialog_email);

        new AlertDialog.Builder(this)
                .setTitle(R.string.title_password_recovery)
                .setMessage(R.string.msg_password_recovery)
                .setView(dialogView)
                .setPositiveButton(R.string.send, (dialog, which) -> {
                    String email = etEmailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, R.string.error_email_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, R.string.error_email_invalid, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Send password reset email (sẽ redirect về ResetPasswordActivity)
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(aVoid -> {
                                new AlertDialog.Builder(LoginActivity.this)
                                    .setTitle(R.string.title_email_sent)
                                    .setMessage(getString(R.string.msg_email_sent_check))
                                    .setPositiveButton("OK", null)
                                    .show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error_with_message, e.getMessage()), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        pbLogin.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }

    @Override
    public void onBiometricAuthSuccess() { 
        // Lấy mật khẩu đã lưu
        String savedPassword = mViewModel.getSavedPassword();
        if (savedPassword != null) {
            String email = etEmail.getText().toString().trim();
            
            // Nếu email chưa nhập, dùng email cuối cùng
            if (TextUtils.isEmpty(email)) {
                email = mViewModel.getLastEmail();
            }
            
            if (!TextUtils.isEmpty(email)) {
                setLoading(true);
                mViewModel.attemptManualLogin(email, savedPassword);
            } else {
                setLoading(false);
                Toast.makeText(this, "Vui lòng nhập Email trước", Toast.LENGTH_SHORT).show();
            }
        } else {
            setLoading(false);
            Toast.makeText(this, "Chưa thiết lập Vân tay cho tài khoản này", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onBiometricAuthError(String errorMessage) { setLoading(false); Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show(); }
    @Override
    public void onBiometricAuthFailed() { }
}