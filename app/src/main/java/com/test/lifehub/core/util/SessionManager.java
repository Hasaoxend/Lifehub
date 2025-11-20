package com.test.lifehub.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "LifeHubUserSession_Secure"; // Đổi tên file để reset data cũ không an toàn

    // Key lưu trữ
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token"; // Token này sẽ được MÃ HÓA tự động
    private static final String KEY_BIOMETRIC_ENABLED = "is_biometric_enabled";
    private static final String KEY_THEME_MODE = "theme_mode";

    private SharedPreferences sharedPreferences;

    public SessionManager(Context context) {
        try {
            // Sử dụng MasterKey (Chuẩn mới thay cho MasterKeys deprecated)
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "CRITICAL: Không thể tạo bộ nhớ bảo mật.", e);
            // TUYỆT ĐỐI KHÔNG fallback về MODE_PRIVATE.
            // Nếu lỗi, ứng dụng nên crash hoặc thông báo lỗi thay vì lộ dữ liệu.
            sharedPreferences = null;
        }
    }

    // Helper check lỗi
    private boolean isSecure() {
        return sharedPreferences != null;
    }

    public void createLoginSession(String token) {
        if (!isSecure()) return;
        // EncryptedSharedPreferences sẽ tự động MÃ HÓA token bằng AES-256 trước khi lưu
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_TOKEN, token)
                .apply();
    }

    public String getUserToken() {
        if (!isSecure()) return null;
        // Tự động GIẢI MÃ khi đọc ra
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    public void logoutUser() {
        if (!isSecure()) return;
        sharedPreferences.edit().remove(KEY_IS_LOGGED_IN).remove(KEY_USER_TOKEN).apply();
    }

    public void setThemeMode(int mode) {
        if (isSecure()) sharedPreferences.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode() {
        return isSecure() ? sharedPreferences.getInt(KEY_THEME_MODE, -1) : -1;
    }

    public boolean isLoggedIn() {
        return isSecure() && sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        if(isSecure()) sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return isSecure() && sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }
}