package com.test.lifehub.features.four_calendar.ui;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.util.Date;
import java.util.List;

/**
 * Data holder cho 1 ngày trong Month View
 */
class MonthDayData {
    public Date date;
    public List<CalendarEvent> events;
    public boolean isCurrentMonth;

    public MonthDayData(Date date, List<CalendarEvent> events, boolean isCurrentMonth) {
        this.date = date;
        this.events = events;
        this.isCurrentMonth = isCurrentMonth;
    }
}