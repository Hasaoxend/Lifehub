package com.test.lifehub.core.security;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.test.lifehub.R;

import java.util.concurrent.Executor;

/**
 * BiometricHelper - Xác thực Sinh trắc học (Biometric Authentication)
 */
public class BiometricHelper {

    public interface BiometricAuthListener {
        void onBiometricAuthSuccess();
        void onBiometricAuthError(String errorMessage);
        void onBiometricAuthFailed();
    }

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Hiển thị Biometric Prompt
     * @param mandatory Nếu true: Chỉ dùng vân tay (BIOMETRIC_STRONG). Nếu false: Cho phép PIN/Pattern.
     */
    public static void showBiometricPrompt(AppCompatActivity activity, boolean mandatory, BiometricAuthListener listener) {
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt.AuthenticationCallback authCallback =
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            listener.onBiometricAuthError(activity.getString(R.string.biometric_error_user_canceled));
                        } else {
                            listener.onBiometricAuthError(activity.getString(R.string.biometric_error_auth, errString));
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        listener.onBiometricAuthSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        listener.onBiometricAuthFailed();
                    }
                };

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, authCallback);

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.title_biometric_auth))
                .setSubtitle(activity.getString(R.string.biometric_subtitle))
                .setDescription(activity.getString(R.string.biometric_description));

        if (mandatory) {
            // Chỉ vân tay
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG);
            builder.setNegativeButtonText(activity.getString(R.string.biometric_cancel));
        } else {
            // Vân tay + PIN/Pattern
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            // Lưu ý: Không được setNegativeButtonText khi dùng DEVICE_CREDENTIAL
        }

        biometricPrompt.authenticate(builder.build());
    }
    
    /**
     * Overload mặc định (không bắt buộc)
     */
    public static void showBiometricPrompt(AppCompatActivity activity, BiometricAuthListener listener) {
        showBiometricPrompt(activity, false, listener);
    }
}