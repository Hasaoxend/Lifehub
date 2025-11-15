package com.test.lifehub.features.two_productivity.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.NoteEntry;

import java.util.Date; // <-- IMPORT NÀY

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity để Thêm (Add) hoặc Sửa (Edit) một Ghi chú (Note).
 * (Phiên bản đã sửa lỗi Timestamp)
 */
@AndroidEntryPoint
public class AddEditNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "EXTRA_NOTE_ID";

    // --- Views ---
    private Toolbar mToolbar;
    private EditText mEtTitle, mEtContent;

    // --- Logic ---
    private ProductivityViewModel mViewModel;
    private String mCurrentNoteId;
    private NoteEntry mCurrentNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        mToolbar = findViewById(R.id.toolbar_add_edit_note);
        mEtTitle = findViewById(R.id.et_note_title);
        mEtContent = findViewById(R.id.et_note_content);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_NOTE_ID)) {
            getSupportActionBar().setTitle("Sửa Ghi chú");
            mCurrentNoteId = intent.getStringExtra(EXTRA_NOTE_ID);

            if (mCurrentNoteId != null && !mCurrentNoteId.isEmpty()){
                mViewModel.getNoteById(mCurrentNoteId).observe(this, noteEntry -> {
                    if (noteEntry != null) {
                        mCurrentNote = noteEntry;
                        populateUi(noteEntry);
                    }
                });
            }

        } else {
            getSupportActionBar().setTitle("Ghi chú Mới");
            mCurrentNoteId = null;
            mCurrentNote = null;
        }
    }

    private void populateUi(NoteEntry note) {
        if (note == null) return;
        mEtTitle.setText(note.getTitle());
        mEtContent.setText(note.getContent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        // Ẩn nút Xóa nếu là Thêm mới
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        if (mCurrentNoteId == null) {
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveNote();
            return true;
        } else if (id == R.id.action_delete) {
            deleteNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Xóa Ghi chú (chỉ khi ở chế độ Sửa)
     */
    private void deleteNote() {
        if (mCurrentNote != null) {
            mViewModel.deleteNote(mCurrentNote);
            Toast.makeText(this, "Đã xóa Ghi chú", Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK); // Đặt kết quả OK
            finish();
        }
    }

    /**
     * Logic chính: Lưu Ghi chú
     */
    private void saveNote() {
        String title = mEtTitle.getText().toString().trim();
        String content = mEtContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Vui lòng nhập Tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Vui lòng nhập Nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCurrentNoteId == null) {
            // ----- THÊM MỚI -----
            NoteEntry newNote = new NoteEntry(
                    title,
                    content,
                    new Date() // SỬA LỖI: Dùng new Date() thay vì .getTime()
            );
            mViewModel.insertNote(newNote);
            Toast.makeText(this, "Đã lưu Ghi chú", Toast.LENGTH_SHORT).show();

        } else {
            // ----- CẬP NHẬT (SỬA) -----
            if (mCurrentNote != null) {
                mCurrentNote.setTitle(title);
                mCurrentNote.setContent(content);
                mCurrentNote.setLastModified(new Date()); // SỬA LỖI

                mViewModel.updateNote(mCurrentNote);
                Toast.makeText(this, "Đã cập nhật Ghi chú", Toast.LENGTH_SHORT).show();
            } else {
                NoteEntry updatedNote = new NoteEntry(title, content, new Date()); // SỬA LỖI
                updatedNote.documentId = mCurrentNoteId;
                mViewModel.updateNote(updatedNote);
                Toast.makeText(this, "Đã cập nhật Ghi chú", Toast.LENGTH_SHORT).show();
            }
        }

        setResult(Activity.RESULT_OK);
        finish();
    }
}