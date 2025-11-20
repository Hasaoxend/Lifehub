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
    private static final String PREF_NAME = "LifeHubUserSession_Secure";

    // Key lưu trữ
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_BIOMETRIC_ENABLED = "is_biometric_enabled";
    private static final String KEY_THEME_MODE = "theme_mode";

    // --- MỚI: Key lưu trạng thái lần đầu mở app ---
    private static final String KEY_IS_FIRST_RUN = "is_first_run";

    private SharedPreferences sharedPreferences;

    public SessionManager(Context context) {
        try {
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
            sharedPreferences = null;
        }
    }

    private boolean isSecure() {
        return sharedPreferences != null;
    }

    public void createLoginSession(String token) {
        if (!isSecure()) return;
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_TOKEN, token)
                .apply();
    }

    public String getUserToken() {
        if (!isSecure()) return null;
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

    // --- CÁC HÀM MỚI CHO INTRO ---

    /**
     * Lưu trạng thái đã xem Intro hay chưa.
     * @param isFirstRun true: Chưa xem (Lần đầu), false: Đã xem.
     */
    public void setFirstRun(boolean isFirstRun) {
        if (isSecure()) {
            sharedPreferences.edit().putBoolean(KEY_IS_FIRST_RUN, isFirstRun).apply();
        }
    }

    /**
     * Kiểm tra xem có phải lần đầu mở app không.
     * Mặc định trả về TRUE nếu chưa có dữ liệu.
     */
    public boolean isFirstRun() {
        return isSecure() && sharedPreferences.getBoolean(KEY_IS_FIRST_RUN, true);
    }
}