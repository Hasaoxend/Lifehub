package com.test.lifehub.features.four_calendar.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WeekViewFragment extends Fragment {

    private LinearLayout mTimeAxisContainer;
    private LinearLayout mWeekDaysContainer;
    private ScrollView mScrollView;
    private CalendarViewModel mViewModel;
    private Calendar mCurrentWeek;

    private static final int HOUR_HEIGHT_DP = 60;
    private static final int HEADER_HEIGHT_DP = 40;
    private static final int DAY_COLUMN_WIDTH_DP = 120;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week_view_custom, container, false);

        mTimeAxisContainer = view.findViewById(R.id.time_axis_container);
        mWeekDaysContainer = view.findViewById(R.id.week_days_container);
        mScrollView = view.findViewById(R.id.main_scroll_view);
        mTimeAxisContainer.setPadding(0, dpToPx(HEADER_HEIGHT_DP), 0, 0);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
        mCurrentWeek = Calendar.getInstance();
        setupTimeAxis();
        setupWeekDays();
        observeEvents();
    }

    private void setupTimeAxis() {
        mTimeAxisContainer.removeAllViews();
        for (int hour = 0; hour < 24; hour++) {
            TextView hourLabel = new TextView(requireContext());
            hourLabel.setText(String.format(Locale.getDefault(), "%02d:00", hour));
            hourLabel.setTextSize(12);
            hourLabel.setTextColor(Color.GRAY);
            hourLabel.setPadding(dpToPx(8), 0, dpToPx(8), 0);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(HOUR_HEIGHT_DP));
            hourLabel.setLayoutParams(params);
            mTimeAxisContainer.addView(hourLabel);
        }
    }

    private void setupWeekDays() {
        mWeekDaysContainer.removeAllViews();
        Calendar cal = (Calendar) mCurrentWeek.clone();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        updateDateTitle(cal);
        for (int i = 0; i < 7; i++) {
            LinearLayout dayColumn = createDayColumn(cal);
            mWeekDaysContainer.addView(dayColumn);
            
            // Thêm đường kẻ dọc giữa các ngày (trừ ngày cuối)
            if (i < 6) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    dpToPx(1), 
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundResource(R.drawable.week_day_divider);
                mWeekDaysContainer.addView(divider);
            }
            
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void updateDateTitle(Calendar startOfWeek) {
        if (getActivity() == null || !(getActivity() instanceof CalendarActivity)) return;
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);
        SimpleDateFormat sdf;
        String title;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String yearFormat = (startOfWeek.get(Calendar.YEAR) == currentYear) ? "" : " yyyy";
        if (startOfWeek.get(Calendar.MONTH) == endOfWeek.get(Calendar.MONTH)) {
            sdf = new SimpleDateFormat("d", Locale.getDefault());
            String start = sdf.format(startOfWeek.getTime());
            sdf = new SimpleDateFormat("d MMM" + yearFormat, Locale.getDefault());
            String end = sdf.format(endOfWeek.getTime());
            title = start + " - " + end;
        } else {
            sdf = new SimpleDateFormat("d MMM", Locale.getDefault());
            String start = sdf.format(startOfWeek.getTime());
            sdf = new SimpleDateFormat("d MMM" + yearFormat, Locale.getDefault());
            String end = sdf.format(endOfWeek.getTime());
            title = start + " - " + end;
        }
        ((CalendarActivity) requireActivity()).setCurrentDateTitle(title);
    }

    private LinearLayout createDayColumn(Calendar day) {
        LinearLayout column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setLayoutParams(new LinearLayout.LayoutParams(
                dpToPx(DAY_COLUMN_WIDTH_DP),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView header = new TextView(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("EEE\ndd", Locale.getDefault());
        header.setText(sdf.format(day.getTime()));
        header.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        header.setPadding(0, dpToPx(8), 0, dpToPx(8));
        header.setTextSize(12);
        header.setHeight(dpToPx(HEADER_HEIGHT_DP));
        Calendar today = Calendar.getInstance();
        if (day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            header.setBackgroundColor(Color.parseColor("#FFF9C4"));
            header.setTextColor(Color.parseColor("#FF6F00"));
        }
        column.addView(header);

        FrameLayout contentFrame = new FrameLayout(requireContext());
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout slotsContainer = new LinearLayout(requireContext());
        slotsContainer.setOrientation(LinearLayout.VERTICAL);
        for (int hour = 0; hour < 24; hour++) {
            View slot = createTimeSlot(day, hour);
            slotsContainer.addView(slot);
        }
        contentFrame.addView(slotsContainer);

        FrameLayout eventsContainer = new FrameLayout(requireContext());
        eventsContainer.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        eventsContainer.setId(R.id.events_container);
        eventsContainer.setPadding(dpToPx(2), 0, dpToPx(2), 0);
        contentFrame.addView(eventsContainer);

        column.addView(contentFrame);
        column.setTag(day.getTimeInMillis());
        return column;
    }

    private View createTimeSlot(Calendar day, int hour) {
        View slot = new View(requireContext());
        slot.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(HOUR_HEIGHT_DP)));
        slot.setBackgroundResource(R.drawable.time_slot_background);
        slot.setOnClickListener(v -> {
            Calendar eventTime = (Calendar) day.clone();
            eventTime.set(Calendar.HOUR_OF_DAY, hour);
            eventTime.set(Calendar.MINUTE, 0);
            showCreateEventDialog(eventTime);
        });
        return slot;
    }

    private void observeEvents() {
        mViewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            renderEvents(events);
        });
    }

    private void renderEvents(List<CalendarEvent> events) {
        if (events == null) events = new ArrayList<>();
        
        // ✅ SỬA LỖI: Sắp xếp events theo startTime (do Repository không còn orderBy)
        List<CalendarEvent> sortedEvents = new ArrayList<>(events);
        java.util.Collections.sort(sortedEvents, new java.util.Comparator<CalendarEvent>() {
            @Override
            public int compare(CalendarEvent e1, CalendarEvent e2) {
                if (e1.getStartTime() == null) return 1;
                if (e2.getStartTime() == null) return -1;
                return e1.getStartTime().compareTo(e2.getStartTime());
            }
        });

        // Clear events từ tất cả các cột ngày (bỏ qua dividers)
        for (int i = 0; i < 7; i++) {
            int actualIndex = i * 2; // Index thực tế khi có dividers
            if (actualIndex >= mWeekDaysContainer.getChildCount()) break;
            
            View dayColumn = mWeekDaysContainer.getChildAt(actualIndex);
            if (dayColumn == null) continue;
            FrameLayout eventsContainer = dayColumn.findViewById(R.id.events_container);
            if (eventsContainer != null) eventsContainer.removeAllViews();
        }

        Calendar weekStart = (Calendar) mCurrentWeek.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.clear(Calendar.MINUTE);
        weekStart.clear(Calendar.SECOND);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 7);

        // --- PASS 1: PHÂN ĐOẠN ---
        Map<Integer, List<EventSegment>> segmentsPerDay = new HashMap<>();
        for (CalendarEvent event : sortedEvents) { // ✅ Dùng sortedEvents
            Calendar eventStart = Calendar.getInstance();
            eventStart.setTime(event.getStartTime());
            Calendar eventEnd = Calendar.getInstance();
            eventEnd.setTime(event.getEndTime());
            if (eventEnd.before(weekStart) || eventStart.after(weekEnd)) continue;
            Calendar dayIterator = (Calendar) weekStart.clone();
            for (int i = 0; i < 7; i++) {
                Calendar dayStart = (Calendar) dayIterator.clone();
                Calendar dayEnd = (Calendar) dayIterator.clone();
                dayEnd.add(Calendar.DAY_OF_MONTH, 1);
                if (eventStart.before(dayEnd) && eventEnd.after(dayStart)) {
                    Calendar segmentStart = (Calendar) (eventStart.after(dayStart) ? eventStart.clone() : dayStart.clone());
                    Calendar segmentEnd = (Calendar) (eventEnd.before(dayEnd) ? eventEnd.clone() : dayEnd.clone());
                    EventSegment segment = new EventSegment(event, segmentStart, segmentEnd);
                    if (!segmentsPerDay.containsKey(i)) {
                        segmentsPerDay.put(i, new ArrayList<>());
                    }
                    segmentsPerDay.get(i).add(segment);
                }
                dayIterator.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        // --- PASS 2: TÍNH TOÁN LAYOUT ---
        for (int i = 0; i < 7; i++) {
            List<EventSegment> daySegments = segmentsPerDay.get(i);
            if (daySegments == null || daySegments.isEmpty()) continue;
            Collections.sort(daySegments, (s1, s2) -> s1.segmentStart.compareTo(s2.segmentStart));
            List<List<EventSegment>> visualColumns = new ArrayList<>();
            for (EventSegment segment : daySegments) {
                boolean placed = false;
                for (List<EventSegment> column : visualColumns) {
                    EventSegment lastSegmentInColumn = column.get(column.size() - 1);
                    if (!segment.overlaps(lastSegmentInColumn)) {
                        column.add(segment);
                        segment.layoutColumn = visualColumns.indexOf(column);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    List<EventSegment> newColumn = new ArrayList<>();
                    newColumn.add(segment);
                    segment.layoutColumn = visualColumns.size();
                    visualColumns.add(newColumn);
                }
            }
            int numColumns = visualColumns.size();
            for (EventSegment segment : daySegments) {
                segment.totalLayoutColumns = numColumns;
            }
        }

        // --- PASS 3: VẼ ---
        for (int i = 0; i < 7; i++) {
            List<EventSegment> daySegments = segmentsPerDay.get(i);
            if (daySegments == null) continue;
            for (EventSegment segment : daySegments) {
                renderEventSegment(segment, i);
            }
        }
    }

    private void renderEventSegment(EventSegment segment, int dayColumnIndex) {
        // Tính toán index thực tế khi có dividers
        // Với mỗi ngày: 0,2,4,6,8,10,12 (ngày 0 ở index 0, ngày 1 ở index 2,...)
        int actualIndex = dayColumnIndex * 2; // Nhân 2 vì có divider giữa các ngày
        
        if (actualIndex >= mWeekDaysContainer.getChildCount()) return;
        
        View dayColumn = mWeekDaysContainer.getChildAt(actualIndex);
        if (dayColumn == null) return;
        FrameLayout eventsContainer = dayColumn.findViewById(R.id.events_container);
        if (eventsContainer == null) return;

        TextView eventView = new TextView(requireContext());
        eventView.setText(segment.originalEvent.getTitle());
        eventView.setTextSize(10);
        eventView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));

        GradientDrawable eventBackground = new GradientDrawable();
        eventBackground.setShape(GradientDrawable.RECTANGLE);
        eventBackground.setStroke(dpToPx(1), Color.BLACK);

        int eventColor = generateColorFromEvent(segment.originalEvent);
        eventBackground.setColor(eventColor);

        eventView.setTextColor(isColorDark(eventColor) ? Color.WHITE : Color.BLACK);
        eventView.setBackground(eventBackground);

        int startHour = segment.segmentStart.get(Calendar.HOUR_OF_DAY);
        int startMinute = segment.segmentStart.get(Calendar.MINUTE);
        int topMarginDp = (startHour * 60) + startMinute;

        long durationMillis = segment.segmentEnd.getTimeInMillis() - segment.segmentStart.getTimeInMillis();
        int durationMinutes = (int) (durationMillis / 60000);

        if (durationMinutes <= 0 && segment.segmentEnd.get(Calendar.HOUR_OF_DAY) == 0 && segment.segmentEnd.get(Calendar.MINUTE) == 0
                && (segment.segmentEnd.get(Calendar.DAY_OF_YEAR) > segment.segmentStart.get(Calendar.DAY_OF_YEAR) || segment.segmentEnd.get(Calendar.YEAR) > segment.segmentStart.get(Calendar.YEAR))) {
            durationMinutes = (24 * 60) - topMarginDp;
        }
        if (durationMinutes < 15) durationMinutes = 15;
        int heightDp = durationMinutes;

        int totalWidthPx = dpToPx(DAY_COLUMN_WIDTH_DP) - eventsContainer.getPaddingLeft() - eventsContainer.getPaddingRight();
        int segmentWidthPx = totalWidthPx / segment.totalLayoutColumns;
        int segmentLeftMarginPx = (segmentWidthPx * segment.layoutColumn);

        if (segment.layoutColumn > 0) {
            segmentLeftMarginPx += dpToPx(1);
            segmentWidthPx -= dpToPx(1);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                segmentWidthPx,
                dpToPx(heightDp)
        );
        params.topMargin = dpToPx(topMarginDp);
        params.leftMargin = segmentLeftMarginPx;
        eventView.setLayoutParams(params);

        eventView.setOnClickListener(v -> showEventDetail(segment.originalEvent));
        eventsContainer.addView(eventView);
    }

    private int generateColorFromEvent(CalendarEvent event) {
        if (event.getTitle() == null || event.getTitle().isEmpty()) {
            return 0xFF2196F3;
        }
        int hash = event.getTitle().hashCode();
        float hue = (float) (Math.abs(hash) % 360);
        float saturation = 0.7f;
        float value = 0.9f;
        return Color.HSVToColor(new float[]{hue, saturation, value});
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private void showCreateEventDialog(Calendar time) {
        CalendarEvent newEvent = new CalendarEvent();
        newEvent.setStartTime(time.getTime());
        Calendar endTime = (Calendar) time.clone();
        endTime.add(Calendar.HOUR_OF_DAY, 1);
        newEvent.setEndTime(endTime.getTime());
        AddEditEventDialog dialog = AddEditEventDialog.newInstance(newEvent);
        dialog.show(getParentFragmentManager(), "AddEventDialog");
    }

    private void showEventDetail(CalendarEvent event) {
        AddEditEventDialog dialog = AddEditEventDialog.newInstance(event);
        dialog.show(getParentFragmentManager(), "EventDetailDialog");
    }

    public void nextWeek() {
        mCurrentWeek.add(Calendar.WEEK_OF_YEAR, 1);
        setupWeekDays();
        renderEvents(mViewModel.getAllEvents().getValue());
    }

    public void previousWeek() {
        mCurrentWeek.add(Calendar.WEEK_OF_YEAR, -1);
        setupWeekDays();
        renderEvents(mViewModel.getAllEvents().getValue());
    }

    public void scrollToToday() {
        mCurrentWeek = Calendar.getInstance();
        setupWeekDays();
        renderEvents(mViewModel.getAllEvents().getValue());

        if (mScrollView == null) return;
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int yScroll = dpToPx((currentHour * HOUR_HEIGHT_DP) + currentMinute);
        yScroll = yScroll - (mScrollView.getHeight() / 3);
        if (yScroll < 0) yScroll = 0;
        final int finalYScroll = yScroll;
        mScrollView.post(() -> mScrollView.smoothScrollTo(0, finalYScroll));
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return 0;
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}