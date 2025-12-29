package com.test.lifehub.features.four_calendar.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DayEventsDialog extends DialogFragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_EVENTS = "events";
    private static final String ARG_HOLIDAY = "holiday";

    public static DayEventsDialog newInstance(Date date, ArrayList<CalendarEvent> events, String holidayName) {
        DayEventsDialog dialog = new DayEventsDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        args.putSerializable(ARG_EVENTS, events);
        args.putString(ARG_HOLIDAY, holidayName);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_day_events, null); // (Cần file layout này)

        TextView tvDate = view.findViewById(R.id.tv_dialog_date);
        TextView tvHoliday = view.findViewById(R.id.tv_dialog_holiday);
        RecyclerView recyclerView = view.findViewById(R.id.rv_day_events);
        TextView tvEmpty = view.findViewById(R.id.tv_empty_day_events);

        Date date = (Date) getArguments().getSerializable(ARG_DATE);
        ArrayList<CalendarEvent> events = (ArrayList<CalendarEvent>) getArguments().getSerializable(ARG_EVENTS);
        String holidayName = getArguments().getString(ARG_HOLIDAY);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(date));
        
        // Hiển thị ngày lễ nếu có
        if (holidayName != null && !holidayName.isEmpty()) {
            tvHoliday.setText("🎉 " + holidayName);
            tvHoliday.setVisibility(View.VISIBLE);
        } else {
            tvHoliday.setVisibility(View.GONE);
        }

        boolean hasEvents = events != null && !events.isEmpty();
        boolean hasHoliday = holidayName != null && !holidayName.isEmpty();
        
        if (!hasEvents && !hasHoliday) {
            // Không có gì cả
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else if (!hasEvents && hasHoliday) {
            // Chỉ có ngày lễ, không có sự kiện
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
        } else {
            // Có sự kiện
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            // SỬA LỖI: Khởi tạo 1 tham số, sau đó submitList
            DayEventsAdapter adapter = new DayEventsAdapter(event -> {
                dismiss();
                AddEditEventDialog dialog = AddEditEventDialog.newInstance(event);
                dialog.show(getParentFragmentManager(), "EventDetailDialog");
            });
            recyclerView.setAdapter(adapter);
            adapter.submitList(events);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton("Đóng", (dialog, which) -> dismiss());

        return builder.create();
    }
}