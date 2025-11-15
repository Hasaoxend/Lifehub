package com.test.lifehub.features.two_productivity.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView; // Thêm import

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity hiển thị danh sách Ghi chú (Notes).
 * (Phiên bản đã sửa lỗi Hilt)
 */
@AndroidEntryPoint // <-- SỬA LỖI: ĐÃ THÊM
public class NotesListActivity extends AppCompatActivity {

    private ProductivityViewModel mViewModel;
    private NoteAdapter mNoteAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFab;
    private Toolbar mToolbar;
    private TextView mEmptyView; // Thêm View cho danh sách rỗng

    // Launcher để nhận kết quả từ AddEditNoteActivity
    private final ActivityResultLauncher<Intent> noteActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // (Tùy chọn: Hiển thị Toast "Đã lưu")
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        // --- Ánh xạ Views ---
        mToolbar = findViewById(R.id.toolbar_notes_list);
        mRecyclerView = findViewById(R.id.recycler_view_notes_list);
        mFab = findViewById(R.id.fab_add_note);
        mEmptyView = findViewById(R.id.empty_view_notes); // (Giả sử bạn đã thêm ID này vào XML)

        // --- Cài đặt Toolbar ---
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Ghi chú");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        // --- Cài đặt RecyclerView ---
        mNoteAdapter = new NoteAdapter(this, noteActivityResultLauncher);
        mRecyclerView.setAdapter(mNoteAdapter);
        // DÒNG MỚI (2 cột)
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        // --- Cài đặt FAB ---
        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, AddEditNoteActivity.class);
            noteActivityResultLauncher.launch(intent);
        });

        // --- Cài đặt ViewModel ---
        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        // --- Quan sát (Observe) Dữ liệu ---
        mViewModel.getAllNotes().observe(this, notes -> {
            mNoteAdapter.submitList(notes);
            // Xử lý hiển thị danh sách rỗng
            if (notes == null || notes.isEmpty()) {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            } else {
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            }
        });
    }
}