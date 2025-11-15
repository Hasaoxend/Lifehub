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

        // Dòng mới: Tìm ID của thẻ máy tính
        MaterialCardView cardCalculator = view.findViewById(R.id.card_calculator);


        //Weather
        MaterialCardView cardWeather = view.findViewById(R.id.card_weather);

        cardWeather.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), WeatherActivity.class);
            startActivity(intent);
        });

        // Khi nhấn vào Ghi chú -> Mở NotesListActivity
        cardNotes.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NotesListActivity.class);
            startActivity(intent);
        });

        // Khi nhấn vào Công việc -> Mở TaskListActivity (chế độ Công việc)
        cardTodo.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TaskListActivity.class);
            intent.putExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_GENERAL);
            // (Không gửi ProjectId -> sẽ mở Root)
            startActivity(intent);
        });

        // Khi nhấn vào Mua sắm -> Mở TaskListActivity (chế độ Mua sắm)
        cardShopping.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), TaskListActivity.class);
            intent.putExtra(Constants.EXTRA_TASK_TYPE, Constants.TASK_TYPE_SHOPPING);
            startActivity(intent);
        });

        // Khối mới: Khi nhấn vào Máy tính -> Mở CalculatorActivity
        cardCalculator.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), CalculatorActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Không cần làm gì ở đây nữa
    }
}