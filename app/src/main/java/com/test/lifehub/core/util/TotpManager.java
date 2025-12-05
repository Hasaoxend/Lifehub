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
 * 
 * === MỤC ĐÍCH ===
 * Class này implement TOTP (RFC 6238) - tương tự Google Authenticator.
 * Tạo mã OTP 6 số thay đổi mỗi 30 giây cho 2FA authentication.
 * 
 * === THUẬT TOÁN TOTP (RFC 6238) ===
 * 
 * 1. PARAMETERS:
 *    - Secret Key: 160-bit (20 bytes) ngẫu nhiên, encode Base32
 *    - Time Step: 30 giây (mã mới mỗi 30s)
 *    - Digits: 6 số
 *    - Algorithm: HMAC-SHA1
 * 
 * 2. FLOW TẠO MÃ:
 *    ```
 *    Current Time (Unix timestamp)
 *         |
 *         v
 *    Time Index = Time / 30
 *         |
 *         v
 *    HMAC-SHA1(Secret, Time Index) -> 20 bytes hash
 *         |
 *         v
 *    Dynamic Truncation (lấy 4 bytes)
 *         |
 *         v
 *    Convert to 6-digit number
 *         |
 *         v
 *    TOTP Code: "123456"
 *    ```
 * 
 * 3. DYNAMIC TRUNCATION:
 *    - Lấy byte cuối của hash -> offset (0-15)
 *    - Lấy 4 bytes từ vị trí offset
 *    - Convert sang integer 31-bit
 *    - Mod 10^6 để ra 6 số
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * 
 * ```java
 * // 1. Tạo secret mới cho user
 * String secret = TotpManager.generateSecret();
 * // Ví dụ: "JBSWY3DPEHPK3PXP"
 * 
 * // 2. Lưu secret vào Firestore (nên mã hóa!)
 * TotpAccount account = new TotpAccount();
 * account.setSecret(secret);
 * repository.insert(account);
 * 
 * // 3. Hiển thị QR code cho user scan
 * String qrUrl = TotpManager.generateQRCodeUrl(
 *     "user@example.com",
 *     "MyApp",
 *     secret
 * );
 * Bitmap qrBitmap = TotpManager.generateQRCode(qrUrl, 512, 512);
 * imageView.setImageBitmap(qrBitmap);
 * 
 * // 4. Tạo mã OTP hiện tại
 * String currentCode = TotpManager.getCurrentCode(secret);
 * // Ví dụ: "842156" (thay đổi mỗi 30s)
 * 
 * // 5. Xác thực mã user nhập
 * boolean isValid = TotpManager.validateCode(secret, userInput);
 * if (isValid) {
 *     // Login thành công
 * }
 * 
 * // 6. Lấy thời gian còn lại của mã
 * int remaining = TotpManager.getRemainingSeconds();
 * // Ví dụ: 15 (còn 15s nữa sẽ đổi mã)
 * ```
 * 
 * === TIME WINDOW VALIDATION ===
 * 
 * validateCode() chấp nhận mã trong 3 time windows:
 * - Window hiện tại (0s - 30s)
 * - Window trước (-30s - 0s)
 * - Window sau (30s - 60s)
 * 
 * Lý do: Cho phép sai lệch đồng hồ giữa client và server.
 * 
 * ```
 * Timeline:
 * |-------- Window -1 --------|-------- Window 0 --------|-------- Window +1 --------|
 * |  Code: 123456             |  Code: 789012           |  Code: 345678            |
 * | (chấp nhận)               | (chấp nhận)            | (chấp nhận)               |
 * ```
 * 
 * === QR CODE FORMAT ===
 * 
 * URL format theo RFC:
 * ```
 * otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}
 * 
 * Ví dụ:
 * otpauth://totp/MyApp:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=MyApp
 * ```
 * 
 * === BẢO MẬT ===
 * 
 * 1. SECRET KEY:
 *    - Tạo bằng SecureRandom (cryptographically strong)
 *    - 160-bit = 20 bytes (chuẩn TOTP)
 *    - NÊN MÃ HÓA trước khi lưu Firestore!
 * 
 * 2. TIME SYNCHRONIZATION:
 *    - TOTP phụ thuộc vào đồng hồ chính xác
 *    - Nếu đồng hồ sai > 30s -> mã không khớp
 *    - Nên dùng NTP đồng bộ thời gian
 * 
 * 3. BACKUP CODES:
 *    - Nên tạo backup codes cho user
 *    - Nếu mất điện thoại -> dùng backup code để login
 * 
 * === HMAC-SHA1 DETAILS ===
 * 
 * ```java
 * // Input:
 * byte[] key = base32Decode(secret);  // 20 bytes
 * byte[] data = longToBytes(timeIndex);  // 8 bytes
 * 
 * // HMAC:
 * Mac mac = Mac.getInstance("HmacSHA1");
 * mac.init(new SecretKeySpec(key, "HmacSHA1"));
 * byte[] hash = mac.doFinal(data);  // 20 bytes output
 * 
 * // Dynamic Truncation:
 * int offset = hash[19] & 0x0F;  // 0-15
 * int binary = ((hash[offset] & 0x7F) << 24)
 *            | ((hash[offset+1] & 0xFF) << 16)
 *            | ((hash[offset+2] & 0xFF) << 8)
 *            | (hash[offset+3] & 0xFF);
 * 
 * int otp = binary % 1000000;  // 6 digits
 * ```
 * 
 * === SO SÁNH VỚI HOTP ===
 * 
 * | Feature      | HOTP (RFC 4226)   | TOTP (RFC 6238)     |
 * |--------------|-------------------|---------------------|
 * | Counter      | Incremental       | Time-based (30s)    |
 * | Sync Issue   | Counter mismatch  | Clock drift         |
 * | Use Case     | Hardware tokens   | Mobile apps         |
 * 
 * === LƯU Ý QUAN TRỌNG ===
 * 
 * 1. Secret MUST be stored encrypted (AES-256-GCM)
 * 2. QR code chỉ hiển thị 1 lần khi setup
 * 3. Không gửi secret qua network (chỉ QR code local)
 * 4. Time drift > 90s (3 windows) -> fail
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Hỗ trợ SHA256/SHA512 (RFC 6238 option)
 * TODO: Customizable time step (15s, 60s)
 * TODO: Customizable digits (4, 8)
 * TODO: Thêm counter-based HOTP
 * TODO: Generate backup codes (10 codes × 8 digits)
 * FIXME: Xử lý time drift detection
 * 
 * @see <a href="https://tools.ietf.org/html/rfc6238">RFC 6238 - TOTP</a>
 * @see <a href="https://tools.ietf.org/html/rfc4226">RFC 4226 - HOTP</a>
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
            if (parts.length < 2) {
                return null;
            }
            
            // Decode label
            String label = java.net.URLDecoder.decode(parts[0], "UTF-8");
            String query = parts[1];
            
            String issuer = "";
            String accountName = label;
            
            // Parse label: có thể là "issuer:account" hoặc chỉ "account"
            if (label.contains(":")) {
                String[] labelParts = label.split(":", 2);
                issuer = labelParts[0].trim();
                accountName = labelParts[1].trim();
            }
            
            // Parse query parameters (ưu tiên issuer từ query params)
            String secret = "";
            String[] params = query.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].toLowerCase();
                    String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                    
                    if (key.equals("secret")) {
                        secret = value.toUpperCase().replaceAll("\\s", "");
                    } else if (key.equals("issuer")) {
                        issuer = value.trim(); // Override issuer from query param
                    }
                }
            }
            
            if (secret.isEmpty()) {
                return null;
            }
            
            // Nếu vẫn không có issuer, dùng accountName hoặc extract từ email
            if (issuer.isEmpty()) {
                if (accountName.contains("@")) {
                    String domain = accountName.substring(accountName.indexOf("@") + 1);
                    issuer = domain.split("\\.")[0]; // Lấy phần đầu của domain (gmail, facebook, etc)
                    // Capitalize first letter
                    issuer = issuer.substring(0, 1).toUpperCase() + issuer.substring(1);
                } else {
                    issuer = accountName;
                }
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
