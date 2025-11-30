package com.test.lifehub.features.four_calendar.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MonthGridAdapter extends ListAdapter<MonthDayData, MonthGridAdapter.DayViewHolder> {

    public interface OnDayClickListener { void onDayClick(MonthDayData day); }
    private final Context context;
    private final OnDayClickListener listener;

    // (Xóa selectedDate)
    private int todayColor;
    private int defaultTextColor;
    private int holidayTextColor;

    public MonthGridAdapter(Context context, OnDayClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;

        // (Xóa selectedColor)
        todayColor = ContextCompat.getColor(context, R.color.md_theme_light_primary);
        defaultTextColor = ContextCompat.getColor(context, R.color.md_theme_light_onBackground);
        holidayTextColor = ContextCompat.getColor(context, R.color.md_theme_light_primary);
    }

    // (Xóa setSelectedDate)

    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month_grid_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        MonthDayData day = getItem(position);
        holder.tvDate.setText(String.format(Locale.getDefault(), "%d", day.date.getDate()));

        float alpha = day.isCurrentMonth ? 1.0f : 0.3f;
        holder.tvDate.setAlpha(alpha);
        holder.layoutEvents.setAlpha(alpha);
        holder.tvHoliday.setAlpha(alpha);
        holder.tvLunarDate.setAlpha(alpha);

        holder.tvDate.setBackground(null);
        holder.tvDate.setTextColor(defaultTextColor);
        holder.tvDate.setTypeface(Typeface.DEFAULT);
        holder.itemView.setBackgroundColor(Color.TRANSPARENT);

        boolean isToday = isSameDay(day.date, Calendar.getInstance().getTime());

        // Chỉ highlight ngày hôm nay
        if (isToday) {
            holder.tvDate.setBackgroundResource(R.drawable.circle_primary);
            holder.tvDate.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onPrimary));
            holder.tvDate.setTypeface(Typeface.DEFAULT_BOLD);
        }
        // (Xóa logic highlight "isSelected")

        // Hiển thị ngày âm lịch
        if (day.lunarDate != null && !day.lunarDate.isEmpty() && day.isCurrentMonth) {
            holder.tvLunarDate.setText(day.lunarDate);
            holder.tvLunarDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvLunarDate.setVisibility(View.GONE);
        }

        // Hiển thị ngày lễ
        if (day.holidayName != null && day.isCurrentMonth) {
            holder.tvHoliday.setText(day.holidayName);
            holder.tvHoliday.setVisibility(View.VISIBLE);
            if (!isToday) { // Chỉ đổi màu chữ nếu không phải hôm nay
                holder.tvDate.setTextColor(holidayTextColor);
            }
        } else {
            holder.tvHoliday.setVisibility(View.GONE);
        }

        // Hiển thị tên sự kiện (Giữ nguyên logic này)
        holder.layoutEvents.removeAllViews();
        int maxEventsToShow = 2;
        int eventCount = day.events.size();

        for (int i = 0; i < Math.min(eventCount, maxEventsToShow); i++) {
            CalendarEvent event = day.events.get(i);
            holder.layoutEvents.addView(createEventTextView(event));
        }

        if (eventCount > maxEventsToShow) {
            TextView moreText = new TextView(context);
            moreText.setText(String.format(Locale.getDefault(), "+%d nữa", eventCount - maxEventsToShow));
            moreText.setTextSize(8);
            moreText.setTextColor(Color.GRAY);
            holder.layoutEvents.addView(moreText);
        }

        holder.itemView.setOnClickListener(v -> listener.onDayClick(day));
    }

    // (Hàm createEventTextView giữ nguyên)
    private View createEventTextView(CalendarEvent event) {
        TextView tv = new TextView(context);
        tv.setTextSize(8);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);

        int eventColor;
        try {
            eventColor = Color.parseColor(event.getColor());
        } catch (Exception e) {
            int hash = event.getTitle().hashCode();
            float hue = (float) (Math.abs(hash) % 360);
            eventColor = Color.HSVToColor(new float[]{hue, 0.7f, 0.9f});
        }

        SpannableString spannable = new SpannableString("• " + event.getTitle());
        spannable.setSpan(new ForegroundColorSpan(eventColor), 0, 1, 0);
        tv.setText(spannable);

        return tv;
    }

    // (ViewHolder và DiffUtil giữ nguyên)
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvHoliday, tvLunarDate;
        LinearLayout layoutEvents;
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_month_day);
            tvHoliday = itemView.findViewById(R.id.tv_holiday_name);
            tvLunarDate = itemView.findViewById(R.id.tv_lunar_date);
            layoutEvents = itemView.findViewById(R.id.layout_day_events);
        }
    }

    private static final DiffUtil.ItemCallback<MonthDayData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MonthDayData>() {
                @Override
                public boolean areItemsTheSame(@NonNull MonthDayData old, @NonNull MonthDayData newItem) {
                    return old.date.equals(newItem.date);
                }
                @Override
                public boolean areContentsTheSame(@NonNull MonthDayData old, @NonNull MonthDayData newItem) {
                    return old.events.size() == newItem.events.size() &&
                            Objects.equals(old.holidayName, newItem.holidayName);
                }
            };
}