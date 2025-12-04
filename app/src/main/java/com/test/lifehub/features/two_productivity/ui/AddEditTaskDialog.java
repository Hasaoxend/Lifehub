package com.test.lifehub.features.two_productivity.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.core.util.AlarmHelper;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditTaskDialog extends DialogFragment {

    private static final String TAG = "AddEditTaskDialog";

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_TYPE = "task_type";
    private static final String ARG_PROJECT_ID = "project_id"; // ✅ THÊM

    private TextInputEditText mEtTaskName;
    private TextView mTvTitle;
    private ImageButton mBtnSetReminder, mBtnClearReminder;
    private TextView mTvReminderTime;

    private ProductivityViewModel mViewModel;
    private TaskEntry mCurrentTask;
    private int mTaskType;
    private String mProjectId; // ✅ THÊM: ID thư mục

    private Date mReminderDate = null;
    private final Calendar mCalendar = Calendar.getInstance();

    /**
     * ✅ CẬP NHẬT: Thêm projectId
     */
    public static AddEditTaskDialog newInstance(@Nullable TaskEntry task, int taskType, @Nullable String projectId) {
        AddEditTaskDialog dialog = new AddEditTaskDialog();
        Bundle args = new Bundle();
        if (task != null && task.documentId != null) {
            args.putString(ARG_TASK_ID, task.documentId);
        }
        if (projectId != null) {
            args.putString(ARG_PROJECT_ID, projectId);
        }
        args.putInt(ARG_TASK_TYPE, taskType);
        dialog.setArguments(args);
        return dialog;
    }

    public AddEditTaskDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_edit_task, null);

        mEtTaskName = view.findViewById(R.id.et_task_name);
        mTvTitle = view.findViewById(R.id.tv_dialog_task_title);
        mBtnSetReminder = view.findViewById(R.id.btn_set_reminder_task);
        mTvReminderTime = view.findViewById(R.id.tv_reminder_time_task);
        mBtnClearReminder = view.findViewById(R.id.btn_clear_reminder_task);

        mViewModel = new ViewModelProvider(requireActivity()).get(ProductivityViewModel.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dismiss());

        if (getArguments() != null) {
            mTaskType = getArguments().getInt(ARG_TASK_TYPE, Constants.TASK_TYPE_GENERAL);
            String taskId = getArguments().getString(ARG_TASK_ID);
            mProjectId = getArguments().getString(ARG_PROJECT_ID); // ✅ THÊM

            Log.d(TAG, "Dialog created - TaskType: " + mTaskType + ", TaskId: " + taskId + ", ProjectId: " + mProjectId);

            if (taskId != null) {
                mTvTitle.setText(R.string.task_edit);

                LiveData<List<TaskEntry>> tasksLiveData;
                if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
                    tasksLiveData = mViewModel.getAllShoppingItems();
                } else {
                    tasksLiveData = mViewModel.getAllTasks(); // Lấy tất cả task
                }

                tasksLiveData.observe(this, tasks -> {
                    if (mCurrentTask != null) return;
                    if (tasks != null) {
                        for (TaskEntry task : tasks) {
                            if (task.documentId != null && task.documentId.equals(taskId)) {
                                mCurrentTask = task;
                                populateUi(task);
                                Log.d(TAG, "Found task to edit: " + task.getName());
                                break;
                            }
                        }
                    }
                });
            } else {
                if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
                    mTvTitle.setText(R.string.task_add_shopping);
                } else {
                    mTvTitle.setText(R.string.task_add_new);
                }
                updateReminderUi();
            }
        }

        mBtnSetReminder.setOnClickListener(v -> showDateTimePicker());
        mBtnClearReminder.setOnClickListener(v -> {
            mReminderDate = null;
            updateReminderUi();
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> saveTask());
        });

        return dialog;
    }

    private void populateUi(TaskEntry task) {
        if (task == null) return;
        mEtTaskName.setText(task.getName());
        mReminderDate = task.getReminderTime();
        if (mReminderDate != null) {
            mCalendar.setTime(mReminderDate);
        }
        updateReminderUi();
    }

    private void updateReminderUi() {
        if (mReminderDate != null) {
            mTvReminderTime.setText("Nhắc lúc: " + DateFormat.format("HH:mm, dd/MM", mReminderDate));
            mBtnClearReminder.setVisibility(View.VISIBLE);
        } else {
            mTvReminderTime.setText("");
            mBtnClearReminder.setVisibility(View.GONE);
        }
    }

    private void showDateTimePicker() {
        if (mReminderDate != null) {
            mCalendar.setTime(mReminderDate);
        } else {
            mCalendar.setTime(new Date());
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    mCalendar.set(Calendar.YEAR, year);
                    mCalendar.set(Calendar.MONTH, month);
                    mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (view1, hourOfDay, minute) -> {
                                mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                mCalendar.set(Calendar.MINUTE, minute);
                                mCalendar.set(Calendar.SECOND, 0);
                                if (mCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                                    Toast.makeText(getContext(), R.string.task_select_future_time, Toast.LENGTH_SHORT).show();
                                    mReminderDate = null;
                                } else {
                                    mReminderDate = mCalendar.getTime();
                                }
                                updateReminderUi();
                            },
                            mCalendar.get(Calendar.HOUR_OF_DAY),
                            mCalendar.get(Calendar.MINUTE),
                            DateFormat.is24HourFormat(requireContext())
                    );
                    timePickerDialog.show();
                },
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void saveTask() {
        String taskName = mEtTaskName.getText().toString().trim();

        if (TextUtils.isEmpty(taskName)) {
            Toast.makeText(getContext(), R.string.task_content_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String title = (mTaskType == Constants.TASK_TYPE_SHOPPING) ? "Nhắc nhở mua sắm" : "Nhắc nhở công việc";

        if (mCurrentTask == null) {
            // ----- THÊM MỚI -----
            Log.d(TAG, "Creating new task: " + taskName);
            TaskEntry newTask = new TaskEntry(taskName, new Date(), false, mTaskType);
            newTask.setReminderTime(mReminderDate);
            newTask.setProjectId(mProjectId); // ✅ GÁN PROJECT ID

            if (mReminderDate != null) {
                int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                newTask.setAlarmRequestCode(requestCode);
                AlarmHelper.scheduleAlarm(requireContext(), requestCode, mReminderDate, title, taskName);
            }

            mViewModel.insertTask(newTask);
            Toast.makeText(getContext(), R.string.task_added, Toast.LENGTH_SHORT).show();

        } else {
            // ----- CẬP NHẬT -----
            Log.d(TAG, "Updating task: " + taskName);

            if (mCurrentTask.getAlarmRequestCode() != 0) {
                AlarmHelper.cancelAlarm(requireContext(), mCurrentTask.getAlarmRequestCode());
            }

            mCurrentTask.setName(taskName);
            mCurrentTask.setLastModified(new Date());
            mCurrentTask.setReminderTime(mReminderDate);
            // (Không đổi projectId khi sửa)

            if (mReminderDate != null) {
                int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                mCurrentTask.setAlarmRequestCode(requestCode);
                AlarmHelper.scheduleAlarm(requireContext(), requestCode, mReminderDate, title, taskName);
            } else {
                mCurrentTask.setAlarmRequestCode(0);
            }

            mViewModel.updateTask(mCurrentTask);
            Toast.makeText(getContext(), R.string.task_updated, Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }
}