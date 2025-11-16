package com.test.lifehub.features.four_calendar.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarActivity extends AppCompatActivity {

    private static final int VIEW_WEEK = 0;
    private static final int VIEW_MONTH = 1;

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private RecyclerView mRecyclerView;
    private TextView mTvCurrentDate;
    private MaterialButton mBtnPrevious, mBtnNext, mBtnToday;
    private FloatingActionButton mFab;
    private TextView mEmptyView;

    private CalendarViewModel mViewModel;
    private WeekViewAdapter mWeekAdapter;
    private MonthViewAdapter mMonthAdapter;

    private Calendar mCurrentCalendar = Calendar.getInstance();
    private int mCurrentView = VIEW_WEEK;

    private List<CalendarEvent> mAllEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        mToolbar = findViewById(R.id.toolbar_calendar);
        mTabLayout = findViewById(R.id.tab_layout_calendar);
        mRecyclerView = findViewById(R.id.recycler_view_calendar);
        mTvCurrentDate = findViewById(R.id.tv_current_date);
        mBtnPrevious = findViewById(R.id.btn_previous);
        mBtnNext = findViewById(R.id.btn_next);
        mBtnToday = findViewById(R.id.btn_today);
        mFab = findViewById(R.id.fab_add_event);
        mEmptyView = findViewById(R.id.empty_view_calendar);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Lịch");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        setupTabLayout();
        setupRecyclerView();
        setupButtons();

        updateDateDisplay();
        loadEventsForCurrentRange();

        mViewModel.getAllEvents().observe(this, events -> {
            mAllEvents = events != null ? events : new ArrayList<>();
            filterAndDisplayEvents();
        });
    }

    private void setupTabLayout() {
        mTabLayout.addTab(mTabLayout.newTab().setText("Tuần"));
        mTabLayout.addTab(mTabLayout.newTab().setText("Tháng"));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mCurrentView = tab.getPosition();
                switchView();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mWeekAdapter = new WeekViewAdapter(this, event -> {
            // Click event
            showEventDetailDialog(event);
        });

        mMonthAdapter = new MonthViewAdapter(this, date -> {
            // Click date in month view
            showEventsForDate(date);
        });

        mRecyclerView.setAdapter(mWeekAdapter);
    }

    private void setupButtons() {
        mBtnPrevious.setOnClickListener(v -> {
            if (mCurrentView == VIEW_WEEK) {
                mCurrentCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            } else {
                mCurrentCalendar.add(Calendar.MONTH, -1);
            }
            updateDateDisplay();
            loadEventsForCurrentRange();
        });

        mBtnNext.setOnClickListener(v -> {
            if (mCurrentView == VIEW_WEEK) {
                mCurrentCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                mCurrentCalendar.add(Calendar.MONTH, 1);
            }
            updateDateDisplay();
            loadEventsForCurrentRange();
        });

        mBtnToday.setOnClickListener(v -> {
            mCurrentCalendar = Calendar.getInstance();
            updateDateDisplay();
            loadEventsForCurrentRange();
        });

        mFab.setOnClickListener(v -> {
            AddEditEventDialog dialog = AddEditEventDialog.newInstance(null);
            dialog.show(getSupportFragmentManager(), "AddEventDialog");
        });
    }

    private void switchView() {
        if (mCurrentView == VIEW_WEEK) {
            mRecyclerView.setAdapter(mWeekAdapter);
        } else {
            mRecyclerView.setAdapter(mMonthAdapter);
        }
        updateDateDisplay();
        loadEventsForCurrentRange();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf;
        if (mCurrentView == VIEW_WEEK) {
            Calendar weekStart = (Calendar) mCurrentCalendar.clone();
            weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Calendar weekEnd = (Calendar) weekStart.clone();
            weekEnd.add(Calendar.DAY_OF_WEEK, 6);

            sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            String text = sdf.format(weekStart.getTime()) + " - " + sdf.format(weekEnd.getTime());
            mTvCurrentDate.setText(text);
        } else {
            sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            mTvCurrentDate.setText(sdf.format(mCurrentCalendar.getTime()));
        }
    }

    private void loadEventsForCurrentRange() {
        Date startDate, endDate;

        if (mCurrentView == VIEW_WEEK) {
            Calendar weekStart = (Calendar) mCurrentCalendar.clone();
            weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            weekStart.set(Calendar.HOUR_OF_DAY, 0);
            weekStart.set(Calendar.MINUTE, 0);
            weekStart.set(Calendar.SECOND, 0);

            Calendar weekEnd = (Calendar) weekStart.clone();
            weekEnd.add(Calendar.DAY_OF_WEEK, 7);

            startDate = weekStart.getTime();
            endDate = weekEnd.getTime();
        } else {
            Calendar monthStart = (Calendar) mCurrentCalendar.clone();
            monthStart.set(Calendar.DAY_OF_MONTH, 1);
            monthStart.set(Calendar.HOUR_OF_DAY, 0);
            monthStart.set(Calendar.MINUTE, 0);
            monthStart.set(Calendar.SECOND, 0);

            Calendar monthEnd = (Calendar) monthStart.clone();
            monthEnd.add(Calendar.MONTH, 1);

            startDate = monthStart.getTime();
            endDate = monthEnd.getTime();
        }

        mViewModel.setDateRange(startDate, endDate);
        filterAndDisplayEvents();
    }

    private void filterAndDisplayEvents() {
        if (mCurrentView == VIEW_WEEK) {
            List<WeekDayData> weekData = generateWeekData();
            mWeekAdapter.submitList(weekData);
        } else {
            List<MonthDayData> monthData = generateMonthData();
            mMonthAdapter.submitList(monthData);
        }

        updateEmptyView();
    }

    private List<WeekDayData> generateWeekData() {
        List<WeekDayData> weekData = new ArrayList<>();
        Calendar weekStart = (Calendar) mCurrentCalendar.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        for (int i = 0; i < 7; i++) {
            Calendar dayCalendar = (Calendar) weekStart.clone();
            dayCalendar.add(Calendar.DAY_OF_WEEK, i);

            List<CalendarEvent> dayEvents = getEventsForDate(dayCalendar.getTime());
            weekData.add(new WeekDayData(dayCalendar.getTime(), dayEvents));
        }

        return weekData;
    }

    private List<MonthDayData> generateMonthData() {
        List<MonthDayData> monthData = new ArrayList<>();

        Calendar monthStart = (Calendar) mCurrentCalendar.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);

        Calendar displayStart = (Calendar) monthStart.clone();
        displayStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        int weeksToShow = 6;

        for (int i = 0; i < weeksToShow * 7; i++) {
            Calendar dayCalendar = (Calendar) displayStart.clone();
            dayCalendar.add(Calendar.DAY_OF_MONTH, i);

            boolean isCurrentMonth = dayCalendar.get(Calendar.MONTH) == mCurrentCalendar.get(Calendar.MONTH);
            List<CalendarEvent> dayEvents = getEventsForDate(dayCalendar.getTime());

            monthData.add(new MonthDayData(dayCalendar.getTime(), dayEvents, isCurrentMonth));
        }

        return monthData;
    }

    private List<CalendarEvent> getEventsForDate(Date date) {
        List<CalendarEvent> dayEvents = new ArrayList<>();
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(date);

        for (CalendarEvent event : mAllEvents) {
            if (event.getStartTime() == null) continue;

            Calendar eventCal = Calendar.getInstance();
            eventCal.setTime(event.getStartTime());

            if (eventCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    eventCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)) {
                dayEvents.add(event);
            }
        }

        return dayEvents;
    }

    private void updateEmptyView() {
        boolean isEmpty = mAllEvents.isEmpty();
        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showEventDetailDialog(CalendarEvent event) {
        AddEditEventDialog dialog = AddEditEventDialog.newInstance(event);
        dialog.show(getSupportFragmentManager(), "EventDetailDialog");
    }

    private void showEventsForDate(Date date) {
        List<CalendarEvent> events = getEventsForDate(date);
        DayEventsDialog dialog = DayEventsDialog.newInstance(date, (ArrayList<CalendarEvent>) events);
        dialog.show(getSupportFragmentManager(), "DayEventsDialog");
    }
}