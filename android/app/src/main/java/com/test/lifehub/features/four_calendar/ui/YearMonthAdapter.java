package com.test.lifehub.features.four_calendar.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter cho Year View
 * Hiển thị 12 tháng dạng grid mini
 */
public class YearMonthAdapter extends RecyclerView.Adapter<YearMonthAdapter.MonthViewHolder> {

    private List<YearMonthData> mMonthList = new ArrayList<>();
    private final Context mContext;
    private final OnMonthClickListener mListener;

    public interface OnMonthClickListener {
        void onMonthClick(Calendar month);
    }

    public YearMonthAdapter(Context context, OnMonthClickListener listener) {
        this.mContext = context;
        this.mListener = listener;
    }

    public void submitList(List<YearMonthData> list) {
        this.mMonthList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_year_month, parent, false);
        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        YearMonthData monthData = mMonthList.get(position);
        holder.bind(monthData);
    }

    @Override
    public int getItemCount() {
        return mMonthList.size();
    }

    class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonthName;
        GridLayout gridDays;

        MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonthName = itemView.findViewById(R.id.tv_month_name);
            gridDays = itemView.findViewById(R.id.grid_days);
        }

        void bind(YearMonthData monthData) {
            tvMonthName.setText(monthData.monthName);

            // Tạo map ngày → số lượng sự kiện
            Map<Integer, Integer> eventCountMap = new HashMap<>();
            for (CalendarEvent event : monthData.eventsInMonth) {
                if (event.getStartTime() == null) continue;
                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(event.getStartTime());
                int day = eventCal.get(Calendar.DAY_OF_MONTH);
                eventCountMap.put(day, eventCountMap.getOrDefault(day, 0) + 1);
            }

            // Vẽ mini calendar grid (7x6 = 42 ô nhỏ)
            gridDays.removeAllViews();
            gridDays.setColumnCount(7);
            gridDays.setRowCount(6);

            Calendar cal = (Calendar) monthData.monthCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

            Calendar today = Calendar.getInstance();
            int currentMonth = monthData.monthCalendar.get(Calendar.MONTH);

            for (int i = 0; i < 42; i++) {
                TextView dayView = new TextView(mContext);
                dayView.setLayoutParams(new GridLayout.LayoutParams());
                dayView.setTextSize(8);
                dayView.setGravity(android.view.Gravity.CENTER);
                dayView.setPadding(2, 2, 2, 2);

                int day = cal.get(Calendar.DAY_OF_MONTH);
                boolean isCurrentMonth = cal.get(Calendar.MONTH) == currentMonth;
                boolean isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

                if (isCurrentMonth) {
                    dayView.setText(String.valueOf(day));
                    dayView.setTextColor(isToday ? Color.RED : Color.BLACK);

                    // Hiển thị dấu chấm nếu có sự kiện
                    Integer eventCount = eventCountMap.get(day);
                    if (eventCount != null && eventCount > 0) {
                        dayView.setBackgroundResource(R.drawable.day_with_event_dot);
                    }
                } else {
                    dayView.setText("");
                }

                gridDays.addView(dayView);
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Click vào tháng
            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onMonthClick(monthData.monthCalendar);
                }
            });
        }
    }
}
