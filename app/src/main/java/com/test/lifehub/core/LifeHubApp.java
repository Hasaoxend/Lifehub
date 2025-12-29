package com.test.lifehub.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.test.lifehub.core.security.EncryptionManager;
import com.test.lifehub.core.util.LocaleHelper;
import com.test.lifehub.core.util.SessionManager;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Lớp Application tùy chỉnh cho LifeHub.
 * 
 * Quản lý:
 * 1. Theme & Ngôn ngữ
 * 2. Tự động khóa app khi ẩn xuống background (Auto-lock)
 */
@HiltAndroidApp
public class LifeHubApp extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "LifeHubApp";
    private static final long AUTO_LOCK_TIMEOUT = 60 * 1000; // 1 phút (60 giây)

    @Inject
    EncryptionManager encryptionManager;

    private int activityCount = 0;
    private long backgroundTimestamp = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);

        // Khởi tạo các cấu hình cơ bản
        SessionManager sessionManager = new SessionManager(this);
        
        // Luôn mặc định Theme sáng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        // Áp dụng ngôn ngữ
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.setLocale(this, language);
        
        // BẢO MẬT: Tự động khóa khi tắt màn hình
        registerScreenOffReceiver();
    }
    
    /**
     * Đăng ký lắng nghe sự kiện tắt màn hình để khóa dữ liệu ngay lập tức
     */
    private void registerScreenOffReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    Log.d(TAG, "Screen turned off, locking encryption immediately");
                    if (encryptionManager != null) {
                        encryptionManager.lock();
                    }
                }
            }
        }, filter);
    }

    @Override
    protected void attachBaseContext(Context base) {
        String language = LocaleHelper.getLanguage(base);
        super.attachBaseContext(LocaleHelper.setLocale(base, language));
    }

    // --- Activity Lifecycle Callbacks cho Auto-lock ---

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activityCount++;
        
        // Nếu app quay lại từ background
        if (backgroundTimestamp != 0) {
            long timeInBackground = System.currentTimeMillis() - backgroundTimestamp;
            Log.d(TAG, "App foregrounded after " + (timeInBackground / 1000) + "s");
            
            // Nếu quá thời gian timeout, thực hiện khóa encryption
            if (timeInBackground > AUTO_LOCK_TIMEOUT) {
                Log.d(TAG, "Auto-lock timeout exceeded, locking encryption");
                encryptionManager.lock();
            }
            
            backgroundTimestamp = 0; // Reset
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activityCount--;
        
        // Nếu không còn activity nào (app ẩn xuống background hoàn toàn)
        if (activityCount == 0) {
            backgroundTimestamp = System.currentTimeMillis();
            Log.d(TAG, "App backgrounded at " + backgroundTimestamp);
        }
    }

    // Các phương thức khác không dùng nhưng phải override
    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}
}