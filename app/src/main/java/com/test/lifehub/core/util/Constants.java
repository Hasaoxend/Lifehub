package com.test.lifehub.core.util;

/**
 * Lớp chứa các hằng số (constants) dùng chung cho toàn bộ ứng dụng.
 * (Phiên bản đã cập nhật cho Firebase Firestore và kiến trúc mới)
 */
public final class Constants {

    private Constants() {
        // Lớp này không nên được khởi tạo
    }

    // --- Hằng số cho Firestore Collections ---
    // (Quan trọng để các Repository gọi đúng tên)
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_ACCOUNTS = "accounts";
    public static final String COLLECTION_NOTES = "notes";
    public static final String COLLECTION_TASKS = "tasks";


    // --- Hằng số cho Nhắc nhở & Thông báo (Notifications) ---
    // (Những hằng số này vẫn giữ nguyên, chúng vẫn hữu ích)
    public static final String NOTIFICATION_CHANNEL_ID = "LifeHubReminderChannel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Nhắc nhở LifeHub";
    public static final String NOTIFICATION_CHANNEL_DESC = "Thông báo cho Ghi chú và Công việc đã lên lịch";
    public static final String EXTRA_REMINDER_TITLE = "REMINDER_TITLE";
    public static final String EXTRA_REMINDER_CONTENT = "REMINDER_CONTENT";
    public static final String EXTRA_REMINDER_ID = "REMINDER_ID";


    // --- Hằng số cho Module Năng suất (Productivity) ---
    // (Vẫn giữ nguyên để phân biệt 2 loại Task)
    public static final int TASK_TYPE_GENERAL = 0;
    public static final int TASK_TYPE_SHOPPING = 1;


    // --- Hằng số cho Intent Extras ---
    // (THÊM MỚI: Dùng để ProductivityFragment "bảo" TaskListActivity
    // nên hiển thị loại công việc nào (General hay Shopping))
    public static final String EXTRA_TASK_TYPE = "EXTRA_TASK_TYPE";


    // --- CÁC HẰNG SỐ CŨ ĐÃ BỊ XÓA ---
    //
    // 1. DATABASE_NAME: Đã bị xóa vì không còn dùng Room Database.
    //
    // 2. STATUS_SYNCED, STATUS_CREATED, STATUS_UPDATED, STATUS_DELETED:
    //    Đã bị xóa vì Firestore tự động đồng bộ (real-time),
    //    chúng ta không cần quản lý trạng thái đồng bộ thủ công nữa.
    //
}