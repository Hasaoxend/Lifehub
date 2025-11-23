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