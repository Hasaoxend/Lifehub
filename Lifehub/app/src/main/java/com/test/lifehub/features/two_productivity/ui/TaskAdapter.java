package com.test.lifehub.features.two_productivity.ui;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.TaskEntry;

/**
 * Adapter cho RecyclerView hiển thị danh sách Công việc (Tasks).
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 */
public class TaskAdapter extends ListAdapter<TaskEntry, TaskAdapter.TaskViewHolder> {

    private final ProductivityViewModel mViewModel;
    private final FragmentManager mFragmentManager;

    public TaskAdapter(Context context) {
        super(DIFF_CALLBACK);

        // (Như file bạn đã cung cấp, nó tự lấy ViewModel)
        FragmentActivity activity = (FragmentActivity) context;
        this.mViewModel = new ViewModelProvider(activity).get(ProductivityViewModel.class);
        this.mFragmentManager = activity.getSupportFragmentManager();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        final TaskEntry task = getItem(position);

        holder.tvTaskTitle.setText(task.title);
        holder.cbTaskCompleted.setChecked(task.isCompleted);
        updateTaskStrikeThrough(holder.tvTaskTitle, task.isCompleted);

        // Hiển thị/Ẩn icon nhắc nhở
        if (task.reminderTime > 0) {
            holder.ivTaskReminder.setVisibility(View.VISIBLE);
        } else {
            holder.ivTaskReminder.setVisibility(View.GONE);
        }

        // Xử lý khi nhấn checkbox
        holder.cbTaskCompleted.setOnClickListener(v -> {
            task.isCompleted = holder.cbTaskCompleted.isChecked();
            updateTaskStrikeThrough(holder.tvTaskTitle, task.isCompleted);
            // SỬA LỖI: Gọi hàm update (phiên bản Firestore)
            mViewModel.updateTask(task);
        });

        // Xử lý khi nhấn vào item để Sửa
        holder.itemView.setOnClickListener(v -> {
            // SỬA LỖI: Gọi constructor (phiên bản Firestore)
            AddEditTaskDialog dialog = new AddEditTaskDialog(task, task.taskType);
            dialog.show(mFragmentManager, "EditTaskDialog");
        });
    }

    // Hàm gạch ngang chữ khi công việc hoàn thành
    private void updateTaskStrikeThrough(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTaskCompleted;
        TextView tvTaskTitle;
        ImageView ivTaskReminder;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTaskCompleted = itemView.findViewById(R.id.cb_task_completed);
            tvTaskTitle = itemView.findViewById(R.id.tv_task_title);
            ivTaskReminder = itemView.findViewById(R.id.iv_task_reminder);
        }
    }

    // SỬA LỖI: Cập nhật DiffUtil để so sánh String ID
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
            return oldItem.title.equals(newItem.title) &&
                    oldItem.isCompleted == newItem.isCompleted &&
                    oldItem.reminderTime == newItem.reminderTime;
        }
    };
}