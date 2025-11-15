package com.test.lifehub.features.two_productivity.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

import java.util.Date;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity hiển thị danh sách Công việc (Tasks) HOẶC Mua sắm (Shopping).
 * (Phiên bản đã sửa lỗi Date/long)
 */
@AndroidEntryPoint
public class TaskListActivity extends AppCompatActivity
        implements TaskAdapter.OnTaskInteractionListener {

    private ProductivityViewModel mViewModel;
    private TaskAdapter mTaskAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFab;
    private Toolbar mToolbar;
    private TextView mEmptyView;

    private int mTaskType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        // --- 1. Nhận loại Task từ Intent ---
        mTaskType = getIntent().getIntExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);

        // --- 2. Ánh xạ Views ---
        mToolbar = findViewById(R.id.toolbar_task_list);
        mRecyclerView = findViewById(R.id.recycler_view_task_list);
        mFab = findViewById(R.id.fab_add_task);
        mEmptyView = findViewById(R.id.empty_view_tasks);

        // --- 3. Cài đặt Toolbar (Dựa trên loại Task) ---
        setSupportActionBar(mToolbar);
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            getSupportActionBar().setTitle("Danh sách Mua sắm");
        } else {
            getSupportActionBar().setTitle("Công việc (To-do)");
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        // --- 4. Cài đặt RecyclerView ---
        mTaskAdapter = new TaskAdapter(this);
        mRecyclerView.setAdapter(mTaskAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- 5. Cài đặt FAB ---
        mFab.setOnClickListener(v -> {
            AddEditTaskDialog dialog = new AddEditTaskDialog(null, mTaskType);
            dialog.show(getSupportFragmentManager(), "AddTaskDialog");
        });

        // --- 6. Cài đặt ViewModel ---
        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        // --- 7. Quan sát (Observe) Dữ liệu (Dựa trên loại Task) ---
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            mViewModel.getAllShoppingItems().observe(this, tasks -> {
                mTaskAdapter.submitList(tasks);
                // Xử lý hiển thị danh sách rỗng
                if (tasks == null || tasks.isEmpty()) {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            });
        } else {
            mViewModel.getAllTasks().observe(this, tasks -> {
                mTaskAdapter.submitList(tasks);
                // Xử lý hiển thị danh sách rỗng
                if (tasks == null || tasks.isEmpty()) {
                    mRecyclerView.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            });
        }
    }

    // ----- SỬA LỖI: Implement 2 hàm của OnTaskInteractionListener -----

    /**
     * Được gọi từ Adapter khi người dùng nhấn checkbox.
     */
    @Override
    public void onTaskCheckChanged(TaskEntry task, boolean isChecked) {
        // Cập nhật trạng thái và thời gian, sau đó
        // yêu cầu ViewModel cập nhật lên Firestore.
        task.setCompleted(isChecked);

        // SỬA LỖI (incompatible types): Xóa .getTime()
        task.setLastModified(new Date());

        mViewModel.updateTask(task);
    }

    /**
     * Được gọi từ Adapter khi người dùng nhấn vào một item.
     */
    @Override
    public void onTaskClicked(TaskEntry task) {
        // Mở Dialog để Sửa (Edit)
        AddEditTaskDialog dialog = new AddEditTaskDialog(task, task.getTaskType());
        dialog.show(getSupportFragmentManager(), "EditTaskDialog");
    }
}