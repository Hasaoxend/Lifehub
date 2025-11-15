package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

/**
 * POJO (Đối tượng Java) cho một Ghi chú (Note).
 * (Phiên bản đã thêm alarmRequestCode)
 */
public class NoteEntry {

    @Exclude
    public String documentId;

    private String title;
    private String content;
    private Date lastModified;
    private String userOwnerId;
    private Date reminderTime;

    // ✅ THÊM LẠI TÍNH NĂNG: Trường để lưu ID của Alarm
    private int alarmRequestCode;

    public NoteEntry() {
    }

    public NoteEntry(String title, String content, Date lastModified) {
        this.title = title;
        this.content = content;
        this.lastModified = lastModified;
        this.reminderTime = null;
        this.alarmRequestCode = 0; // Mặc định
    }

    // --- Getters & Setters ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
    public String getUserOwnerId() { return userOwnerId; }
    public void setUserOwnerId(String userOwnerId) { this.userOwnerId = userOwnerId; }
    public Date getReminderTime() { return reminderTime; }
    public void setReminderTime(Date reminderTime) { this.reminderTime = reminderTime; }

    // ✅ THÊM LẠI TÍNH NĂNG:
    public int getAlarmRequestCode() { return alarmRequestCode; }
    public void setAlarmRequestCode(int alarmRequestCode) { this.alarmRequestCode = alarmRequestCode; }

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}