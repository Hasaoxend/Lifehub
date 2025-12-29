package com.test.lifehub.features.four_calendar.ui;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.util.Date;
import java.util.List;

/**
 * Data holder cho 1 ngày trong Week View
 */
public class WeekDayData {
    public Date date;
    public List<CalendarEvent> events;

    public WeekDayData(Date date, List<CalendarEvent> events) {
        this.date = date;
        this.events = events;
    }
}