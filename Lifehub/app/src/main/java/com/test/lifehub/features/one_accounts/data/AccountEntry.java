package com.test.lifehub.features.one_accounts.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * POJO (Plain Old Java Object) cho một Tài khoản.
 * Dùng để đọc/ghi trực tiếp với Firebase Firestore.
 * (KHÔNG CÒN LÀ ENTITY CỦA ROOM, KHÔNG CÒN MÃ HÓA)
 */
public class AccountEntry implements Serializable {

    @Exclude // Báo Firestore: "Đừng lưu trường này"
    public String documentId; // ID của tài liệu trên Firestore

    // ----- Dữ liệu cơ bản -----
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