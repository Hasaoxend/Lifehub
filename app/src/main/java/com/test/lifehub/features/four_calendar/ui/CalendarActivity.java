package com.test.lifehub.features.four_calendar.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.test.lifehub.R;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CalendarActivity extends AppCompatActivity {

    // View modes: Year → Month → Day (iOS style)
    private static final int VIEW_YEAR = 0;
    private static final int VIEW_MONTH = 1;
    private static final int VIEW_DAY = 2;

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private FloatingActionButton mFab;
    private MaterialButton mBtnPrevious, mBtnNext, mBtnToday;
    private TextView mTvCurrentDate;

    private CalendarViewModel mViewModel;
    private int mCurrentView = VIEW_MONTH; // Mặc định Month View (như iOS)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_google);

        mToolbar = findViewById(R.id.toolbar_calendar);
        mTabLayout = findViewById(R.id.tab_layout_calendar);
        mFab = findViewById(R.id.fab_add_event); // Tìm FAB
        mBtnPrevious = findViewById(R.id.btn_previous);
        mBtnNext = findViewById(R.id.btn_next);
        mBtnToday = findViewById(R.id.btn_today);
        mTvCurrentDate = findViewById(R.id.tv_current_date);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.calendar_title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        setupTabLayout();
        setupFab();
        setupNavigationButtons();

        if (savedInstanceState == null) {
            // Bắt đầu với Month View (như iOS)
            loadFragment(new MonthViewFragment());
        }
    }

    public void setCurrentDateTitle(String title) {
        if (mTvCurrentDate != null) {
            mTvCurrentDate.setText(title);
        }
    }

    private void setupTabLayout() {
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.calendar_year_view));
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.calendar_month_view));
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.calendar_day_view));

        // Chọn tab Month mặc định
        mTabLayout.selectTab(mTabLayout.getTabAt(VIEW_MONTH));

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

    // *** HÀM SỬA LỖI CHO FAB ***
    private void setupFab() {
        mFab.setOnClickListener(v -> {
            AddEditEventDialog dialog = AddEditEventDialog.newInstance(null);
            dialog.show(getSupportFragmentManager(), "AddEventDialog");
        });
    }

    private void setupNavigationButtons() {
        mBtnToday.setOnClickListener(v -> scrollToToday());

        mBtnPrevious.setOnClickListener(v -> {
            Fragment current = getCurrentFragment();
            if (current instanceof YearViewFragment) {
                ((YearViewFragment) current).previousYear();
            } else if (current instanceof MonthViewFragment) {
                ((MonthViewFragment) current).previousMonth();
            } else if (current instanceof DayViewFragment) {
                ((DayViewFragment) current).previousWeek();
            }
        });

        mBtnNext.setOnClickListener(v -> {
            Fragment current = getCurrentFragment();
            if (current instanceof YearViewFragment) {
                ((YearViewFragment) current).nextYear();
            } else if (current instanceof MonthViewFragment) {
                ((MonthViewFragment) current).nextMonth();
            } else if (current instanceof DayViewFragment) {
                ((DayViewFragment) current).nextWeek();
            }
        });
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container_calendar);
    }

    private void scrollToToday() {
        Fragment current = getCurrentFragment();
        if (current instanceof YearViewFragment) {
            ((YearViewFragment) current).scrollToCurrentYear();
        } else if (current instanceof MonthViewFragment) {
            ((MonthViewFragment) current).scrollToToday();
        } else if (current instanceof DayViewFragment) {
            ((DayViewFragment) current).scrollToToday();
        }
    }

    private void switchView() {
        Fragment fragment;
        switch (mCurrentView) {
            case VIEW_YEAR:
                fragment = new YearViewFragment();
                break;
            case VIEW_DAY:
                fragment = new DayViewFragment();
                break;
            case VIEW_MONTH:
            default:
                fragment = new MonthViewFragment();
                break;
        }
        loadFragment(fragment);
    }

    /**
     * Navigation từ Year View → Month View
     */
    public void navigateToMonth(java.util.Calendar month) {
        mCurrentView = VIEW_MONTH;
        mTabLayout.selectTab(mTabLayout.getTabAt(VIEW_MONTH));
        
        MonthViewFragment fragment = new MonthViewFragment();
        // TODO: Truyền tháng được chọn qua Bundle
        loadFragment(fragment);
    }

    /**
     * Navigation từ Month View → Day View
     */
    public void navigateToDay(java.util.Date day) {
        mCurrentView = VIEW_DAY;
        mTabLayout.selectTab(mTabLayout.getTabAt(VIEW_DAY));
        
        DayViewFragment fragment = new DayViewFragment();
        // TODO: Truyền ngày được chọn qua Bundle
        loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_calendar, fragment)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calendar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_today) {
            scrollToToday();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}