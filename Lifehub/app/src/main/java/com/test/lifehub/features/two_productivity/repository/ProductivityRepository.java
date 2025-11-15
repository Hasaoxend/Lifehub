package com.test.lifehub.features.two_productivity.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.NoteEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository (Kho chứa) cho Module Năng suất.
 * (Phiên bản đã VIẾT LẠI HOÀN TOÀN cho Firebase Firestore)
 */
public class ProductivityRepository {

    private static final String TAG = "ProductivityRepo";
    private static final String NOTES_COLLECTION = "notes";
    private static final String TASKS_COLLECTION = "tasks";

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore mDb;
    private CollectionReference mNotesCollection; // Trỏ đến /users/{UID}/notes
    private CollectionReference mTasksCollection; // Trỏ đến /users/{UID}/tasks

    // LiveData
    private final MutableLiveData<List<NoteEntry>> mAllNotes = new MutableLiveData<>();
    private final MutableLiveData<List<TaskEntry>> mAllTasks = new MutableLiveData<>();
    private final MutableLiveData<List<TaskEntry>> mAllShoppingItems = new MutableLiveData<>();

    public ProductivityRepository(Application application) {
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            // Trỏ CSDL vào đúng thư mục của người dùng
            mNotesCollection = mDb.collection("users").document(uid).collection(NOTES_COLLECTION);
            mTasksCollection = mDb.collection("users").document(uid).collection(TASKS_COLLECTION);

            // Bắt đầu lắng nghe
            listenForNoteChanges();
            listenForTaskChanges();
        }
    }

    /**
     * Lắng nghe thay đổi của Ghi chú (Notes)
     */
    private void listenForNoteChanges() {
        if (mNotesCollection == null) return;
        mNotesCollection.orderBy("lastModified", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { Log.w(TAG, "Lỗi lắng nghe Notes", e); return; }
                    if (snapshot != null) {
                        List<NoteEntry> notes = snapshot.toObjects(NoteEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            notes.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }
                        mAllNotes.setValue(notes);
                    }
                });
    }

    /**
     * Lắng nghe thay đổi của Công việc (Tasks & Shopping)
     */
    private void listenForTaskChanges() {
        if (mTasksCollection == null) return;
        mTasksCollection.orderBy("isCompleted", Query.Direction.ASCENDING)
                .orderBy("lastModified", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) { Log.w(TAG, "Lỗi lắng nghe Tasks", e); return; }
                    if (snapshot != null) {
                        // 1. Lấy TẤT CẢ công việc
                        List<TaskEntry> allTasks = snapshot.toObjects(TaskEntry.class);
                        for (int i = 0; i < snapshot.getDocuments().size(); i++) {
                            allTasks.get(i).documentId = snapshot.getDocuments().get(i).getId();
                        }

                        // 2. Tách (Filter) thành 2 danh sách riêng biệt
                        List<TaskEntry> generalTasks = new ArrayList<>();
                        List<TaskEntry> shoppingItems = new ArrayList<>();
                        for (TaskEntry task : allTasks) {
                            if (task.taskType == Constants.TASK_TYPE_SHOPPING) {
                                shoppingItems.add(task);
                            } else {
                                generalTasks.add(task); // Mặc định là General
                            }
                        }

                        // 3. Đẩy vào 2 LiveData riêng biệt
                        mAllTasks.setValue(generalTasks);
                        mAllShoppingItems.setValue(shoppingItems);
                    }
                });
    }

    // ----- Getters (Lấy dữ liệu) -----
    public LiveData<List<NoteEntry>> getAllNotes() { return mAllNotes; }
    public LiveData<List<TaskEntry>> getAllTasks() { return mAllTasks; }
    public LiveData<List<TaskEntry>> getAllShoppingItems() { return mAllShoppingItems; }

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

    // ----- Cập nhật Ghi chú (Notes) -----

    public void insertNote(NoteEntry note) {
        if (mNotesCollection == null || mAuth.getCurrentUser() == null) return;
        note.userOwnerId = mAuth.getCurrentUser().getUid();
        mNotesCollection.add(note)
                .addOnSuccessListener(ref -> Log.d(TAG, "Đã thêm Ghi chú: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi thêm Ghi chú", e));
    }

    public void updateNote(NoteEntry note) {
        if (mNotesCollection == null || note.documentId == null) return;
        mNotesCollection.document(note.documentId).set(note)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã cập nhật Ghi chú"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi cập nhật Ghi chú", e));
    }

    public void deleteNote(NoteEntry note) {
        if (mNotesCollection == null || note.documentId == null) return;
        mNotesCollection.document(note.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã xóa Ghi chú"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi xóa Ghi chú", e));
    }

    // ----- Cập nhật Công việc (Tasks) -----

    public void insertTask(TaskEntry task) {
        if (mTasksCollection == null || mAuth.getCurrentUser() == null) return;
        task.userOwnerId = mAuth.getCurrentUser().getUid();
        mTasksCollection.add(task)
                .addOnSuccessListener(ref -> Log.d(TAG, "Đã thêm Công việc: " + ref.getId()))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi thêm Công việc", e));
    }

    public void updateTask(TaskEntry task) {
        if (mTasksCollection == null || task.documentId == null) return;
        mTasksCollection.document(task.documentId).set(task)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã cập nhật Công việc"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi cập nhật Công việc", e));
    }

    public void deleteTask(TaskEntry task) {
        if (mTasksCollection == null || task.documentId == null) return;
        mTasksCollection.document(task.documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã xóa Công việc"))
                .addOnFailureListener(e -> Log.w(TAG, "Lỗi xóa Công việc", e));
    }
}