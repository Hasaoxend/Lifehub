package com.test.lifehub.features.four_calendar.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DayEventsAdapter extends RecyclerView.Adapter<DayEventsAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    private final List<CalendarEvent> events;
    private final OnEventClickListener listener;

    public DayEventsAdapter(List<CalendarEvent> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        CalendarEvent event = events.get(position);

        holder.tvTitle.setText(event.getTitle());

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeText = timeFormat.format(event.getStartTime()) + " - " + timeFormat.format(event.getEndTime());
        holder.tvTime.setText(timeText);

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            holder.tvLocation.setText(event.getLocation());
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        try {
            holder.viewColorBar.setBackgroundColor(Color.parseColor(event.getColor()));
        } catch (Exception e) {
            holder.viewColorBar.setBackgroundColor(Color.parseColor("#2196F3"));
        }

        holder.itemView.setOnClickListener(v -> listener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        View viewColorBar;
        TextView tvTitle, tvTime, tvLocation;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorBar = itemView.findViewById(R.id.view_color_bar);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
        }
    }
}