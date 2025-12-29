package com.test.lifehub.features.two_productivity.ui;

import android.graphics.Paint;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;
import com.test.lifehub.features.two_productivity.data.TaskEntry;
import com.test.lifehub.features.two_productivity.data.TaskListItem;

/**
 * Adapter MỚI: Hỗ trợ hiển thị cả Project (Thư mục) và Task (Công việc).
 */
public class TaskListAdapter extends ListAdapter<TaskListItem, RecyclerView.ViewHolder> {

    // Interface để "nói chuyện" ngược lại với Activity.
    public interface OnItemInteractionListener {
        void onTaskCheckChanged(TaskEntry task, boolean isChecked);
        void onTaskClicked(TaskEntry task);
        void onProjectClicked(ProjectEntry project);
        void onProjectEdit(ProjectEntry project);
        void onProjectDelete(ProjectEntry project);
    }

    private final OnItemInteractionListener mListener;

    public TaskListAdapter(@NonNull OnItemInteractionListener listener) {
        super(DIFF_CALLBACK);
        this.mListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        TaskListItem item = getItem(position);
        if (item != null) {
            return item.type;
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TaskListItem.TYPE_PROJECT) {
            View view = inflater.inflate(R.layout.item_project, parent, false);
            return new ProjectViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskListItem item = getItem(position);
        if (item == null) return;

        if (item.type == TaskListItem.TYPE_PROJECT) {
            ((ProjectViewHolder) holder).bind(item.project, mListener);
        } else {
            ((TaskViewHolder) holder).bind(item.task, mListener);
        }
    }

    /**
     * Lấy Item tại một vị trí (dùng cho Swipe-to-Delete)
     */
    public TaskListItem getItemAt(int position) {
        return getItem(position);
    }

    // --- ViewHolder cho Task ---
    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox mCheckbox;
        private final TextView mTaskName;
        private final TextView mTvReminder;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckbox = itemView.findViewById(R.id.item_task_checkbox);
            mTaskName = itemView.findViewById(R.id.item_task_name);
            mTvReminder = itemView.findViewById(R.id.item_task_reminder);
        }

        public void bind(TaskEntry task, OnItemInteractionListener listener) {
            mTaskName.setText(task.getName());

            mCheckbox.setOnCheckedChangeListener(null);
            mCheckbox.setChecked(task.isCompleted());

            updateStrikeThrough(mTaskName, task.isCompleted());

            if (task.getReminderTime() != null && !task.isCompleted()) {
                String reminderText = "Nhắc lúc: " + DateFormat.format("HH:mm, dd/MM", task.getReminderTime());
                mTvReminder.setText(reminderText);
                mTvReminder.setVisibility(View.VISIBLE);
            } else {
                mTvReminder.setVisibility(View.GONE);
            }

            mCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed() && listener != null) {
                    updateStrikeThrough(mTaskName, isChecked);
                    listener.onTaskCheckChanged(task, isChecked);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClicked(task);
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
    }

    // --- ViewHolder cho Project ---
    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView mProjectName;
        private final ImageView mProjectIcon;
        private final ImageButton mProjectMenu;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            mProjectName = itemView.findViewById(R.id.item_project_name);
            mProjectIcon = itemView.findViewById(R.id.item_project_icon);
            mProjectMenu = itemView.findViewById(R.id.item_project_menu);
        }

        public void bind(ProjectEntry project, OnItemInteractionListener listener) {
            mProjectName.setText(project.getName());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClicked(project);
                }
            });

            // Xử lý menu 3 chấm
            mProjectMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.project_item_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_project_edit) {
                        listener.onProjectEdit(project);
                        return true;
                    } else if (id == R.id.action_project_delete) {
                        listener.onProjectDelete(project);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<TaskListItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<TaskListItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull TaskListItem oldItem, @NonNull TaskListItem newItem) {
            if (oldItem.type != newItem.type) return false;

            String oldId = (oldItem.type == TaskListItem.TYPE_PROJECT) ? oldItem.project.documentId : oldItem.task.documentId;
            String newId = (newItem.type == TaskListItem.TYPE_PROJECT) ? newItem.project.documentId : newItem.task.documentId;

            if (oldId == null || newId == null) {
                return false;
            }
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull TaskListItem oldItem, @NonNull TaskListItem newItem) {
            if (oldItem.type == TaskListItem.TYPE_PROJECT) {
                return oldItem.project.getName().equals(newItem.project.getName());
            } else {
                return oldItem.task.getName().equals(newItem.task.getName())
                        && oldItem.task.isCompleted() == newItem.task.isCompleted()
                        && java.util.Objects.equals(oldItem.task.getReminderTime(), newItem.task.getReminderTime());
            }
        }
    };
}