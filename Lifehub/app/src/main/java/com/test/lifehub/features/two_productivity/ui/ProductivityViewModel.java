package com.test.lifehub.features.two_productivity.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.test.lifehub.features.two_productivity.data.NoteEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;
import com.test.lifehub.features.two_productivity.repository.ProductivityRepository;

import java.util.List;

/**
 * ViewModel chịu trách nhiệm cung cấp dữ liệu Năng suất.
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 */
public class ProductivityViewModel extends AndroidViewModel {

    // Repository này là phiên bản GỌI FIRESTORE
    private final ProductivityRepository mRepository;

    // 3 luồng LiveData được cập nhật real-time từ Repository
    private final LiveData<List<NoteEntry>> allNotes;
    private final LiveData<List<TaskEntry>> allTasks;
    private final LiveData<List<TaskEntry>> allShoppingItems;

    public ProductivityViewModel(@NonNull Application application) {
        super(application);
        mRepository = new ProductivityRepository(application);
        allNotes = mRepository.getAllNotes();
        allTasks = mRepository.getAllTasks();
        allShoppingItems = mRepository.getAllShoppingItems();
    }

    // ----- Ghi chú (Notes) -----

    public LiveData<List<NoteEntry>> getAllNotes() {
        return allNotes;
    }

    public LiveData<NoteEntry> getNoteById(String documentId) {
        return mRepository.getNoteById(documentId);
    }

    public void insertNote(NoteEntry note) {
        mRepository.insertNote(note);
    }

    public void updateNote(NoteEntry note) {
        mRepository.updateNote(note);
    }

    public void deleteNote(NoteEntry note) {
        mRepository.deleteNote(note);
    }

    // (Chúng ta không cần decryptNoteContent nữa)


    // ----- Công việc (Tasks & Shopping) -----

    public LiveData<List<TaskEntry>> getAllTasks() {
        return allTasks;
    }

    public LiveData<List<TaskEntry>> getAllShoppingItems() {
        return allShoppingItems;
    }

    public void insertTask(TaskEntry task) {
        mRepository.insertTask(task);
    }

    public void updateTask(TaskEntry task) {
        mRepository.updateTask(task);
    }

    public void deleteTask(TaskEntry task) {
        mRepository.deleteTask(task);
    }
}