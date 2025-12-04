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
        FrameLayout mEventsOverlayContainer;
        ScrollView mScrollView;
        View mCurrentTimeLine;
        Calendar mDayCalendar;

        DayPageHolder(@NonNull View itemView) {
            super(itemView);
            mTimeEventsContainer = itemView.findViewById(R.id.time_events_container);
            mScrollView = itemView.findViewById(R.id.scroll_view);
            mCurrentTimeLine = itemView.findViewById(R.id.current_time_line);
            
            // Tạo overlay container cho events (nằm trên lưới giờ)
            FrameLayout parent = (FrameLayout) mTimeEventsContainer.getParent();
            mEventsOverlayContainer = new FrameLayout(itemView.getContext());
            mEventsOverlayContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
            mEventsOverlayContainer.setPadding(dpToPx(68), 0, dpToPx(8), 0); // 60dp label + 8dp padding
            parent.addView(mEventsOverlayContainer);
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
                
                // Time slot area (clickable)
                View timeSlot = new View(itemView.getContext());
                timeSlot.setLayoutParams(new LinearLayout.LayoutParams(
                        0, 
                        ViewGroup.LayoutParams.MATCH_PARENT, 
                        1f));
                
                final int currentHour = hour;
                timeSlot.setOnClickListener(v -> {
                    Calendar eventTime = (Calendar) mDayCalendar.clone();
                    eventTime.set(Calendar.HOUR_OF_DAY, currentHour);
                    eventTime.set(Calendar.MINUTE, 0);
                    showCreateEventDialog(eventTime);
                });
                
                hourRow.addView(hourLabel);
                hourRow.addView(timeSlot);
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
            // Clear overlay container
            if (mEventsOverlayContainer != null) {
                mEventsOverlayContainer.removeAllViews();
            }
            
            List<CalendarEvent> dayEvents = getEventsForDay(allEvents);
            
            // Tạo segments và tính toán layout
            List<EventSegment> segments = createSegmentsForDay(dayEvents);
            calculateOverlappingLayout(segments);
            
            // Render từng segment
            for (EventSegment segment : segments) {
                renderEventSegment(segment);
            }
        }
        
        /**
         * Tạo segments cho tất cả events trong ngày
         */
        private List<EventSegment> createSegmentsForDay(List<CalendarEvent> dayEvents) {
            List<EventSegment> segments = new ArrayList<>();
            
            Calendar dayStart = (Calendar) mDayCalendar.clone();
            dayStart.set(Calendar.HOUR_OF_DAY, 0);
            dayStart.clear(Calendar.MINUTE);
            dayStart.clear(Calendar.SECOND);
            
            Calendar dayEnd = (Calendar) dayStart.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);
            
            for (CalendarEvent event : dayEvents) {
                Calendar eventStart = Calendar.getInstance();
                eventStart.setTime(event.getStartTime());
                Calendar eventEnd = Calendar.getInstance();
                eventEnd.setTime(event.getEndTime());
                
                // Cắt segment theo biên của ngày
                Calendar segmentStart = eventStart.after(dayStart) ? (Calendar) eventStart.clone() : (Calendar) dayStart.clone();
                Calendar segmentEnd = eventEnd.before(dayEnd) ? (Calendar) eventEnd.clone() : (Calendar) dayEnd.clone();
                
                EventSegment segment = new EventSegment(event, segmentStart, segmentEnd);
                segments.add(segment);
            }
            
            return segments;
        }
        
        /**
         * Tính toán layout cho các events overlap
         */
        private void calculateOverlappingLayout(List<EventSegment> segments) {
            // Sắp xếp theo thời gian bắt đầu
            Collections.sort(segments, (s1, s2) -> {
                int startCompare = s1.segmentStart.compareTo(s2.segmentStart);
                if (startCompare != 0) return startCompare;
                return s1.segmentEnd.compareTo(s2.segmentEnd);
            });
            
            // Gán column cho từng segment
            for (EventSegment segment : segments) {
                int column = 0;
                boolean columnFound = false;
                
                while (!columnFound) {
                    columnFound = true;
                    
                    // Kiểm tra column này có bị chiếm không
                    for (EventSegment other : segments) {
                        if (other == segment) continue;
                        if (other.layoutColumn != column) continue;
                        
                        // Kiểm tra overlap
                        if (segment.overlaps(other)) {
                            column++;
                            columnFound = false;
                            break;
                        }
                    }
                }
                
                segment.layoutColumn = column;
            }
            
            // Tính totalLayoutColumns cho từng segment
            for (EventSegment segment : segments) {
                int maxColumnInGroup = segment.layoutColumn + 1;
                
                for (EventSegment other : segments) {
                    if (other == segment) continue;
                    
                    if (segment.overlaps(other)) {
                        maxColumnInGroup = Math.max(maxColumnInGroup, other.layoutColumn + 1);
                    }
                }
                
                segment.totalLayoutColumns = maxColumnInGroup;
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

        /**
         * Render một event segment với iOS-style (bán trong suốt + cột màu bên trái)
         */
        private void renderEventSegment(EventSegment segment) {
            if (mEventsOverlayContainer == null) return;
            
            CalendarEvent event = segment.originalEvent;
            
            // Xác định xem event có kéo dài nhiều ngày không
            Calendar eventStart = Calendar.getInstance();
            eventStart.setTime(event.getStartTime());
            Calendar eventEnd = Calendar.getInstance();
            eventEnd.setTime(event.getEndTime());
            
            boolean isMultiDay = !isSameDay(eventStart, eventEnd);
            boolean isFirstSegment = isSameDay(segment.segmentStart, eventStart);
            boolean isLastSegment = false;
            
            if (isMultiDay) {
                Calendar compareDate = (Calendar) segment.segmentEnd.clone();
                if (segment.segmentEnd.get(Calendar.HOUR_OF_DAY) == 0 && 
                    segment.segmentEnd.get(Calendar.MINUTE) == 0) {
                    compareDate.add(Calendar.DAY_OF_MONTH, -1);
                }
                isLastSegment = !compareDate.before(eventEnd);
            }
            
            // Container cho event (để chứa cột màu + background)
            FrameLayout eventContainer = new FrameLayout(itemView.getContext());
            
            // Background bán trong suốt (iOS-style)
            View backgroundView = new View(itemView.getContext());
            int eventColor = generateColorFromEvent(event);
            int transparentColor = Color.argb(40, Color.red(eventColor), Color.green(eventColor), Color.blue(eventColor));
            
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setColor(transparentColor);
            background.setCornerRadius(dpToPx(4));
            background.setStroke(dpToPx(1), Color.argb(100, Color.red(eventColor), Color.green(eventColor), Color.blue(eventColor)));
            backgroundView.setBackground(background);
            
            FrameLayout.LayoutParams bgParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            backgroundView.setLayoutParams(bgParams);
            eventContainer.addView(backgroundView);
            
            // Cột màu rõ nét bên trái (iOS-style)
            View colorBar = new View(itemView.getContext());
            colorBar.setBackgroundColor(eventColor);
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                dpToPx(4),
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            barParams.leftMargin = dpToPx(2);
            barParams.topMargin = dpToPx(2);
            barParams.bottomMargin = dpToPx(2);
            colorBar.setLayoutParams(barParams);
            eventContainer.addView(colorBar);
            
            // Text
            TextView eventText = new TextView(itemView.getContext());
            String text = event.getTitle();
            if (isMultiDay) {
                if (!isFirstSegment && !isLastSegment) {
                    text = "◄ " + text + " ►";
                } else if (!isFirstSegment) {
                    text = "◄ " + text;
                } else if (!isLastSegment) {
                    text = text + " ►";
                }
            }
            
            eventText.setText(text);
            eventText.setTextSize(11);
            eventText.setTextColor(isColorDark(eventColor) ? eventColor : Color.BLACK);
            eventText.setMaxLines(3);
            eventText.setEllipsize(android.text.TextUtils.TruncateAt.END);
            
            FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            textParams.leftMargin = dpToPx(10); // Để cách cột màu
            textParams.topMargin = dpToPx(4);
            textParams.rightMargin = dpToPx(4);
            eventText.setLayoutParams(textParams);
            eventContainer.addView(eventText);
            
            // Tính toán vị trí và kích thước
            int startHour = segment.segmentStart.get(Calendar.HOUR_OF_DAY);
            int startMinute = segment.segmentStart.get(Calendar.MINUTE);
            int startMinutesFromMidnight = startHour * 60 + startMinute;
            
            // Duration
            long durationMillis = segment.segmentEnd.getTimeInMillis() - segment.segmentStart.getTimeInMillis();
            int durationMinutes = (int) (durationMillis / 60000);
            
            // Xử lý segment kéo dài đến 00:00 ngày sau
            if (durationMinutes <= 0 && 
                segment.segmentEnd.get(Calendar.HOUR_OF_DAY) == 0 && 
                segment.segmentEnd.get(Calendar.MINUTE) == 0) {
                durationMinutes = (24 * 60) - startMinutesFromMidnight;
            }
            
            if (durationMinutes < 15) durationMinutes = 15; // Tối thiểu 15 phút
            
            // Top position và height
            int topMarginDp = (startMinutesFromMidnight * HOUR_HEIGHT_DP) / 60;
            int heightDp = (durationMinutes * HOUR_HEIGHT_DP) / 60;
            
            // Width dựa trên overlapping
            int containerWidth = mEventsOverlayContainer.getWidth();
            if (containerWidth == 0) {
                containerWidth = dpToPx(250);
            }
            containerWidth -= mEventsOverlayContainer.getPaddingLeft() + mEventsOverlayContainer.getPaddingRight();
            
            int eventWidth = containerWidth / segment.totalLayoutColumns;
            int leftMargin = eventWidth * segment.layoutColumn;
            
            if (segment.layoutColumn > 0) {
                leftMargin += dpToPx(2);
                eventWidth -= dpToPx(2);
            }
            
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    eventWidth,
                    dpToPx(heightDp)
            );
            params.topMargin = dpToPx(topMarginDp);
            params.leftMargin = leftMargin;
            
            eventContainer.setLayoutParams(params);
            eventContainer.setOnClickListener(v -> showEventDetail(event));
            
            mEventsOverlayContainer.addView(eventContainer);
        }
        
        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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
