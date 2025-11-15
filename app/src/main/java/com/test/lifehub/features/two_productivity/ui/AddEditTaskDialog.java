package com.test.lifehub.features.two_productivity.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

import java.util.Date;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Dialog (Hộp thoại) để Thêm/Sửa một Công việc (Task)
 * (Phiên bản đã sửa lỗi 'container' và 'Date/long')
 */
@AndroidEntryPoint
public class AddEditTaskDialog extends DialogFragment {

    // --- Views ---
    private TextInputEditText mEtTaskName;
    private TextView mTvTitle;

    // --- Logic ---
    private ProductivityViewModel mViewModel;
    private final TaskEntry mCurrentTask;
    private final int mTaskType;

    /**
     * Constructor cho Thêm/Sửa
     * @param task Task để sửa (null nếu Thêm mới)
     * @param taskType Loại Task (General / Shopping)
     */
    public AddEditTaskDialog(TaskEntry task, int taskType) {
        this.mCurrentTask = task;
        this.mTaskType = taskType;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // --- 1. Inflate Layout ---
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // SỬA LỖI: (Cannot resolve symbol 'container')
        // Đổi 'container' thành 'null' vì đây là Dialog.
        View view = inflater.inflate(R.layout.dialog_add_edit_task, null);

        // --- 2. Ánh xạ Views ---
        mEtTaskName = view.findViewById(R.id.et_task_name);
        mTvTitle = view.findViewById(R.id.tv_dialog_task_title);

        // --- 3. Khởi tạo ViewModel ---
        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        // --- 4. Cấu hình Dialog ---
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", (dialog, which) -> dismiss());

        // --- 5. Kiểm tra Thêm mới hay Sửa ---
        if (mCurrentTask != null) {
            // ----- CHẾ ĐỘ SỬA -----
            mTvTitle.setText("Sửa Công việc");
            mEtTaskName.setText(mCurrentTask.getName());
        } else {
            // ----- CHẾ ĐỘ THÊM MỚI -----
            if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
                mTvTitle.setText("Thêm Đồ Mua sắm");
            } else {
                mTvTitle.setText("Thêm Công việc Mới");
            }
        }

        // --- 6. Tạo Dialog ---
        AlertDialog dialog = builder.create();

        // (Trick: Override nút Positive để validation)
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                saveTask();
            });
        });

        return dialog;
    }

    /**
     * Logic chính: Lưu Công việc
     */
    private void saveTask() {
        String taskName = mEtTaskName.getText().toString().trim();

        if (TextUtils.isEmpty(taskName)) {
            Toast.makeText(getContext(), "Nội dung không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCurrentTask == null) {
            // ----- THÊM MỚI -----
            TaskEntry newTask = new TaskEntry(
                    taskName,
                    new Date(), // Đã sửa (xóa .getTime())
                    false,
                    mTaskType
            );
            mViewModel.insertTask(newTask);
            Toast.makeText(getContext(), "Đã thêm", Toast.LENGTH_SHORT).show();

        } else {
            // ----- CẬP NHẬT (SỬA) -----
            mCurrentTask.setName(taskName);
            mCurrentTask.setLastModified(new Date()); // Đã sửa (xóa .getTime())
            mViewModel.updateTask(mCurrentTask);
            Toast.makeText(getContext(), "Đã cập nhật", Toast.LENGTH_SHORT).show();
        }

        dismiss(); // Đóng Dialog
    }
}