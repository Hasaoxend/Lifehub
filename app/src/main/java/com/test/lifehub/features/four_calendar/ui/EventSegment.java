package com.test.lifehub.features.four_calendar.ui;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.util.Calendar;

public class EventSegment {
    public CalendarEvent originalEvent;
    public Calendar segmentStart;
    public Calendar segmentEnd;

    public int layoutColumn = 0;
    public int totalLayoutColumns = 1;

    public EventSegment(CalendarEvent originalEvent, Calendar segmentStart, Calendar segmentEnd) {
        this.originalEvent = originalEvent;
        this.segmentStart = segmentStart;
        this.segmentEnd = segmentEnd;
    }

    public boolean overlaps(EventSegment other) {
        return this.segmentStart.before(other.segmentEnd) &&
                this.segmentEnd.after(other.segmentStart);
    }
}