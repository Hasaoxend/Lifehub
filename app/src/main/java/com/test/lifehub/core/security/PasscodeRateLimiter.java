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
 * PasscodeRateLimiter - Chống Brute Force Attack cho PIN
 * 10 lần thử, khóa 5 phút.
 */
@Singleton
public class PasscodeRateLimiter {

    private static final String TAG = "PasscodeRateLimiter";
    private static final String PREF_NAME = "lifehub_passcode_limiter_secure";
    
    // Keys
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_START_TIME = "lockout_start_time";
    
    // Configuration - Match Web
    private static final int MAX_ATTEMPTS = 10;
    private static final long LOCKOUT_DURATION_MS = 5 * 60 * 1000; // 5 phút
    
    private final SharedPreferences securePrefs;
    
    @Inject
    public PasscodeRateLimiter(@ApplicationContext Context context) {
        this.securePrefs = createSecurePrefs(context);
    }
    
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
            Log.e(TAG, "Failed to create secure prefs", e);
            return context.getSharedPreferences(PREF_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }
    
    public boolean isLocked() {
        long lockoutStart = securePrefs.getLong(KEY_LOCKOUT_START_TIME, 0);
        if (lockoutStart == 0) return false;
        
        long elapsed = System.currentTimeMillis() - lockoutStart;
        if (elapsed >= LOCKOUT_DURATION_MS) {
            resetAttempts();
            return false;
        }
        return true;
    }
    
    public void recordFailedAttempt() {
        int attempts = securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        SharedPreferences.Editor editor = securePrefs.edit();
        editor.putInt(KEY_FAILED_ATTEMPTS, attempts);
        
        if (attempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_LOCKOUT_START_TIME, System.currentTimeMillis());
            Log.w(TAG, "Passcode locked after " + attempts + " attempts");
        }
        editor.apply();
    }
    
    public void resetAttempts() {
        securePrefs.edit()
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_START_TIME)
                .apply();
    }
    
    public long getRemainingLockTimeSeconds() {
        if (!isLocked()) return 0;
        long lockoutStart = securePrefs.getLong(KEY_LOCKOUT_START_TIME, 0);
        long elapsed = System.currentTimeMillis() - lockoutStart;
        long remaining = LOCKOUT_DURATION_MS - elapsed;
        return Math.max(0, remaining / 1000);
    }

    public int getRemainingAttempts() {
        return Math.max(0, MAX_ATTEMPTS - securePrefs.getInt(KEY_FAILED_ATTEMPTS, 0));
    }
}
