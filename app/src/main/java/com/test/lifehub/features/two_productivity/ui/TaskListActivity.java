package com.test.lifehub.features.two_productivity.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.test.lifehub.R;
import com.test.lifehub.core.util.AlarmHelper;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;
import com.test.lifehub.features.two_productivity.data.TaskListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TaskListActivity extends AppCompatActivity
        implements TaskListAdapter.OnItemInteractionListener {

    private static final String TAG = "TaskListActivity";

    public static final String EXTRA_PROJECT_ID = "EXTRA_PROJECT_ID";
    public static final String EXTRA_PROJECT_NAME = "EXTRA_PROJECT_NAME";

    private ProductivityViewModel mViewModel;
    private TaskListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFabMain, mFabAddProject, mFabAddTask;
    private TextView mLabelProject, mLabelTask;
    private Toolbar mToolbar;
    private TextView mEmptyView;
    private CoordinatorLayout mCoordinatorLayout;

    private int mTaskType;

    private String mCurrentProjectId = null;
    private String mCurrentProjectName = null;
    private boolean isFabMenuOpen = false;
    private final OvershootInterpolator interpolator = new OvershootInterpolator();

    // Xóa các biến fabMargin (không cần nữa)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        mTaskType = getIntent().getIntExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);
        mCurrentProjectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        mCurrentProjectName = getIntent().getStringExtra(EXTRA_PROJECT_NAME);

        Log.d(TAG, "TaskListActivity created with type: " + mTaskType + " | ProjectId: " + mCurrentProjectId);

        // --- Ánh xạ Views ---
        mToolbar = findViewById(R.id.toolbar_task_list);
        mRecyclerView = findViewById(R.id.recycler_view_task_list);
        mEmptyView = findViewById(R.id.empty_view_tasks);
        mCoordinatorLayout = findViewById(R.id.task_list_coordinator_layout);

        mFabMain = findViewById(R.id.fab_main);
        mFabAddProject = findViewById(R.id.fab_add_project);
        mFabAddTask = findViewById(R.id.fab_add_task);
        mLabelProject = findViewById(R.id.label_add_project);
        mLabelTask = findViewById(R.id.label_add_task);

        setSupportActionBar(mToolbar);
        updateToolbarTitle();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> onBackPressed());

        mAdapter = new TaskListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupSwipeToDelete();
        setupFabMenu();

        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);
        mViewModel.setCurrentProjectId(mCurrentProjectId);

        observeData();
    }

    private void setupFabMenu() {
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            mFabAddProject.setVisibility(View.GONE);
            mLabelProject.setVisibility(View.GONE);
        }

        mFabMain.setOnClickListener(v -> {
            if (isFabMenuOpen) {
                closeFabMenu(true);
            } else {
                openFabMenu();
            }
        });

        mFabAddProject.setOnClickListener(v -> {
            closeFabMenu(false);
            AddEditProjectDialog dialog = AddEditProjectDialog.newInstance(null, mCurrentProjectId);
            dialog.show(getSupportFragmentManager(), "AddProjectDialog");
        });

        mFabAddTask.setOnClickListener(v -> {
            closeFabMenu(false);
            AddEditTaskDialog dialog = AddEditTaskDialog.newInstance(null, mTaskType, mCurrentProjectId);
            dialog.show(getSupportFragmentManager(), "AddTaskDialog");
        });
    }

    /**
     * ✅ SỬA LỖI 1 (FAB):
     * Chỉ dùng setVisibility, alpha và rotation.
     * Xóa bỏ hoàn toàn TranslationY (di chuyển) vì XML đã xử lý vị trí.
     */
    private void openFabMenu() {
        if (isFabMenuOpen) return;
        isFabMenuOpen = true;

        mFabMain.animate().rotation(135f).setInterpolator(interpolator).setDuration(300).start();

        // --- Hiển thị FAB Task ---
        mFabAddTask.setVisibility(View.VISIBLE);
        mLabelTask.setVisibility(View.VISIBLE);
        mFabAddTask.setAlpha(0f);
        mLabelTask.setAlpha(0f);

        mFabAddTask.animate().alpha(1f).setInterpolator(interpolator).setDuration(300).start();
        mLabelTask.animate().alpha(1f).setDuration(300).start();

        // --- Hiển thị FAB Project (nếu là General Task) ---
        if (mTaskType == Constants.TASK_TYPE_GENERAL) {
            mFabAddProject.setVisibility(View.VISIBLE);
            mLabelProject.setVisibility(View.VISIBLE);

            mFabAddProject.setAlpha(0f);
            mLabelProject.setAlpha(0f);

            mFabAddProject.animate().alpha(1f).setInterpolator(interpolator).setDuration(300).start();
            mLabelProject.animate().alpha(1f).setDuration(300).start();
        }
    }

    /**
     * ✅ SỬA LỖI 1 (FAB):
     * Sửa lỗi "biến mất" bằng cách dùng setVisibility(View.INVISIBLE)
     */
    private void closeFabMenu(boolean resetRotation) {
        if (!isFabMenuOpen) return;
        isFabMenuOpen = false;

        if (resetRotation) {
            mFabMain.animate().rotation(0f).setInterpolator(interpolator).setDuration(300).start();
        }

        // --- Ẩn FAB Task ---
        mFabAddTask.animate().alpha(0f).setInterpolator(interpolator).setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mFabAddTask.setVisibility(View.INVISIBLE); // Dùng INVISIBLE
                    }
                });
        mLabelTask.animate().alpha(0f).setDuration(300)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mLabelTask.setVisibility(View.INVISIBLE);
                    }
                });

        // --- Ẩn FAB Project ---
        if (mTaskType == Constants.TASK_TYPE_GENERAL) {
            mFabAddProject.animate().alpha(0f).setInterpolator(interpolator).setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFabAddProject.setVisibility(View.INVISIBLE);
                        }
                    });
            mLabelProject.animate().alpha(0f).setDuration(300)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLabelProject.setVisibility(View.INVISIBLE);
                        }
                    });
        }
    }

    private void updateToolbarTitle() {
        String title;
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            title = "Danh sách Mua sắm";
        } else {
            title = (mCurrentProjectName != null) ? mCurrentProjectName : "Công việc (To-do)";
        }
        getSupportActionBar().setTitle(title);
    }

    private void observeData() {
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            mFabAddProject.setVisibility(View.GONE);
            mLabelProject.setVisibility(View.GONE);

            mViewModel.getAllShoppingItems().observe(this, tasks -> {
                Log.d(TAG, "Shopping items updated: " + (tasks != null ? tasks.size() : 0) + " items");
                List<TaskListItem> items = new ArrayList<>();
                if (tasks != null) {
                    Collections.sort(tasks, Comparator.comparing(TaskEntry::isCompleted)
                            .thenComparing(TaskEntry::getLastModified, Comparator.reverseOrder()));
                    for (TaskEntry task : tasks) {
                        items.add(new TaskListItem(task));
                    }
                }
                mAdapter.submitList(items);
                updateEmptyView(items);
            });
        } else {
            // ----- CHẾ ĐỘ CÔNG VIỆC (CÓ THƯ MỤC) -----

            // Lắng nghe Root
            mViewModel.getProjectsInRoot().observe(this, projects -> {
                if (mCurrentProjectId == null) {
                    List<TaskEntry> tasks = mViewModel.getTasksInRoot().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAdapter.submitList(items);
                    updateEmptyView(items);
                    Log.d(TAG, "Root projects updated");
                }
            });
            mViewModel.getTasksInRoot().observe(this, tasks -> {
                if (mCurrentProjectId == null) {
                    List<ProjectEntry> projects = mViewModel.getProjectsInRoot().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAdapter.submitList(items);
                    updateEmptyView(items);
                    Log.d(TAG, "Root tasks updated");
                }
            });

            // Lắng nghe Sub-Project
            mViewModel.getProjectsInProject().observe(this, projects -> {
                if (mCurrentProjectId != null) {
                    List<TaskEntry> tasks = mViewModel.getTasksInProject().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAdapter.submitList(items);
                    updateEmptyView(items);
                    Log.d(TAG, "Sub-projects updated");
                }
            });
            mViewModel.getTasksInProject().observe(this, tasks -> {
                if (mCurrentProjectId != null) {
                    List<ProjectEntry> projects = mViewModel.getProjectsInProject().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAdapter.submitList(items);
                    updateEmptyView(items);
                    Log.d(TAG, "Sub-tasks updated");
                }
            });
        }
    }

    private List<TaskListItem> convertToListItems(List<ProjectEntry> projects, List<TaskEntry> tasks) {
        List<TaskListItem> items = new ArrayList<>();

        if (projects != null) {
            projects.forEach(p -> items.add(new TaskListItem(p)));
        }

        if (tasks != null) {
            Collections.sort(tasks, Comparator.comparing(TaskEntry::isCompleted)
                    .thenComparing(TaskEntry::getLastModified, Comparator.reverseOrder()));
            tasks.forEach(t -> items.add(new TaskListItem(t)));
        }

        return items;
    }

    private void updateEmptyView(List<TaskListItem> items) {
        if (items == null || items.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                TaskListItem item = mAdapter.getItemAt(position);
                if (item == null) return;

                if (item.type == TaskListItem.TYPE_PROJECT) {
                    mAdapter.notifyItemChanged(position);
                    Toast.makeText(TaskListActivity.this, "Vui lòng xóa thư mục bằng menu 3 chấm", Toast.LENGTH_SHORT).show();
                } else {
                    deleteTask(item.task);
                }
            }
        }).attachToRecyclerView(mRecyclerView);
    }

    private void deleteTask(TaskEntry taskToDelete) {
        if (taskToDelete.getAlarmRequestCode() != 0) {
            AlarmHelper.cancelAlarm(this, taskToDelete.getAlarmRequestCode());
            Log.d(TAG, "Đã hủy Alarm cho task (do xóa): " + taskToDelete.getName());
        }

        mViewModel.deleteTask(taskToDelete);

        // === SỬA LỖI: KHÔNG ĐỂ SNACKBAR ĐẨY LAYOUT ===
        // Gắn vào root view thay vì CoordinatorLayout
        View root = findViewById(android.R.id.content);

        Snackbar.make(root != null ? root : mCoordinatorLayout, "Đã xóa công việc", Snackbar.LENGTH_LONG)
                .setAction("Hoàn tác", v -> {
                    mViewModel.insertTask(taskToDelete);
                    if (taskToDelete.getReminderTime() != null) {
                        AlarmHelper.scheduleAlarm(
                                TaskListActivity.this,
                                taskToDelete.getAlarmRequestCode(),
                                taskToDelete.getReminderTime(),
                                (mTaskType == Constants.TASK_TYPE_SHOPPING) ? "Nhắc nhở mua sắm" : "Nhắc nhở công việc",
                                taskToDelete.getName()
                        );
                    }
                    Log.d(TAG, "Đã hoàn tác xóa task: " + taskToDelete.getName());
                })
                .show();
    }

    // ----- Implement các hàm của Interface -----

    @Override
    public void onTaskCheckChanged(TaskEntry task, boolean isChecked) {
        task.setCompleted(isChecked);
        task.setLastModified(new Date());

        if (isChecked && task.getAlarmRequestCode() != 0) {
            AlarmHelper.cancelAlarm(this, task.getAlarmRequestCode());
            task.setAlarmRequestCode(0);
            task.setReminderTime(null);
            Log.d(TAG, "Task completed, canceling alarm for: " + task.getName());
        }

        mViewModel.updateTask(task);
        Log.d(TAG, "Task checked: " + task.getName() + " - " + isChecked);
    }

    @Override
    public void onTaskClicked(TaskEntry task) {
        Log.d(TAG, "Task clicked: " + task.getName());
        AddEditTaskDialog dialog = AddEditTaskDialog.newInstance(task, task.getTaskType(), task.getProjectId());
        dialog.show(getSupportFragmentManager(), "EditTaskDialog");
    }

    @Override
    public void onProjectClicked(ProjectEntry project) {
        Log.d(TAG, "Project clicked: " + project.getName());
        mCurrentProjectId = project.documentId;
        mCurrentProjectName = project.getName();

        // ✅ SỬA LỖI 2: Báo cho ViewModel biết projectID mới
        mViewModel.setCurrentProjectId(mCurrentProjectId);

        updateToolbarTitle();
    }

    @Override
    public void onProjectEdit(ProjectEntry project) {
        AddEditProjectDialog dialog = AddEditProjectDialog.newInstance(project, project.getProjectId());
        dialog.show(getSupportFragmentManager(), "EditProjectDialog");
    }

    @Override
    public void onProjectDelete(ProjectEntry project) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Xóa thư mục \"" + project.getName() + "\"? Các công việc bên trong sẽ được chuyển ra ngoài màn hình chính.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    mViewModel.deleteProject(project);
                })
                .setNegativeButton("Hủy", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    /**
     * ✅ SỬA LỖI 2 (Logic Thư mục): Xử lý nút Back
     */
    @Override
    public void onBackPressed() {
        if (isFabMenuOpen) {
            closeFabMenu(true);
        } else if (mCurrentProjectId != null) {
            // ---- ĐANG Ở TRONG THƯ MỤC -> QUAY LẠI ROOT ----
            mCurrentProjectId = null;
            mCurrentProjectName = null;

            // ✅ Báo cho ViewModel biết đã quay về root
            mViewModel.setCurrentProjectId(null);

            updateToolbarTitle();
        } else {
            // ---- ĐANG Ở ROOT -> THOÁT ACTIVITY ----
            super.onBackPressed();
        }
    }
}
