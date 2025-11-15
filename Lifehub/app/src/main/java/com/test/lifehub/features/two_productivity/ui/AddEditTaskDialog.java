package com.test.lifehub.features.two_productivity.ui; // (Kiểm tra lại tên package của bạn)

import static androidx.fragment.app.FragmentManager.TAG;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.test.lifehub.R;
import com.test.lifehub.core.services.ReminderService;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

import java.util.Calendar;

/**
 * DialogFragment để Thêm hoặc Sửa một Công việc (Task).
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 */
public class AddEditTaskDialog extends DialogFragment {

    private EditText etTaskTitle;
    private Button btnSaveTask, btnSetReminderTask;
    private TextView tvDialogTitle;
    private ProductivityViewModel mViewModel;

    private TaskEntry mCurrentTask; // Dùng khi ở chế độ Sửa
    private int mTaskType; // Dùng khi ở chế độ Thêm mới
    private long mReminderTime = 0; // Thời gian nhắc nhở (0 = không có)
    private boolean mReminderChanged = false; // Cờ (flag) để biết có cần đặt báo thức mới không

    public AddEditTaskDialog(TaskEntry task, int taskType) {
        this.mCurrentTask = task;
        this.mTaskType = taskType;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_edit_task, container, false);

        etTaskTitle = view.findViewById(R.id.et_task_title);
        btnSaveTask = view.findViewById(R.id.btn_save_task);
        btnSetReminderTask = view.findViewById(R.id.btn_set_reminder_task);
        tvDialogTitle = view.findViewById(R.id.tv_dialog_task_title);

        mViewModel = new ViewModelProvider(requireActivity()).get(ProductivityViewModel.class);

        if (mCurrentTask != null) {
            // Chế độ Sửa
            etTaskTitle.setText(mCurrentTask.title);
            mReminderTime = mCurrentTask.reminderTime;
            tvDialogTitle.setText(mCurrentTask.taskType == Constants.TASK_TYPE_GENERAL ? "Sửa Công việc" : "Sửa Đồ mua sắm");
        } else {
            // Chế độ Thêm mới
            tvDialogTitle.setText(mTaskType == Constants.TASK_TYPE_GENERAL ? "Công việc mới" : "Thêm đồ mua sắm");
        }

        // Cập nhật text của nút Nhắc nhở
        updateReminderButtonText();

        btnSaveTask.setOnClickListener(v -> saveTask());
        btnSetReminderTask.setOnClickListener(v -> pickDateTime());

        return view;
    }

    private void saveTask() {
        String title = etTaskTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(getContext(), "Nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        // KHÔNG CẦN LUỒNG NỀN (Executor)

        if (mCurrentTask == null) {
            // 1. Tạo Task mới
            TaskEntry newTask = new TaskEntry();
            newTask.title = title;
            newTask.isCompleted = false;
            newTask.taskType = mTaskType;
            newTask.reminderTime = mReminderTime;
            // (userOwnerId và lastModified sẽ được Repository xử lý)
            mViewModel.insertTask(newTask);

            // Đặt báo thức nếu có
            if (mReminderTime > 0) {
                // Dùng hashCode của title + time làm ID (đủ duy nhất cho TH này)
                int reminderId = (title + mReminderTime).hashCode();
                setReminder(reminderId, title, mReminderTime);
            }
        } else {
            // 2. Cập nhật Task cũ
            mCurrentTask.title = title;
            mCurrentTask.reminderTime = mReminderTime;
            mViewModel.updateTask(mCurrentTask);

            // Đặt báo thức nếu thời gian thay đổi
            if (mReminderChanged && mReminderTime > 0) {
                int reminderId = mCurrentTask.documentId.hashCode(); // Dùng ID tài liệu
                setReminder(reminderId, title, mReminderTime);
            }
        }
        dismiss(); // Đóng Dialog
    }

    // --- Logic Nhắc nhở (Tương tự AddEditNoteActivity) ---

    private void updateReminderButtonText() {
        if (mReminderTime > 0) {
            // (Tùy chọn: Bạn có thể định dạng (format) thời gian này cho dễ đọc)
            btnSetReminderTask.setText("Nhắc lúc: " + mReminderTime);
        } else {
            btnSetReminderTask.setText("Thêm nhắc nhở");
        }
    }

    private final Calendar mReminderCalendar = Calendar.getInstance();

    private void pickDateTime() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year1, monthOfYear, dayOfMonth) -> {
                    mReminderCalendar.set(Calendar.YEAR, year1);
                    mReminderCalendar.set(Calendar.MONTH, monthOfYear);
                    mReminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    pickTime();
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void pickTime() {
        int hour = mReminderCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = mReminderCalendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> {
                    mReminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    mReminderCalendar.set(Calendar.MINUTE, minute1);
                    mReminderCalendar.set(Calendar.SECOND, 0);

                    long selectedTime = mReminderCalendar.getTimeInMillis();

                    if (selectedTime <= System.currentTimeMillis()) {
                        Toast.makeText(getContext(), "Vui lòng chọn thời gian trong tương lai", Toast.LENGTH_SHORT).show();
                        mReminderTime = 0;
                    } else {
                        mReminderTime = selectedTime;
                        Toast.makeText(getContext(), "Đã đặt nhắc nhở (chưa lưu)", Toast.LENGTH_SHORT).show();
                    }
                    mReminderChanged = true; // Đánh dấu là thời gian đã thay đổi
                    updateReminderButtonText();

                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void setReminder(int reminderId, String title, long reminderTimeInMillis) {
        String content = "Bạn có nhắc nhở cho công việc: " + title;

        Intent intent = new Intent(getContext(), ReminderService.class);
        intent.putExtra(Constants.EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(Constants.EXTRA_REMINDER_TITLE, "Nhắc nhở Công việc");
        intent.putExtra(Constants.EXTRA_REMINDER_CONTENT, content);

        PendingIntent pendingIntent = PendingIntent.getService(getContext(),
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(getContext(), "Cần cấp quyền Đặt báo thức chính xác", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, pendingIntent);
            Log.d(TAG, "Đã đặt báo thức thành công cho ID: " + reminderId);

        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Không thể đặt báo thức. Vui lòng kiểm tra quyền.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}