package com.test.lifehub.features.four_calendar.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // Xóa nếu không dùng
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.features.four_calendar.utils.LunarCalendar;
import com.test.lifehub.R;
import com.test.lifehub.features.four_calendar.data.CalendarEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MonthViewFragment extends Fragment {

    private RecyclerView mMonthRecyclerView; // Đổi tên
    private MonthGridAdapter mMonthAdapter; // Đổi tên
    private CalendarViewModel mViewModel;
    private Calendar mCurrentMonth;

    // (Xóa các biến của Agenda)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Quay lại layout cũ (hoặc layout đã sửa ở bước 1)
        View view = inflater.inflate(R.layout.fragment_month_view, container, false);

        mMonthRecyclerView = view.findViewById(R.id.recycler_month_grid);

        // (Xóa các findView của Agenda)

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
        mCurrentMonth = Calendar.getInstance();

        setupRecyclerView(); // Đổi tên

        observeEvents();
        updateDateTitle();
    }

    private void setupRecyclerView() { // Đổi tên
        mMonthRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 7));

        // Khôi phục lại logic click cũ: Mở Dialog
        mMonthAdapter = new MonthGridAdapter(requireContext(), day -> {
            if (day.isCurrentMonth) {
                showEventsForDay(day);
            }
        });

        mMonthRecyclerView.setAdapter(mMonthAdapter);
        loadMonthGrid();
    }

    // (Xóa setupDayEventsRecyclerView và updateAgendaForDay)

    private void observeEvents() {
        mViewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            if (events != null) {
                loadMonthGrid();
            }
        });
    }

    private void loadMonthGrid() {
        List<MonthDayData> days = generateMonthDays();
        mMonthAdapter.submitList(days);
        // (Không cần cập nhật agenda nữa)
    }

    // (Các hàm generateMonthDays, getVietnameseHolidayName, getEventsForDay giữ nguyên)
    private List<MonthDayData> generateMonthDays() {
        List<MonthDayData> days = new ArrayList<>();
        List<CalendarEvent> allEvents = mViewModel.getAllEvents().getValue();
        if (allEvents == null) allEvents = new ArrayList<>();

        Calendar cal = (Calendar) mCurrentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        cal.add(Calendar.DAY_OF_MONTH, -firstDayOfWeek);

        for (int i = 0; i < 42; i++) {
            MonthDayData dayData = new MonthDayData();
            dayData.date = cal.getTime();
            dayData.isCurrentMonth = cal.get(Calendar.MONTH) == mCurrentMonth.get(Calendar.MONTH);
            dayData.events = getEventsForDay(allEvents, cal);
            dayData.holidayName = getVietnameseHolidayName(cal);
            dayData.lunarDate = LunarCalendar.getLunarDateString(cal.getTime());
            days.add(dayData);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private String getVietnameseHolidayName(Calendar day) {
        int month = day.get(Calendar.MONTH);
        int date = day.get(Calendar.DAY_OF_MONTH);
        
        // Solar calendar holidays
        if (month == Calendar.JANUARY && date == 1) return "Tết Dương lịch";
        if (month == Calendar.FEBRUARY && date == 14) return "Valentine";
        if (month == Calendar.MARCH && date == 8) return "Quốc tế Phụ nữ";
        if (month == Calendar.APRIL && date == 30) return "Ngày Chiến thắng";
        if (month == Calendar.MAY && date == 1) return "Quốc tế Lao động";
        if (month == Calendar.JUNE && date == 1) return "Quốc tế Thiếu nhi";
        if (month == Calendar.SEPTEMBER && date == 2) return "Quốc khánh";
        if (month == Calendar.OCTOBER && date == 20) return "Ngày Phụ nữ VN";
        if (month == Calendar.NOVEMBER && date == 20) return "Ngày Nhà giáo VN";
        if (month == Calendar.DECEMBER && date == 25) return "Giáng sinh";
        
        // Lunar calendar holidays
        LunarCalendar.LunarDate lunar = LunarCalendar.convertSolarToLunar(day.getTime());
        if (lunar.isValid) {
            // Tết Nguyên Đán (Lunar New Year) - 1/1 to 3/1 lunar
            if (lunar.month == 1 && lunar.day >= 1 && lunar.day <= 3) {
                if (lunar.day == 1) return "Tết Nguyên Đán";
                return "Tết (Mùng " + lunar.day + ")";
            }
            
            // Tết Nguyên Tiêu (Lantern Festival) - 15/1 lunar
            if (lunar.month == 1 && lunar.day == 15) return "Tết Nguyên Tiêu";
            
            // Tết Hàn Thực (Cold Food Festival) - 3/3 lunar
            if (lunar.month == 3 && lunar.day == 3) return "Tết Hàn Thực";
            
            // Giỗ Tổ Hùng Vương - 10/3 lunar
            if (lunar.month == 3 && lunar.day == 10) return "Giỗ Tổ Hùng Vương";
            
            // Phật Đản (Buddha's Birthday) - 15/4 lunar
            if (lunar.month == 4 && lunar.day == 15) return "Phật Đản";
            
            // Tết Đoan Ngọ (Dragon Boat Festival) - 5/5 lunar
            if (lunar.month == 5 && lunar.day == 5) return "Tết Đoan Ngọ";
            
            // Vu Lan (Ghost Festival) - 15/7 lunar
            if (lunar.month == 7 && lunar.day == 15) return "Vu Lan";
            
            // Tết Trung Thu (Mid-Autumn Festival) - 15/8 lunar
            if (lunar.month == 8 && lunar.day == 15) return "Tết Trung Thu";
            
            // Tết Trùng Cửu - 9/9 lunar
            if (lunar.month == 9 && lunar.day == 9) return "Tết Trùng Cửu";
            
            // Tết Đông Chí (Winter Solstice) - around 22/12 solar, but marking 23/11 lunar
            if (lunar.month == 11 && lunar.day == 23) return "Tết Ông Công Ông Táo";
            
            // Tết Âm lịch (Lunar New Year's Eve) - 30/12 lunar (or 29/12 in short month)
            if (lunar.month == 12 && (lunar.day == 30 || lunar.day == 29)) return "Giao Thừa";
        }
        
        return null;
    }

    private List<CalendarEvent> getEventsForDay(List<CalendarEvent> allEvents, Calendar day) {
        List<CalendarEvent> dayEvents = new ArrayList<>();
        Calendar dayStart = (Calendar) day.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0); dayStart.clear(Calendar.MINUTE); dayStart.clear(Calendar.SECOND);
        Calendar dayEnd = (Calendar) dayStart.clone();
        dayEnd.add(Calendar.DAY_OF_MONTH, 1);
        for (CalendarEvent event : allEvents) {
            Calendar eventStart = Calendar.getInstance();
            eventStart.setTime(event.getStartTime());
            Calendar eventEnd = Calendar.getInstance();
            eventEnd.setTime(event.getEndTime());
            if (eventStart.before(dayEnd) && eventEnd.after(dayStart)) {
                dayEvents.add(event);
            }
        }
        Collections.sort(dayEvents, (e1, e2) -> e1.getStartTime().compareTo(e2.getStartTime()));
        return dayEvents;
    }

    // KHÔI PHỤC LẠI HÀM NÀY
    private void showEventsForDay(MonthDayData day) {
        // Mở DayEventsDialog (bạn đã có file này)
        DayEventsDialog dialog = DayEventsDialog.newInstance(
                day.date,
                (ArrayList<CalendarEvent>) day.events
        );
        dialog.show(getParentFragmentManager(), "DayEventsDialog");
    }

    // (Các hàm điều hướng giữ nguyên)
    private void updateDateTitle() {
        if (getActivity() == null || !(getActivity() instanceof CalendarActivity)) return;
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String title = sdf.format(mCurrentMonth.getTime());
        ((CalendarActivity) requireActivity()).setCurrentDateTitle(title);
    }

    public void nextMonth() {
        mCurrentMonth.add(Calendar.MONTH, 1);
        loadMonthGrid();
        updateDateTitle();
    }

    public void previousMonth() {
        mCurrentMonth.add(Calendar.MONTH, -1);
        loadMonthGrid();
        updateDateTitle();
    }

    public void scrollToToday() {
        mCurrentMonth = Calendar.getInstance();
        loadMonthGrid();
        updateDateTitle();
        // (Bạn có thể thêm logic cuộn đến hôm nay nếu muốn)
    }

    // (Xóa các hàm helper của Agenda: findPositionInAdapter, findDayData)

    private boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}