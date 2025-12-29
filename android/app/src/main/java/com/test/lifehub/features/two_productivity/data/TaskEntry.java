package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.Date;

/**
 * TaskEntry - POJO cho Công việc/Task
 * 
 * === MỤC ĐÍCH ===
 * Quản lý 2 loại task:
 * 1. Task thông thường (taskType = 0): Công việc cần làm
 * 2. Shopping item (taskType = 1): Danh sách mua sắm
 * 
 * === FIRESTORE STRUCTURE ===
 * users/{userId}/tasks/{taskId}
 *   ├─ name: String             -> Tên task
 *   ├─ completed: boolean       -> Đã hoàn thành chưa
 *   ├─ taskType: int           -> 0=Task, 1=Shopping
 *   ├─ lastModified: Timestamp -> Thời gian sửa cuối
 *   ├─ reminderTime: Timestamp -> Nhắc nhở (optional)
 *   ├─ alarmRequestCode: int   -> ID của AlarmManager
 *   ├─ projectId: String       -> Thuộc project nào (null = root)
 *   └─ userOwnerId: String     -> ID user sở hữu
 * 
 * === TÍNH NĂNG NỔI BẬT ===
 * 1. Project Hierarchy:
 *    - projectId = null: Task ở root level
 *    - projectId = "xyz": Task thuộc project "xyz"
 * 
 * 2. Task Types:
 *    - taskType = 0: Task thông thường (ví dụ: "Hoàn thành báo cáo")
 *    - taskType = 1: Shopping item (ví dụ: "Mua sữa")
 * 
 * 3. Reminders:
 *    - reminderTime: Thời gian nhắc
 *    - alarmRequestCode: ID để hủy alarm qua AlarmManager
 * 
 * === ANNOTATIONS ===
 * @Exclude: documentId không lưu vào Firestore
 * @PropertyName("completed"): Map với field "completed" trong Firestore
 *   - isCompleted() -> read
 *   - setCompleted() -> write
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * ```java
 * // Tạo task thông thường
 * TaskEntry task = new TaskEntry();
 * task.setName("Học Android");
 * task.setCompleted(false);
 * task.setTaskType(0);  // Task type
 * task.setProjectId("project123");  // Thuộc project
 * 
 * // Tạo shopping item
 * TaskEntry shopping = new TaskEntry();
 * shopping.setName("Mua sữa");
 * shopping.setTaskType(1);  // Shopping type
 * shopping.setProjectId(null);  // Root level
 * 
 * // Set reminder
 * task.setReminderTime(new Date(System.currentTimeMillis() + 86400000)); // +1 day
 * task.setAlarmRequestCode(456);
 * ```
 * 
 * @see ProductivityRepository Repository quản lý tasks
 * @see TaskReminderHelper Helper đặt reminder cho tasks
 * @see ProjectEntry Project chứa task
 */
public class TaskEntry {

    // ===== FIRESTORE DOCUMENT ID =====
    @Exclude
    public String documentId;  // ID document từ Firestore (không sync)

    // ===== PRIVATE FIELDS =====

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