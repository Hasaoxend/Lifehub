package com.test.lifehub.features.two_productivity.data; // (Đã sửa package)

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO cho một Ghi chú (Note).
 * (KHÔNG CÒN LÀ ENTITY CỦA ROOM, KHÔNG CÒN MÃ HÓA)
 */
public class NoteEntry implements Serializable {

    @Exclude
    public String documentId;

    // ----- Dữ liệu Ghi chú (VĂN BẢN THUẦN) -----
    public String title;
    public String content; // <-- SỬA LỖI: Từ byte[] thành String

    // ----- Thông tin Người sở hữu (Bắt buộc cho Bảo mật) -----
    public String userOwnerId;

    // ----- Thông tin đồng bộ -----
    @ServerTimestamp
    public Date lastModified;

    public NoteEntry() {
        // Constructor rỗng (bắt buộc cho Firestore)
    }
}