package com.test.lifehub.features.four_calendar.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import com.test.lifehub.features.four_calendar.repository.CalendarRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CalendarViewModel extends ViewModel {

    private final CalendarRepository mRepository;
    private final LiveData<List<CalendarEvent>> allEvents;

    private final MutableLiveData<Date[]> dateRangeLiveData = new MutableLiveData<>();
    private final LiveData<List<CalendarEvent>> eventsForRange;

    @Inject
    public CalendarViewModel(CalendarRepository repository) {
        this.mRepository = repository;
        allEvents = mRepository.getAllEvents();

        eventsForRange = Transformations.switchMap(dateRangeLiveData, dateRange -> {
            if (dateRange != null && dateRange.length == 2) {
                return mRepository.getEventsForDateRange(dateRange[0], dateRange[1]);
            }
            return new MutableLiveData<>();
        });
    }

    public LiveData<List<CalendarEvent>> getAllEvents() {
        return allEvents;
    }

    public LiveData<List<CalendarEvent>> getEventsForRange() {
        return eventsForRange;
    }

    public void setDateRange(Date startDate, Date endDate) {
        dateRangeLiveData.setValue(new Date[]{startDate, endDate});
    }

    public LiveData<CalendarEvent> getEventById(String documentId) {
        return mRepository.getEventById(documentId);
    }

    public void insertEvent(CalendarEvent event) {
        mRepository.insertEvent(event);
    }

    public void updateEvent(CalendarEvent event) {
        mRepository.updateEvent(event);
    }

    public void deleteEvent(CalendarEvent event) {
        mRepository.deleteEvent(event);
    }
}