package com.test.lifehub.core.util;

/**
 * Lớp chứa các hằng số (constants) dùng chung cho toàn bộ ứng dụng.
 */
public final class Constants {

    private Constants() {
        // Lớp này không nên được khởi tạo
    }

    // --- Hằng số cho CSDL Room ---
    public static final String DATABASE_NAME = "lifehub_database.db";


    // --- Hằng số cho Nhắc nhở & Thông báo (Notifications) ---
    public static final String NOTIFICATION_CHANNEL_ID = "LifeHubReminderChannel";
    public static final String NOTIFICATION_CHANNEL_NAME = "Nhắc nhở LifeHub";
    public static final String NOTIFICATION_CHANNEL_DESC = "Thông báo cho Ghi chú và Công việc đã lên lịch";
    public static final String EXTRA_REMINDER_TITLE = "REMINDER_TITLE";
    public static final String EXTRA_REMINDER_CONTENT = "REMINDER_CONTENT";
    public static final String EXTRA_REMINDER_ID = "REMINDER_ID";


    // --- Hằng số cho Module Năng suất (Productivity) ---
    public static final int TASK_TYPE_GENERAL = 0;
    public static final int TASK_TYPE_SHOPPING = 1;


    // ----- SỬA LỖI: HẰNG SỐ ĐỒNG BỘ TRUNG TÂM -----
    // (Dùng cho tất cả các Bảng: Account, Note, Task)

    /** 0 = Đã đồng bộ (SYNCED) */
    public static final int STATUS_SYNCED = 0;

    /** 1 = Mới tạo (CREATED) (Cần đẩy lên server) */
    public static final int STATUS_CREATED = 1;

    /** 2 = Đã sửa (UPDATED) (Cần đẩy lên server) */
    public static final int STATUS_UPDATED = 2;

    /** 3 = Đã xóa (DELETED) (Cần đẩy lên server) */
    public static final int STATUS_DELETED = 3;

}