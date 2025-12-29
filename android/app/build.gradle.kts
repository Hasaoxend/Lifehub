plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.services)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.kapt)
}

android {
    namespace = "com.test.lifehub"
    compileSdk = 35 // Android 15 (Stable)
    
    defaultConfig {
        applicationId = "com.test.lifehub"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Đọc API key từ local.properties (an toàn, không commit lên Git)
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${properties.getProperty("OPENWEATHER_API_KEY", "")}\"")
    }
    
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    // API WEATHER & GSON (Giữ lại 1 bản chuẩn từ libs version catalog)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)


    // FIREBASE
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)

    // BIOMETRIC & SECURITY
    implementation(libs.biometric)
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Cập nhật bản mới nhất cho MasterKey

    // HILT DEPENDENCIES
    implementation(libs.google.dagger.hilt.android)
    implementation(libs.swiperefreshlayout)
    kapt(libs.google.dagger.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Mockito for unit testing
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    
    // Android Architecture Components testing
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Coroutines testing (if needed)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Calendar
    implementation("com.kizitonwose.calendar:view:2.4.1")

    // TOTP & QR Scanner
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)
    implementation(libs.commons.codec)
    implementation(libs.barcode.scanning)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
}

kapt {
    correctErrorTypes = true
}