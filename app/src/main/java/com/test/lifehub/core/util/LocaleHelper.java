package com.test.lifehub.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/**
 * Helper class để quản lý ngôn ngữ của ứng dụng
 */
public class LocaleHelper {

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_VIETNAMESE = "vi";
    
    private static final String PREF_NAME = "LifeHubLanguagePrefs";
    private static final String KEY_LANGUAGE = "app_language";

    /**
     * Áp dụng ngôn ngữ cho Context
     */
    public static Context setLocale(Context context, String language) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, language);
        }
        return updateResourcesLegacy(context, language);
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        configuration.setLayoutDirection(locale);

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }

    /**
     * Lưu ngôn ngữ đã chọn vào SharedPreferences thông thường (không mã hóa)
     * Dùng SharedPreferences thông thường vì cần truy cập sớm trong Application lifecycle
     */
    public static void saveLanguage(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    /**
     * Lấy ngôn ngữ đã lưu từ SharedPreferences thông thường
     */
    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedLanguage = prefs.getString(KEY_LANGUAGE, null);
        
        // Nếu chưa có ngôn ngữ đã lưu, dùng ngôn ngữ hệ thống
        if (savedLanguage == null || savedLanguage.isEmpty()) {
            String systemLang = Locale.getDefault().getLanguage();
            // Chỉ hỗ trợ tiếng Anh và tiếng Việt
            if (systemLang.equals(LANGUAGE_VIETNAMESE)) {
                return LANGUAGE_VIETNAMESE;
            }
            return LANGUAGE_ENGLISH; // Mặc định tiếng Anh
        }
        
        return savedLanguage;
    }

    /**
     * Lấy tên hiển thị của ngôn ngữ
     */
    public static String getLanguageDisplayName(String language) {
        switch (language) {
            case LANGUAGE_VIETNAMESE:
                return "Tiếng Việt";
            case LANGUAGE_ENGLISH:
                return "English";
            default:
                return "English";
        }
    }
}
