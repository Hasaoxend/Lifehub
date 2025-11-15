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

@AndroidEntryPoint
public class AddEditTaskDialog extends DialogFragment {

    private TextInputEditText mEtTaskName;
    private TextView mTvTitle;

    private ProductivityViewModel mViewModel;
    private final TaskEntry mCurrentTask;
    private final int mTaskType;

    // (Chúng ta sẽ thêm logic cho Nút Đặt giờ ở bước sau)

    public AddEditTaskDialog(TaskEntry task, int taskType) {
        this.mCurrentTask = task;
        this.mTaskType = taskType;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_edit_task, null);

        mEtTaskName = view.findViewById(R.id.et_task_name);
        mTvTitle = view.findViewById(R.id.tv_dialog_task_title);
        // (Ánh xạ nút Đặt giờ ở đây)

        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", (dialog, which) -> dismiss());

        if (mCurrentTask != null) {
            mTvTitle.setText("Sửa Công việc");
            mEtTaskName.setText(mCurrentTask.getName());
            // (Cập nhật UI Đặt giờ ở đây)
        } else {
            if (mTaskType == Constants.TASK_TYPE_SHOPPING) {
                mTvTitle.setText("Thêm Đồ Mua sắm");
            } else {
                mTvTitle.setText("Thêm Công việc Mới");
            }
        }

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                saveTask();
            });
        });

        return dialog;
    }

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
                    new Date(), // SỬA LỖI: Dùng new Date()
                    false,
                    mTaskType
            );
            // (Lưu reminderTime ở đây)
            mViewModel.insertTask(newTask);
            Toast.makeText(getContext(), "Đã thêm", Toast.LENGTH_SHORT).show();

        } else {
            // ----- CẬP NHẬT (SỬA) -----
            mCurrentTask.setName(taskName);
            mCurrentTask.setLastModified(new Date()); // SỬA LỖI
            // (Lưu reminderTime ở đây)
            mViewModel.updateTask(mCurrentTask);
            Toast.makeText(getContext(), "Đã cập nhật", Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }
}