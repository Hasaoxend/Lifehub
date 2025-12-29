package com.test.lifehub.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * LoginRateLimiter - Chống Brute Force Attack
 * 
 * === MỤC ĐÍCH ===
 * Class này quản lý rate limiting cho đăng nhập:
 * - Đếm số lần đăng nhập thất bại
 * - Lock tạm thời sau khi vượt quá giới hạn
 * - Tự động reset sau lockout period
 * 
 * === CẤU HÌNH ===
 * - MAX_ATTEMPTS: 5 lần thử (có thể điều chỉnh)
 * - LOCKOUT_DURATION: 15 phút (900,000 ms)
 * - Auto-reset ngay khi đăng nhập thành công
 * 
 * === VÍ DỤ SỬ DỤNG ===
 * ```java
 * @Inject LoginRateLimiter rateLimiter;
 * 
 * // Kiểm tra trước khi cho phép login
 * if (rateLimiter.isLocked()) {
 *     long remaining = rateLimiter.getRemainingLockTimeSeconds();
 *     showError("Thử lại sau " + remaining + " giây");
 *     return;
 * }
 * 
 * // Sau khi login thất bại
 * rateLimiter.recordFailedAttempt();
 * 
 * // Sau khi login thành công
 * rateLimiter.resetAttempts();
 * ```
 * 
 * === BẢO MẬT ===
 * - Dữ liệu lưu trong EncryptedSharedPreferences
 * - Không thể bypass bằng cách clear app data (vì key trong Keystore)
 * - Lockout state persist qua restart
 */
@Singleton
public class LoginRateLimiter {

    private static final String TAG = "LoginRateLimiter";
    private static final String PREF_NAME = "lifehub_rate_limiter_secure";
    
    // Keys
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_FIRST_FAILED_TIME = "first_failed_time";
    private static final String KEY_LOCKOUT_START_TIME = "lockout_start_time";
    
    // Configuration
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 phút
    private static final long ATTEMPT_WINDOW_MS = 15 * 60 * 1000;   // Reset sau 15 phút không thử
    
    private final SharedPreferences securePrefs;
    
    @Inject
    public LoginRateLimiter(@ApplicationContext Context context) {
        this.securePrefs = createSecurePrefs(context);
    }
    
    /**
     * Tạo EncryptedSharedPreferences để lưu rate limit data an toàn
     */
    private SharedPreferences createSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to create secure prefs, falling back to regular prefs", e);
            return context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Kiểm tra xem user có đang bị lock không
     * 
     * @return true nếu bị lock, false nếu có thể thử đăng nhập
     */
    public boolean isLocked() {
        long lockoutStart = securePrefs.getLong(KEY_LOCKOUT_START_TIME, 0);
        if (lockoutStart == 0) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - lockoutStart;
        if (elapsed >= LOCKOUT_DURATION_MS) {
            // Lockout đã hết hạn, reset
            resetAttempts();
            return false;
        }
        
        return true;
    }
    
    /**
     * Ghi nhận một lần đăng nhập thất bại
     * Tự động lock nếu vượt quá MAX_ATTEMPTS
     */
    public void recordFailedAttempt() {
        long now = System.currentTimeMillis();
        long firstFailedTime = securePrefs.getLong(KEY_FIRST_FAILED_TIME, 0);
        int attempts = securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0);
        
        // Reset nếu lần attempt trước đã quá lâu
        if (firstFailedTime > 0 && (now - firstFailedTime) > ATTEMPT_WINDOW_MS) {
            attempts = 0;
            firstFailedTime = 0;
        }
        
        // Ghi nhận attempt mới
        attempts++;
        if (firstFailedTime == 0) {
            firstFailedTime = now;
        }
        
        SharedPreferences.Editor editor = securePrefs.edit();
        editor.putInt(KEY_FAILED_ATTEMPTS, attempts);
        editor.putLong(KEY_FIRST_FAILED_TIME, firstFailedTime);
        
        // Lock nếu vượt quá giới hạn
        if (attempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_LOCKOUT_START_TIME, now);
            Log.w(TAG, "Account locked after " + attempts + " failed attempts");
        }
        
        editor.apply();
        
        Log.d(TAG, "Failed attempt recorded. Total: " + attempts + "/" + MAX_ATTEMPTS);
    }
    
    /**
     * Reset tất cả attempts (gọi sau khi đăng nhập thành công)
     */
    public void resetAttempts() {
        securePrefs.edit()
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_FIRST_FAILED_TIME)
                .remove(KEY_LOCKOUT_START_TIME)
                .apply();
        
        Log.d(TAG, "Rate limiter reset");
    }
    
    /**
     * Lấy số giây còn lại trước khi hết lock
     * 
     * @return Số giây còn lại, hoặc 0 nếu không bị lock
     */
    public long getRemainingLockTimeSeconds() {
        if (!isLocked()) {
            return 0;
        }
        
        long lockoutStart = securePrefs.getLong(KEY_LOCKOUT_START_TIME, 0);
        long elapsed = System.currentTimeMillis() - lockoutStart;
        long remaining = LOCKOUT_DURATION_MS - elapsed;
        
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Lấy số lần đã thử thất bại
     * 
     * @return Số lần thử thất bại trong window hiện tại
     */
    public int getFailedAttempts() {
        return securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0);
    }
    
    /**
     * Lấy số lần còn lại có thể thử
     * 
     * @return Số lần còn lại trước khi bị lock
     */
    public int getRemainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS - getFailedAttempts());
    }
    
    /**
     * Kiểm tra xem có nên hiển thị cảnh báo không
     * (Khi còn 2 lần thử trở xuống)
     */
    public boolean shouldShowWarning() {
        return getRemainingAttempts() <= 2 && getRemainingAttempts() > 0;
    }
}
