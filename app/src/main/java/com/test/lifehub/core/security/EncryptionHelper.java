package com.test.lifehub.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext; // <-- QUAN TRỌNG: Thêm import này

/**
 * Helper class để Mã hóa/Giải mã dữ liệu nhạy cảm (như mật khẩu tài khoản)
 * trước khi lưu lên Firestore.
 */
@Singleton
public class EncryptionHelper {

    private static final String TAG = "EncryptionHelper";
    private static final String KEY_PREFS_NAME = "lifehub_secure_keys";
    private static final String ALIAS_DATA_KEY = "data_encryption_key";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private final SharedPreferences securePrefs;
    private byte[] secretKeyBytes;

    /**
     * SỬA LỖI TẠI ĐÂY:
     * Thêm @ApplicationContext trước tham số Context.
     * Điều này báo cho Hilt biết hãy cung cấp Application Context.
     */
    @Inject
    public EncryptionHelper(@ApplicationContext Context context) {
        this.securePrefs = createSecurePrefs(context);
        loadOrGenerateKey();
    }

    private SharedPreferences createSecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    KEY_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            Log.e(TAG, "Không thể khởi tạo SecurePrefs", e);
            return null;
        }
    }

    /**
     * Lấy khóa AES từ SecurePrefs hoặc tạo mới nếu chưa có.
     */
    private void loadOrGenerateKey() {
        if (securePrefs == null) return;

        String base64Key = securePrefs.getString(ALIAS_DATA_KEY, null);
        if (base64Key == null) {
            // Tạo key mới 256-bit
            byte[] key = new byte[32];
            new SecureRandom().nextBytes(key);
            base64Key = Base64.encodeToString(key, Base64.DEFAULT);
            securePrefs.edit().putString(ALIAS_DATA_KEY, base64Key).apply();
        }
        secretKeyBytes = Base64.decode(base64Key, Base64.DEFAULT);
    }

    /**
     * Mã hóa văn bản (AES-GCM).
     * @return Chuỗi Base64 chứa (IV + CipherText)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        if (secretKeyBytes == null) return plainText; // Fallback nếu lỗi keystore

        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Kết hợp IV và CipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi mã hóa", e);
            return "";
        }
    }

    /**
     * Giải mã văn bản.
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        if (secretKeyBytes == null) return encryptedText;

        try {
            byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);

            // Tách IV
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // Tách CipherText
            int cipherTextLen = combined.length - IV_LENGTH_BYTE;
            byte[] cipherText = new byte[cipherTextLen];
            System.arraycopy(combined, IV_LENGTH_BYTE, cipherText, 0, cipherTextLen);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plainTextBytes = cipher.doFinal(cipherText);

            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Lỗi giải mã (có thể do sai key hoặc data cũ chưa mã hóa)", e);
            return encryptedText; // Trả về gốc nếu không giải mã được (hỗ trợ data cũ)
        }
    }
}