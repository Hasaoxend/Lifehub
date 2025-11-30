package com.test.lifehub.features.four_calendar.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.core.util.AlarmHelper;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddEditEventDialog extends DialogFragment {

    private static final String ARG_EVENT = "event";

    private TextInputEditText etTitle, etDescription, etLocation;
    private TextView tvStartTime, tvEndTime, tvTitle;
    private Button btnSelectStartTime, btnSelectEndTime, btnDelete;
    private Spinner spinnerRepeat;

    private CalendarViewModel mViewModel;
    private CalendarEvent mCurrentEvent;

    private Calendar mStartCalendar = Calendar.getInstance();
    private Calendar mEndCalendar = Calendar.getInstance();

    public static AddEditEventDialog newInstance(@Nullable CalendarEvent event) {
        AddEditEventDialog dialog = new AddEditEventDialog();
        Bundle args = new Bundle();
        if (event != null) {
            args.putSerializable(ARG_EVENT, event);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_edit_event, null);

        etTitle = view.findViewById(R.id.et_event_title);
        etDescription = view.findViewById(R.id.et_event_description);
        etLocation = view.findViewById(R.id.et_event_location);
        tvStartTime = view.findViewById(R.id.tv_start_time);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        btnSelectStartTime = view.findViewById(R.id.btn_select_start_time);
        btnSelectEndTime = view.findViewById(R.id.btn_select_end_time);
        btnDelete = view.findViewById(R.id.btn_delete_event);
        spinnerRepeat = view.findViewById(R.id.spinner_repeat);
        tvTitle = view.findViewById(R.id.tv_dialog_event_title);

        mViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);

        // Setup spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repeat_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(adapter);

        if (getArguments() != null && getArguments().containsKey(ARG_EVENT)) {
            mCurrentEvent = (CalendarEvent) getArguments().getSerializable(ARG_EVENT);
            populateUI(mCurrentEvent);
            tvTitle.setText("Sửa Sự kiện");
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("Sự kiện Mới");
            btnDelete.setVisibility(View.GONE);
            // Default: 1 hour from now
            mStartCalendar.add(Calendar.HOUR_OF_DAY, 1);
            mStartCalendar.set(Calendar.MINUTE, 0);
            mEndCalendar.setTime(mStartCalendar.getTime());
            mEndCalendar.add(Calendar.HOUR_OF_DAY, 1);
            updateTimeDisplays();
        }

        setupButtons();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", (dialog, which) -> dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> saveEvent());
        });

        return dialog;
    }

    private void populateUI(CalendarEvent event) {
        etTitle.setText(event.getTitle());
        etDescription.setText(event.getDescription());
        etLocation.setText(event.getLocation());

        if (event.getStartTime() != null) {
            mStartCalendar.setTime(event.getStartTime());
        }
        if (event.getEndTime() != null) {
            mEndCalendar.setTime(event.getEndTime());
        }

        updateTimeDisplays();

        // Set repeat type
        String repeatType = event.getRepeatType();
        if (repeatType != null) {
            switch (repeatType) {
                case Constants.REPEAT_NONE:
                    spinnerRepeat.setSelection(0);
                    break;
                case Constants.REPEAT_DAILY:
                    spinnerRepeat.setSelection(1);
                    break;
                case Constants.REPEAT_WEEKLY:
                    spinnerRepeat.setSelection(2);
                    break;
                case Constants.REPEAT_MONTHLY:
                    spinnerRepeat.setSelection(3);
                    break;
            }
        }
    }

    private void setupButtons() {
        btnSelectStartTime.setOnClickListener(v -> showDateTimePicker(true));
        btnSelectEndTime.setOnClickListener(v -> showDateTimePicker(false));

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa sự kiện này?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        if (mCurrentEvent != null) {
                            if (mCurrentEvent.getAlarmRequestCode() != 0) {
                                AlarmHelper.cancelAlarm(requireContext(), mCurrentEvent.getAlarmRequestCode());
                            }
                            mViewModel.deleteEvent(mCurrentEvent);
                            Toast.makeText(getContext(), "Đã xóa sự kiện", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void showDateTimePicker(boolean isStartTime) {
        Calendar calendar = isStartTime ? mStartCalendar : mEndCalendar;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (view1, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);

                                // ✅ SỬA LỖI: Validation ngày bắt đầu/kết thúc
                                long currentTime = System.currentTimeMillis();
                                
                                // Validate: Ngày kết thúc không được ở quá khứ
                                if (!isStartTime && mEndCalendar.getTimeInMillis() < currentTime) {
                                    Toast.makeText(getContext(), "Ngày kết thúc không được ở quá khứ", Toast.LENGTH_LONG).show();
                                    mEndCalendar.setTime(mStartCalendar.getTime());
                                    mEndCalendar.add(Calendar.HOUR_OF_DAY, 1);
                                    updateTimeDisplays();
                                    return;
                                }
                                
                                // Validate: Ngày bắt đầu không được quá xa trong tương lai (> 5 năm)
                                Calendar maxFuture = Calendar.getInstance();
                                maxFuture.add(Calendar.YEAR, 5);
                                if (isStartTime && mStartCalendar.getTimeInMillis() > maxFuture.getTimeInMillis()) {
                                    Toast.makeText(getContext(), "Ngày bắt đầu không được quá 5 năm trong tương lai", Toast.LENGTH_LONG).show();
                                    mStartCalendar.setTimeInMillis(currentTime);
                                    mStartCalendar.add(Calendar.HOUR_OF_DAY, 1);
                                    updateTimeDisplays();
                                    return;
                                }
                                
                                // Validate: End time must be after start time
                                if (!isStartTime && mEndCalendar.getTimeInMillis() <= mStartCalendar.getTimeInMillis()) {
                                    Toast.makeText(getContext(), "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                                    mEndCalendar.setTime(mStartCalendar.getTime());
                                    mEndCalendar.add(Calendar.HOUR_OF_DAY, 1);
                                }

                                updateTimeDisplays();
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateTimeDisplays() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());
        tvStartTime.setText(sdf.format(mStartCalendar.getTime()));
        tvEndTime.setText(sdf.format(mEndCalendar.getTime()));
    }

    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(getContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
            return;
        }

        CalendarEvent event;
        if (mCurrentEvent != null) {
            event = mCurrentEvent;
            // Cancel old alarm
            if (event.getAlarmRequestCode() != 0) {
                AlarmHelper.cancelAlarm(requireContext(), event.getAlarmRequestCode());
            }
        } else {
            event = new CalendarEvent();
        }

        event.setTitle(title);
        event.setDescription(description);
        event.setLocation(location);
        event.setStartTime(mStartCalendar.getTime());
        event.setEndTime(mEndCalendar.getTime());

        // Set repeat type
        int repeatPosition = spinnerRepeat.getSelectedItemPosition();
        String repeatType;
        switch (repeatPosition) {
            case 1: repeatType = Constants.REPEAT_DAILY; break;
            case 2: repeatType = Constants.REPEAT_WEEKLY; break;
            case 3: repeatType = Constants.REPEAT_MONTHLY; break;
            default: repeatType = Constants.REPEAT_NONE; break;
        }
        event.setRepeatType(repeatType);

        // Set reminder (15 minutes before)
        Calendar reminderCal = (Calendar) mStartCalendar.clone();
        reminderCal.add(Calendar.MINUTE, -15);

        if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
            event.setReminderTime(reminderCal.getTime());
            int requestCode = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            event.setAlarmRequestCode(requestCode);
            AlarmHelper.scheduleAlarm(
                    requireContext(),
                    requestCode,
                    reminderCal.getTime(),
                    "Nhắc nhở sự kiện",
                    title
            );
        }

        if (mCurrentEvent != null) {
            mViewModel.updateEvent(event);
            Toast.makeText(getContext(), "Đã cập nhật sự kiện", Toast.LENGTH_SHORT).show();
        } else {
            mViewModel.insertEvent(event);
            Toast.makeText(getContext(), "Đã thêm sự kiện", Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }
}