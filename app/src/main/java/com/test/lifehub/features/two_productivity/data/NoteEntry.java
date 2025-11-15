package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.util.Date; // <-- Import Date

/**
 * POJO (Đối tượng Java) cho một Ghi chú (Note).
 * (Phiên bản đã sửa lỗi Timestamp và thêm lại trường Reminder)
 */
public class NoteEntry {

    @Exclude
    public String documentId;

    // --- Các trường dữ liệu (Fields) ---
    private String title;
    private String content;
    private Date lastModified; // <-- SỬA LỖI: Đổi từ long sang Date
    private String userOwnerId;
    private Date reminderTime; // <-- THÊM LẠI: Trường đặt giờ

    /**
     * Constructor rỗng (BẮT BUỘC cho Firestore)
     */
    public NoteEntry() {
    }

    /**
     * Constructor chính
     */
    public NoteEntry(String title, String content, Date lastModified) {
        this.title = title;
        this.content = content;
        this.lastModified = lastModified;
        this.reminderTime = null; // Mặc định không có nhắc giờ
    }

    // --- Getters & Setters (BẮT BUỘC cho Firestore) ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getLastModified() { // Sửa thành Date
        return lastModified;
    }

    public void setLastModified(Date lastModified) { // Sửa thành Date
        this.lastModified = lastModified;
    }

    public String getUserOwnerId() {
        return userOwnerId;
    }

    public void setUserOwnerId(String userOwnerId) {
        this.userOwnerId = userOwnerId;
    }

    public Date getReminderTime() { // <-- THÊM LẠI
        return reminderTime;
    }

    public void setReminderTime(Date reminderTime) { // <-- THÊM LẠI
        this.reminderTime = reminderTime;
    }

    // --- Getter/Setter cho ID (Không phải của Firestore) ---
    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    @Exclude
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}