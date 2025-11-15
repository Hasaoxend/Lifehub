package com.test.lifehub.features.two_productivity.ui;

import android.content.Context;
import android.graphics.Paint;
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

/**
 * Adapter cho RecyclerView của Công việc (Tasks).
 * (Đã VIẾT LẠI HOÀN TOÀN để xóa ViewModel và dùng Interface Listener)
 */
public class TaskAdapter extends ListAdapter<TaskEntry, TaskAdapter.TaskViewHolder> {

    /**
     * Interface (giao diện) để Adapter "nói chuyện" ngược lại với Activity.
     */
    public interface OnTaskInteractionListener {
        void onTaskCheckChanged(TaskEntry task, boolean isChecked);
        void onTaskClicked(TaskEntry task);
    }

    private final OnTaskInteractionListener mListener;

    /**
     * SỬA LỖI: Constructor bây giờ nhận một Listener,
     * KHÔNG còn tự ý tạo ViewModel.
     */
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

        // Áp dụng gạch ngang nếu đã hoàn thành
        updateStrikeThrough(holder.mTaskName, currentTask.isCompleted());

        // --- Xử lý sự kiện ---

        // 1. Khi nhấn vào Checkbox
        holder.mCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Chỉ gọi update nếu trạng thái thực sự thay đổi
            if (buttonView.isPressed() && mListener != null) {
                // Gạch ngang ngay lập tức
                updateStrikeThrough(holder.mTaskName, isChecked);
                // Báo cho Activity biết
                mListener.onTaskCheckChanged(currentTask, isChecked);
            }
        });

        // 2. Khi nhấn vào item (để Sửa)
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                // Báo cho Activity biết
                mListener.onTaskClicked(currentTask);
            }
        });
    }

    /**
     * Helper: Thêm/xóa gạch ngang chữ
     */
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

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckbox = itemView.findViewById(R.id.item_task_checkbox);
            mTaskName = itemView.findViewById(R.id.item_task_name);
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<TaskEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskEntry oldItem, @NonNull TaskEntry newItem) {
            // SỬA LỖI: Thêm kiểm tra null
            if (oldItem.documentId == null || newItem.documentId == null) {
                return false;
            }
            return oldItem.documentId.equals(newItem.documentId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskEntry oldItem, @NonNull TaskEntry newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.isCompleted() == newItem.isCompleted();
        }
    };
}