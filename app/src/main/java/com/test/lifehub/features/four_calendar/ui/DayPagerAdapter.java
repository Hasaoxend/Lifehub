package com.test.lifehub.features.four_calendar.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * ViewPager2 Adapter cho Day View
 * Tạo vô hạn pages để swipe qua các ngày
 */
public class DayPagerAdapter extends RecyclerView.Adapter<DayPagerAdapter.DayPageHolder> {

    private static final int HOUR_HEIGHT_DP = 60;
    private Calendar mWeekStart;
    private final CalendarViewModel mViewModel;
    private final Fragment mFragment;
    private List<DayPageHolder> mActiveHolders = new ArrayList<>();

    public DayPagerAdapter(Fragment fragment, Calendar weekStart, CalendarViewModel viewModel) {
        this.mFragment = fragment;
        this.mWeekStart = weekStart;
        this.mViewModel = viewModel;
    }

    public void updateWeek(Calendar newWeekStart) {
        this.mWeekStart = newWeekStart;
        notifyDataSetChanged();
    }

    public void notifyTimeUpdate() {
        for (DayPageHolder holder : mActiveHolders) {
            holder.updateCurrentTimeLine();
        }
    }

    @NonNull
    @Override
    public DayPageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day_page, parent, false);
        return new DayPageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayPageHolder holder, int position) {
        Calendar day = (Calendar) mWeekStart.clone();
        day.add(Calendar.DAY_OF_MONTH, position);
        holder.bind(day);
        
        if (!mActiveHolders.contains(holder)) {
            mActiveHolders.add(holder);
        }
    }

    @Override
    public void onViewRecycled(@NonNull DayPageHolder holder) {
        super.onViewRecycled(holder);
        mActiveHolders.remove(holder);
    }

    @Override
    public int getItemCount() {
        return 7; // 7 ngày trong tuần
    }

    class DayPageHolder extends RecyclerView.ViewHolder {
        LinearLayout mTimeEventsContainer;
        ScrollView mScrollView;
        View mCurrentTimeLine;
        Calendar mDayCalendar;

        DayPageHolder(@NonNull View itemView) {
            super(itemView);
            mTimeEventsContainer = itemView.findViewById(R.id.time_events_container);
            mScrollView = itemView.findViewById(R.id.scroll_view);
            mCurrentTimeLine = itemView.findViewById(R.id.current_time_line);
        }

        void bind(Calendar day) {
            mDayCalendar = day;
            setupTimeEventsGrid();
            updateCurrentTimeLine();
            observeEvents();
        }

        private void setupTimeEventsGrid() {
            mTimeEventsContainer.removeAllViews();
            
            for (int hour = 0; hour < 24; hour++) {
                // Tạo hàng cho mỗi giờ
                LinearLayout hourRow = new LinearLayout(itemView.getContext());
                hourRow.setOrientation(LinearLayout.HORIZONTAL);
                hourRow.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        dpToPx(HOUR_HEIGHT_DP)));
                hourRow.setBackgroundResource(R.drawable.time_slot_background);
                
                // Label giờ
                TextView hourLabel = new TextView(itemView.getContext());
                hourLabel.setText(String.format("%02d:00", hour));
                hourLabel.setTextSize(12);
                hourLabel.setTextColor(Color.GRAY);
                hourLabel.setPadding(dpToPx(8), 0, dpToPx(8), 0);
                hourLabel.setLayoutParams(new LinearLayout.LayoutParams(
                        dpToPx(60), 
                        ViewGroup.LayoutParams.MATCH_PARENT));
                hourLabel.setGravity(android.view.Gravity.CENTER_VERTICAL);
                
                // Container cho events
                FrameLayout eventSlot = new FrameLayout(itemView.getContext());
                eventSlot.setLayoutParams(new LinearLayout.LayoutParams(
                        0, 
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        1f));
                
                final int currentHour = hour;
                eventSlot.setOnClickListener(v -> {
                    Calendar eventTime = (Calendar) mDayCalendar.clone();
                    eventTime.set(Calendar.HOUR_OF_DAY, currentHour);
                    eventTime.set(Calendar.MINUTE, 0);
                    showCreateEventDialog(eventTime);
                });
                
                hourRow.addView(hourLabel);
                hourRow.addView(eventSlot);
                mTimeEventsContainer.addView(hourRow);
            }
        }

        /**
         * Update vạch đỏ current time
         */
        void updateCurrentTimeLine() {
            Calendar now = Calendar.getInstance();
            
            // Chỉ hiển thị vạch đỏ nếu đây là ngày hôm nay
            if (mDayCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                mDayCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                
                mCurrentTimeLine.setVisibility(View.VISIBLE);
                
                int currentHour = now.get(Calendar.HOUR_OF_DAY);
                int currentMinute = now.get(Calendar.MINUTE);
                int topMarginDp = (currentHour * HOUR_HEIGHT_DP) + currentMinute;
                
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mCurrentTimeLine.getLayoutParams();
                params.topMargin = dpToPx(topMarginDp);
                params.leftMargin = dpToPx(68); // 60dp (label width) + 8dp padding
                mCurrentTimeLine.setLayoutParams(params);
            } else {
                mCurrentTimeLine.setVisibility(View.GONE);
            }
        }

        private void observeEvents() {
            mViewModel.getAllEvents().observe(mFragment.getViewLifecycleOwner(), events -> {
                if (events != null) {
                    renderEvents(events);
                }
            });
        }

        private void renderEvents(List<CalendarEvent> allEvents) {
            // Clear các events cũ trong tất cả các slots
            for (int i = 0; i < mTimeEventsContainer.getChildCount(); i++) {
                View hourRow = mTimeEventsContainer.getChildAt(i);
                if (hourRow instanceof LinearLayout) {
                    FrameLayout eventSlot = (FrameLayout) ((LinearLayout) hourRow).getChildAt(1);
                    eventSlot.removeAllViews();
                }
            }
            
            List<CalendarEvent> dayEvents = getEventsForDay(allEvents);
            
            for (CalendarEvent event : dayEvents) {
                renderEvent(event);
            }
        }

        private List<CalendarEvent> getEventsForDay(List<CalendarEvent> allEvents) {
            List<CalendarEvent> dayEvents = new ArrayList<>();
            
            Calendar dayStart = (Calendar) mDayCalendar.clone();
            dayStart.set(Calendar.HOUR_OF_DAY, 0);
            dayStart.clear(Calendar.MINUTE);
            dayStart.clear(Calendar.SECOND);
            
            Calendar dayEnd = (Calendar) dayStart.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);
            
            for (CalendarEvent event : allEvents) {
                if (event.getStartTime() == null || event.getEndTime() == null) continue;
                
                Calendar eventStart = Calendar.getInstance();
                eventStart.setTime(event.getStartTime());
                Calendar eventEnd = Calendar.getInstance();
                eventEnd.setTime(event.getEndTime());
                
                if (eventStart.before(dayEnd) && eventEnd.after(dayStart)) {
                    dayEvents.add(event);
                }
            }
            
            Collections.sort(dayEvents, (e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()));
            return dayEvents;
        }

        private void renderEvent(CalendarEvent event) {
            TextView eventView = new TextView(itemView.getContext());
            eventView.setText(event.getTitle());
            eventView.setTextSize(11);
            eventView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            eventView.setMaxLines(2);
            eventView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            
            GradientDrawable eventBackground = new GradientDrawable();
            eventBackground.setShape(GradientDrawable.RECTANGLE);
            eventBackground.setCornerRadius(dpToPx(4));
            
            int eventColor = generateColorFromEvent(event);
            eventBackground.setColor(eventColor);
            eventView.setTextColor(isColorDark(eventColor) ? Color.WHITE : Color.BLACK);
            eventView.setBackground(eventBackground);
            
            Calendar eventStart = Calendar.getInstance();
            eventStart.setTime(event.getStartTime());
            int startHour = eventStart.get(Calendar.HOUR_OF_DAY);
            
            // Tìm slot tương ứng với giờ bắt đầu
            if (startHour >= 0 && startHour < mTimeEventsContainer.getChildCount()) {
                View hourRow = mTimeEventsContainer.getChildAt(startHour);
                if (hourRow instanceof LinearLayout) {
                    FrameLayout eventSlot = (FrameLayout) ((LinearLayout) hourRow).getChildAt(1);
                    
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
                    eventView.setLayoutParams(params);
                    
                    eventView.setOnClickListener(v -> showEventDetail(event));
                    eventSlot.addView(eventView);
                }
            }
        }

        private void showCreateEventDialog(Calendar time) {
            CalendarEvent newEvent = new CalendarEvent();
            newEvent.setStartTime(time.getTime());
            Calendar endTime = (Calendar) time.clone();
            endTime.add(Calendar.HOUR_OF_DAY, 1);
            newEvent.setEndTime(endTime.getTime());
            
            AddEditEventDialog dialog = AddEditEventDialog.newInstance(newEvent);
            dialog.show(mFragment.getParentFragmentManager(), "AddEventDialog");
        }

        private void showEventDetail(CalendarEvent event) {
            AddEditEventDialog dialog = AddEditEventDialog.newInstance(event);
            dialog.show(mFragment.getParentFragmentManager(), "EventDetailDialog");
        }

        private int generateColorFromEvent(CalendarEvent event) {
            if (event.getTitle() == null || event.getTitle().isEmpty()) {
                return 0xFF2196F3;
            }
            int hash = event.getTitle().hashCode();
            float hue = (float) (Math.abs(hash) % 360);
            return Color.HSVToColor(new float[]{hue, 0.7f, 0.9f});
        }

        private boolean isColorDark(int color) {
            double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
            return darkness >= 0.5;
        }

        private int dpToPx(int dp) {
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}
