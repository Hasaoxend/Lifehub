package com.test.lifehub.features.four_calendar.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WeekViewAdapter extends RecyclerView.Adapter<WeekViewAdapter.WeekDayViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    private List<WeekDayData> weekData = new ArrayList<>();
    private final OnEventClickListener listener;
    private final Context context;

    public WeekViewAdapter(Context context, OnEventClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WeekDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_day, parent, false);
        return new WeekDayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekDayViewHolder holder, int position) {
        WeekDayData data = weekData.get(position);

        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd", Locale.getDefault());

        holder.tvDayOfWeek.setText(dayFormat.format(data.date));
        holder.tvDate.setText(dateFormat.format(data.date));

        // Highlight today
        Calendar today = Calendar.getInstance();
        Calendar itemCal = Calendar.getInstance();
        itemCal.setTime(data.date);

        if (today.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)) {
            holder.tvDate.setBackgroundResource(R.drawable.circle_primary);
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            holder.tvDate.setBackground(null);
            holder.tvDate.setTextColor(Color.BLACK);
        }

        // Clear previous events
        holder.layoutEvents.removeAllViews();

        // Add events
        for (CalendarEvent event : data.events) {
            View eventView = createEventView(event);
            holder.layoutEvents.addView(eventView);
        }

        // Show event count if > 3
        if (data.events.size() > 3) {
            TextView tvMore = new TextView(context);
            tvMore.setText("+" + (data.events.size() - 3) + " sự kiện");
            tvMore.setTextSize(10);
            tvMore.setPadding(8, 4, 8, 4);
            holder.layoutEvents.addView(tvMore);
        }
    }

    private View createEventView(CalendarEvent event) {
        TextView eventView = new TextView(context);
        eventView.setText(event.getTitle());
        eventView.setTextSize(12);
        eventView.setPadding(8, 4, 8, 4);
        eventView.setMaxLines(1);

        try {
            eventView.setBackgroundColor(Color.parseColor(event.getColor()));
        } catch (Exception e) {
            eventView.setBackgroundColor(Color.parseColor("#2196F3"));
        }

        eventView.setTextColor(Color.WHITE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 4, 0, 4);
        eventView.setLayoutParams(params);

        eventView.setOnClickListener(v -> listener.onEventClick(event));

        return eventView;
    }

    @Override
    public int getItemCount() {
        return weekData.size();
    }

    public void submitList(List<WeekDayData> data) {
        this.weekData = data;
        notifyDataSetChanged();
    }

    static class WeekDayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayOfWeek, tvDate;
        LinearLayout layoutEvents;

        public WeekDayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tv_day_of_week);
            tvDate = itemView.findViewById(R.id.tv_date);
            layoutEvents = itemView.findViewById(R.id.layout_events);
        }
    }
}