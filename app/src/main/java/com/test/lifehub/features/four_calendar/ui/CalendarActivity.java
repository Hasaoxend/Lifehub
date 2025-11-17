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

    private static final int VIEW_WEEK = 0;
    private static final int VIEW_MONTH = 1;

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private FloatingActionButton mFab; // Nút FAB
    private MaterialButton mBtnPrevious, mBtnNext, mBtnToday;
    private TextView mTvCurrentDate;

    private CalendarViewModel mViewModel;
    private int mCurrentView = VIEW_WEEK;

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
        getSupportActionBar().setTitle("Lịch");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mViewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        setupTabLayout();
        setupFab(); // *** ĐẢM BẢO GỌI HÀM NÀY ***
        setupNavigationButtons();

        if (savedInstanceState == null) {
            loadFragment(new WeekViewFragment());
        }
    }

    public void setCurrentDateTitle(String title) {
        if (mTvCurrentDate != null) {
            mTvCurrentDate.setText(title);
        }
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

    // *** HÀM SỬA LỖI CHO FAB ***
    private void setupFab() {
        mFab.setOnClickListener(v -> {
            // (Giả sử bạn có AddEditEventDialog.java)
            AddEditEventDialog dialog = AddEditEventDialog.newInstance(null);
            dialog.show(getSupportFragmentManager(), "AddEventDialog");
        });
    }

    private void setupNavigationButtons() {
        mBtnToday.setOnClickListener(v -> scrollToToday());

        mBtnPrevious.setOnClickListener(v -> {
            Fragment current = getCurrentFragment();
            if (current instanceof WeekViewFragment) {
                ((WeekViewFragment) current).previousWeek();
            } else if (current instanceof MonthViewFragment) {
                ((MonthViewFragment) current).previousMonth();
            }
        });

        mBtnNext.setOnClickListener(v -> {
            Fragment current = getCurrentFragment();
            if (current instanceof WeekViewFragment) {
                ((WeekViewFragment) current).nextWeek();
            } else if (current instanceof MonthViewFragment) {
                ((MonthViewFragment) current).nextMonth();
            }
        });
    }

    private Fragment getCurrentFragment() {
        return getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container_calendar);
    }

    private void scrollToToday() {
        Fragment current = getCurrentFragment();
        if (current instanceof WeekViewFragment) {
            ((WeekViewFragment) current).scrollToToday();
        } else if (current instanceof MonthViewFragment) {
            ((MonthViewFragment) current).scrollToToday();
        }
    }

    private void switchView() {
        Fragment fragment = (mCurrentView == VIEW_WEEK)
                ? new WeekViewFragment()
                : new MonthViewFragment();
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