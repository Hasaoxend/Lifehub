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
        
        // Sắp xếp events theo startTime
        List<CalendarEvent> sortedEvents = new ArrayList<>(events);
        Collections.sort(sortedEvents, (e1, e2) -> {
            if (e1.getStartTime() == null) return 1;
            if (e2.getStartTime() == null) return -1;
            return e1.getStartTime().compareTo(e2.getStartTime());
        });

        // Clear tất cả events cũ
        for (int i = 0; i < 7; i++) {
            int actualIndex = i * 2; // Có divider giữa các ngày
            if (actualIndex >= mWeekDaysContainer.getChildCount()) break;
            View dayColumn = mWeekDaysContainer.getChildAt(actualIndex);
            if (dayColumn == null) continue;
            FrameLayout eventsContainer = dayColumn.findViewById(R.id.events_container);
            if (eventsContainer != null) eventsContainer.removeAllViews();
        }

        // Xác định tuần hiện tại
        Calendar weekStart = (Calendar) mCurrentWeek.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 7);

        // === BƯỚC 1: PHÂN ĐOẠN SỰ KIỆN ===
        Map<Integer, List<EventSegment>> segmentsPerDay = new HashMap<>();
        
        for (CalendarEvent event : sortedEvents) {
            if (event.getStartTime() == null || event.getEndTime() == null) continue;
            
            Calendar eventStart = Calendar.getInstance();
            eventStart.setTime(event.getStartTime());
            Calendar eventEnd = Calendar.getInstance();
            eventEnd.setTime(event.getEndTime());
            
            // Bỏ qua events ngoài tuần này
            if (eventEnd.before(weekStart) || eventStart.after(weekEnd)) continue;
            
            // Duyệt qua 7 ngày trong tuần
            Calendar currentDay = (Calendar) weekStart.clone();
            for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
                // Xác định khoảng thời gian của ngày này
                Calendar dayStart = (Calendar) currentDay.clone();
                dayStart.set(Calendar.HOUR_OF_DAY, 0);
                dayStart.set(Calendar.MINUTE, 0);
                dayStart.set(Calendar.SECOND, 0);
                dayStart.set(Calendar.MILLISECOND, 0);
                
                Calendar dayEnd = (Calendar) dayStart.clone();
                dayEnd.add(Calendar.DAY_OF_MONTH, 1);
                
                // Kiểm tra event có giao với ngày này không
                if (eventStart.before(dayEnd) && eventEnd.after(dayStart)) {
                    // Tạo segment cho ngày này
                    Calendar segmentStart = eventStart.after(dayStart) ? (Calendar) eventStart.clone() : (Calendar) dayStart.clone();
                    Calendar segmentEnd = eventEnd.before(dayEnd) ? (Calendar) eventEnd.clone() : (Calendar) dayEnd.clone();
                    
                    EventSegment segment = new EventSegment(event, segmentStart, segmentEnd);
                    
                    if (!segmentsPerDay.containsKey(dayIndex)) {
                        segmentsPerDay.put(dayIndex, new ArrayList<>());
                    }
                    segmentsPerDay.get(dayIndex).add(segment);
                }
                
                currentDay.add(Calendar.DAY_OF_MONTH, 1);
            }
        }

        // === BƯỚC 2: TÍNH TOÁN LAYOUT ===
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            List<EventSegment> segments = segmentsPerDay.get(dayIndex);
            if (segments == null || segments.isEmpty()) continue;
            
            // Sắp xếp theo thời gian bắt đầu, sau đó theo thời gian kết thúc
            Collections.sort(segments, (s1, s2) -> {
                int startCompare = s1.segmentStart.compareTo(s2.segmentStart);
                if (startCompare != 0) return startCompare;
                return s1.segmentEnd.compareTo(s2.segmentEnd);
            });
            
            // Gán column cho từng segment (Greedy Algorithm)
            for (EventSegment segment : segments) {
                int column = 0;
                boolean columnFound = false;
                
                while (!columnFound) {
                    columnFound = true;
                    
                    // Kiểm tra xem column này có bị chiếm bởi segment nào overlap không
                    for (EventSegment other : segments) {
                        if (other == segment) continue;
                        if (other.layoutColumn != column) continue;
                        
                        // Kiểm tra overlap
                        if (segment.segmentStart.before(other.segmentEnd) && 
                            segment.segmentEnd.after(other.segmentStart)) {
                            // Column này bị chiếm, thử column tiếp theo
                            column++;
                            columnFound = false;
                            break;
                        }
                    }
                }
                
                segment.layoutColumn = column;
            }
            
            // Tìm số columns tối đa trong ngày
            int maxColumns = 1;
            for (EventSegment segment : segments) {
                maxColumns = Math.max(maxColumns, segment.layoutColumn + 1);
            }
            
            // Với mỗi segment, tìm tất cả segments overlap với nó
            // và tính totalColumns cho nhóm overlap đó
            for (EventSegment segment : segments) {
                int maxColumnInGroup = segment.layoutColumn + 1;
                
                for (EventSegment other : segments) {
                    if (other == segment) continue;
                    
                    // Kiểm tra overlap
                    if (segment.segmentStart.before(other.segmentEnd) && 
                        segment.segmentEnd.after(other.segmentStart)) {
                        maxColumnInGroup = Math.max(maxColumnInGroup, other.layoutColumn + 1);
                    }
                }
                
                segment.totalLayoutColumns = maxColumnInGroup;
            }
        }

        // === BƯỚC 3: VẼ SEGMENTS ===
        for (int dayIndex = 0; dayIndex < 7; dayIndex++) {
            List<EventSegment> segments = segmentsPerDay.get(dayIndex);
            if (segments == null) continue;
            
            for (EventSegment segment : segments) {
                renderEventSegment(segment, dayIndex);
            }
        }
    }

    private void renderEventSegment(EventSegment segment, int dayColumnIndex) {
        // Lấy eventsContainer của ngày này
        int actualIndex = dayColumnIndex * 2; // Nhân 2 vì có divider
        if (actualIndex >= mWeekDaysContainer.getChildCount()) return;
        
        View dayColumn = mWeekDaysContainer.getChildAt(actualIndex);
        if (dayColumn == null) return;
        
        FrameLayout eventsContainer = dayColumn.findViewById(R.id.events_container);
        if (eventsContainer == null) return;

        // Lấy thông tin event gốc
        Calendar eventStart = Calendar.getInstance();
        eventStart.setTime(segment.originalEvent.getStartTime());
        Calendar eventEnd = Calendar.getInstance();
        eventEnd.setTime(segment.originalEvent.getEndTime());
        
        // Kiểm tra sự kiện nhiều ngày
        boolean isMultiDay = !isSameDay(eventStart, eventEnd);
        boolean isFirstSegment = isSameDay(segment.segmentStart, eventStart);
        boolean isLastSegment = false;
        
        if (isMultiDay) {
            // Nếu segmentEnd là 00:00, lùi về ngày trước để so sánh
            Calendar compareDate = (Calendar) segment.segmentEnd.clone();
            if (segment.segmentEnd.get(Calendar.HOUR_OF_DAY) == 0 && 
                segment.segmentEnd.get(Calendar.MINUTE) == 0) {
                compareDate.add(Calendar.DAY_OF_MONTH, -1);
            }
            isLastSegment = !compareDate.before(eventEnd);
        }

        // Container cho event (chứa colorBar + background + text)
        FrameLayout eventContainer = new FrameLayout(requireContext());
        
        // Background bán trong suốt (iOS-style)
        View backgroundView = new View(requireContext());
        int eventColor = generateColorFromEvent(segment.originalEvent);
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
        View colorBar = new View(requireContext());
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
        TextView eventText = new TextView(requireContext());
        String text = segment.originalEvent.getTitle();
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
        eventText.setTextSize(10);
        eventText.setTextColor(isColorDark(eventColor) ? eventColor : Color.BLACK);
        eventText.setMaxLines(3);
        eventText.setEllipsize(android.text.TextUtils.TruncateAt.END);
        
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.leftMargin = dpToPx(10);
        textParams.topMargin = dpToPx(4);
        textParams.rightMargin = dpToPx(4);
        eventText.setLayoutParams(textParams);
        eventContainer.addView(eventText);

        // Tính vị trí và kích thước
        int startHour = segment.segmentStart.get(Calendar.HOUR_OF_DAY);
        int startMinute = segment.segmentStart.get(Calendar.MINUTE);
        int startMinutesFromMidnight = startHour * 60 + startMinute;
        
        // Top margin: phút từ 00:00 * tỉ lệ pixel/phút
        int topMarginDp = (startMinutesFromMidnight * HOUR_HEIGHT_DP) / 60;

        // Duration
        long durationMillis = segment.segmentEnd.getTimeInMillis() - segment.segmentStart.getTimeInMillis();
        int durationMinutes = (int) (durationMillis / 60000);
        
        // Xử lý segment kéo dài đến 00:00 ngày sau
        if (durationMinutes <= 0 && 
            segment.segmentEnd.get(Calendar.HOUR_OF_DAY) == 0 && 
            segment.segmentEnd.get(Calendar.MINUTE) == 0) {
            // Kéo dài từ segmentStart đến 24:00
            durationMinutes = (24 * 60) - startMinutesFromMidnight;
        }
        
        if (durationMinutes < 15) durationMinutes = 15; // Tối thiểu 15 phút
        
        // Height: duration * tỉ lệ pixel/phút
        int heightDp = (durationMinutes * HOUR_HEIGHT_DP) / 60;

        // Width và left margin dựa trên layout column
        int containerWidth = eventsContainer.getWidth();
        if (containerWidth == 0) {
            // Container chưa được measure, dùng giá trị tính toán
            containerWidth = dpToPx(DAY_COLUMN_WIDTH_DP) - 
                           eventsContainer.getPaddingLeft() - 
                           eventsContainer.getPaddingRight();
        }
        
        int eventWidth = containerWidth / segment.totalLayoutColumns;
        int leftMargin = eventWidth * segment.layoutColumn;
        
        // Thêm khoảng cách nhỏ giữa các columns
        if (segment.layoutColumn > 0) {
            leftMargin += dpToPx(2);
            eventWidth -= dpToPx(2);
        }

        // Set layout params
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(eventWidth, dpToPx(heightDp));
        params.topMargin = dpToPx(topMarginDp);
        params.leftMargin = leftMargin;
        
        eventContainer.setLayoutParams(params);
        eventContainer.setOnClickListener(v -> showEventDetail(segment.originalEvent));
        
        eventsContainer.addView(eventContainer);
    }
    
    /**
     * Kiểm tra 2 Calendar có cùng ngày không
     */
    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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