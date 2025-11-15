package com.test.lifehub.features.two_productivity.data;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.Date;

/**
 * POJO cho Project (Thư mục Todo)
 */
public class ProjectEntry implements Serializable {

    @Exclude
    public String documentId;

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