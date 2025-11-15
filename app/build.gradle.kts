// File: Lifehub/app/build.gradle.kts
// (Chép đè toàn bộ nội dung này)

plugins {
    alias(libs.plugins.android.application)

    // ----- SỬA LỖI Ở ĐÂY -----
    alias(libs.plugins.google.gms.services) // KHÔNG CÓ "apply false"

    // (Các plugin Hilt, Kotlin... của bạn)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.kapt)
}

android {
    namespace = "com.test.lifehub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.test.lifehub"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Rất tốt
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Khối này bây giờ sẽ hợp lệ
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ----- FIREBASE -----
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // ----- BIOMETRIC (Vân tay) -----
    implementation(libs.biometric)

    // ----- SECURITY (Mã hóa) -----
    implementation(libs.security.crypto)

    // ----- HILT DEPENDENCIES -----
    implementation(libs.google.dagger.hilt.android)
    kapt(libs.google.dagger.hilt.compiler) // Dùng kapt() ở đây

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// ----- KHỐI NÀY VẪN GIỮ NGUYÊN -----
kapt {
    correctErrorTypes = true
}