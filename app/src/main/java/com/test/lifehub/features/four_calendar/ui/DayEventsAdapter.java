package com.test.lifehub.features.four_calendar.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class DayEventsAdapter extends ListAdapter<CalendarEvent, DayEventsAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(CalendarEvent event);
    }

    private final OnEventClickListener listener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public DayEventsAdapter(OnEventClickListener listener) {
        super(DIFF_CALLBACK);
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
        CalendarEvent event = getItem(position);
        holder.bind(event, listener, timeFormat);
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        View viewColorBar;
        TextView tvTitle, tvTime, tvLocation;
        // Biến để giữ drawable
        private GradientDrawable colorBarBackground;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorBar = itemView.findViewById(R.id.view_color_bar);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvLocation = itemView.findViewById(R.id.tv_event_location);

            // *** SỬA LỖI CRASH: ***
            // Tạo một GradientDrawable mới bằng code
            colorBarBackground = new GradientDrawable();
            // Gán nó làm background cho view.
            // (Nó sẽ thay thế android:background="#2196F3" trong XML)
            viewColorBar.setBackground(colorBarBackground);
        }

        public void bind(CalendarEvent event, OnEventClickListener listener, SimpleDateFormat timeFormat) {
            tvTitle.setText(event.getTitle());

            String timeText = timeFormat.format(event.getStartTime()) + " - " + timeFormat.format(event.getEndTime());
            tvTime.setText(timeText);

            if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                tvLocation.setText(event.getLocation());
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            int eventColor;
            try {
                eventColor = Color.parseColor(event.getColor());
            } catch (Exception e) {
                eventColor = generateColorFromTitle(event.getTitle());
            }

            // *** SỬA LỖI CRASH: ***
            // Giờ chúng ta chỉ cần set màu cho drawable đã tạo
            colorBarBackground.setColor(eventColor);

            itemView.setOnClickListener(v -> listener.onEventClick(event));
        }

        private int generateColorFromTitle(String title) {
            if (title == null || title.isEmpty()) {
                return 0xFF2196F3;
            }
            int hash = title.hashCode();
            float hue = (float) (Math.abs(hash) % 360);
            return Color.HSVToColor(new float[]{hue, 0.7f, 0.9f});
        }
    }

    // (DIFF_CALLBACK giữ nguyên, đã sửa lỗi 'getId' trước đó)
    private static final DiffUtil.ItemCallback<CalendarEvent> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CalendarEvent>() {
                @Override
                public boolean areItemsTheSame(@NonNull CalendarEvent oldItem, @NonNull CalendarEvent newItem) {
                    return oldItem.getStartTime().equals(newItem.getStartTime()) &&
                            Objects.equals(oldItem.getTitle(), newItem.getTitle());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CalendarEvent oldItem, @NonNull CalendarEvent newItem) {
                    return Objects.equals(oldItem.getEndTime(), newItem.getEndTime()) &&
                            Objects.equals(oldItem.getLocation(), newItem.getLocation()) &&
                            Objects.equals(oldItem.getColor(), newItem.getColor());
                }
            };
}