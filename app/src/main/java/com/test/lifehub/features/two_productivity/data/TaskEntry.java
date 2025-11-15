package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.util.Date; // <-- Import Date

/**
 * POJO (Đối tượng Java) cho một Công việc (Task)
 * (Phiên bản đã sửa lỗi Timestamp và thêm lại trường Reminder)
 */
public class TaskEntry {

    @Exclude
    public String documentId;

    // --- Các trường dữ liệu (Fields) ---
    private String name;
    private Date lastModified; // <-- SỬA LỖI: Đổi từ long sang Date
    private boolean isCompleted;
    private int taskType;
    private String userOwnerId;
    private Date reminderTime; // <-- THÊM LẠI: Trường đặt giờ

    /**
     * Constructor rỗng (BẮT BUỘC cho Firestore)
     */
    public TaskEntry() {
    }

    /**
     * Constructor chính
     */
    public TaskEntry(String name, Date lastModified, boolean isCompleted, int taskType) {
        this.name = name;
        this.lastModified = lastModified;
        this.isCompleted = isCompleted;
        this.taskType = taskType;
        this.reminderTime = null; // Mặc định không có nhắc giờ
    }

    // --- Getters & Setters (BẮT BUỘC cho Firestore) ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastModified() { // Sửa thành Date
        return lastModified;
    }

    public void setLastModified(Date lastModified) { // Sửa thành Date
        this.lastModified = lastModified;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getTaskType() {
        return taskType;
    }

    public void setTaskType(int taskType) {
        this.taskType = taskType;
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