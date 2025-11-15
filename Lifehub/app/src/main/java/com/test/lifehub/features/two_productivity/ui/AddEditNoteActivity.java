package com.test.lifehub.features.two_productivity.ui;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.services.ReminderService;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.two_productivity.data.NoteEntry;

import java.util.Calendar;

/**
 * Activity để Thêm/Sửa một Ghi chú (Note).
 * (Phiên bản đã sửa lỗi EXTRA_NOTE_ID)
 */
public class AddEditNoteActivity extends AppCompatActivity {

    // ----- SỬA LỖI: THÊM HẰNG SỐ (KEY) BỊ THIẾU -----
    public static final String EXTRA_NOTE_ID = "NOTE_ID";
    // ---------------------------------------------

    private static final String TAG = "AddEditNoteActivity";

    // --- Views ---
    private EditText etNoteTitle, etNoteContent;
    private FloatingActionButton fabSave;
    private Button btnSetReminder;
    private Toolbar mToolbar;

    // --- Logic ---
    private ProductivityViewModel mViewModel;
    private NoteEntry mCurrentNote;
    private String mNoteDocumentId;
    private boolean mIsDataLoaded = false;

    private final Calendar mReminderCalendar = Calendar.getInstance();
    private long mReminderTime = 0;
    private boolean mReminderChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        // 1. Ánh xạ Views
        etNoteTitle = findViewById(R.id.et_note_title);
        etNoteContent = findViewById(R.id.et_note_content);
        fabSave = findViewById(R.id.fab_save_note);
        btnSetReminder = findViewById(R.id.btn_set_reminder_note);
        mToolbar = findViewById(R.id.toolbar_note);

        // 2. Cài đặt Toolbar
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mToolbar.setNavigationOnClickListener(v -> finish());

        // 3. Khởi tạo ViewModel
        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        // 4. Lấy ID từ Intent (Sử dụng hằng số vừa định nghĩa)
        if (getIntent().hasExtra(EXTRA_NOTE_ID)) {
            mNoteDocumentId = getIntent().getStringExtra(EXTRA_NOTE_ID);
        }

        // 5. Quyết định chế độ (Thêm/Sửa)
        if (mNoteDocumentId == null) {
            getSupportActionBar().setTitle("Ghi chú mới");
            mIsDataLoaded = true;
        } else {
            getSupportActionBar().setTitle("Sửa Ghi chú");
            loadNoteData(mNoteDocumentId);
        }

        // 6. Cài đặt Listeners
        fabSave.setOnClickListener(v -> saveNote());
        btnSetReminder.setOnClickListener(v -> pickDateTime());
    }

    //
    // ----- (TẤT CẢ CÁC HÀM CÒN LẠI GIỮ NGUYÊN) -----
    // (loadNoteData, populateUi, saveNote, onCreateOptionsMenu,
    //  onOptionsItemSelected, confirmDelete, pickDateTime,
    //  pickTime, setReminder, updateReminderButtonText)
    //

    // ... (Copy/paste chúng từ file AddEditNoteActivity (phiên bản Firestore) trước đó) ...
    private void loadNoteData(String id) {
        mViewModel.getNoteById(id).observe(this, note -> {
            if (note != null && !mIsDataLoaded) {
                mCurrentNote = note;
                populateUi(note.title, note.content);
                updateReminderButtonText();
            }
        });
    }

    private void populateUi(String title, String content) {
        etNoteTitle.setText(title);
        etNoteContent.setText(content);
        mIsDataLoaded = true;
    }

    private void saveNote() {
        if (!mIsDataLoaded) {
            Toast.makeText(this, "Dữ liệu đang tải...", Toast.LENGTH_SHORT).show();
            return;
        }
        String title = etNoteTitle.getText().toString().trim();
        String content = etNoteContent.getText().toString().trim();
        if (title.isEmpty()) {
            etNoteTitle.setError("Tiêu đề không được để trống");
            return;
        }
        int reminderId = (int) System.currentTimeMillis();
        if (mNoteDocumentId == null) {
            NoteEntry newNote = new NoteEntry();
            newNote.title = title;
            newNote.content = content;
            mViewModel.insertNote(newNote);
        } else {
            mCurrentNote.title = title;
            mCurrentNote.content = content;
            mViewModel.updateNote(mCurrentNote);
            reminderId = mCurrentNote.documentId.hashCode();
        }
        if (mReminderChanged && mReminderTime > 0) {
            setReminder(reminderId, title, mReminderTime);
        }
        Toast.makeText(AddEditNoteActivity.this, "Đã lưu ghi chú!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_edit_menu, menu);
        if (mNoteDocumentId == null) {
            MenuItem deleteItem = menu.findItem(R.id.menu_delete);
            deleteItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_save) {
            saveNote();
            return true;
        } else if (id == R.id.menu_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete() {
        if (mCurrentNote == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận Xóa Ghi chú")
                .setMessage("Bạn có chắc chắn muốn xóa ghi chú này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    mViewModel.deleteNote(mCurrentNote);
                    Toast.makeText(this, "Đã xóa ghi chú", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .setIcon(R.drawable.ic_warning)
                .show();
    }

    private void updateReminderButtonText() {
        if (mReminderTime > 0) {
            btnSetReminder.setText("Nhắc lúc: " + mReminderTime);
        } else {
            btnSetReminder.setText("Đặt nhắc nhở");
        }
    }

    private void pickDateTime() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    mReminderCalendar.set(Calendar.YEAR, year1);
                    mReminderCalendar.set(Calendar.MONTH, monthOfYear);
                    mReminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    pickTime();
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void pickTime() {
        int hour = mReminderCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = mReminderCalendar.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    mReminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    mReminderCalendar.set(Calendar.MINUTE, minute1);
                    mReminderCalendar.set(Calendar.SECOND, 0);
                    long selectedTime = mReminderCalendar.getTimeInMillis();
                    if (selectedTime <= System.currentTimeMillis()) {
                        Toast.makeText(this, "Vui lòng chọn thời gian trong tương lai", Toast.LENGTH_SHORT).show();
                        mReminderTime = 0;
                    } else {
                        mReminderTime = selectedTime;
                        Toast.makeText(this, "Đã đặt nhắc nhở (chưa lưu)", Toast.LENGTH_SHORT).show();
                    }
                    mReminderChanged = true;
                    updateReminderButtonText();
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void setReminder(int reminderId, String title, long reminderTimeInMillis) {
        String content = "Bạn có nhắc nhở cho ghi chú: " + (title.isEmpty() ? "(Không tiêu đề)" : title);
        Intent intent = new Intent(this, ReminderService.class);
        intent.putExtra(Constants.EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(Constants.EXTRA_REMINDER_TITLE, "Nhắc nhở Ghi chú");
        intent.putExtra(Constants.EXTRA_REMINDER_CONTENT, content);
        PendingIntent pendingIntent = PendingIntent.getService(this,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Cần cấp quyền Đặt báo thức chính xác", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeInMillis, pendingIntent);
            Log.d(TAG, "Đã đặt báo thức thành công cho ID: " + reminderId);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Không thể đặt báo thức. Vui lòng kiểm tra quyền.", Toast.LENGTH_LONG).show();
        }
    }
}