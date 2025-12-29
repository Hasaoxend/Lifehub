package com.test.lifehub.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * CrossPlatformEncryptionHelper - Mã hóa đa nền tảng với Login Password
 * 
 * === APPROACH: BITWARDEN/PROTON ===
 * Sử dụng password đăng nhập Firebase để derive encryption key.
 * - User chỉ cần nhớ 1 password (login = encryption)
 * - Key được derive bằng PBKDF2, KHÔNG lưu trên server
 * - Salt được lưu trên Firestore để sync giữa các devices
 * 
 * === THUẬT TOÁN ===
 * 1. Key Derivation: PBKDF2WithHmacSHA256
 *    - Iterations: 100,000 (chống brute-force)
 *    - Salt: 16 bytes ngẫu nhiên (lưu trên Firestore)
 *    - Output: 256-bit key
 * 
 * 2. Encryption: AES-256-GCM
 *    - IV: 12 bytes (unique mỗi lần encrypt)
 *    - Tag: 128-bit authentication
 * 
 * === DATA FORMAT ===
 * Encrypted data: Base64(IV[12] + Ciphertext + Tag[16])
 * Salt: Lưu trên Firestore tại /users/{uid}/encryptionSalt
 * 
 * === SỬ DỤNG ===
 * ```java
 * // Khi user đăng nhập thành công
 * String loginPassword = "user_firebase_password";
 * helper.initializeWithPassword(loginPassword);
 * 
 * // Mã hóa password trước khi lưu Firestore
 * String encrypted = helper.encrypt("password123");
 * 
 * // Giải mã khi hiển thị
 * String decrypted = helper.decrypt(encrypted);
 * ```
 * 
 * === QUAN TRỌNG ===
 * - Salt PHẢI được sync với Firestore để Web/Extension decrypt được
 * - Khi user đổi password Firebase, cần re-encrypt tất cả data
 * 
 * @see EncryptionHelper Class cũ sử dụng Android Keystore (không cross-platform)
 */
import javax.inject.Inject;
import javax.inject.Singleton;
import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class CrossPlatformEncryptionHelper {

    private static final String TAG = "CrossPlatformEncryption";
    private static final String PREFS_NAME = "lifehub_encryption_prefs";
    private static final String KEY_SALT = "encryption_salt";
    private static final String KEY_INITIALIZED = "encryption_initialized";
    
    // Encryption parameters - MUST match Web/Extension implementation
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 100000;
    
    private final SharedPreferences prefs;
    private byte[] derivedKeyBytes = null;
    private byte[] saltBytes = null;
    private boolean isInitialized = false;

    @Inject
    public CrossPlatformEncryptionHelper(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSalt();
    }

    /**
     * Kiểm tra xem đã có salt (đã setup encryption) chưa
     */
    public boolean hasEncryptionSetup() {
        return prefs.contains(KEY_SALT);
    }

    /**
     * Kiểm tra xem key đã được unlock (user đã nhập master password) chưa
     */
    public boolean isUnlocked() {
        return isInitialized && derivedKeyBytes != null;
    }

    /**
     * Lấy salt đã lưu (dùng để sync lên Firestore)
     */
    public String getSaltBase64() {
        if (saltBytes == null) return null;
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP);
    }

    /**
     * Set salt từ Firestore (khi đồng bộ từ cloud)
     */
    public void setSaltFromBase64(String saltBase64) {
        if (saltBase64 == null || saltBase64.isEmpty()) return;
        
        saltBytes = Base64.decode(saltBase64, Base64.DEFAULT);
        prefs.edit().putString(KEY_SALT, saltBase64).apply();
    }

    /**
     * Load salt từ local storage
     */
    private void loadSalt() {
        String saltBase64 = prefs.getString(KEY_SALT, null);
        if (saltBase64 != null) {
            saltBytes = Base64.decode(saltBase64, Base64.DEFAULT);
        }
    }

    /**
     * Tạo salt mới (chỉ gọi lần đầu setup)
     */
    private void generateNewSalt() {
        saltBytes = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(saltBytes);
        
        String saltBase64 = Base64.encodeToString(saltBytes, Base64.NO_WRAP);
        prefs.edit()
            .putString(KEY_SALT, saltBase64)
            .putBoolean(KEY_INITIALIZED, true)
            .apply();
    }

    /**
     * Khởi tạo với Master Password
     * 
     * @param masterPassword Master password từ user
     * @param isNewSetup true nếu đây là lần đầu setup (tạo salt mới)
     * @return true nếu thành công
     */
    public boolean initializeWithMasterPassword(String masterPassword, boolean isNewSetup) {
        if (masterPassword == null || masterPassword.isEmpty()) {
            Log.e(TAG, "Master password is empty");
            return false;
        }

        try {
            // Chỉ tạo salt mới nếu THỰC SỰ cần (chưa có salt) hoặc nếu là setup mới
            if (isNewSetup) {
                Log.d(TAG, "Generating new salt (setup or change)");
                generateNewSalt();
            } else if (saltBytes == null) {
                Log.e(TAG, "No salt available!");
                return false;
            } else {
                Log.d(TAG, "Using existing salt");
            }

            // Derive key từ password + salt bằng PBKDF2
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            PBEKeySpec spec = new PBEKeySpec(
                masterPassword.toCharArray(),
                saltBytes,
                PBKDF2_ITERATIONS,
                KEY_LENGTH_BITS
            );
            
            SecretKey secretKey = factory.generateSecret(spec);
            derivedKeyBytes = secretKey.getEncoded();
            
            // Clear password từ memory (security)
            spec.clearPassword();
            
            isInitialized = true;
            Log.d(TAG, "Encryption initialized successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing encryption", e);
            return false;
        }
    }

    /**
     * Xác thực Master Password (kiểm tra password đúng)
     * Bằng cách thử decrypt một chuỗi test đã biết
     */
    public boolean verifyMasterPassword(String masterPassword, String testEncrypted) {
        if (!initializeWithMasterPassword(masterPassword, false)) {
            return false;
        }
        
        try {
            String decrypted = decrypt(testEncrypted);
            // Nếu decrypt thành công và không throw exception, password đúng
            return decrypted != null && !decrypted.equals(testEncrypted);
        } catch (Exception e) {
            // Password sai sẽ gây ra exception khi decrypt
            derivedKeyBytes = null;
            isInitialized = false;
            return false;
        }
    }

    /**
     * Mã hóa văn bản bằng AES-256-GCM
     * 
     * @param plainText Văn bản cần mã hóa
     * @return Base64(IV + Ciphertext + Tag)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        if (!isUnlocked()) {
            Log.e(TAG, "Encryption not initialized. Call initializeWithMasterPassword first.");
            return plainText;
        }

        try {
            // Tạo IV ngẫu nhiên
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            // Khởi tạo cipher
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(derivedKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Ghép IV + CipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
            
        } catch (Exception e) {
            Log.e(TAG, "Encryption error", e);
            return "";
        }
    }

    /**
     * Giải mã văn bản đã mã hóa
     * 
     * @param encryptedText Base64(IV + Ciphertext + Tag)
     * @return Văn bản gốc
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        if (!isUnlocked()) {
            Log.e(TAG, "Encryption not initialized. Call initializeWithMasterPassword first.");
            return encryptedText;
        }

        try {
            byte[] combined = Base64.decode(encryptedText, Base64.DEFAULT);

            // Tách IV
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);

            // Tách CipherText
            int cipherTextLen = combined.length - IV_LENGTH_BYTES;
            byte[] cipherText = new byte[cipherTextLen];
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherTextLen);

            // Giải mã
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(derivedKeyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            byte[] plainTextBytes = cipher.doFinal(cipherText);

            return new String(plainTextBytes, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            Log.e(TAG, "Decryption error (wrong password or corrupted data)", e);
            return encryptedText; // Trả về gốc nếu lỗi
        }
    }

    /**
     * Xóa key khỏi memory (lock)
     */
    public void lock() {
        if (derivedKeyBytes != null) {
            // Zero out the key for security
            java.util.Arrays.fill(derivedKeyBytes, (byte) 0);
            derivedKeyBytes = null;
        }
        isInitialized = false;
    }

    /**
     * Xóa toàn bộ dữ liệu encryption (reset)
     */
    public void clearAll() {
        lock();
        saltBytes = null;
        prefs.edit().clear().apply();
    }
    /**
     * Tạo mã khôi phục ngẫu nhiên (32 ký tự hex)
     */
    public String generateRecoveryCode() {
        byte[] bytes = new byte[16];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toUpperCase();
    }
}
