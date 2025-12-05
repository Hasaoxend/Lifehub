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

/**
 * Fragment hiển thị Lịch theo chế độ Tháng (Month View)
 * 
 * Chức năng:
 * - Hiển thị lưới 6x7 (42 ô) đại diện cho các ngày trong tháng
 * - Mỗi ô hiển thị:
 *   + Ngày dương lịch (to, in đậm)
 *   + Ngày âm lịch (nhỏ, bên dưới)
 *   + Tên ngày lễ (nếu có)
 *   + Danh sách sự kiện trong ngày (tối đa 2 sự kiện)
 * - Click vào một ngày để xem tất cả sự kiện chi tiết
 * - Hỗ trợ cả ngày lễ dương lịch và âm lịch Việt Nam
 * 
 * Cách hoạt động:
 * - Lắng nghe thay đổi sự kiện từ CalendarViewModel (LiveData)
 * - Mỗi khi có thay đổi: tính toán lại 42 ngày và cập nhật adapter
 * - generateMonthDays(): tạo danh sách 42 ngày (bao gồm ngày tháng trước/sau)
 * - getVietnameseHolidayName(): xác định ngày lễ (cả dương và âm lịch)
 */
@AndroidEntryPoint
public class MonthViewFragment extends Fragment {

    private static final String ARG_YEAR = "arg_year";
    private static final String ARG_MONTH = "arg_month";

    private RecyclerView mMonthRecyclerView; // RecyclerView hiển thị lưới tháng
    private MonthGridAdapter mMonthAdapter;   // Adapter quản lý 42 ô ngày
    private CalendarViewModel mViewModel;     // ViewModel chứa dữ liệu sự kiện
    private Calendar mCurrentMonth;           // Tháng đang hiển thị

    /**
     * Tạo MonthViewFragment với tháng cụ thể
     */
    public static MonthViewFragment newInstance(Calendar month) {
        MonthViewFragment fragment = new MonthViewFragment();
        if (month != null) {
            Bundle args = new Bundle();
            args.putInt(ARG_YEAR, month.get(Calendar.YEAR));
            args.putInt(ARG_MONTH, month.get(Calendar.MONTH));
            fragment.setArguments(args);
        }
        return fragment;
    }

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
        
        // Lấy tháng từ Bundle (nếu có)
        mCurrentMonth = Calendar.getInstance();
        if (getArguments() != null) {
            int year = getArguments().getInt(ARG_YEAR, -1);
            int month = getArguments().getInt(ARG_MONTH, -1);
            if (year != -1 && month != -1) {
                mCurrentMonth.set(Calendar.YEAR, year);
                mCurrentMonth.set(Calendar.MONTH, month);
                mCurrentMonth.set(Calendar.DAY_OF_MONTH, 1);
            }
        }

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
    /**
     * Tạo danh sách ngày để hiển thị trong lưới tháng
     * 
     * Quy tắc:
     * - Hiển thị tối đa 5 hàng x 7 cột = 35 ô
     * - Bắt đầu từ Chủ Nhật của tuần chứa ngày 1 của tháng
     * - Chỉ hiển thị ngày của tháng hiện tại (không hiển thị tháng trước/sau)
     * 
     * Mỗi ngày chứa:
     * - date: Ngày dương lịch
     * - isCurrentMonth: Luôn true (chỉ hiển thị ngày trong tháng)
     * - events: Danh sách sự kiện trong ngày
     * - holidayName: Tên ngày lễ (nếu có)
     * - lunarDate: Ngày âm lịch dạng "15/8"
     * 
     * @return Danh sách tối đa 35 MonthDayData
     */
    private List<MonthDayData> generateMonthDays() {
        List<MonthDayData> days = new ArrayList<>();
        List<CalendarEvent> allEvents = mViewModel.getAllEvents().getValue();
        if (allEvents == null) allEvents = new ArrayList<>();

        // Tính số ngày trong tháng
        Calendar cal = (Calendar) mCurrentMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Tính vị trí bắt đầu (0 = Chủ Nhật, 1 = Thứ 2, ...)
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        
        // Thêm các ô trống ở đầu
        for (int i = 0; i < firstDayOfWeek; i++) {
            MonthDayData emptyDay = new MonthDayData();
            emptyDay.date = new Date(0); // Empty date
            emptyDay.isCurrentMonth = false;
            emptyDay.events = new ArrayList<>();
            days.add(emptyDay);
        }
        
        // Thêm các ngày trong tháng
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            MonthDayData dayData = new MonthDayData();
            dayData.date = cal.getTime();
            dayData.isCurrentMonth = true;
            dayData.events = getEventsForDay(allEvents, cal);
            dayData.holidayName = getVietnameseHolidayName(cal);
            dayData.lunarDate = LunarCalendar.getLunarDateString(cal.getTime());
            days.add(dayData);
        }
        
        // Đảm bảo có đủ 5 hàng (35 ô)
        while (days.size() < 35) {
            MonthDayData emptyDay = new MonthDayData();
            emptyDay.date = new Date(0);
            emptyDay.isCurrentMonth = false;
            emptyDay.events = new ArrayList<>();
            days.add(emptyDay);
        }
        
        // Chỉ lấy 35 ô đầu tiên (5 hàng)
        return days.size() > 35 ? days.subList(0, 35) : days;
    }

    /**
     * Xác định tên ngày lễ Việt Nam cho một ngày cụ thể
     * 
     * Hỗ trợ 2 loại ngày lễ:
     * 
     * 1. Ngày lễ Dương lịch (Solar):
     *    - Tết Dương lịch (1/1)
     *    - Valentine (14/2)
     *    - Quốc tế Phụ nữ (8/3)
     *    - Ngày Chiến thắng (30/4)
     *    - Quốc tế Lao động (1/5)
     *    - Quốc tế Thiếu nhi (1/6)
     *    - Quốc khánh (2/9)
     *    - Ngày Phụ nữ VN (20/10)
     *    - Ngày Nhà giáo VN (20/11)
     *    - Giáng sinh (25/12)
     * 
     * 2. Ngày lễ Âm lịch (Lunar):
     *    - Tết Nguyên Đán (1-3/1 âm)
     *    - Tết Nguyên Tiêu (15/1 âm)
     *    - Tết Hàn Thực (3/3 âm)
     *    - Giỗ Tổ Hùng Vương (10/3 âm)
     *    - Phật Đản (15/4 âm)
     *    - Tết Đoan Ngọ (5/5 âm)
     *    - Vu Lan (15/7 âm)
     *    - Tết Trung Thu (15/8 âm)
     *    - Tết Trùng Cửu (9/9 âm)
     *    - Tết Ông Công Ông Táo (23/12 âm)
     *    - Giao Thừa (29-30/12 âm)
     * 
     * @param day Ngày cần kiểm tra
     * @return Tên ngày lễ, hoặc null nếu không phải ngày lễ
     */
    private String getVietnameseHolidayName(Calendar day) {
        int month = day.get(Calendar.MONTH); // 0-11 in Java Calendar
        int date = day.get(Calendar.DAY_OF_MONTH);
        
        // Các ngày lễ dương lịch
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
        
        // Các ngày lễ âm lịch - chuyển đổi ngày dương sang âm và so sánh
        LunarCalendar.LunarDate lunarDate = LunarCalendar.convertSolarToLunar(day.getTime());
        if (lunarDate != null && lunarDate.isValid) {
            int lunarDay = lunarDate.day;
            int lunarMonth = lunarDate.month;
            boolean isLeap = lunarDate.isLeapMonth;
            
            // Tết Nguyên Đán (1-3/1 âm)
            if (lunarMonth == 1 && !isLeap && lunarDay >= 1 && lunarDay <= 3) {
                if (lunarDay == 1) return "Tết Nguyên Đán";
                return "Tết (Mùng " + lunarDay + ")";
            }
            
            // Tết Nguyên Tiêu (15/1 âm)
            if (lunarMonth == 1 && !isLeap && lunarDay == 15) return "Tết Nguyên Tiêu";
            
            // Tết Hàn Thực (3/3 âm)
            if (lunarMonth == 3 && !isLeap && lunarDay == 3) return "Tết Hàn Thực";
            
            // Giỗ Tổ Hùng Vương (10/3 âm)
            if (lunarMonth == 3 && !isLeap && lunarDay == 10) return "Giỗ Tổ Hùng Vương";
            
            // Phật Đản (15/4 âm)
            if (lunarMonth == 4 && !isLeap && lunarDay == 15) return "Phật Đản";
            
            // Tết Đoan Ngọ (5/5 âm)
            if (lunarMonth == 5 && !isLeap && lunarDay == 5) return "Tết Đoan Ngọ";
            
            // Vu Lan (15/7 âm)
            if (lunarMonth == 7 && !isLeap && lunarDay == 15) return "Vu Lan";
            
            // Tết Trung Thu (15/8 âm)
            if (lunarMonth == 8 && !isLeap && lunarDay == 15) return "Tết Trung Thu";
            
            // Tết Trùng Cửu (9/9 âm)
            if (lunarMonth == 9 && !isLeap && lunarDay == 9) return "Tết Trùng Cửu";
            
            // Tết Ông Công Ông Táo (23/12 âm)
            if (lunarMonth == 12 && !isLeap && lunarDay == 23) return "Tết Ông Công Ông Táo";
            
            // Giao Thừa (29-30/12 âm - tùy tháng 12 có 29 hay 30 ngày)
            if (lunarMonth == 12 && !isLeap && (lunarDay == 29 || lunarDay == 30)) return "Giao Thừa";
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


    private void showEventsForDay(MonthDayData day) {
        // Mở DayEventsDialog với cả ngày lễ và sự kiện
        DayEventsDialog dialog = DayEventsDialog.newInstance(
                day.date,
                (ArrayList<CalendarEvent>) day.events,
                day.holidayName
        );
        dialog.show(getParentFragmentManager(), "DayEventsDialog");
    }


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