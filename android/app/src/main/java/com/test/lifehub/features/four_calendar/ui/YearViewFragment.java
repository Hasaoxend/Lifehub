package com.test.lifehub.features.four_calendar.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * YEAR VIEW - Giao diện hiển thị cả năm (iOS style)
 * 
 * Chức năng:
 * - Hiển thị 12 tháng trong năm dạng grid (3 cột x 4 hàng)
 * - Mỗi tháng hiển thị lưới mini với dấu chấm cho ngày có sự kiện
 * - Vuốt trái/phải để chuyển năm
 * - Click vào tháng → Zoom vào Month View
 * - Pinch zoom từ Month View
 */
@AndroidEntryPoint
public class YearViewFragment extends Fragment {

    private RecyclerView mYearRecyclerView;
    private YearMonthAdapter mYearAdapter;
    private CalendarViewModel mViewModel;
    private Calendar mCurrentYear;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_year_view, container, false);
        mYearRecyclerView = view.findViewById(R.id.recycler_year_grid);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
        mCurrentYear = Calendar.getInstance();

        setupRecyclerView();
        observeEvents();
        updateYearTitle();
    }

    private void setupRecyclerView() {
        // Grid 3 cột (3 tháng mỗi hàng)
        mYearRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        mYearAdapter = new YearMonthAdapter(requireContext(), month -> {
            // Click vào tháng → Chuyển sang Month View
            if (getActivity() instanceof CalendarActivity) {
                ((CalendarActivity) getActivity()).navigateToMonth(month);
            }
        });

        mYearRecyclerView.setAdapter(mYearAdapter);
        loadYear();
    }

    private void observeEvents() {
        mViewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                loadYear(); // Reload để cập nhật dấu chấm sự kiện
            }
        });
    }

    private void loadYear() {
        List<YearMonthData> months = generate12Months();
        mYearAdapter.submitList(months);
    }

    /**
     * Tạo danh sách 12 tháng trong năm
     */
    private List<YearMonthData> generate12Months() {
        List<YearMonthData> months = new ArrayList<>();
        List<CalendarEvent> allEvents = mViewModel.getAllEvents().getValue();
        if (allEvents == null) allEvents = new ArrayList<>();

        Calendar cal = (Calendar) mCurrentYear.clone();
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        for (int month = 0; month < 12; month++) {
            YearMonthData monthData = new YearMonthData();
            monthData.monthCalendar = (Calendar) cal.clone();
            monthData.monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.getTime());
            monthData.eventsInMonth = getEventsForMonth(allEvents, cal);
            months.add(monthData);
            cal.add(Calendar.MONTH, 1);
        }

        return months;
    }

    private List<CalendarEvent> getEventsForMonth(List<CalendarEvent> allEvents, Calendar month) {
        List<CalendarEvent> monthEvents = new ArrayList<>();
        int targetMonth = month.get(Calendar.MONTH);
        int targetYear = month.get(Calendar.YEAR);

        for (CalendarEvent event : allEvents) {
            if (event.getStartTime() == null) continue;
            Calendar eventCal = Calendar.getInstance();
            eventCal.setTime(event.getStartTime());
            if (eventCal.get(Calendar.MONTH) == targetMonth && eventCal.get(Calendar.YEAR) == targetYear) {
                monthEvents.add(event);
            }
        }
        return monthEvents;
    }

    private void updateYearTitle() {
        if (getActivity() instanceof CalendarActivity) {
            String title = String.valueOf(mCurrentYear.get(Calendar.YEAR));
            ((CalendarActivity) requireActivity()).setCurrentDateTitle(title);
        }
    }

    public void nextYear() {
        mCurrentYear.add(Calendar.YEAR, 1);
        loadYear();
        updateYearTitle();
    }

    public void previousYear() {
        mCurrentYear.add(Calendar.YEAR, -1);
        loadYear();
        updateYearTitle();
    }

    public void scrollToCurrentYear() {
        mCurrentYear = Calendar.getInstance();
        loadYear();
        updateYearTitle();
    }
}
