package com.test.lifehub.features.two_productivity.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.util.Constants;

/**
 * Fragment chính cho tab Năng suất.
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 */
public class ProductivityFragment extends Fragment {

    private ProductivityViewModel mViewModel;
    private NoteAdapter mNoteAdapter;
    private TaskAdapter mTaskAdapter;
    private TaskAdapter mShoppingAdapter;

    // Launcher để nhận kết quả từ AddEditNoteActivity
    private final ActivityResultLauncher<Intent> noteActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // (Tùy chọn: Hiển thị Toast khi quay về)
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_productivity, container, false);

        // --- 1. Cấu hình RecyclerView cho Ghi chú ---
        RecyclerView noteRecyclerView = view.findViewById(R.id.recycler_view_notes);
        noteRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // (Adapter sẽ được cung cấp ở file tiếp theo)
        mNoteAdapter = new NoteAdapter(requireContext(), noteActivityResultLauncher);
        noteRecyclerView.setAdapter(mNoteAdapter);

        // --- 2. Cấu hình RecyclerView cho Công việc ---
        RecyclerView taskRecyclerView = view.findViewById(R.id.recycler_view_tasks);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // (Adapter sẽ được cung cấp ở file tiếp theo)
        mTaskAdapter = new TaskAdapter(requireContext());
        taskRecyclerView.setAdapter(mTaskAdapter);

        // --- 3. Cấu hình RecyclerView cho Mua sắm ---
        RecyclerView shoppingRecyclerView = view.findViewById(R.id.recycler_view_shopping);
        shoppingRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mShoppingAdapter = new TaskAdapter(requireContext());
        shoppingRecyclerView.setAdapter(mShoppingAdapter);

        // --- 4. Cấu hình Nút Bấm ---
        FloatingActionButton fabAddTask = view.findViewById(R.id.fab_add_task);
        FloatingActionButton fabAddNote = view.findViewById(R.id.fab_add_note);
        FloatingActionButton fabAddShopping = view.findViewById(R.id.fab_add_shopping);

        // Nút Thêm Công việc
        fabAddTask.setOnClickListener(v -> {
            AddEditTaskDialog dialog = new AddEditTaskDialog(null, Constants.TASK_TYPE_GENERAL);
            dialog.show(getParentFragmentManager(), "AddTaskDialog");
        });

        // Nút Thêm Đồ Mua sắm
        fabAddShopping.setOnClickListener(v -> {
            AddEditTaskDialog dialog = new AddEditTaskDialog(null, Constants.TASK_TYPE_SHOPPING);
            dialog.show(getParentFragmentManager(), "AddShoppingDialog");
        });

        // Nút Thêm Ghi chú
        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEditNoteActivity.class);
            noteActivityResultLauncher.launch(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel (phiên bản Firestore)
        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        // "Quan sát" (Observe) 3 luồng LiveData real-time

        mViewModel.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            mNoteAdapter.submitList(notes);
        });

        mViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            mTaskAdapter.submitList(tasks);
        });

        mViewModel.getAllShoppingItems().observe(getViewLifecycleOwner(), items -> {
            mShoppingAdapter.submitList(items);
        });
    }
}