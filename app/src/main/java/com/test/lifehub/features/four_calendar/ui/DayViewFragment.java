package com.test.lifehub.features.four_calendar.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.test.lifehub.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * DAY VIEW - Giao diện hiển thị chi tiết 1 ngày (iOS style)
 * 
 * Chức năng chính:
 * - Header 7 nút ngày (Mon-Sun) để chọn ngày trong tuần
 * - Timeline 00:00-23:00 bên trái
 * - Vạch đỏ current time indicator (update real-time)
 * - ViewPager2 để swipe trái/phải đổi ngày
 * - Tự động chuyển tuần khi swipe hết Chủ Nhật → Thứ 2 tuần sau
 * - Click vào time slot → Tạo sự kiện
 * - Click vào sự kiện → Xem/Sửa
 */
@AndroidEntryPoint
public class DayViewFragment extends Fragment {

    private TabLayout mWeekDaysTabLayout;
    private ViewPager2 mDayPager;
    private DayPagerAdapter mPagerAdapter;
    private CalendarViewModel mViewModel;
    
    private Calendar mCurrentWeekStart; // Thứ 2 của tuần hiện tại
    private int mCurrentDayIndex = 0;   // 0-6 (Mon-Sun)
    
    private Handler mTimeUpdateHandler;
    private Runnable mTimeUpdateRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_view, container, false);
        
        mWeekDaysTabLayout = view.findViewById(R.id.tab_week_days);
        mDayPager = view.findViewById(R.id.pager_days);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(requireActivity()).get(CalendarViewModel.class);
        
        // Khởi tạo tuần hiện tại (bắt đầu từ thứ 2)
        mCurrentWeekStart = Calendar.getInstance();
        mCurrentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        mCurrentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        mCurrentWeekStart.clear(Calendar.MINUTE);
        mCurrentWeekStart.clear(Calendar.SECOND);
        mCurrentWeekStart.clear(Calendar.MILLISECOND);
        
        // Tính index ngày hiện tại trong tuần
        Calendar today = Calendar.getInstance();
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        mCurrentDayIndex = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;

        setupViewPager();
        setupWeekDaysTabs();
        updateDayTitle();
        
        // Start real-time clock update
        startTimeUpdates();
    }

    private void setupViewPager() {
        mPagerAdapter = new DayPagerAdapter(this, mCurrentWeekStart, mViewModel);
        mDayPager.setAdapter(mPagerAdapter);
        mDayPager.setCurrentItem(mCurrentDayIndex, false);
        mDayPager.setOffscreenPageLimit(1);

        mDayPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mCurrentDayIndex = position;
                
                // Nếu swipe qua Chủ Nhật (position 6) → Chuyển tuần tiếp theo
                if (position > 6) {
                    nextWeek();
                    mDayPager.setCurrentItem(0, false);
                }
                // Nếu swipe qua Thứ 2 (position < 0) → Chuyển tuần trước
                else if (position < 0) {
                    previousWeek();
                    mDayPager.setCurrentItem(6, false);
                }
                
                updateDayTitle();
                mWeekDaysTabLayout.selectTab(mWeekDaysTabLayout.getTabAt(position), true);
            }
        });
    }

    private void setupWeekDaysTabs() {
        mWeekDaysTabLayout.removeAllTabs();
        
        // Sử dụng string resources cho tên ngày
        String[] dayNames = {
            getString(R.string.day_mon),
            getString(R.string.day_tue),
            getString(R.string.day_wed),
            getString(R.string.day_thu),
            getString(R.string.day_fri),
            getString(R.string.day_sat),
            getString(R.string.day_sun)
        };
        Calendar cal = (Calendar) mCurrentWeekStart.clone();
        
        for (int i = 0; i < 7; i++) {
            TabLayout.Tab tab = mWeekDaysTabLayout.newTab();
            
            // Custom view cho tab
            View tabView = LayoutInflater.from(requireContext()).inflate(R.layout.tab_day_item, null);
            TextView tvDayName = tabView.findViewById(R.id.tv_day_name);
            TextView tvDayDate = tabView.findViewById(R.id.tv_day_date);
            View dotIndicator = tabView.findViewById(R.id.dot_indicator);
            
            tvDayName.setText(dayNames[i]);
            tvDayDate.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            
            // Kiểm tra nếu là ngày hôm nay
            Calendar today = Calendar.getInstance();
            boolean isToday = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                              cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
            
            // Lưu tag để xử lý sau
            tvDayDate.setTag(isToday ? "today" : "normal");
            
            // Style ban đầu - sẽ update khi select
            if (isToday) {
                // Ngày hôm nay khi chưa select: chữ đỏ
                tvDayDate.setTextColor(Color.RED);
                tvDayDate.setBackground(null);
            } else {
                tvDayDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                tvDayDate.setBackground(null);
            }
            
            // TODO: Hiển thị dấu chấm nếu ngày có sự kiện
            
            tab.setCustomView(tabView);
            mWeekDaysTabLayout.addTab(tab);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Sync tab với pager
        mWeekDaysTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    TextView tvDayDate = customView.findViewById(R.id.tv_day_date);
                    boolean isToday = "today".equals(tvDayDate.getTag());
                    
                    if (isToday) {
                        // Ngày hôm nay được chọn: viền đỏ, chữ trắng
                        tvDayDate.setBackgroundResource(R.drawable.today_circle_selected);
                        tvDayDate.setTextColor(Color.WHITE);
                    } else {
                        // Ngày khác được chọn: nền xanh, chữ trắng
                        tvDayDate.setBackgroundResource(R.drawable.circle_primary);
                        tvDayDate.setTextColor(Color.WHITE);
                    }
                }
                mDayPager.setCurrentItem(tab.getPosition(), true);
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                View customView = tab.getCustomView();
                if (customView != null) {
                    TextView tvDayDate = customView.findViewById(R.id.tv_day_date);
                    boolean isToday = "today".equals(tvDayDate.getTag());
                    
                    if (isToday) {
                        // Ngày hôm nay khi không được chọn: chữ đỏ, không background
                        tvDayDate.setBackground(null);
                        tvDayDate.setTextColor(Color.RED);
                    } else {
                        // Ngày khác khi không được chọn: màu mặc định
                        tvDayDate.setBackground(null);
                        tvDayDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                    }
                }
            }
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        mWeekDaysTabLayout.selectTab(mWeekDaysTabLayout.getTabAt(mCurrentDayIndex));
    }

    private void updateDayTitle() {
        if (getActivity() instanceof CalendarActivity) {
            Calendar selectedDay = (Calendar) mCurrentWeekStart.clone();
            selectedDay.add(Calendar.DAY_OF_MONTH, mCurrentDayIndex);
            
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
            String title = sdf.format(selectedDay.getTime());
            ((CalendarActivity) requireActivity()).setCurrentDateTitle(title);
        }
    }

    /**
     * Start updating current time indicator mỗi phút
     */
    private void startTimeUpdates() {
        mTimeUpdateHandler = new Handler(Looper.getMainLooper());
        mTimeUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Notify adapter to update current time line
                if (mPagerAdapter != null) {
                    mPagerAdapter.notifyTimeUpdate();
                }
                // Update every minute
                mTimeUpdateHandler.postDelayed(this, 60000);
            }
        };
        mTimeUpdateHandler.post(mTimeUpdateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTimeUpdateHandler != null && mTimeUpdateRunnable != null) {
            mTimeUpdateHandler.removeCallbacks(mTimeUpdateRunnable);
        }
    }

    public void nextWeek() {
        mCurrentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
        setupWeekDaysTabs();
        mPagerAdapter.updateWeek(mCurrentWeekStart);
        updateDayTitle();
    }

    public void previousWeek() {
        mCurrentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
        setupWeekDaysTabs();
        mPagerAdapter.updateWeek(mCurrentWeekStart);
        updateDayTitle();
    }

    public void scrollToToday() {
        mCurrentWeekStart = Calendar.getInstance();
        mCurrentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        
        Calendar today = Calendar.getInstance();
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK);
        mCurrentDayIndex = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        
        setupWeekDaysTabs();
        mPagerAdapter.updateWeek(mCurrentWeekStart);
        mDayPager.setCurrentItem(mCurrentDayIndex, true);
        updateDayTitle();
    }
}
