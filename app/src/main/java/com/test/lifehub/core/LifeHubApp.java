package com.test.lifehub.core;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

import com.test.lifehub.core.util.LocaleHelper;
import com.test.lifehub.core.util.SessionManager;

import dagger.hilt.android.HiltAndroidApp; // <-- THÊM IMPORT NÀY

/**
 * Lớp Application tùy chỉnh cho LifeHub.
 * Lớp này được khởi tạo đầu tiên khi ứng dụng mở lên.
 * Chúng ta dùng nó để áp dụng Giao diện (Sáng/Tối) đã lưu.
 */
@HiltAndroidApp // <-- THÊM CHÚ THÍCH NÀY
public class LifeHubApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Khởi tạo SessionManager
        SessionManager sessionManager = new SessionManager(this);

        // 2. Áp dụng ngôn ngữ đã lưu
        String language = LocaleHelper.getLanguage(this);
        LocaleHelper.setLocale(this, language);

        // 3. Lấy lựa chọn Giao diện (Theme) đã lưu
        int themeMode = sessionManager.getThemeMode();

        // 4. Áp dụng Giao diện đó cho toàn bộ ứng dụng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // Áp dụng ngôn ngữ trước khi khởi tạo context
        String language = LocaleHelper.getLanguage(base);
        super.attachBaseContext(LocaleHelper.setLocale(base, language));
    }
}