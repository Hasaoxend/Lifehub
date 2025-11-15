package com.test.lifehub.features.two_productivity.ui;

import android.content.Context;
import android.content.Intent;
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

import java.util.Objects; // <-- Thêm Import

/**
 * Adapter cho RecyclerView hiển thị danh sách Ghi chú (Notes).
 * (Phiên bản đã sửa lỗi "content" VÀ "EXTRA_NOTE_ID")
 */
public class NoteAdapter extends ListAdapter<NoteEntry, NoteAdapter.NoteViewHolder> {

    private final Context mContext;
    private final ActivityResultLauncher<Intent> mLauncher;

    public NoteAdapter(Context context, ActivityResultLauncher<Intent> launcher) {
        super(DIFF_CALLBACK);
        this.mContext = context;
        this.mLauncher = launcher;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntry note = getItem(position);
        holder.tvNoteTitle.setText(note.title);

        if (note.content != null && !note.content.isEmpty()) {
            holder.tvNotePreview.setText(note.content);
        } else {
            holder.tvNotePreview.setText("(Không có nội dung)");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, AddEditNoteActivity.class);
            // SỬA LỖI: Dòng này giờ sẽ hoạt động
            intent.putExtra(AddEditNoteActivity.EXTRA_NOTE_ID, note.documentId);
            mLauncher.launch(intent);
        });
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView tvNoteTitle, tvNotePreview;
        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNoteTitle = itemView.findViewById(R.id.tv_note_title);
            tvNotePreview = itemView.findViewById(R.id.tv_note_preview);
        }
    }

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
            return Objects.equals(oldItem.title, newItem.title) &&
                    Objects.equals(oldItem.content, newItem.content);
        }
    };
}