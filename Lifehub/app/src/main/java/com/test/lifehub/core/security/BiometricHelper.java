package com.test.lifehub.core.security;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity; // Sử dụng AppCompatActivity thay vì Context
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

/**
 * Lớp trợ giúp (Helper) để quản lý tất cả logic liên quan đến
 * Xác thực Sinh trắc học (Biometric - Vân tay, Khuôn mặt).
 */
public class BiometricHelper {

    /**
     * Một Interface (giao diện) để gửi kết quả xác thực
     * trở lại Activity/Fragment đã gọi nó.
     */
    public interface BiometricAuthListener {
        void onBiometricAuthSuccess(); // Gọi khi xác thực thành công
        void onBiometricAuthError(String errorMessage); // Gọi khi có lỗi
        void onBiometricAuthFailed(); // Gọi khi xác thực thất bại (ví dụ: vân tay không khớp)
    }

    /**
     * Kiểm tra xem thiết bị có sẵn sàng cho xác thực sinh trắc học không.
     * @param context Context của ứng dụng.
     * @return true nếu sẵn sàng, false nếu không.
     */
    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Thiết bị có cảm biến và đã đăng ký vân tay/khuôn mặt
                return true;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // Thiết bị không có cảm biến
                return false;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // Cảm biến tạm thời không khả dụng
                return false;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // Thiết bị có cảm biến, nhưng người dùng chưa đăng ký vân tay/khuôn mặt
                // Bạn có thể gợi ý người dùng vào Cài đặt để thêm
                return false;
            default:
                return false;
        }
    }

    /**
     * Hiển thị hộp thoại (prompt) yêu cầu xác thực sinh trắc học.
     * @param activity Activity hiện tại (phải là AppCompatActivity).
     * @param listener Một listener để nhận kết quả trả về.
     */
    public static void showBiometricPrompt(AppCompatActivity activity, BiometricAuthListener listener) {
        // Lấy Executor để chạy callback trên luồng chính (UI thread)
        Executor executor = ContextCompat.getMainExecutor(activity);

        // Tạo callback để xử lý kết quả
        BiometricPrompt.AuthenticationCallback authCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Xử lý các lỗi nghiêm trọng hoặc khi người dùng hủy
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            listener.onBiometricAuthError("Người dùng đã hủy");
                        } else {
                            listener.onBiometricAuthError("Lỗi xác thực: " + errString);
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // Xác thực thành công!
                        listener.onBiometricAuthSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        // Xác thực thất bại (vân tay không khớp, thử lại)
                        listener.onBiometricAuthFailed();
                    }
                };

        // Khởi tạo BiometricPrompt
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, authCallback);

        // Cấu hình thông tin hiển thị trên hộp thoại
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực LifeHub")
                .setSubtitle("Vui lòng xác thực để mở khóa")
                .setDescription("Sử dụng vân tay hoặc khuôn mặt của bạn")
                .setNegativeButtonText("Hủy") // Cho phép người dùng hủy
                .build();

        // Hiển thị hộp thoại
        biometricPrompt.authenticate(promptInfo);
    }
}