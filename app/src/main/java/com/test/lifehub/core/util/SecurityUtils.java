package com.test.lifehub.core.util;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Tiện ích hỗ trợ Hashing (Băm) và Salting (Thêm muối)
 * Dùng cho bảo mật PIN hoặc mật khẩu cục bộ.
 */
public class SecurityUtils {

    // Tạo ra một chuỗi Salt ngẫu nhiên
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    // Hàm Băm (Hash) mật khẩu kèm Salt sử dụng SHA-256
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Thêm salt vào trước khi băm
            md.update(Base64.decode(salt, Base64.NO_WRAP));
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.encodeToString(hashedPassword, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Hàm kiểm tra mật khẩu nhập vào có khớp với hash đã lưu không
    public static boolean verifyPassword(String inputPassword, String storedSalt, String storedHash) {
        String newHash = hashPassword(inputPassword, storedSalt);
        return newHash != null && newHash.equals(storedHash);
    }
}