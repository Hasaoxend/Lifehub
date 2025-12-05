package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.Date;

/**
 * ProjectEntry - POJO cho Thư mục/Project (Folder/Category)
 * 
 * === MỤC ĐÍCH ===
 * Quản lý các thư mục để nhóm Tasks theo chuề đề.
 * Ví dụ: "Công việc", "Cá nhân", "Học tập", ...
 * 
 * === FIRESTORE STRUCTURE ===
 * users/{userId}/projects/{projectId}
 *   ├─ name: String            -> Tên thư mục
 *   ├─ color: String           -> Màu hiển thị (hex: #FF5722)
 *   ├─ userOwnerId: String    -> Firebase Auth UID
 *   ├─ createdDate: Timestamp -> Ngày tạo
 *   ├─ lastModified: Timestamp-> Lần sửa cuối
 *   └─ projectId: String      -> ID thư mục cha (null = root)
 * 
 * === TÍNH NĂNG NỔI BẬT ===
 * 1. Nested Folders (Thư mục lồng nhau):
 *    - projectId = null: Thư mục gốc (root folder)
 *    - projectId = "abc": Thư mục con của "abc"
 * 
 *    Ví dụ:
 *    ```
 *    Root
 *    ├─ Công việc (projectId=null)
 *    │  ├─ Dự án A (projectId="congviec_id")
 *    │  └─ Dự án B (projectId="congviec_id")
 *    └─ Cá nhân (projectId=null)
 *       └─ Sức khỏe (projectId="canhan_id")
 *    ```
 * 
 * 2. Color Coding:
 *    - Mỗi thư mục có màu riêng
 *    - Hiển thị trên UI để dễ phân biệt
 *    - Mặc định: #808080 (gray)
 * 
 * 3. Serializable:
 *    - Implements Serializable để truyền qua Intent
 *    - Dùng cho Edit/View Project Activity
 * 
 * === LIÊN KẾT VỚI TASKS ===
 * ```java
 * // Tạo thư mục
 * ProjectEntry project = new ProjectEntry("Công việc", userId);
 * project.setColor("#FF5722");
 * repository.insertProject(project);
 * 
 * // Tạo task thuộc thư mục
 * TaskEntry task = new TaskEntry();
 * task.setName("Hoàn thành báo cáo");
 * task.setProjectId(project.documentId);  // Liên kết với project
 * repository.insertTask(task);
 * ```
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * ```java
 * // Tạo thư mục gốc
 * ProjectEntry root = new ProjectEntry("Công việc", userId);
 * root.setProjectId(null);  // Root folder
 * root.setColor("#2196F3");
 * 
 * // Tạo thư mục con
 * ProjectEntry sub = new ProjectEntry("Dự án A", userId);
 * sub.setProjectId(root.documentId);  // Thuộc thư mục "Công việc"
 * sub.setColor("#4CAF50");
 * ```
 * 
 * @see ProductivityRepository Repository quản lý projects
 * @see TaskEntry Tasks thuộc projects
 */
public class ProjectEntry implements Serializable {

    // ===== FIRESTORE DOCUMENT ID =====
    @Exclude
    public String documentId;  // ID document từ Firestore (không sync)

    private String name;
    private String color;
    private String userOwnerId;
    private Date createdDate;
    private Date lastModified;

    // ✅ THÊM: ID của thư mục cha (null = root)
    private String projectId;

    public ProjectEntry() {
        // Constructor rỗng
    }

    public ProjectEntry(String name, String userOwnerId) {
        this.name = name;
        this.userOwnerId = userOwnerId;
        this.color = "#808080";
        this.createdDate = new Date();
        this.lastModified = new Date();
        this.projectId = null; // Mặc định là thư mục gốc
    }

    // --- Getters & Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getUserOwnerId() { return userOwnerId; }
    public void setUserOwnerId(String userOwnerId) { this.userOwnerId = userOwnerId; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }

    public Date getLastModified() { return lastModified; }
    public void setLastModified(Date lastModified) { this.lastModified = lastModified; }

    // ✅ THÊM MỚI:
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    @Override
    public String toString() {
        return name;
    }
}