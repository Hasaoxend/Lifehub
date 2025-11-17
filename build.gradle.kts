// File: Lifehub/build.gradle.kts
// (Đây là file build.gradle.kts ở thư mục root CỦA DỰ ÁN)

plugins {
    // Khai báo (chưa áp dụng - apply false) tất cả các plugin
    // mà các module con sẽ dùng.
    alias(libs.plugins.android.application) apply false

    // ----- SỬA LỖI Ở ĐÂY -----
    alias(libs.plugins.google.gms.services) apply false // ĐẢM BẢO CÓ "apply false"

    // (Các plugin Hilt, Kotlin... của bạn)
    alias(libs.plugins.google.dagger.hilt.android) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.kapt) apply false

}