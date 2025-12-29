package com.test.lifehub.features.two_productivity.data;

/**
 * Lớp Wrapper (bọc) để RecyclerView có thể hiển thị
 * cả Project (thư mục) và Task (công việc) trong cùng 1 danh sách.
 */
public class TaskListItem {

    public static final int TYPE_PROJECT = 0;
    public static final int TYPE_TASK = 1;

    public int type;
    public ProjectEntry project;
    public TaskEntry task;

    // Constructor cho Project
    public TaskListItem(ProjectEntry project) {
        this.type = TYPE_PROJECT;
        this.project = project;
        this.task = null;
    }

    // Constructor cho Task
    public TaskListItem(TaskEntry task) {
        this.type = TYPE_TASK;
        this.project = null;
        this.task = task;
    }

    // Cần thiết cho DiffUtil
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TaskListItem that = (TaskListItem) obj;
        if (type != that.type) return false;

        if (type == TYPE_PROJECT) {
            if (project.documentId == null || that.project.documentId == null) return false;
            return project.documentId.equals(that.project.documentId);
        } else {
            if (task.documentId == null || that.task.documentId == null) return false;
            return task.documentId.equals(that.task.documentId);
        }
    }
}