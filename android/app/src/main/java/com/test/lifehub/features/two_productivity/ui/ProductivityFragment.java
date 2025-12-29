package com.test.lifehub.features.two_productivity.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.test.lifehub.R;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.features.four_calendar.ui.CalendarActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * ProductivityFragment - Màn hình chính cho tính năng Năng suất
 * 
 * === MỤC ĐÍCH ===
 * Fragment này hiển thị dashboard với 6 cards cho các tính năng:
 * 1. Ghi chú (Notes)
 * 2. Công việc (Tasks/Todo)
 * 3. Mua sắm (Shopping list)
 * 4. Máy tính (Calculator)
 * 5. Thời tiết (Weather)
 * 6. Lịch (Calendar)
 * 
 * === KIẾN TRÚC ===
 * Fragment trong BottomNavigationView:
 * ```
 * MainActivity
 *   ├─ AccountFragment
 *   ├─ ProductivityFragment <- ĐÂY
 *   └─ SettingsFragment
 * ```
 * 
 * === NAVIGATION FLOW ===
 * Mỗi card navigate đến Activity tương ứng:
 * ```
 * ProductivityFragment
 *   ├─ Click "Ghi chú" -> NotesListActivity
 *   ├─ Click "Công việc" -> TaskListActivity (TASK_TYPE_GENERAL)
 *   ├─ Click "Mua sắm" -> TaskListActivity (TASK_TYPE_SHOPPING)
 *   ├─ Click "Máy tính" -> CalculatorActivity
 *   ├─ Click "Thời tiết" -> WeatherActivity
 *   └─ Click "Lịch" -> CalendarActivity
 * ```
 * 
 * === INTENT EXTRAS ===
 * Khi navigate đến TaskListActivity, truyền EXTRA_TASK_TYPE để phân biệt:
 * ```java
 * // Công việc thông thường
 * intent.putExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);
 * 
 * // Danh sách mua sắm
 * intent.putExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_SHOPPING);
 * ```
 * 
 * === UI COMPONENTS ===
 * 6 MaterialCardView từ layout:
 * - card_notes: Ghi chú
 * - card_todo: Công việc
 * - card_shopping: Mua sắm
 * - card_calculator: Máy tính
 * - card_weather: Thời tiết
 * - card_calendar: Lịch
 * 
 * === HILT INJECTION ===
 * @AndroidEntryPoint cho phép:
 * - Tự động inject dependencies (nếu cần)
 * - Fragment lifecycle-aware
 * 
 * === VÍ DỤ MỞ RỘNG ===
 * Để thêm card mới:
 * ```java
 * // 1. Thêm MaterialCardView trong fragment_productivity.xml
 * <MaterialCardView android:id="@+id/card_new_feature" .../>
 * 
 * // 2. Tìm view trong onCreateView()
 * MaterialCardView cardNew = view.findViewById(R.id.card_new_feature);
 * 
 * // 3. Set click listener
 * cardNew.setOnClickListener(v -> {
 *     Intent intent = new Intent(getContext(), NewFeatureActivity.class);
 *     startActivity(intent);
 * });
 * ```
 * 
 * === LƯU Ý ===
 * - Fragment KHÔNG giữ state (không cần ViewModel)
 * - Chỉ là navigation hub
 * - Không cần onSaveInstanceState (không có data cần lưu)
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm badge hiển thị số lượng items (VD: 5 notes chưa đọc)
 * TODO: Thêm quick actions trên card (long press để hiển menu)
 * TODO: Customizable card order (kéo thả để sắp xếp lại)
 * TODO: Thêm widget cho màn hình chính
 * 
 * @see NotesListActivity Hiển thị danh sách ghi chú
 * @see TaskListActivity Hiển thị tasks/shopping list
 * @see WeatherActivity Hiển thị thời tiết
 */
@AndroidEntryPoint
public class ProductivityFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_productivity, container, false);

        MaterialCardView cardNotes = view.findViewById(R.id.card_notes);
        MaterialCardView cardTodo = view.findViewById(R.id.card_todo);
        MaterialCardView cardShopping = view.findViewById(R.id.card_shopping);
        MaterialCardView cardCalculator = view.findViewById(R.id.card_calculator);
        MaterialCardView cardWeather = view.findViewById(R.id.card_weather);
        MaterialCardView cardCalendar = view.findViewById(R.id.card_calendar);

        // Ghi chú
        cardNotes.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotesListActivity.class);
            startActivity(intent);
        });

        // Công việc
        cardTodo.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TaskListActivity.class);
            intent.putExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);
            startActivity(intent);
        });

        // Mua sắm
        cardShopping.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TaskListActivity.class);
            intent.putExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_SHOPPING);
            startActivity(intent);
        });

        // Máy tính
        cardCalculator.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CalculatorActivity.class);
            startActivity(intent);
        });

        // Thời tiết
        // Thời tiết
        cardWeather.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), WeatherActivity.class);
            startActivity(intent);
        });

        // ✅ Lịch (MỚI)
        cardCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CalendarActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}