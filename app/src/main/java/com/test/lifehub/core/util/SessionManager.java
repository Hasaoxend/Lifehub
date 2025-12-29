package com.test.lifehub.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Quản lý phiên đăng nhập và các thiết lập bảo mật của ứng dụng
 * 
 * Class này sử dụng EncryptedSharedPreferences để lưu trữ dữ liệu một cách an toàn:
 * - Thông tin đăng nhập (token, trạng thái)
 * - Cài đặt sinh trắc học (biometric)
 * - Ngôn ngữ và giao diện (theme, language)
 * - Tài khoản TOTP (2FA)
 * - Trạng thái lần đầu mở app
 * 
 * Tất cả dữ liệu được mã hóa bằng AES256-GCM trước khi lưu vào thiết bị.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";
    private static final String PREF_NAME = "LifeHubUserSession_Secure"; // Tên file lưu trữ mã hóa

    // Các khóa lưu trữ thông tin đăng nhập
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";      // Đã đăng nhập chưa?
    private static final String KEY_USER_TOKEN = "user_token";          // Token xác thực của user
    private static final String KEY_BIOMETRIC_ENABLED = "is_biometric_enabled"; // Bật sinh trắc học?
    private static final String KEY_THEME_MODE = "theme_mode";          // Chế độ giao diện (sáng/tối)
    private static final String KEY_ENCRYPTION_PASSWORD = "enc_pwd";    // Mật khẩu login (để biometric unlock encryption key)

    // Khóa lưu trạng thái lần đầu mở app
    private static final String KEY_IS_FIRST_RUN = "is_first_run";
    private static final String KEY_LAST_EMAIL = "last_email";
    
    // Khóa lưu ngôn ngữ ứng dụng
    private static final String KEY_LANGUAGE = "app_language";
    
    // Khóa lưu thông tin TOTP (xác thực 2 yếu tố)
    private static final String KEY_TOTP_ACCOUNTS = "totp_accounts"; // Danh sách tài khoản TOTP (dạng JSON)
    private static final String KEY_TOTP_ENABLED = "totp_enabled";    // Đã bật TOTP chưa?
    
    // File preferences riêng cho Autofill (PLAIN - không encrypted để service đọc được)
    private static final String AUTOFILL_PREF_NAME = "lifehub_autofill_prefs";
    public static final String KEY_AUTOFILL_BIOMETRIC = "autofill_biometric_enabled";
    public static final String KEY_AUTOFILL_SERVICE = "autofill_service_enabled";

    private SharedPreferences sharedPreferences;
    private SharedPreferences autofillPreferences; // Plain prefs cho autofill

    /**
     * Khởi tạo SessionManager với bộ nhớ mã hóa an toàn
     * 
     * Sử dụng EncryptedSharedPreferences với:
     * - MasterKey: AES256-GCM (khóa chính)
     * - PrefKey: AES256-SIV (mã hóa tên key)
     * - PrefValue: AES256-GCM (mã hóa giá trị)
     */
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
            
            // Plain preferences cho autofill (có thể đọc từ process khác)
            autofillPreferences = context.getSharedPreferences(AUTOFILL_PREF_NAME, Context.MODE_PRIVATE);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "CRITICAL: Không thể tạo bộ nhớ bảo mật.", e);
            sharedPreferences = null;
            autofillPreferences = context.getSharedPreferences(AUTOFILL_PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    /**
     * Kiểm tra xem bộ nhớ bảo mật đã khởi tạo thành công chưa
     * Nếu không thành công, mọi thao tác sẽ không thực hiện để tránh lỗi
     */
    private boolean isSecure() {
        return sharedPreferences != null;
    }

    /**
     * Tạo phiên đăng nhập mới cho người dùng
     * Lưu token xác thực và đánh dấu là đã đăng nhập
     * 
     * @param token Token xác thực từ Firebase Authentication
     */
    public void createLoginSession(String token, String email) {
        if (!isSecure()) return;
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_TOKEN, token)
                .putString(KEY_LAST_EMAIL, email)
                .apply();
    }

    /**
     * Lấy token xác thực của người dùng hiện tại
     * @return Token xác thực, hoặc null nếu chưa đăng nhập
     */
    public String getUserToken() {
        if (!isSecure()) return null;
        return sharedPreferences.getString(KEY_USER_TOKEN, null);
    }

    /**
     * Đăng xuất người dùng
     * Xóa token và trạng thái đăng nhập (nhưng giữ lại các cài đặt khác)
     */
    public void logoutUser() {
        if (!isSecure()) return;
        sharedPreferences.edit()
                .remove(KEY_IS_LOGGED_IN)
                .remove(KEY_USER_TOKEN)
                .remove(KEY_ENCRYPTION_PASSWORD)
                .apply();
    }

    /**
     * Lưu chế độ giao diện (sáng/tối/theo hệ thống)
     * @param mode Mã chế độ: AppCompatDelegate.MODE_NIGHT_YES/NO/FOLLOW_SYSTEM
     */
    public void setThemeMode(int mode) {
        if (isSecure()) sharedPreferences.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    /**
     * Lấy chế độ giao diện đã lưu
     * @return Mã chế độ, hoặc -1 nếu chưa có cài đặt
     */
    public int getThemeMode() {
        return isSecure() ? sharedPreferences.getInt(KEY_THEME_MODE, -1) : -1;
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập chưa
     * @return true nếu đã đăng nhập và có token hợp lệ
     */
    public boolean isLoggedIn() {
        return isSecure() && sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Bật/tắt xác thực sinh trắc học (vân tay, khuôn mặt)
     * @param enabled true để bật, false để tắt
     */
    public void setBiometricEnabled(boolean enabled) {
        if(isSecure()) sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
        
        // ⚠️ Sync với autofillPreferences để service đọc được
        // Chỉ lưu boolean flag, không phải data nhạy cảm -> an toàn với plain prefs
        if (autofillPreferences != null) {
            autofillPreferences.edit().putBoolean(KEY_AUTOFILL_BIOMETRIC, enabled).apply();
        }
    }

    /**
     * Kiểm tra xem xác thực sinh trắc học đã được bật chưa
     * @return true nếu đã bật xác thực sinh trắc học
     */
    public boolean isBiometricEnabled() {
        return isSecure() && sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    
    // --- AUTOFILL SERVICE ---
    
    private static final String KEY_AUTOFILL_ENABLED = "autofill_enabled";
    
    /**
     * Bật/tắt dịch vụ Autofill
     * ⚠️ RÀNG BUỘC: Chỉ bật được khi Biometric đã bật
     * @param enabled true để bật, false để tắt
     */
    public void setAutofillEnabled(boolean enabled) {
        if(isSecure()) sharedPreferences.edit().putBoolean(KEY_AUTOFILL_ENABLED, enabled).apply();
        
        // ⚠️ Sync với autofillPreferences để service đọc được
        if (autofillPreferences != null) {
            autofillPreferences.edit().putBoolean(KEY_AUTOFILL_SERVICE, enabled).apply();
        }
    }
    
    /**
     * Kiểm tra xem dịch vụ Autofill đã được bật chưa
     * @return true nếu đã bật Autofill
     */
    public boolean isAutofillEnabled() {
        return isSecure() && sharedPreferences.getBoolean(KEY_AUTOFILL_ENABLED, false);
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

    // --- CÁC HÀM CHO TOTP ---

    /**
     * Lưu danh sách tài khoản TOTP
     * @param accountsJson JSON string chứa danh sách tài khoản
     */
    public void saveTotpAccounts(String accountsJson) {
        if (isSecure()) {
            sharedPreferences.edit().putString(KEY_TOTP_ACCOUNTS, accountsJson).apply();
        }
    }

    /**
     * Lấy danh sách tài khoản TOTP
     * @return JSON string chứa danh sách tài khoản
     */
    public String getTotpAccounts() {
        if (!isSecure()) return "[]";
        return sharedPreferences.getString(KEY_TOTP_ACCOUNTS, "[]");
    }

    /**
     * Bật/tắt tính năng TOTP
     * @param enabled true để bật, false để tắt
     */
    public void setTotpEnabled(boolean enabled) {
        if (isSecure()) {
            sharedPreferences.edit().putBoolean(KEY_TOTP_ENABLED, enabled).apply();
        }
    }

    /**
     * Kiểm tra xem TOTP đã được bật chưa
     * @return true nếu TOTP đã bật
     */
    public boolean isTotpEnabled() {
        return isSecure() && sharedPreferences.getBoolean(KEY_TOTP_ENABLED, false);
    }

    // --- CÁC HÀM CHO NGÔN NGỮ ---

    /**
     * Lưu ngôn ngữ đã chọn
     * @param language Mã ngôn ngữ ("en" hoặc "vi")
     */
    public void setLanguage(String language) {
        if (isSecure()) {
            sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply();
        }
    }

    /**
     * Lấy ngôn ngữ đã lưu
     * @return Mã ngôn ngữ đã lưu, hoặc null nếu chưa có
     */
    public String getLanguage() {
        if (!isSecure()) return null;
        return sharedPreferences.getString(KEY_LANGUAGE, null);
    }
    /**
     * Lưu mật khẩu đăng nhập (MÃ HÓA) để dùng cho biometric unlock encryption key
     * @param password Mật khẩu người dùng nhập khi login
     */
    public void saveEncryptionPassword(String password) {
        if (isSecure()) {
            sharedPreferences.edit().putString(KEY_ENCRYPTION_PASSWORD, password).apply();
        }
    }

    /**
     * Lấy mật khẩu đăng nhập đã lưu
     * @return Mật khẩu, hoặc null nếu chưa lưu
     */
    public String getEncryptionPassword() {
        if (!isSecure()) return null;
        return sharedPreferences.getString(KEY_ENCRYPTION_PASSWORD, null);
    }

    /**
     * Xóa mật khẩu đăng nhập đã lưu (khi logout hoặc tắt biometric)
     */
    public void clearEncryptionPassword() {
        if (isSecure()) {
            sharedPreferences.edit().remove(KEY_ENCRYPTION_PASSWORD).apply();
        }
    }
    /**
     * Lấy email đăng nhập cuối cùng
     */
    public String getLastEmail() {
        if (!isSecure()) return null;
        return sharedPreferences.getString(KEY_LAST_EMAIL, null);
    }
}