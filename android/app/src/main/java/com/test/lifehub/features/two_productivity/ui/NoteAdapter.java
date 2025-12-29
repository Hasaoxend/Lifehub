package com.test.lifehub.features.two_productivity.ui;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.NoteEntry;

/**
 * Adapter cho RecyclerView của Ghi chú (Notes).
 * (Đã sửa lỗi hiển thị Date)
 */
public class NoteAdapter extends ListAdapter<NoteEntry, NoteAdapter.NoteViewHolder> {

    private final Context mContext;
    private final ActivityResultLauncher<Intent> noteActivityResultLauncher;

    public NoteAdapter(Context context, ActivityResultLauncher<Intent> launcher) {
        super(DIFF_CALLBACK);
        this.mContext = context;
        this.noteActivityResultLauncher = launcher;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntry currentNote = getItem(position);
        if (currentNote == null) {
            return;
        }

        holder.mTvTitle.setText(currentNote.getTitle());
        holder.mTvContent.setText(currentNote.getContent());

        // SỬA LỖI: Chuyển Date sang long trước khi hiển thị
        if (currentNote.getLastModified() != null) {
            String timeAgo = DateUtils.getRelativeTimeSpanString(
                    currentNote.getLastModified().getTime(), // <-- SỬA LỖI .getTime()
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS).toString();
            holder.mTvTimestamp.setText(timeAgo);
        } else {
            holder.mTvTimestamp.setText(""); // Xử lý trường hợp null
        }

        // Xử lý sự kiện nhấp vào item -> Mở AddEditNoteActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, AddEditNoteActivity.class);
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, currentNote.documentId);
            noteActivityResultLauncher.launch(intent);
        });
    }

    // --- ViewHolder ---
    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTvTitle;
        private final TextView mTvContent;
        private final TextView mTvTimestamp;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            mTvTitle = itemView.findViewById(R.id.item_note_title);
            mTvContent = itemView.findViewById(R.id.item_note_content);
            mTvTimestamp = itemView.findViewById(R.id.item_note_timestamp);
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<NoteEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<NoteEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull NoteEntry oldItem, @NonNull NoteEntry newItem) {
            if (oldItem.documentId == null || newItem.documentId == null) {
                return false;
            }
            return oldItem.documentId.equals(newItem.documentId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull NoteEntry oldItem, @NonNull NoteEntry newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getContent().equals(newItem.getContent());
        }
    };
}