package com.test.lifehub.core.security;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.test.lifehub.R;
import com.test.lifehub.ui.LoginActivity;

import java.util.concurrent.Executor;

/**
 * BiometricHelper - Xác thực Sinh trắc học (Biometric Authentication)
 * 
 * === MỤC ĐÍCH ===
 * Cung cấp static helper methods để xác thực user bằng:
 * - Vân tay (Fingerprint)
 * - Nhận diện khuôn mặt (Face Recognition)
 * - Iris scan (nếu thiết bị hỗ trợ)
 * 
 * === API SỬ DỤNG ===
 * AndroidX Biometric Library:
 * - BiometricManager: Kiểm tra khả năng hỗ trợ biometric
 * - BiometricPrompt: Hiển thị dialog xác thực
 * - Authenticators.BIOMETRIC_STRONG: Yêu cầu mức bảo mật cao (Class 3)
 * 
 * === CLASS 3 BIOMETRIC (STRONG) ===
 * BIOMETRIC_STRONG đảm bảo:
 * 1. False Accept Rate (FAR) < 0.002%
 * 2. Spoof và Impersonation resistance
 * 3. Phù hợp cho financial transactions
 * 
 * === KIẾN TRÚC ===
 * Static Helper Pattern:
 * - Không cần khởi tạo instance
 * - Gọi trực tiếp: BiometricHelper.isBiometricAvailable(context)
 * - Nhẹ và dễ sử dụng
 * 
 * === VÍ DỤ SỬ DỤNG ===
 * ```java
 * // 1. Kiểm tra thiết bị có hỗ trợ biometric không
 * if (BiometricHelper.isBiometricAvailable(this)) {
 *     // 2. Hiển thị prompt
 *     BiometricHelper.showBiometricPrompt(this, new BiometricAuthListener() {
 *         @Override
 *         public void onBiometricAuthSuccess() {
 *             // Xác thực thành công -> Mở khóa app
 *             startActivity(new Intent(this, MainActivity.class));
 *         }
 * 
 *         @Override
 *         public void onBiometricAuthError(String errorMessage) {
 *             // Lỗi (user hủy, quá nhiều lần thử, ...)
 *             Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
 *         }
 * 
 *         @Override
 *         public void onBiometricAuthFailed() {
 *             // Vân tay không khớp (cho phép thử lại)
 *             Toast.makeText(this, "Vân tay không khớp", Toast.LENGTH_SHORT).show();
 *         }
 *     });
 * } else {
 *     // Fallback: Dùng PIN/Password
 *     showPasswordDialog();
 * }
 * ```
 * 
 * === TRẠNG THÁI THIẾT BỊ ===
 * BiometricManager.canAuthenticate() trả về:
 * 
 * 1. BIOMETRIC_SUCCESS:
 *    - Thiết bị có cảm biến
 *    - User đã đăng ký vân tay/khuôn mặt
 *    - Sẵn sàng xác thực ✅
 * 
 * 2. BIOMETRIC_ERROR_NO_HARDWARE:
 *    - Thiết bị không có cảm biến biometric
 *    - Không thể sử dụng tính năng này ❌
 * 
 * 3. BIOMETRIC_ERROR_HW_UNAVAILABLE:
 *    - Có cảm biến nhưng tạm thời không khả dụng
 *    - Thử lại sau hoặc dùng fallback
 * 
 * 4. BIOMETRIC_ERROR_NONE_ENROLLED:
 *    - Thiết bị có cảm biến
 *    - User CHƯA đăng ký vân tay/khuôn mặt
 *    - Gợi ý user vào Settings > Security > Fingerprint
 * 
 * === XỬ LÝ LỖI ===
 * Callback onAuthenticationError() xử lý:
 * - ERROR_USER_CANCELED: User nhấn nút Cancel
 * - ERROR_TIMEOUT: Hết thời gian chờ (30 giây)
 * - ERROR_LOCKOUT: Thử sai quá nhiều lần (30 giây lockout)
 * - ERROR_LOCKOUT_PERMANENT: Lockout vĩnh viễn (cần reboot)
 * 
 * === BẢO MẬT ===
 * ⚠️ QUAN TRỌNG:
 * 1. Biometric authentication CHỈ xác nhận user identity
 * 2. KHÔNG nên dùng để mã hóa dữ liệu (dùng EncryptionHelper)
 * 3. Luôn có fallback method (PIN/Password)
 * 4. Sensitive data cần mã hóa thêm với AES
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm support cho Biometric + CryptoObject
 * TODO: Implement BiometricPrompt.CryptoObject để mã hóa data
 * TODO: Thêm customizable prompt (title, subtitle, description)
 * FIXME: Xử lý trường hợp user thay đổi fingerprint enrolled
 * 
 * @see LoginActivity Sử dụng biometric khi mở app
 * @see EncryptionHelper Kết hợp biometric + encryption
 */
public class BiometricHelper {

    /**
     * BiometricAuthListener - Callback Interface cho kết quả xác thực
     * 
     * Interface này cho phép Activity/Fragment nhận kết quả xác thực
     * mà không cần tạo inner class phức tạp.
     * 
     * === 3 CALLBACKS ===
     * 1. onBiometricAuthSuccess(): Xác thực thành công
     * 2. onBiometricAuthError(String): Lỗi không thể tiếp tục
     * 3. onBiometricAuthFailed(): Vân tay không khớp (cho phép thử lại)
     */
    public interface BiometricAuthListener {
        /**
         * Gọi khi xác thực THÀNH CÔNG
         * User identity đã được xác nhận.
         */
        void onBiometricAuthSuccess();
        
        /**
         * Gọi khi có LỖI NGHIÊM TRỌNG
         * @param errorMessage Chi tiết lỗi (user-friendly message)
         * 
         * Ví dụ lỗi:
         * - User hủy xác thực
         * - Quá nhiều lần thử (lockout)
         * - Cảm biến không khả dụng
         */
        void onBiometricAuthError(String errorMessage);
        
        /**
         * Gọi khi xác thực THẤT BẠI (nhưng cho phép thử lại)
         * Ví dụ: Vân tay không khớp, thử ngón tay khác
         * 
         * Lưu ý: Callback này GỌI NHIỀU LẦN nếu user thử tiếp
         */
        void onBiometricAuthFailed();
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
                            listener.onBiometricAuthError(activity.getString(R.string.biometric_error_user_canceled));
                        } else {
                            listener.onBiometricAuthError(activity.getString(R.string.biometric_error_auth, errString));
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
                .setTitle(activity.getString(R.string.title_biometric_auth))
                .setSubtitle(activity.getString(R.string.biometric_subtitle))
                .setDescription(activity.getString(R.string.biometric_description))
                .setNegativeButtonText(activity.getString(R.string.biometric_cancel))
                .build();

        // Hiển thị hộp thoại
        biometricPrompt.authenticate(promptInfo);
    }
}