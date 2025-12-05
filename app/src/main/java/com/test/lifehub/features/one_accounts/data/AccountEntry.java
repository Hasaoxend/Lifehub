package com.test.lifehub.features.one_accounts.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AccountEntry - POJO cho Tài khoản
 * 
 * === MỤC ĐÍCH ===
 * Lưu trữ thông tin tài khoản (email, mật khẩu, ...) lên Firestore
 * 
 * === FIRESTORE STRUCTURE ===
 * users/{userId}/accounts/{accountId}
 *   ├─ serviceName: String       -> Tên dịch vụ (Gmail, Facebook, ...)
 *   ├─ username: String          -> Email/username
 *   ├─ password: String          -> Mật khẩu (ĐÃ MÃ HÓA AES-256)
 *   ├─ websiteUrl: String        -> URL website (optional)
 *   ├─ notes: String             -> Ghi chú (optional)
 *   ├─ customFields: Map         -> Fields tùy chỉnh
 *   ├─ userOwnerId: String       -> Firebase Auth UID
 *   └─ lastModified: Timestamp  -> Tự động set bởi Firestore
 * 
 * === BẢO MẬT QUAN TRỌNG ===
 * ⚠️ MẬT KHẨU PHẢI ĐƯỢC MÃ HÓA TRƯỚC KHI LƯU:
 * 
 * ```java
 * // SAI - Không bao giờ làm thế này!
 * account.password = "MyPassword123";  // PLAIN TEXT - RẤT NGUY HIỂM!
 * 
 * // ĐÚNG - Luôn mã hóa trước
 * String encrypted = EncryptionHelper.encrypt("MyPassword123", secretKey);
 * account.password = encrypted;  // OK - Đã mã hóa
 * 
 * // Khi hiển thị cho user
 * String decrypted = EncryptionHelper.decrypt(account.password, secretKey);
 * textView.setText(decrypted);
 * ```
 * 
 * === CUSTOM FIELDS ===
 * Hỗ trợ thêm fields tùy chỉnh cho từng loại tài khoản:
 * 
 * ```java
 * // Ví dụ: Thêm "Security Question" cho Gmail
 * Map<String, Object> field = new HashMap<>();
 * field.put("value", "Tên thú cưng của bạn?");
 * field.put("type", AccountEntry.FIELD_TYPE_TEXT);
 * account.customFields.put("securityQuestion", field);
 * 
 * // Ví dụ: Thêm "PIN" cho Banking (mã hóa)
 * Map<String, Object> pinField = new HashMap<>();
 * pinField.put("value", encryptedPIN);
 * pinField.put("type", AccountEntry.FIELD_TYPE_PASSWORD);
 * account.customFields.put("pin", pinField);
 * ```
 * 
 * === ANNOTATIONS ===
 * @Exclude: documentId không lưu vào Firestore
 * @ServerTimestamp: Firestore tự động set timestamp khi write
 * 
 * === FIRESTORE SECURITY RULES ===
 * ```javascript
 * // Chỉ cho phép user đọc/ghi tài khoản của chính mình
 * match /users/{userId}/accounts/{accountId} {
 *   allow read, write: if request.auth.uid == userId;
 * }
 * ```
 * 
 * === SERIALIZABLE ===
 * Implements Serializable để có thể:
 * - Truyền qua Intent extras
 * - Lưu vào Bundle
 * 
 * @see AccountRepository Repository quản lý accounts
 * @see EncryptionHelper Mã hóa/giải mã mật khẩu
 */
public class AccountEntry implements Serializable {

    // ===== FIRESTORE DOCUMENT ID =====
    @Exclude // Báo Firestore: "Đừng lưu trường này"
    public String documentId; // ID của tài liệu trên Firestore

    // ===== DỮ LIỆU CƠ BẢN =====
    public String serviceName;
    public String username;
    public String websiteUrl;

    // ----- Dữ liệu nhạy cảm (Giờ là VĂN BẢN THUẦN) -----
    public String password;
    public String notes;

    // ----- Trường Tùy chỉnh (Dùng Map linh hoạt) -----
    // Key: Tên trường (String)
    // Value: Map<String, Object> (ví dụ: { "value": "Giá trị", "type": 1 })
    public Map<String, Object> customFields;

    // ----- Thông tin Người sở hữu (Bắt buộc cho Luật Bảo mật) -----
    public String userOwnerId; // Sẽ lưu UID của Firebase Auth

    // ----- Thông tin đồng bộ -----
    @ServerTimestamp // Firestore sẽ tự động gán ngày giờ của Server
    public Date lastModified;

    // Hằng số cho Loại Trường (Field Type) - Vẫn giữ
    public static final int FIELD_TYPE_TEXT = 0;
    public static final int FIELD_TYPE_PASSWORD = 1;

    public AccountEntry() {
        // Constructor rỗng (bắt buộc cho Firestore)
        this.customFields = new HashMap<>();
    }
}