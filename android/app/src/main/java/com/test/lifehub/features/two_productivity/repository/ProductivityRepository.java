package com.test.lifehub.features.two_productivity.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.NoteEntry;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.scopes.ActivityRetainedScoped; // <-- THÊM IMPORT NÀY

/**
 * ProductivityRepository - Quản lý dữ liệu Năng suất từ Firestore
 * 
 * === NHIỆM VỤ ===
 * Repository tổng hợp quản lý 3 loại dữ liệu:
 * 1. Notes (Ghi chú): Tạo/sửa/xóa ghi chú cá nhân
 * 2. Tasks (Công việc): Quản lý task với reminder, completion status
 * 3. Projects (Dự án): Hỗ trợ hiệu quả theo dự án, nested projects
 * 
 * === FIRESTORE STRUCTURE ===
 * users/{userId}/
 *   ├─ notes/{noteId}
 *   │   ├─ title: String
 *   │   ├─ content: String
 *   │   ├─ lastModified: Timestamp
 *   │   └─ reminderTime: Timestamp (optional)
 *   ├─ tasks/{taskId}
 *   │   ├─ name: String
 *   │   ├─ completed: boolean
 *   │   ├─ taskType: int (0=Task, 1=Shopping)
 *   │   ├─ projectId: String (null = root level)
 *   │   └─ reminderTime: Timestamp (optional)
 *   └─ projects/{projectId}
 *       ├─ name: String
 *       ├─ color: String (hex code)
 *       ├─ projectId: String (parent project, null = root)
 *       └─ createdDate: Timestamp
 * 
 * === FEATURES NỔI BẬT ===
 * 1. Realtime Sync: Tự động cập nhật UI khi data thay đổi
 * 2. Project Hierarchy: Hỗ trợ sub-project (project lồng project)
 * 3. Task Organization: Tasks có thể thuộc project hoặc ở root level
 * 4. Shopping List: Task loại 1 (taskType=1) là shopping items
 * 5. User Isolation: Mỗi user chỉ thấy data của mình
 * 
 * === DEPENDENCIES ===
 * @Inject FirebaseAuth: Lấy userId hiện tại
 * @Inject FirebaseFirestore: Truy xuất Firestore database
 * 
 * === LIFECYCLE ===
 * 1. Constructor: Tự động gọi startListening()
 * 2. startListening(): Bắt đầu 3 listeners (notes, tasks, projects)
 * 3. stopListening(): Dừng tất cả listeners (gọi khi logout)
 * 
 * === LƯU Ý BẢO MẬT ===
 * - Phát hiện user thay đổi: Nếu userId khác -> dừng listener cũ + xóa data cũ
 * - Tránh data leak giữa các user khác nhau
 * - Gọi stopListening() khi logout để tránh memory leak
 * 
 * === PHÁT TRIỂN TIẾP ===
 * TODO: Thêm offline support với Firestore persistence
 * TODO: Implement pagination cho danh sách lớn (>100 items)
 * TODO: Thêm query filter theo date range
 * TODO: Thêm batch operations cho performance
 * 
 * @see NoteEntry POJO cho ghi chú
 * @see TaskEntry POJO cho task
 * @see ProjectEntry POJO cho project
 * @see ProductivityViewModel ViewModel sử dụng repository này
 */
@ActivityRetainedScoped // <-- THAY THẾ @Singleton BẰNG CÁI NÀY
public class ProductivityRepository {

    private static final String TAG = "ProductivityRepo";
    private static final String COLLECTION_PROJECTS = "projects";

    // ===== DEPENDENCIES =====
    private final FirebaseAuth mAuth;         // Firebase Authentication
    private final FirebaseFirestore mDb;      // Firestore Database
    
    // ===== COLLECTION REFERENCES =====
    private CollectionReference mNotesCollection;     // Reference đến notes collection
    private CollectionReference mTasksCollection;     // Reference đến tasks collection
    private CollectionReference mProjectsCollection;  // Reference đến projects collection

    // ===== LIVEDATA =====
    // LiveData cho UI observe - tự động update UI khi data thay đổi
    private final MutableLiveData<List<NoteEntry>> mAllNotes = new MutableLiveData<>();
    private final MutableLiveData<List<TaskEntry>> mAllTasks = new MutableLiveData<>();
    private final MutableLiveData<List<TaskEntry>> mAllShoppingItems = new MutableLiveData<>();
    private final MutableLiveData<List<ProjectEntry>> mAllProjects = new MutableLiveData<>();
    
    // ✅ BẢO MẬT: Track user để detect user change
    private String currentUserId = null;
    private ListenerRegistration notesListener = null;
    private ListenerRegistration tasksListener = null;
    private ListenerRegistration projectsListener = null;

    @Inject
    public ProductivityRepository(FirebaseAuth auth, FirebaseFirestore db) {
        this.mAuth = auth;
        this.mDb = db;
        
        // ✅ BẢO MẬT: Khởi tạo listener với user tracking
        startListening();
    }
    
    /**
     * ✅ BẢO MẬT: Bắt đầu listening với user change detection
     */
    public void startListening() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not logged in, cannot start listening");
            stopListening();
            clearAllData();
            return;
        }
        
        String newUserId = user.getUid();
        
        // Detect user change
        if (currentUserId != null && !currentUserId.equals(newUserId)) {
            Log.d(TAG, "User changed from " + currentUserId + " to " + newUserId + ", clearing old data");
            stopListening();
            clearAllData();
        }
        
        // Nếu đã listening cho cùng user, skip
        if (currentUserId != null && currentUserId.equals(newUserId) && notesListener != null) {
            Log.d(TAG, "Already listening for user: " + newUserId);
            return;
        }
        
        currentUserId = newUserId;
        String uid = user.getUid();
        mNotesCollection = mDb.collection(Constants.COLLECTION_USERS).document(uid).collection(Constants.COLLECTION_NOTES);
        mTasksCollection = mDb.collection(Constants.COLLECTION_USERS).document(uid).collection(Constants.COLLECTION_TASKS);
        mProjectsCollection = mDb.collection(Constants.COLLECTION_USERS).document(uid).collection(COLLECTION_PROJECTS);

        Log.d(TAG, "Repository initialized. User: " + uid);
        listenForNoteChanges();
        listenForTaskChanges();
        listenForProjectChanges();
    }
    
    /**
     * ✅ BẢO MẬT: Dừng tất cả listeners
     * Gọi khi user logout để tránh data leak
     */
    public void stopListening() {
        if (notesListener != null) {
            notesListener.remove();
            notesListener = null;
        }
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
        if (projectsListener != null) {
            projectsListener.remove();
            projectsListener = null;
        }
        currentUserId = null;
        Log.d(TAG, "Stopped all Firestore listeners");
    }
    
    /**
     * ✅ BẢO MẬT: Clear toàn bộ data trong LiveData
     * Gọi khi user logout hoặc switch user
     */
    private void clearAllData() {
        mAllNotes.setValue(new ArrayList<>());
        mAllTasks.setValue(new ArrayList<>());
        mAllShoppingItems.setValue(new ArrayList<>());
        mAllProjects.setValue(new ArrayList<>());
        Log.d(TAG, "Cleared all LiveData");
    }

    private void listenForNoteChanges() {
        if (mNotesCollection == null) return;
        notesListener = mNotesCollection.orderBy("lastModified", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { Log.w(TAG, "❌ Lỗi lắng nghe Notes", e); return; }
                    if (snapshot != null) {
                        List<NoteEntry> notes = snapshot.toObjects(NoteEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            notes.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        mAllNotes.setValue(notes);
                        Log.d(TAG, "✅ Notes updated: " + notes.size() + " items");
                    }
                });
    }

    /**
     * ✅ SỬA LỖI (Index): Đơn giản hóa Query để không cần Index
     * Chúng ta sẽ sắp xếp (sort) ở client (Activity/ViewModel)
     */
    private void listenForTaskChanges() {
        if (mTasksCollection == null) return;
        Log.d(TAG, "Lắng nghe TẤT CẢ Tasks...");

        // Query đơn giản nhất: Lấy tất cả, sắp xếp theo thời gian
        tasksListener = mTasksCollection.orderBy("lastModified", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "❌ Lỗi lắng nghe Tasks", e);
                        return;
                    }
                    if (snapshot != null) {
                        List<TaskEntry> allTasks = snapshot.toObjects(TaskEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            allTasks.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }

                        // Lọc ra 2 danh sách
                        List<TaskEntry> generalTasks = new ArrayList<>();
                        List<TaskEntry> shoppingItems = new ArrayList<>();
                        for (TaskEntry task : allTasks) {
                            if (task.getTaskType() == Constants.TASK_TYPE_SHOPPING) {
                                shoppingItems.add(task);
                            } else {
                                generalTasks.add(task); // Bao gồm cả task trong project
                            }
                        }

                        // Cập nhật cả 2 LiveData
                        mAllTasks.setValue(generalTasks);
                        mAllShoppingItems.setValue(shoppingItems);
                        Log.d(TAG, "✅ Tasks updated: " + generalTasks.size() + " tasks, " + shoppingItems.size() + " shopping items");
                    }
                });
    }

    // Lắng nghe Projects (sắp xếp A-Z)
    private void listenForProjectChanges() {
        if (mProjectsCollection == null) return;
        projectsListener = mProjectsCollection.orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { Log.w(TAG, "❌ Lỗi lắng nghe Projects", e); return; }
                    if (snapshot != null) {
                        List<ProjectEntry> projects = snapshot.toObjects(ProjectEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            projects.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        mAllProjects.setValue(projects);
                        Log.d(TAG, "✅ Projects updated: " + projects.size() + " items");
                    }
                });
    }

    // ----- Getters -----
    public LiveData<List<NoteEntry>> getAllNotes() { return mAllNotes; }
    public LiveData<List<TaskEntry>> getAllTasks() { return mAllTasks; }
    public LiveData<List<TaskEntry>> getAllShoppingItems() { return mAllShoppingItems; }
    public LiveData<List<ProjectEntry>> getAllProjects() { return mAllProjects; }

    public LiveData<NoteEntry> getNoteById(String documentId) {
        MutableLiveData<NoteEntry> noteData = new MutableLiveData<>();
        if (mNotesCollection == null) return noteData;
        mNotesCollection.document(documentId).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                NoteEntry note = snapshot.toObject(NoteEntry.class);
                if (note != null) {
                    note.documentId = snapshot.getId();
                    noteData.setValue(note);
                }
            }
        });
        return noteData;
    }

    // ----- Notes -----
    public void insertNote(NoteEntry note) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (mNotesCollection == null || currentUser == null) return;
        note.setUserOwnerId(currentUser.getUid());
        mNotesCollection.add(note)
                .addOnSuccessListener(ref -> Log.d(TAG, "✅ Đã thêm Note: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi thêm Note", e));
    }
    public void updateNote(NoteEntry note) {
        if (mNotesCollection == null || note.documentId == null) return;
        mNotesCollection.document(note.documentId).set(note)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã cập nhật Note"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi cập nhật Note", e));
    }
    public void deleteNote(NoteEntry note) {
        if (mNotesCollection == null || note.documentId == null) return;
        mNotesCollection.document(note.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã xóa Note"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi xóa Note", e));
    }

    // ----- Tasks -----
    public void insertTask(TaskEntry task) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "insertTask called. ProjectId: " + task.getProjectId());
        Log.d(TAG, "User: " + (currentUser != null ? currentUser.getUid() : "NULL"));

        if (mTasksCollection == null || currentUser == null) {
            Log.e(TAG, "Cannot insert task: collection or user is null");
            return;
        }

        task.setUserOwnerId(currentUser.getUid());
        mTasksCollection.add(task)
                .addOnSuccessListener(ref -> Log.d(TAG, "✅ Đã thêm Task: " + ref.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi thêm Task", e));
    }
    public void updateTask(TaskEntry task) {
        if (mTasksCollection == null || task.documentId == null) return;
        mTasksCollection.document(task.documentId).set(task)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã cập nhật Task"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi cập nhật Task", e));
    }
    public void deleteTask(TaskEntry task) {
        if (mTasksCollection == null || task.documentId == null) return;
        mTasksCollection.document(task.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã xóa Task"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi xóa Task", e));
    }

    // ----- Projects -----
    public void insertProject(String name, String parentProjectId) {
        if (mProjectsCollection == null || mAuth.getCurrentUser() == null) return;
        ProjectEntry project = new ProjectEntry(name, mAuth.getCurrentUser().getUid());
        project.setProjectId(parentProjectId); // Gán ID cha
        mProjectsCollection.add(project)
                .addOnSuccessListener(ref -> Log.d(TAG, "✅ Đã tạo Project: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi tạo Project", e));
    }

    public void updateProjectName(String projectId, String newName) {
        if (mProjectsCollection == null || projectId == null) return;
        mProjectsCollection.document(projectId)
                .update("name", newName, "lastModified", new Date())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã đổi tên Project"))
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi đổi tên Project", e));
    }

    public void deleteProject(ProjectEntry project) {
        if (mProjectsCollection == null || project.documentId == null) return;

        WriteBatch batch = mDb.batch();

        mTasksCollection.whereEqualTo("projectId", project.documentId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
                        batch.update(querySnapshot.getDocuments().get(i).getReference(), "projectId", null);
                    }

                    batch.delete(mProjectsCollection.document(project.documentId));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã xóa Project và cập nhật tasks"))
                            .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi xóa Project (batch)", e));
                })
                .addOnFailureListener(e -> Log.w(TAG, "❌ Lỗi tìm task để xóa project", e));
    }
}