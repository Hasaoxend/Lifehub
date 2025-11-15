package com.test.lifehub.di;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.test.lifehub.core.util.PreferenceManager;
import com.test.lifehub.core.util.SessionManager; // <-- Đảm bảo đã import
import com.test.lifehub.features.two_productivity.data.WeatherApiService;

import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    // ===== THÊM LẠI HÀM NÀY NẾU BỊ MẤT =====
    @Provides
    @Singleton
    public SessionManager provideSessionManager(@ApplicationContext Context context) {
        return new SessionManager(context);
    }
    // ======================================

    @Provides
    @Singleton
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    @Provides
    @Singleton
    public FirebaseFirestore provideFirestore() {
        return FirebaseFirestore.getInstance();
    }

    // ----- CÁC HÀM CỦA TÍNH NĂNG THỜI TIẾT (ĐÃ THÊM TRƯỚC ĐÓ) -----

    @Provides
    @Singleton
    public PreferenceManager providePreferenceManager(@ApplicationContext Context context) {
        return new PreferenceManager(context);
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
                // URL CƠ SỞ PHẢI LÀ TÊN MIỀN GỐC (KHÔNG CÓ /data/2.5)
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public WeatherApiService provideWeatherApiService(Retrofit retrofit) {
        return retrofit.create(WeatherApiService.class);
    }


}