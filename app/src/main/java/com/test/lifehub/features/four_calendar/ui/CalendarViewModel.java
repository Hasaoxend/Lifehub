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

/**
 * CalendarViewModel - ViewModel cho CalendarFragment
 * 
 * === MỤC ĐÍCH ===
 * Quản lý UI state và expose LiveData cho màn hình Lịch.
 * Cung cấp 2 chế độ xem:
 * 1. Tất cả sự kiện (getAllEvents)
 * 2. Sự kiện trong khoảng thời gian (getEventsForRange)
 * 
 * === KIẾN TRÚC MVVM ===
 * ```
 * CalendarFragment (View)
 *        |
 *        | observe LiveData
 *        v
 * CalendarViewModel <- ĐÂY
 *        |
 *        | delegate CRUD
 *        v
 * CalendarRepository
 *        |
 *        v
 * Firestore
 * ```
 * 
 * === TÍNH NĂNG NỔI BẬT ===
 * 
 * 1. DATE RANGE FILTERING:
 *    - User chọn tháng trên CalendarView
 *    - Fragment gọi setDateRange(startDate, endDate)
 *    - ViewModel tự động lọc events trong khoảng thời gian
 * 
 *    Ví dụ:
 *    ```java
 *    // User chọn tháng 12/2024
 *    Date start = new Date(2024, 11, 1);  // 1/12/2024
 *    Date end = new Date(2024, 11, 31);   // 31/12/2024
 *    viewModel.setDateRange(start, end);
 *    
 *    // ViewModel tự động query Firestore:
 *    // WHERE startTime >= start AND endTime <= end
 *    ```
 * 
 * 2. TRANSFORMATIONS.SWITCHMAP PATTERN:
 *    - Khi dateRangeLiveData thay đổi -> switchMap tự động gọi repository
 *    - eventsForRange tự động cập nhật với events mới
 * 
 *    Flow:
 *    ```
 *    setDateRange(start, end)
 *         |
 *         v
 *    dateRangeLiveData.setValue([start, end])
 *         |
 *         v
 *    switchMap() triggers -> repository.getEventsForDateRange()
 *         |
 *         v
 *    eventsForRange.setValue(filteredEvents)
 *         |
 *         v
 *    Fragment observe và cập nhật UI
 *    ```
 * 
 * 3. CRUD OPERATIONS:
 *    - insertEvent(): Thêm sự kiện mới
 *    - updateEvent(): Sửa sự kiện hiện có
 *    - deleteEvent(): Xóa sự kiện
 *    - getEventById(): Lấy chi tiết 1 sự kiện (dùng cho Edit)
 * 
 * === LIVEDATA TYPES ===
 * 
 * 1. allEvents (LiveData<List<CalendarEvent>>):
 *    - Tất cả sự kiện của user
 *    - Dùng cho month view, list view
 *    - Tự động sync với Firestore realtime
 * 
 * 2. eventsForRange (LiveData<List<CalendarEvent>>):
 *    - Chỉ sự kiện trong khoảng thời gian
 *    - Dùng khi user zoom vào 1 tháng cụ thể
 *    - Giảm data transfer từ Firestore
 * 
 * 3. dateRangeLiveData (MutableLiveData<Date[]>):
 *    - Internal state chứa [startDate, endDate]
 *    - Trigger cho switchMap khi thay đổi
 *    - KHÔNG expose ra Fragment (private)
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * ```java
 * // Fragment
 * @AndroidEntryPoint
 * public class CalendarFragment extends Fragment {
 *     private CalendarViewModel viewModel;
 * 
 *     @Override
 *     public void onViewCreated(View view, Bundle savedInstanceState) {
 *         viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);
 * 
 *         // 1. Hiển thị tất cả events
 *         viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
 *             calendarView.setEvents(events);
 *         });
 * 
 *         // 2. Lọc theo tháng khi user thay đổi
 *         calendarView.setOnMonthChangeListener((year, month) -> {
 *             Date start = getFirstDayOfMonth(year, month);
 *             Date end = getLastDayOfMonth(year, month);
 *             viewModel.setDateRange(start, end);
 *         });
 * 
 *         viewModel.getEventsForRange().observe(getViewLifecycleOwner(), events -> {
 *             adapter.submitList(events);
 *         });
 * 
 *         // 3. Thêm event mới
 *         CalendarEvent newEvent = new CalendarEvent();
 *         newEvent.setTitle("Meeting");
 *         newEvent.setStartTime(new Date());
 *         viewModel.insertEvent(newEvent);
 *     }
 * }
 * ```
 * 
 * === SCOPE ===
 * @HiltViewModel:
 * - Inject CalendarRepository tự động
 * - Lifecycle gắn với Fragment
 * - Destroy khi Fragment destroy
 * 
 * === LƯU Ý QUAN TRỌNG ===
 * 1. switchMap() chỉ active khi Fragment observe eventsForRange
 * 2. Nếu không cần filter -> dùng getAllEvents() thay vì eventsForRange
 * 3. dateRangeLiveData PRIVATE - không expose ra Fragment
 * 4. ViewModel không giữ reference đến Context/View
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm search events theo keyword
 * TODO: Filter theo loại sự kiện (công việc, cá nhân, ...)
 * TODO: Hỗ trợ recurring events (sự kiện lặp lại)
 * TODO: Export events sang iCal format
 * FIXME: Xử lý timezone cho events cross-timezone
 * 
 * @see CalendarRepository Data source
 * @see CalendarFragment UI layer
 * @see CalendarEvent Data model
 */
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