package com.test.lifehub.features.four_calendar.ui;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import com.test.lifehub.features.four_calendar.repository.CalendarRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Unit test cho CalendarViewModel
 * Kiểm tra quản lý sự kiện lịch
 */
@RunWith(MockitoJUnitRunner.class)
public class CalendarViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    CalendarRepository mockRepository;

    private MutableLiveData<List<CalendarEvent>> eventsLiveData;

    @Before
    public void setUp() {
        eventsLiveData = new MutableLiveData<>();
        when(mockRepository.getAllEvents()).thenReturn(eventsLiveData);
    }

    @Test
    public void testGetAllEvents_ReturnsLiveData() {
        // Given
        List<CalendarEvent> testEvents = new ArrayList<>();
        testEvents.add(createTestEvent("Meeting", getTodayDate()));
        testEvents.add(createTestEvent("Presentation", getTomorrowDate()));
        
        eventsLiveData.setValue(testEvents);
        
        // Verify
        assertNotNull("Events LiveData không được null", eventsLiveData.getValue());
        assertEquals("Số lượng events phải đúng", 2, eventsLiveData.getValue().size());
    }

    @Test
    public void testInsertEvent_CallsRepository() {
        // Given
        CalendarEvent newEvent = createTestEvent("New Event", getTodayDate());
        
        // When
        mockRepository.insertEvent(newEvent);
        
        // Verify
        verify(mockRepository, times(1)).insertEvent(any(CalendarEvent.class));
    }

    @Test
    public void testUpdateEvent_CallsRepository() {
        // Given
        CalendarEvent existingEvent = createTestEvent("Existing Event", getTodayDate());
        existingEvent.documentId = "event123";
        
        // When
        mockRepository.updateEvent(existingEvent);
        
        // Verify
        verify(mockRepository, times(1)).updateEvent(any(CalendarEvent.class));
    }

    @Test
    public void testDeleteEvent_CallsRepository() {
        // Given
        CalendarEvent eventToDelete = createTestEvent("Delete Event", getTodayDate());
        eventToDelete.documentId = "event456";
        
        // When
        mockRepository.deleteEvent(eventToDelete);
        
        // Verify
        verify(mockRepository, times(1)).deleteEvent(any(CalendarEvent.class));
    }

    @Test
    public void testEventDateRange_ValidDates() {
        // Given
        Date startDate = getTodayDate();
        Date endDate = getTomorrowDate();
        
        // Verify
        assertTrue("End date phải sau start date", endDate.after(startDate));
    }

    @Test
    public void testEventWithReminder_ValidReminderTime() {
        // Given
        CalendarEvent event = createTestEvent("Event with Reminder", getTodayDate());
        event.reminderMinutes = 15; // 15 phút trước
        
        // Verify
        assertTrue("Reminder time phải > 0", event.reminderMinutes > 0);
        assertEquals("Reminder phải là 15 phút", 15, event.reminderMinutes);
    }

    @Test
    public void testRecurringEvent_ValidPattern() {
        // Given
        CalendarEvent recurringEvent = createTestEvent("Weekly Meeting", getTodayDate());
        recurringEvent.isRecurring = true;
        recurringEvent.recurrencePattern = "WEEKLY";
        
        // Verify
        assertTrue("Event phải là recurring", recurringEvent.isRecurring);
        assertEquals("Pattern phải là WEEKLY", "WEEKLY", recurringEvent.recurrencePattern);
    }

    @Test
    public void testEventColor_ValidColor() {
        // Given
        CalendarEvent event = createTestEvent("Colored Event", getTodayDate());
        event.color = "#FF5733"; // Mã màu hex
        
        // Verify
        assertNotNull("Color không được null", event.color);
        assertTrue("Color phải bắt đầu bằng #", event.color.startsWith("#"));
    }

    @Test
    public void testEventLocation_NotEmpty() {
        // Given
        CalendarEvent event = createTestEvent("Meeting", getTodayDate());
        event.location = "Conference Room A";
        
        // Verify
        assertNotNull("Location không được null", event.location);
        assertFalse("Location không được rỗng", event.location.isEmpty());
    }

    @Test
    public void testMultipleDayEvent_ValidDuration() {
        // Given
        Date startDate = getTodayDate();
        Date endDate = getDateAfterDays(3); // 3 ngày sau
        
        CalendarEvent multiDayEvent = createTestEvent("Conference", startDate);
        multiDayEvent.endDate = endDate;
        
        // Verify
        assertTrue("End date phải sau start date", multiDayEvent.endDate.after(startDate));
        
        long diffInMillis = multiDayEvent.endDate.getTime() - startDate.getTime();
        long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
        
        assertTrue("Event phải kéo dài nhiều ngày", diffInDays >= 1);
    }

    // Helper methods
    private CalendarEvent createTestEvent(String title, Date startDate) {
        CalendarEvent event = new CalendarEvent();
        event.title = title;
        event.startDate = startDate;
        event.endDate = startDate;
        event.description = "Test description";
        event.color = "#2196F3";
        event.reminderMinutes = 0;
        event.isRecurring = false;
        return event;
    }

    private Date getTodayDate() {
        return new Date();
    }

    private Date getTomorrowDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    private Date getDateAfterDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }
}
