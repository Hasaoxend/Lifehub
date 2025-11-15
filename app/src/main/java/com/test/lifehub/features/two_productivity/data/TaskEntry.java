package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.util.Date; // <-- THÊM IMPORT NÀY

/**
 * POJO (Đối tượng Java) cho một Công việc (Task)
 * (Phiên bản đã sửa lỗi Timestamp sang Date)
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

    /**
     * Constructor rỗng (BẮT BUỘC cho Firestore)
     */
    public TaskEntry() {
    }

    /**
     * Constructor chính
     */
    public TaskEntry(String name, Date lastModified, boolean isCompleted, int taskType) { // <-- SỬA LỖI: Đổi sang Date
        this.name = name;
        this.lastModified = lastModified;
        this.isCompleted = isCompleted;
        this.taskType = taskType;
    }

    // --- Getters & Setters (BẮT BUỘC cho Firestore) ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastModified() { // <-- SỬA LỖI: Đổi sang Date
        return lastModified;
    }

    public void setLastModified(Date lastModified) { // <-- SỬA LỖI: Đổi sang Date
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