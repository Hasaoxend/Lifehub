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

    public static DayEventsDialog newInstance(Date date, ArrayList<CalendarEvent> events) {
        DayEventsDialog dialog = new DayEventsDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        args.putSerializable(ARG_EVENTS, events);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_day_events, null);

        TextView tvDate = view.findViewById(R.id.tv_dialog_date);
        RecyclerView recyclerView = view.findViewById(R.id.rv_day_events);
        TextView tvEmpty = view.findViewById(R.id.tv_empty_day_events);

        Date date = (Date) getArguments().getSerializable(ARG_DATE);
        ArrayList<CalendarEvent> events = (ArrayList<CalendarEvent>) getArguments().getSerializable(ARG_EVENTS);

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(date));

        if (events == null || events.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            DayEventsAdapter adapter = new DayEventsAdapter(events, event -> {
                dismiss();
                AddEditEventDialog dialog = AddEditEventDialog.newInstance(event);
                dialog.show(getParentFragmentManager(), "EventDetailDialog");
            });
            recyclerView.setAdapter(adapter);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton("Đóng", (dialog, which) -> dismiss());

        return builder.create();
    }
}