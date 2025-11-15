package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO (Plain Old Java Object) cho một "Project" (Thư mục Todo).
 * Dùng để đọc/ghi trực tiếp với Firebase Firestore.
 */
public class ProjectEntry implements Serializable {

    @Exclude
    public String documentId;

    public String name; // Tên của Project (ví dụ: "Việc cá nhân", "Đồ án")
    public String color; // (Tương lai) Mã màu (ví dụ: "#E44332")

    public String userOwnerId; // Chủ sở hữu

    @ServerTimestamp
    public Date createdDate;

    public ProjectEntry() {
        // Constructor rỗng (bắt buộc cho Firestore)
    }

    // Constructor tiện lợi
    public ProjectEntry(String name, String userOwnerId) {
        this.name = name;
        this.userOwnerId = userOwnerId;
        this.color = "#808080"; // Mặc định màu xám
    }

    // Ghi đè hàm này để Spinner (menu thả xuống) hiển thị tên
    @Override
    public String toString() {
        return name;
    }
}