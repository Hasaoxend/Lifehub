package com.test.lifehub.features.one_accounts.ui;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Lớp tiện ích để tạo Mật khẩu 1 lần (TOTP)
 * dựa trên thuật toán RFC 6238.
 */
public class TotpGenerator {

    private static final int CODE_DIGITS = 6;
    // ----- SỬA DÒNG NÀY -----
    public static final int TIME_STEP = 30; // Đổi từ "private" sang "public"
    // -----------------------
    private static final String ALGORITHM = "HmacSHA1";

    /**
     * Tạo mã TOTP 6 số.
     * @param base32Secret Mã bí mật (dưới dạng Base32)
     * @return Chuỗi 6 số TOTP, hoặc "Lỗi" nếu thất bại.
     */
    public static String generateTotp(String base32Secret) {
        if (base32Secret == null || base32Secret.isEmpty()) {
            return "--- ---";
        }

        try {
            // 1. Giải mã Base32 secret
            byte[] key = Base32.decode(base32Secret);

            // 2. Lấy "bước" thời gian hiện tại
            long timeStep = System.currentTimeMillis() / 1000 / TIME_STEP;
            byte[] timeBytes = new byte[8];
            for (int i = 7; i >= 0; i--) {
                timeBytes[i] = (byte) (timeStep & 0xFF);
                timeStep >>= 8;
            }

            // 3. Chạy thuật toán HMAC-SHA1
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);

            // 4. Trích xuất 4 byte từ hash (Dynamic Truncation)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) |
                    ((hash[offset + 1] & 0xFF) << 16) |
                    ((hash[offset + 2] & 0xFF) << 8) |
                    (hash[offset + 3] & 0xFF);

            // 5. Lấy 6 chữ số cuối
            long otp = binary % (long) Math.pow(10, CODE_DIGITS);

            // 6. Định dạng (Thêm số 0 ở đầu nếu cần)
            String result = Long.toString(otp);
            while (result.length() < CODE_DIGITS) {
                result = "0" + result;
            }
            // Thêm khoảng trắng cho dễ đọc
            return result.substring(0, 3) + " " + result.substring(3);

        } catch (NoSuchAlgorithmException | InvalidKeyException | Base32.DecodingException e) {
            e.printStackTrace();
            return "Lỗi Mã";
        }
    }

    /**
     * Lớp con nội bộ để giải mã Base32.
     * Giữ nó ở đây để file này tự hoạt động độc lập.
     */
    private static class Base32 {
        private static final String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        private static final HashMap<Character, Integer> base32Lookup = new HashMap<>();
        static {
            for (int i = 0; i < base32Chars.length(); i++) {
                base32Lookup.put(base32Chars.charAt(i), i);
            }
        }

        public static class DecodingException extends Exception {
            public DecodingException(String message) {
                super(message);
            }
        }

        public static byte[] decode(final String base32) throws DecodingException {
            String s = base32.toUpperCase().replaceAll("=", "");
            if (s.length() == 0) return new byte[0];

            int sLen = s.length();
            int numBytes = (sLen * 5) / 8;
            byte[] bytes = new byte[numBytes];
            int bytePos = 0, buffer = 0, bitsLeft = 0;

            for (int i = 0; i < sLen; i++) {
                if (!base32Lookup.containsKey(s.charAt(i))) {
                    throw new DecodingException("Ký tự Base32 không hợp lệ");
                }
                int val = base32Lookup.get(s.charAt(i));
                buffer = (buffer << 5) | val;
                bitsLeft += 5;
                if (bitsLeft >= 8) {
                    bytes[bytePos++] = (byte) (buffer >> (bitsLeft - 8));
                    bitsLeft -= 8;
                }
            }
            return bytes;
        }
    }
}