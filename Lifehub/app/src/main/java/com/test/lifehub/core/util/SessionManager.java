package com.test.lifehub.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate; // <-- Thêm Import
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Lớp tiện ích để quản lý phiên (Session) đăng nhập VÀ Cài đặt Giao diện
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "LifeHubUserSession";
    private static final String KEY_ALIAS = "_lifehub_master_key_alias";

    // Khóa (key) cho các giá trị lưu trữ
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token";
    private static final String KEY_BIOMETRIC_ENABLED = "is_biometric_enabled";

    // ----- CÁC KEY MỚI CHO GIAO DIỆN -----
    private static final String KEY_THEME_MODE = "theme_mode";

    private final SharedPreferences sharedPreferences;

    /**
     * Khởi tạo SessionManager.
     * @param context Context của ứng dụng.
     */
    public SessionManager(Context context) {
        SharedPreferences prefs = null;
        try {
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();

            String masterKeyAlias = MasterKeys.getOrCreate(spec);

            prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Không thể tạo EncryptedSharedPreferences.", e);
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.sharedPreferences = prefs;
    }

    // (Các hàm isLoggedIn, createLoginSession, v.v. giữ nguyên...)

    // ----- CÁC HÀM MỚI CHO GIAO DIỆN -----

    /**
     * Lưu lựa chọn Giao diện của người dùng.
     * @param mode (AppCompatDelegate.MODE_NIGHT_NO, MODE_NIGHT_YES, hoặc MODE_NIGHT_FOLLOW_SYSTEM)
     */
    public void setThemeMode(int mode) {
        sharedPreferences.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /**
     * Lấy lựa chọn Giao diện đã lưu.
     * @return Lựa chọn (mode) đã lưu, mặc định là Tự động theo Hệ thống.
     */
    public int getThemeMode() {
        // Mặc định là -1 (Tự động theo Hệ thống)
        return sharedPreferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // (Các hàm còn lại giữ nguyên...)

    public void createLoginSession(String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_TOKEN, token);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserToken() {
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    public void logoutUser() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Chỉ xóa session, KHÔNG XÓA CÀI ĐẶT (Giao diện, Vân tay)
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_TOKEN);
        editor.apply();
    }

    public void setBiometricEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }
}