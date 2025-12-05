package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

/**
 * NoteEntry - POJO (Plain Old Java Object) cho Ghi chú
 * 
 * === MỤC ĐÍCH ===
 * Class này map trực tiếp với Firestore document:
 * users/{userId}/notes/{noteId}
 * 
 * === FIRESTORE MAPPING ===
 * Firestore Document Fields:
 *   ├─ title: String          -> private String title
 *   ├─ content: String        -> private String content
 *   ├─ lastModified: Timestamp -> private Date lastModified
 *   ├─ userOwnerId: String    -> private String userOwnerId
 *   ├─ reminderTime: Timestamp -> private Date reminderTime (optional)
 *   └─ alarmRequestCode: int  -> private int alarmRequestCode
 * 
 * === ANNOTATIONS ===
 * @Exclude: Field không lưu vào Firestore
 *   - documentId: Chỉ dùng local, không sync với Firestore
 * 
 * === FIELDS ===
 * - title: Tiêu đề ghi chú (ví dụ: "Học Android")
 * - content: Nội dung chi tiết
 * - lastModified: Thời gian sửa cuối cùng
 * - userOwnerId: ID user sở hữu (Firebase Auth UID)
 * - reminderTime: Thời gian nhắc (optional, null = không có reminder)
 * - alarmRequestCode: ID của AlarmManager để hủy alarm khi cần
 * 
 * === GETTERS/SETTERS ===
 * - Firestore YÊU CẦU PHẢI có getters/setters public
 * - Constructor rỗng (no-arg) bắt buộc cho Firestore deserialization
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * ```java
 * // Tạo note mới
 * NoteEntry note = new NoteEntry();
 * note.setTitle("Shopping List");
 * note.setContent("Milk, Bread, Eggs");
 * note.setLastModified(new Date());
 * 
 * // Lưu vào Firestore
 * repository.insertNote(note);
 * 
 * // Set reminder
 * note.setReminderTime(new Date(System.currentTimeMillis() + 3600000)); // +1 hour
 * note.setAlarmRequestCode(12345);
 * ```
 * 
 * @see ProductivityRepository Repository quản lý notes
 * @see NoteReminderHelper Helper đặt reminder cho notes
 */
public class NoteEntry {

    // ===== FIRESTORE DOCUMENT ID =====
    /**
     * Document ID từ Firestore
     * 
     * @Exclude: Không lưu vào Firestore
     * Chỉ dùng local để:
     * - Update/delete document
     * - Hiển thị trên UI
     * - Track item trong RecyclerView
     */
    @Exclude
    public String documentId;

    // ===== PRIVATE FIELDS (FIRESTORE SYNC) =====

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