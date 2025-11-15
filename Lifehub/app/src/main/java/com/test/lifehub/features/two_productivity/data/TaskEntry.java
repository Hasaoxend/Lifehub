package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO cho một Công việc (Task).
 * (KHÔNG CÒN LÀ ENTITY CỦA ROOM)
 */
public class TaskEntry implements Serializable {

    @Exclude
    public String documentId;

    // ----- Dữ liệu Công việc -----
    public String title;
    public boolean isCompleted;
    public long reminderTime; // (Giữ nguyên)
    public int taskType; // (Giữ nguyên)

    // ----- Thông tin Người sở hữu (Bắt buộc cho Bảo mật) -----
    public String userOwnerId;

    // ----- Thông tin đồng bộ -----
    @ServerTimestamp
    public Date lastModified;

    // Hằng số cho Loại Công việc
    public static final int TASK_TYPE_GENERAL = 0;
    public static final int TASK_TYPE_SHOPPING = 1;

    public TaskEntry() {
        // Constructor rỗng (bắt buộc cho Firestore)
    }
}