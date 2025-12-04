package com.test.lifehub.features.four_calendar.ui;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.util.Calendar;
import java.util.List;

/**
 * Data class cho Year View
 * Chứa thông tin 1 tháng trong năm
 */
public class YearMonthData {
    public Calendar monthCalendar;      // Tháng này (Calendar object)
    public String monthName;            // Tên tháng (VD: "December")
    public List<CalendarEvent> eventsInMonth; // Danh sách sự kiện trong tháng
}
