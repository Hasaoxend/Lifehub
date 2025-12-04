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

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Công cụ Mã hóa/Giải mã Dữ liệu Nhạy cảm
 * 
 * Class này cung cấp các phương thức để mã hóa và giải mã dữ liệu nhạy cảm 
 * (như mật khẩu tài khoản) trước khi lưu lên Firestore.
 * 
 * Thuật toán mã hóa:
 * - AES-GCM (Advanced Encryption Standard - Galois/Counter Mode)
 * - Độ dài khóa: 256-bit (rất an toàn)
 * - IV (Initialization Vector): 12 bytes, ngẫu nhiên cho mỗi lần mã hóa
 * - Tag: 128-bit để xác thực tính toàn vẹn dữ liệu
 * 
 * Cách hoạt động:
 * 1. Khóa mã hóa được tạo ngẫu nhiên và lưu trong EncryptedSharedPreferences
 * 2. Mỗi lần mã hóa: tạo IV mới -> mã hóa -> ghép IV và CipherText -> Base64
 * 3. Giải mã: tách IV và CipherText -> giải mã bằng khóa -> trả về plaintext
 * 
 * Lưu ý: Singleton để sử dụng chung trong toàn app và tránh tạo khóa mới nhiều lần.
 */
@Singleton
public class EncryptionHelper {

    private static final String TAG = "EncryptionHelper";
    private static final String KEY_PREFS_NAME = "lifehub_secure_keys";  // Tên file lưu khóa mã hóa
    private static final String ALIAS_DATA_KEY = "data_encryption_key";  // Khóa lưu trong SecurePrefs
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";    // Thuật toán mã hóa
    private static final int TAG_LENGTH_BIT = 128;      // Độ dài tag xác thực (128-bit)
    private static final int IV_LENGTH_BYTE = 12;       // Độ dài IV (12 bytes cho GCM)

    private final SharedPreferences securePrefs;
    private byte[] secretKeyBytes; // Khóa AES 256-bit dưới dạng byte array

    /**
     * Khởi tạo EncryptionHelper với Dependency Injection
     * 
     * @ApplicationContext: Bắt Hilt cung cấp Application Context (không phải Activity Context)
     *                      Điều này quan trọng để tránh memory leak khi lưu instance Singleton
     * 
     * Quá trình khởi tạo:
     * 1. Tạo EncryptedSharedPreferences để lưu khóa mã hóa an toàn
     * 2. Tải khóa AES hiện có hoặc tạo khóa mới nếu chưa tồn tại
     */
    @Inject
    public EncryptionHelper(@ApplicationContext Context context) {
        this.securePrefs = createSecurePrefs(context);
        loadOrGenerateKey();
    }

    /**
     * Tạo bộ nhớ mã hóa để lưu khóa AES một cách an toàn
     * 
     * Sử dụng EncryptedSharedPreferences với MasterKey (AES256-GCM)
     * Khóa này được quản lý bởi Android Keystore System - rất an toàn
     * 
     * @return SharedPreferences đã mã hóa, hoặc null nếu khởi tạo thất bại
     */
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
     * Tải khóa AES từ bộ nhớ bảo mật, hoặc tạo khóa mới nếu chưa có
     * 
     * Quy trình:
     * 1. Kiểm tra xem đã có khóa trong SecurePrefs chưa
     * 2. Nếu chưa: tạo khóa ngẫu nhiên 256-bit bằng SecureRandom
     * 3. Mã hóa khóa thành Base64 và lưu vào SecurePrefs
     * 4. Chuyển Base64 thành byte array để sử dụng
     * 
     * Lưu ý: SecureRandom đảm bảo khóa thật sự ngẫu nhiên, không thể dự đoán
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
     * Mã hóa văn bản thành chuỗi Base64 bằng thuật toán AES-GCM
     * 
     * Quy trình mã hóa:
     * 1. Tạo IV (Initialization Vector) ngẫu nhiên 12 bytes
     * 2. Khởi tạo AES Cipher với chế độ GCM (Galois/Counter Mode)
     * 3. Mã hóa plaintext thành ciphertext
     * 4. Ghép IV + CipherText vào một mảng byte
     * 5. Chuyển sang Base64 để dễ lưu trữ và truyền tải
     * 
     * @param plainText Văn bản gốc cần mã hóa (ví dụ: mật khẩu)
     * @return Chuỗi Base64 chứa [IV + CipherText], hoặc chuỗi rỗng nếu lỗi
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

            // Ghép IV và CipherText vào một mảng byte
            // Định dạng: [12 bytes IV][N bytes CipherText]
            // Khi giải mã, ta sẽ tách 12 bytes đầu làm IV, phần còn lại là CipherText
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
     * Giải mã chuỗi Base64 đã mã hóa về văn bản gốc
     * 
     * Quy trình giải mã:
     * 1. Chuyển chuỗi Base64 về mảng byte
     * 2. Tách 12 bytes đầu làm IV
     * 3. Phần còn lại là CipherText
     * 4. Khởi tạo AES Cipher với chế độ DECRYPT và IV tách được
     * 5. Giải mã CipherText thành plaintext
     * 
     * @param encryptedText Chuỗi Base64 đã mã hóa
     * @return Văn bản gốc, hoặc chuỗi gốc nếu giải mã thất bại (hỗ trợ dữ liệu cũ )
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        if (secretKeyBytes == null) return encryptedText;

        try {
            byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);

            // Tách IV (12 bytes đầu tiên)
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // Tách CipherText (phần còn lại)
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