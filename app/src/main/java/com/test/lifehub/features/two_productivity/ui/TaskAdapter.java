package com.test.lifehub.features.two_productivity.ui;

import android.graphics.Paint;
import android.text.format.DateFormat; // Thêm import
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

public class TaskAdapter extends ListAdapter<TaskEntry, TaskAdapter.TaskViewHolder> {

    public interface OnTaskInteractionListener {
        void onTaskCheckChanged(TaskEntry task, boolean isChecked);
        void onTaskClicked(TaskEntry task);
    }

    private final OnTaskInteractionListener mListener;

    public TaskAdapter(OnTaskInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.mListener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskEntry currentTask = getItem(position);
        if (currentTask == null) {
            return;
        }

        holder.mTaskName.setText(currentTask.getName());
        holder.mCheckbox.setChecked(currentTask.isCompleted());
        updateStrikeThrough(holder.mTaskName, currentTask.isCompleted());

        // SỬA LỖI TÍNH NĂNG (Vấn đề 2): Hiển thị thời gian Đặt giờ
        if (currentTask.getReminderTime() != null) {
            String reminderText = "Nhắc lúc: " + DateFormat.format("HH:mm, dd/MM", currentTask.getReminderTime());
            holder.mTvReminder.setText(reminderText);
            holder.mTvReminder.setVisibility(View.VISIBLE);
        } else {
            holder.mTvReminder.setVisibility(View.GONE);
        }

        // --- Xử lý sự kiện ---
        holder.mCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed() && mListener != null) {
                updateStrikeThrough(holder.mTaskName, isChecked);
                mListener.onTaskCheckChanged(currentTask, isChecked);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onTaskClicked(currentTask);
            }
        });
    }

    private void updateStrikeThrough(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    // --- ViewHolder ---
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox mCheckbox;
        private final TextView mTaskName;
        private final TextView mTvReminder; // <-- THÊM VIEW

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckbox = itemView.findViewById(R.id.item_task_checkbox);
            mTaskName = itemView.findViewById(R.id.item_task_name);
            mTvReminder = itemView.findViewById(R.id.item_task_reminder); // <-- ÁNH XẠ VIEW
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<TaskEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskEntry oldItem, @NonNull TaskEntry newItem) {
            if (oldItem.documentId == null || newItem.documentId == null) {
                return false;
            }
            return oldItem.documentId.equals(newItem.documentId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskEntry oldItem, @NonNull TaskEntry newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.isCompleted() == newItem.isCompleted()
                    // Thêm kiểm tra reminderTime
                    && (oldItem.getReminderTime() == newItem.getReminderTime() ||
                    oldItem.getReminderTime() != null && oldItem.getReminderTime().equals(newItem.getReminderTime()));
        }
    };
}