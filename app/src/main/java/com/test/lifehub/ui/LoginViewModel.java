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
import com.test.lifehub.core.util.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel cho LoginActivity.
 * Chịu trách nhiệm cho TOÀN BỘ logic:
 * 1. Kiểm tra trạng thái ban đầu (thay thế onStart)
 * 2. Xử lý đăng nhập thủ công
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
        ERROR_EMPTY_FIELDS      // Lỗi: Thiếu thông tin
    }

    // ----- LiveData -----
    private final MutableLiveData<InitialCheckState> _initialState = new MutableLiveData<>(InitialCheckState.IDLE);
    public final LiveData<InitialCheckState> initialState = _initialState;

    private final MutableLiveData<LoginState> _loginState = new MutableLiveData<>(LoginState.IDLE);
    public final LiveData<LoginState> loginState = _loginState;

    // ----- Dependencies (Được Hilt tiêm vào) -----
    private final FirebaseAuth mAuth;
    private final SessionManager mSessionManager;
    private final Application mApplication; // Cần Context cho BiometricHelper

    @Inject
    public LoginViewModel(FirebaseAuth auth, SessionManager sessionManager, Application application) {
        this.mAuth = auth;
        this.mSessionManager = sessionManager;
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
            mSessionManager.createLoginSession(currentUser.getUid());

            // 2. Kiểm tra vân tay
            if (mSessionManager.isBiometricEnabled() && BiometricHelper.isBiometricAvailable(mApplication)) {
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
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            _loginState.setValue(LoginState.ERROR_EMPTY_FIELDS);
            return;
        }

        // 1. Thông báo cho Activity biết: "Đang tải"
        _loginState.setValue(LoginState.LOADING);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        if (user.isEmailVerified()) {
                            // 2a. Thành công
                            mSessionManager.createLoginSession(user.getUid());
                            _loginState.setValue(LoginState.SUCCESS);
                        } else {
                            // 2b. Lỗi: Chưa xác thực
                            mAuth.signOut();
                            _loginState.setValue(LoginState.ERROR_EMAIL_UNVERIFIED);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // 2c. Lỗi: Sai thông tin hoặc lỗi mạng
                    Log.w(TAG, "signInWithEmail:failure", e);
                    _loginState.setValue(LoginState.ERROR_BAD_CREDENTIALS);
                });
    }
}