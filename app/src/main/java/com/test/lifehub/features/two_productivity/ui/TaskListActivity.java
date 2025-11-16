package com.test.lifehub.features.two_productivity.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback; // <-- Thư viện quan trọng
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private FloatingActionButton mFab;
    private Toolbar mToolbar;
    private SearchView mSearchView;
    private TextView mEmptyView;
    private CoordinatorLayout mCoordinatorLayout;

    private int mTaskType;
    private String mCurrentProjectId = null;
    private String mCurrentProjectName = null;

    private List<TaskListItem> mAllItems = new ArrayList<>();
    private BottomSheetDialog mBottomSheetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list_drawer);

        mTaskType = getIntent().getIntExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);
        mCurrentProjectId = getIntent().getStringExtra(EXTRA_PROJECT_ID);
        mCurrentProjectName = getIntent().getStringExtra(EXTRA_PROJECT_NAME);

        Log.d(TAG, "TaskListActivity created with type: " + mTaskType + " | ProjectId: " + mCurrentProjectId);

        mToolbar = findViewById(R.id.toolbar_task_list);
        mRecyclerView = findViewById(R.id.recycler_view_task_list);
        mSearchView = findViewById(R.id.search_view_tasks);
        mEmptyView = findViewById(R.id.empty_view_tasks);
        mCoordinatorLayout = findViewById(R.id.task_list_coordinator_layout);
        mFab = findViewById(R.id.fab_main);

        setSupportActionBar(mToolbar);
        updateToolbarTitle();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed()); // Cập nhật luôn ở đây cho nhất quán

        mAdapter = new TaskListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupSwipeToDelete();
        setupFabDrawer();
        setupSearchView();

        // Auto-dismiss drawer khi scroll
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    dismissBottomSheet();
                }
            }
        });

        // ==================================================================
        // BẮT ĐẦU SỬA LỖI onBackpressed
        // 1. Tạo một OnBackPressedCallback
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Đây là logic cũ từ hàm onBackPressed() của bạn
                if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
                    dismissBottomSheet();
                } else if (mCurrentProjectId != null) {
                    mCurrentProjectId = null;
                    mCurrentProjectName = null;
                    mViewModel.setCurrentProjectId(null);
                    updateToolbarTitle();
                } else {
                    // Nếu không rơi vào 2 trường hợp trên, hãy vô hiệu hóa callback này
                    // và gọi lại onBackPressed() để thực hiện hành vi mặc định (thoát Activity)
                    setEnabled(false);

                    // SỬA LỖI: Dùng getOnBackPressedDispatcher() cho Java
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };

        // 2. Thêm callback vào OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
        // KẾT THÚC SỬA LỖI
        // ==================================================================


        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);
        mViewModel.setCurrentProjectId(mCurrentProjectId);

        observeData();
    }

    private void setupFabDrawer() {
        mFab.setOnClickListener(v -> showBottomSheetMenu());
    }

    private void showBottomSheetMenu() {
        if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
            return;
        }

        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_fab_menu, null);
        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setContentView(bottomSheetView);

        View menuAddProject = bottomSheetView.findViewById(R.id.menu_add_project);
        View menuAddTask = bottomSheetView.findViewById(R.id.menu_add_task);

        // Ẩn menu Project nếu là Shopping
        if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
            menuAddProject.setVisibility(View.GONE);
        } else {
            menuAddProject.setOnClickListener(v -> {
                dismissBottomSheet();
                AddEditProjectDialog dialog = AddEditProjectDialog.newInstance(null, mCurrentProjectId);
                dialog.show(getSupportFragmentManager(), "AddProjectDialog");
            });
        }

        menuAddTask.setOnClickListener(v -> {
            dismissBottomSheet();
            AddEditTaskDialog dialog = AddEditTaskDialog.newInstance(null, mTaskType, mCurrentProjectId);
            dialog.show(getSupportFragmentManager(), "AddTaskDialog");
        });

        mBottomSheetDialog.show();
    }

    private void dismissBottomSheet() {
        if (mBottomSheetDialog != null && mBottomSheetDialog.isShowing()) {
            mBottomSheetDialog.dismiss();
        }
    }

    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                dismissBottomSheet();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                dismissBottomSheet();
                filterItems(newText);
                return true;
            }
        });

        mSearchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                dismissBottomSheet();
            }
        });
    }

    private void filterItems(String query) {
        List<TaskListItem> filteredList = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filteredList = mAllItems;
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (TaskListItem item : mAllItems) {
                if (item.type == TaskListItem.TYPE_PROJECT) {
                    if (item.project.getName().toLowerCase().contains(lowerCaseQuery)) {
                        filteredList.add(item);
                    }
                } else {
                    if (item.task.getName().toLowerCase().contains(lowerCaseQuery)) {
                        filteredList.add(item);
                    }
                }
            }
        }

        mAdapter.submitList(filteredList);
        updateEmptyView(filteredList);
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
            mViewModel.getAllShoppingItems().observe(this, tasks -> {
                List<TaskListItem> items = new ArrayList<>();
                if (tasks != null) {
                    Collections.sort(tasks, Comparator.comparing(TaskEntry::isCompleted)
                            .thenComparing(TaskEntry::getLastModified, Comparator.reverseOrder()));
                    for (TaskEntry task : tasks) {
                        items.add(new TaskListItem(task));
                    }
                }
                mAllItems = items;
                filterItems(mSearchView.getQuery().toString());
            });
        } else {
            mViewModel.getProjectsInRoot().observe(this, projects -> {
                if (mCurrentProjectId == null) {
                    List<TaskEntry> tasks = mViewModel.getTasksInRoot().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAllItems = items;
                    filterItems(mSearchView.getQuery().toString());
                }
            });
            mViewModel.getTasksInRoot().observe(this, tasks -> {
                if (mCurrentProjectId == null) {
                    List<ProjectEntry> projects = mViewModel.getProjectsInRoot().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAllItems = items;
                    filterItems(mSearchView.getQuery().toString());
                }
            });

            mViewModel.getProjectsInProject().observe(this, projects -> {
                if (mCurrentProjectId != null) {
                    List<TaskEntry> tasks = mViewModel.getTasksInProject().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAllItems = items;
                    filterItems(mSearchView.getQuery().toString());
                }
            });
            mViewModel.getTasksInProject().observe(this, tasks -> {
                if (mCurrentProjectId != null) {
                    List<ProjectEntry> projects = mViewModel.getProjectsInProject().getValue();
                    List<TaskListItem> items = convertToListItems(projects, tasks);
                    mAllItems = items;
                    filterItems(mSearchView.getQuery().toString());
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
                dismissBottomSheet();
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
        }

        mViewModel.deleteTask(taskToDelete);

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
                })
                .show();
    }

    @Override
    public void onTaskCheckChanged(TaskEntry task, boolean isChecked) {
        dismissBottomSheet();
        task.setCompleted(isChecked);
        task.setLastModified(new Date());

        if (isChecked && task.getAlarmRequestCode() != 0) {
            AlarmHelper.cancelAlarm(this, task.getAlarmRequestCode());
            task.setAlarmRequestCode(0);
            task.setReminderTime(null);
        }

        mViewModel.updateTask(task);
    }

    @Override
    public void onTaskClicked(TaskEntry task) {
        dismissBottomSheet();
        AddEditTaskDialog dialog = AddEditTaskDialog.newInstance(task, task.getTaskType(), task.getProjectId());
        dialog.show(getSupportFragmentManager(), "EditTaskDialog");
    }

    @Override
    public void onProjectClicked(ProjectEntry project) {
        dismissBottomSheet();
        mCurrentProjectId = project.documentId;
        mCurrentProjectName = project.getName();
        mViewModel.setCurrentProjectId(mCurrentProjectId);
        updateToolbarTitle();
    }

    @Override
    public void onProjectEdit(ProjectEntry project) {
        dismissBottomSheet();
        AddEditProjectDialog dialog = AddEditProjectDialog.newInstance(project, project.getProjectId());
        dialog.show(getSupportFragmentManager(), "EditProjectDialog");
    }

    @Override
    public void onProjectDelete(ProjectEntry project) {
        dismissBottomSheet();
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

    //
    // PHƯƠNG THỨC onBackPressed() CŨ ĐÃ BỊ XÓA VÀ THAY BẰNG CALLBACK TRONG onCreate()
    //

    @Override
    protected void onPause() {
        super.onPause();
        dismissBottomSheet();
    }
}