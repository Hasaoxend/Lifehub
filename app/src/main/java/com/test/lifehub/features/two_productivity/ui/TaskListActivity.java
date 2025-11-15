package com.test.lifehub.features.two_productivity.ui;

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

        mTaskType = getIntent().getIntExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);

        mToolbar = findViewById(R.id.toolbar_task_list);
        mRecyclerView = findViewById(R.id.recycler_view_task_list);
        mFab = findViewById(R.id.fab_add_task);
        mEmptyView = findViewById(R.id.empty_view_tasks);

        setSupportActionBar(mToolbar);
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            getSupportActionBar().setTitle("Danh sách Mua sắm");
        } else {
            getSupportActionBar().setTitle("Công việc (To-do)");
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mTaskAdapter = new TaskAdapter(this);
        mRecyclerView.setAdapter(mTaskAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mFab.setOnClickListener(v -> {
            AddEditTaskDialog dialog = new AddEditTaskDialog(null, mTaskType);
            dialog.show(getSupportFragmentManager(), "AddTaskDialog");
        });

        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            mViewModel.getAllShoppingItems().observe(this, tasks -> {
                mTaskAdapter.submitList(tasks);
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

    /**
     * Được gọi từ Adapter khi người dùng nhấn checkbox.
     */
    @Override
    public void onTaskCheckChanged(TaskEntry task, boolean isChecked) {
        task.setCompleted(isChecked);
        task.setLastModified(new Date()); // SỬA LỖI: Dùng new Date()
        mViewModel.updateTask(task);
    }

    /**
     * Được gọi từ Adapter khi người dùng nhấn vào một item.
     */
    @Override
    public void onTaskClicked(TaskEntry task) {
        AddEditTaskDialog dialog = new AddEditTaskDialog(task, task.getTaskType());
        dialog.show(getSupportFragmentManager(), "EditTaskDialog");
    }
}