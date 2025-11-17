package com.test.lifehub.features.four_calendar.ui;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MonthDayData {
    public Date date;
    public List<CalendarEvent> events = new ArrayList<>();
    public boolean isCurrentMonth;
    public String holidayName = null;
}