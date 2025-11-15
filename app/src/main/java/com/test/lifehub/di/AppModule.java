package com.test.lifehub.di;

import android.app.Application; // <-- XÓA IMPORT NÀY (HOẶC GIỮ NẾU CẦN)
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.test.lifehub.core.util.SessionManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Module Dagger-Hilt để "dạy" Hilt cách cung cấp (provide)
 * các dependencies (phụ thuộc) cho toàn ứng dụng.
 * (Phiên bản đã sửa lỗi Dependency Cycle)
 */
@Module
@InstallIn(SingletonComponent.class) // Tồn tại suốt vòng đời ứng dụng
public class AppModule {

    // ----- XÓA 2 HÀM DƯ THỪA -----
    // Hilt đã tự biết cách cung cấp Application và @ApplicationContext
    // nên 2 hàm 'provideApplicationContext' và 'provideApplication'
    // đã bị xóa để tránh vòng lặp phụ thuộc (dependency cycle).
    //
    // @Provides
    // @Singleton
    // public Context provideApplicationContext(@ApplicationContext Context context) {
    //     return context;
    // }
    //
    // @Provides
    // @Singleton
    // public Application provideApplication(Application application) {
    //     return application;
    // }
    // ---------------------------------


    /**
     * Dạy Hilt cách tạo SessionManager.
     * Nó cần Context, và Hilt sẽ tự động cung cấp @ApplicationContext.
     */
    @Provides
    @Singleton
    public SessionManager provideSessionManager(@ApplicationContext Context context) {
        // Hilt tự động biết @ApplicationContext là gì,
        // chúng ta không cần "provide" nó nữa.
        return new SessionManager(context);
    }

    /**
     * Dạy Hilt cách cung cấp (provide) FirebaseAuth.
     * Đây là nơi duy nhất gọi getInstance().
     */
    @Provides
    @Singleton
    public FirebaseAuth provideFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    /**
     * Dạy Hilt cách cung cấp (provide) FirebaseFirestore.
     * Đây là nơi duy nhất gọi getInstance().
     */
    @Provides
    @Singleton
    public FirebaseFirestore provideFirestore() {
        return FirebaseFirestore.getInstance();
    }
}