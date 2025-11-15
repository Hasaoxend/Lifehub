package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.Date;

/**
 * POJO cho Task
 * (Phiên bản đã thêm projectId)
 */
public class TaskEntry {

    @Exclude
    public String documentId;

    private String name;
    private Date lastModified;
    private boolean completed;
    private int taskType;
    private String userOwnerId;
    private Date reminderTime;
    private int alarmRequestCode;

    // ✅ THÊM MỚI: Trường để liên kết với Project
    private String projectId;

    public TaskEntry() {
    }

    public TaskEntry(String name, Date lastModified, boolean isCompleted, int taskType) {
        this.name = name;
        this.lastModified = lastModified;
        this.completed = isCompleted;
        this.taskType = taskType;
        this.reminderTime = null;
        this.alarmRequestCode = 0;
        this.projectId = null; // Mặc định là không có project (root)
    }

    // --- Getters & Setters ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }

    @PropertyName("completed")
    public boolean isCompleted() { return completed; }
    @PropertyName("completed")
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getTaskType() { return taskType; }
    public void setTaskType(int taskType) { this.taskType = taskType; }

    public String getUserOwnerId() { return userOwnerId; }
    public void setUserOwnerId(String userOwnerId) { this.userOwnerId = userOwnerId; }

    public Date getReminderTime() { return reminderTime; }
    public void setReminderTime(Date reminderTime) { this.reminderTime = reminderTime; }

    public int getAlarmRequestCode() { return alarmRequestCode; }
    public void setAlarmRequestCode(int alarmRequestCode) { this.alarmRequestCode = alarmRequestCode; }

    // ✅ THÊM MỚI:
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}