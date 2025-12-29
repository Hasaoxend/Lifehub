package com.test.lifehub.ui;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.security.LoginRateLimiter;
import com.test.lifehub.core.util.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel cho LoginActivity.
 * Chịu trách nhiệm cho TOÀN BỘ logic:
 * 1. Kiểm tra trạng thái ban đầu (thay thế onStart)
 * 2. Xử lý đăng nhập thủ công
 * 3. Khởi tạo encryption với login password (Bitwarden approach)
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private static final String TAG = "LoginViewModel";

    // ----- Trạng thái (State) -----
    // Trạng thái kiểm tra ban đầu (thay cho onStart)
    public enum InitialCheckState {
        IDLE,                   // Chờ (chưa làm gì)
        SHOW_LOGIN_FORM,        // Hiển thị form đăng nhập (chưa login)
        SHOW_BIOMETRIC_PROMPT,  // Đã login, hiện nút vân tay
        NAVIGATE_TO_MAIN        // Đã login, không dùng vân tay -> vào thẳng
    }

    // Trạng thái cho nút bấm Đăng nhập
    public enum LoginState {
        IDLE,       // Chờ
        LOADING,    // Đang tải
        SUCCESS,    // Thành công
        ERROR_EMAIL_UNVERIFIED, // Lỗi: Email chưa xác thực
        ERROR_BAD_CREDENTIALS,  // Lỗi: Sai email/pass
        ERROR_EMPTY_FIELDS,     // Lỗi: Thiếu thông tin
        ERROR_RATE_LIMITED,     // Lỗi: Đã bị khóa do thử quá nhiều lần
        ERROR_ENCRYPTION_FAILED, // Lỗi: Không thể khởi tạo mã hóa (vd: Sai master key)
        ERROR_NEEDS_PIN_SETUP    // Mới: Cần thiết lập mã PIN
    }


    // ----- LiveData -----
    private final MutableLiveData<InitialCheckState> _initialState = new MutableLiveData<>(InitialCheckState.IDLE);
    public final LiveData<InitialCheckState> initialState = _initialState;

    private final MutableLiveData<LoginState> _loginState = new MutableLiveData<>(LoginState.IDLE);
    public final LiveData<LoginState> loginState = _loginState;

    // ----- Dependencies (Được Hilt tiêm vào) -----
    private final FirebaseAuth mAuth;
    private final SessionManager mSessionManager;
    private final LoginRateLimiter mRateLimiter;
    private final EncryptionManager mEncryptionManager;
    private final Application mApplication;
    
    // Lưu password tạm để khởi tạo encryption sau khi login thành công
    private String pendingPassword = null;

    @Inject
    public LoginViewModel(FirebaseAuth auth, SessionManager sessionManager, 
                          LoginRateLimiter rateLimiter, EncryptionManager encryptionManager,
                          Application application) {
        this.mAuth = auth;
        this.mSessionManager = sessionManager;
        this.mRateLimiter = rateLimiter;
        this.mEncryptionManager = encryptionManager;
        this.mApplication = application;
    }

    /**
     * Thay thế cho logic trong onStart() của Activity.
     * Chỉ gọi hàm này MỘT LẦN khi ViewModel được tạo.
     */
    public void performInitialCheck() {
        // Chỉ chạy một lần
        if (_initialState.getValue() != InitialCheckState.IDLE) {
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null && currentUser.isEmailVerified()) {
            // 1. Đã đăng nhập VÀ xác thực
            mSessionManager.createLoginSession(currentUser.getUid(), currentUser.getEmail());

            // 2. Kiểm tra vân tay
            if (mSessionManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(mApplication)) {
                // IMPORTANT: Nếu bật vân tay, PHẢI hiện trình xác thực vân tay trước.
                // Không được tự động vào thẳng Main kể cả khi có password lưu.
                _initialState.setValue(InitialCheckState.SHOW_BIOMETRIC_PROMPT);
            } else {
                _initialState.setValue(InitialCheckState.NAVIGATE_TO_MAIN);
            }

        } else if (currentUser != null && !currentUser.isEmailVerified()) {
            // 3. Đã đăng nhập, nhưng CHƯA xác thực
            mAuth.signOut();
            _initialState.setValue(InitialCheckState.SHOW_LOGIN_FORM);
            // (Chúng ta có thể thêm một LiveData<String> _toastMessage để báo lỗi này)
        } else {
            // 4. CHƯA đăng nhập
            _initialState.setValue(InitialCheckState.SHOW_LOGIN_FORM);
        }
    }

    /**
     * Xử lý khi người dùng nhấn nút "Đăng nhập" thủ công.
     */
    public void attemptManualLogin(String email, String password) {
        // 0. Kiểm tra rate limiting trước
        if (mRateLimiter.isLocked()) {
            _loginState.setValue(LoginState.ERROR_RATE_LIMITED);
            return;
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            _loginState.setValue(LoginState.ERROR_EMPTY_FIELDS);
            return;
        }

        // 1. Thông báo cho Activity biết: "Đang tải"
        _loginState.setValue(LoginState.LOADING);
        
        // Lưu password để khởi tạo encryption sau khi login thành công
        this.pendingPassword = password;

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        if (user.isEmailVerified()) {
                            // 2a. Thành công - reset rate limiter
                            mRateLimiter.resetAttempts();
                            mSessionManager.createLoginSession(user.getUid(), email);

                            // Nếu biometric bật, lưu pass (mã hóa) để sau này tự động unlock encryption key
                            if (mSessionManager.isBiometricEnabled()) {
                                mSessionManager.saveEncryptionPassword(pendingPassword);
                            } else {
                                mSessionManager.clearEncryptionPassword();
                            }
                            
                            // Khởi tạo encryption (thử unlock bằng mật khẩu đăng nhập tạm thời nêú là user cũ)
                            // HOẶC báo cho Activity biết cần qua màn hình setup PIN
                            if (pendingPassword != null) {
                                String secret = pendingPassword;
                                pendingPassword = null;
                                
                                mEncryptionManager.initialize(secret, result -> {
                                    if (result == EncryptionManager.InitResult.SUCCESS) {
                                        Log.d(TAG, "Encryption initialized (Login Password matches)");
                                        _loginState.setValue(LoginState.SUCCESS);
                                    } else if (result == EncryptionManager.InitResult.NEEDS_SETUP) {
                                        Log.d(TAG, "Encryption needs setup");
                                        _loginState.setValue(LoginState.ERROR_NEEDS_PIN_SETUP);
                                    } else {
                                        // FAILURE means the vault is locked (password doesn't match current salt)
                                        // Since Firebase Auth SUCCEEDED, we let them into MainActivity 
                                        // which will immediately redirect them to Enter PIN (MasterPasswordActivity).
                                        Log.i(TAG, "Login password didn't unlock vault (likely PIN is set). Proceeding to main.");
                                        _loginState.setValue(LoginState.SUCCESS);
                                    }
                                });
                            } else {
                                // Nếu không có pass (vd: vân tay), thử check setup trước
                                mEncryptionManager.initialize("", result -> {
                                    if (result == EncryptionManager.InitResult.NEEDS_SETUP) {
                                        _loginState.setValue(LoginState.ERROR_NEEDS_PIN_SETUP);
                                    } else {
                                        // Even on failure, if they are here (authed), allow them to PIN screen
                                        Log.i(TAG, "No initial password, proceeding to main for PIN check");
                                        _loginState.setValue(LoginState.SUCCESS);
                                    }
                                });
                            }

                        } else {
                            // 2b. Lỗi: Chưa xác thực (không tính là failed attempt)
                            mAuth.signOut();
                            _loginState.setValue(LoginState.ERROR_EMAIL_UNVERIFIED);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // 2c. Lỗi: Sai thông tin - ghi nhận failed attempt
                    Log.w(TAG, "signInWithEmail:failure", e);
                    mRateLimiter.recordFailedAttempt();
                    
                    // Kiểm tra nếu bị lock sau lần thử này
                    if (mRateLimiter.isLocked()) {
                        _loginState.setValue(LoginState.ERROR_RATE_LIMITED);
                    } else {
                        _loginState.setValue(LoginState.ERROR_BAD_CREDENTIALS);
                    }
                });
    }
    
    /**
     * Lấy số giây còn lại trước khi hết lock
     */
    public long getRemainingLockTimeSeconds() {
        return mRateLimiter.getRemainingLockTimeSeconds();
    }
    
    /**
     * Lấy số lần còn lại có thể thử
     */
    public int getRemainingAttempts() {
        return mRateLimiter.getRemainingAttempts();
    }
    
    /**
     * Kiểm tra có nên hiện cảnh báo không (còn <= 2 lần)
     */
    public boolean shouldShowWarning() {
        return mRateLimiter.shouldShowWarning();
    }

    /**
     * Lấy mật khẩu đã lưu trong SessionManager
     */
    public String getSavedPassword() {
        return mSessionManager.getEncryptionPassword();
    }

    /**
     * Kích hoạt giải mã encryption thủ công (dành cho Biometric flow ở Activity)
     */
    public void initializeEncryption(String password, EncryptionManager.InitCallback callback) {
        mEncryptionManager.initializeWithLoginPassword(password, false, callback);
    }

    /**
     * Lấy email đăng nhập cuối cùng
     */
    public String getLastEmail() {
        return mSessionManager.getLastEmail();
    }
}