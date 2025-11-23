package com.test.lifehub.core.util;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.apache.commons.codec.binary.Base32;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TotpManager - Quản lý Time-based One-Time Password (TOTP)
 * Tương tự như Google Authenticator
 */
public class TotpManager {

    private static final String TAG = "TotpManager";
    private static final int SECRET_SIZE = 20; // 160 bits
    private static final String ALGORITHM = "HmacSHA1";
    private static final int DIGITS = 6;
    private static final int TIME_STEP = 30; // seconds

    /**
     * Tạo secret key ngẫu nhiên cho tài khoản TOTP mới
     * @return Secret key dạng Base32 string
     */
    public static String generateSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_SIZE];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes).replaceAll("=", "");
    }

    /**
     * Tạo mã TOTP 6 chữ số hiện tại
     * @param secret Secret key (Base32)
     * @return Mã OTP 6 chữ số
     */
    public static String getCurrentCode(String secret) {
        long timeIndex = new Date().getTime() / 1000 / TIME_STEP;
        return generateCode(secret, timeIndex);
    }

    /**
     * Xác thực mã TOTP người dùng nhập vào
     * @param secret Secret key (Base32)
     * @param code Mã OTP 6 chữ số người dùng nhập
     * @return true nếu mã đúng
     */
    public static boolean validateCode(String secret, String code) {
        long timeIndex = new Date().getTime() / 1000 / TIME_STEP;
        // Kiểm tra cả time window hiện tại và ±1 window (cho phép sai lệch 30s)
        for (int i = -1; i <= 1; i++) {
            String validCode = generateCode(secret, timeIndex + i);
            if (validCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tạo mã TOTP cho một time index cụ thể
     * @param secret Secret key (Base32)
     * @param timeIndex Time index (Unix time / 30)
     * @return Mã OTP 6 chữ số
     */
    private static String generateCode(String secret, long timeIndex) {
        try {
            Base32 base32 = new Base32();
            byte[] key = base32.decode(secret.toUpperCase());
            
            byte[] data = ByteBuffer.allocate(8).putLong(timeIndex).array();
            
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] hash = mac.doFinal(data);
            
            int offset = hash[hash.length - 1] & 0xF;
            long truncatedHash = 0;
            for (int i = 0; i < 4; i++) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= Math.pow(10, DIGITS);
            
            return String.format("%0" + DIGITS + "d", truncatedHash);
        } catch (Exception e) {
            e.printStackTrace();
            return "000000";
        }
    }

    /**
     * Tạo QR code bitmap cho tài khoản TOTP
     * @param accountName Tên tài khoản (email hoặc username)
     * @param issuer Tên dịch vụ (ví dụ: "LifeHub")
     * @param secret Secret key (Base32)
     * @param size Kích thước QR code (pixels)
     * @return Bitmap của QR code
     */
    public static Bitmap generateQRCode(String accountName, String issuer, String secret, int size) {
        try {
            // Tạo URI theo chuẩn otpauth://
            String uri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                issuer, accountName, secret, issuer
            );
            
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(uri, BarcodeFormat.QR_CODE, size, size);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse URI từ QR code otpauth://
     * @param uri URI string từ QR code
     * @return TotpAccount object hoặc null nếu parse thất bại
     */
    public static TotpAccount parseOtpAuthUri(String uri) {
        try {
            if (!uri.startsWith("otpauth://totp/")) {
                return null;
            }
            
            String[] parts = uri.substring(15).split("\\?");
            String label = parts[0];
            String query = parts[1];
            
            String issuer = "";
            String accountName = label;
            if (label.contains(":")) {
                String[] labelParts = label.split(":", 2);
                issuer = labelParts[0];
                accountName = labelParts[1];
            }
            
            String secret = "";
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue[0].equals("secret")) {
                    secret = keyValue[1];
                } else if (keyValue[0].equals("issuer") && issuer.isEmpty()) {
                    issuer = keyValue[1];
                }
            }
            
            if (secret.isEmpty()) {
                return null;
            }
            
            return new TotpAccount(accountName, issuer, secret);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lấy thời gian còn lại (giây) trước khi mã TOTP hết hiệu lực
     * @return Số giây còn lại (0-29)
     */
    public static int getTimeRemaining() {
        long currentTime = new Date().getTime() / 1000;
        return TIME_STEP - (int)(currentTime % TIME_STEP);
    }

    /**
     * Class đại diện cho một tài khoản TOTP
     */
    public static class TotpAccount {
        private String accountName;
        private String issuer;
        private String secret;

        public TotpAccount(String accountName, String issuer, String secret) {
            this.accountName = accountName;
            this.issuer = issuer;
            this.secret = secret;
        }

        public String getAccountName() {
            return accountName;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getSecret() {
            return secret;
        }

        public String getCurrentCode() {
            return TotpManager.getCurrentCode(secret);
        }

        public boolean validateCode(String code) {
            return TotpManager.validateCode(secret, code);
        }
    }
}
