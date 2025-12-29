package com.test.lifehub.core.util;

import android.content.Context;
import android.content.SharedPreferences;
import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class PreferenceManager {

    private static final String PREF_NAME = "LifeHubPrefs";
    private static final String KEY_SAVED_CITY = "saved_city";
    private final SharedPreferences prefs;

    @Inject
    public PreferenceManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveCity(String city) {
        prefs.edit().putString(KEY_SAVED_CITY, city).apply();
    }

    public String getSavedCity() {
        return prefs.getString(KEY_SAVED_CITY, null); // Trả về null nếu chưa lưu
    }
}