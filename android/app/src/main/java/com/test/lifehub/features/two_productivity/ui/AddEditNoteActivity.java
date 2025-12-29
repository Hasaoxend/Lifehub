package com.test.lifehub.features.two_productivity.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log; // Thêm import
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.test.lifehub.R;
import com.test.lifehub.core.util.AlarmHelper; // <-- THÊM IMPORT NÀY
import com.test.lifehub.features.two_productivity.data.NoteEntry;

import java.util.Calendar;
import java.util.Date;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditNoteActivity extends AppCompatActivity {

    public static final String EXTRA_NOTE_ID = "EXTRA_NOTE_ID";
    private static final String TAG = "AddEditNoteActivity"; // Thêm TAG

    // --- Views ---
    private Toolbar mToolbar;
    private EditText mEtTitle, mEtContent;
    private ImageButton mBtnSetReminder, mBtnClearReminder;
    private TextView mTvReminderTime;

    // --- Logic ---
    private ProductivityViewModel mViewModel;
    private String mCurrentNoteId;
    private NoteEntry mCurrentNote;

    // --- Biến cho Đặt giờ ---
    private Date mReminderDate = null;
    private final Calendar mCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        mToolbar = findViewById(R.id.toolbar_add_edit_note);
        mEtTitle = findViewById(R.id.et_note_title);
        mEtContent = findViewById(R.id.et_note_content);
        mBtnSetReminder = findViewById(R.id.btn_set_reminder);
        mTvReminderTime = findViewById(R.id.tv_reminder_time);
        mBtnClearReminder = findViewById(R.id.btn_clear_reminder);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        mBtnSetReminder.setOnClickListener(v -> showDateTimePicker());
        mBtnClearReminder.setOnClickListener(v -> {
            mReminderDate = null;
            updateReminderUi();
        });

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_NOTE_ID)) {
            getSupportActionBar().setTitle(R.string.title_edit_note);
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
            getSupportActionBar().setTitle(R.string.title_new_note);
            mCurrentNoteId = null;
            mCurrentNote = null;
            updateReminderUi();
        }
    }

    private void populateUi(NoteEntry note) {
        if (note == null) return;
        mEtTitle.setText(note.getTitle());
        mEtContent.setText(note.getContent());

        mReminderDate = note.getReminderTime();
        if (mReminderDate != null) {
            mCalendar.setTime(mReminderDate);
        }
        updateReminderUi();
    }

    private void updateReminderUi() {
        if (mReminderDate != null) {
            mTvReminderTime.setText("Nhắc lúc: " + DateFormat.format("HH:mm, dd/MM/yyyy", mReminderDate));
            mBtnClearReminder.setVisibility(View.VISIBLE);
        } else {
            mTvReminderTime.setText("");
            mBtnClearReminder.setVisibility(View.GONE);
        }
    }

    private void showDateTimePicker() {
        if (mReminderDate != null) {
            mCalendar.setTime(mReminderDate);
        } else {
            mCalendar.setTime(new Date());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    mCalendar.set(Calendar.YEAR, year);
                    mCalendar.set(Calendar.MONTH, month);
                    mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (view1, hourOfDay, minute) -> {
                                mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                mCalendar.set(Calendar.MINUTE, minute);
                                mCalendar.set(Calendar.SECOND, 0);

                                if (mCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                                    Toast.makeText(this, R.string.task_select_future_time, Toast.LENGTH_SHORT).show();
                                    mReminderDate = null;
                                } else {
                                    mReminderDate = mCalendar.getTime();
                                }
                                updateReminderUi();
                            },
                            mCalendar.get(Calendar.HOUR_OF_DAY),
                            mCalendar.get(Calendar.MINUTE),
                            DateFormat.is24HourFormat(this)
                    );
                    timePickerDialog.show();
                },
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
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

    private void deleteNote() {
        if (mCurrentNote != null) {
            // ✅ SỬA LỖI TÍNH NĂNG: Hủy Alarm cũ
            if (mCurrentNote.getAlarmRequestCode() != 0) {
                AlarmHelper.cancelAlarm(this, mCurrentNote.getAlarmRequestCode());
            }

            mViewModel.deleteNote(mCurrentNote);
            Toast.makeText(this, R.string.note_deleted_msg, Toast.LENGTH_SHORT).show();
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    private void saveNote() {
        String title = mEtTitle.getText().toString().trim();
        String content = mEtContent.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(this, R.string.note_title_content_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String alarmTitle = "Nhắc nhở Ghi chú";

        if (mCurrentNoteId == null) {
            // ----- THÊM MỚI -----
            Log.d(TAG, "Creating new note: " + title);
            NoteEntry newNote = new NoteEntry(title, content, new Date());
            newNote.setReminderTime(mReminderDate);

            // ✅ SỬA LỖI TÍNH NĂNG: Lên lịch Alarm
            if (mReminderDate != null) {
                int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                newNote.setAlarmRequestCode(requestCode);
                AlarmHelper.scheduleAlarm(this, requestCode, mReminderDate, alarmTitle, title);
            }

            mViewModel.insertNote(newNote);
            Toast.makeText(this, R.string.note_saved_msg, Toast.LENGTH_SHORT).show();

        } else {
            // ----- CẬP NHẬT (SỬA) -----
            Log.d(TAG, "Updating note: " + title);
            if (mCurrentNote != null) {

                // ✅ SỬA LỖI TÍNH NĂNG: Hủy Alarm cũ
                if (mCurrentNote.getAlarmRequestCode() != 0) {
                    AlarmHelper.cancelAlarm(this, mCurrentNote.getAlarmRequestCode());
                }

                mCurrentNote.setTitle(title);
                mCurrentNote.setContent(content);
                mCurrentNote.setLastModified(new Date());
                mCurrentNote.setReminderTime(mReminderDate);

                // ✅ SỬA LỖI TÍNH NĂNG: Lên lịch Alarm mới
                if (mReminderDate != null) {
                    int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
                    mCurrentNote.setAlarmRequestCode(requestCode);
                    AlarmHelper.scheduleAlarm(this, requestCode, mReminderDate, alarmTitle, title);
                } else {
                    mCurrentNote.setAlarmRequestCode(0);
                }

                mViewModel.updateNote(mCurrentNote);
                Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show();
            }
        }

        setResult(Activity.RESULT_OK);
        finish();
    }
}