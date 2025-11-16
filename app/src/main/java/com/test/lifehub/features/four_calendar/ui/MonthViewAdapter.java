package com.test.lifehub.features.four_calendar.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MonthViewAdapter extends RecyclerView.Adapter<MonthViewAdapter.MonthDayViewHolder> {

    public interface OnDateClickListener {
        void onDateClick(Date date);
    }

    private List<MonthDayData> monthData = new ArrayList<>();
    private final OnDateClickListener listener;
    private final Context context;

    public MonthViewAdapter(Context context, OnDateClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MonthDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_month_day, parent, false);
        return new MonthDayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MonthDayViewHolder holder, int position) {
        MonthDayData data = monthData.get(position);

        SimpleDateFormat dateFormat = new SimpleDateFormat("d", Locale.getDefault());
        holder.tvDate.setText(dateFormat.format(data.date));

        // Style for current month vs other months
        if (!data.isCurrentMonth) {
            holder.tvDate.setAlpha(0.3f);
        } else {
            holder.tvDate.setAlpha(1.0f);
        }

        // Highlight today
        Calendar today = Calendar.getInstance();
        Calendar itemCal = Calendar.getInstance();
        itemCal.setTime(data.date);

        if (today.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)) {
            holder.tvDate.setBackgroundResource(R.drawable.circle_primary);
            holder.tvDate.setTextColor(Color.WHITE);
        } else {
            holder.tvDate.setBackground(null);
            holder.tvDate.setTextColor(Color.BLACK);
        }

        // Show indicator if has events
        if (!data.events.isEmpty()) {
            holder.viewIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.viewIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onDateClick(data.date));
    }

    @Override
    public int getItemCount() {
        return monthData.size();
    }

    public void submitList(List<MonthDayData> data) {
        this.monthData = data;
        notifyDataSetChanged();
    }

    static class MonthDayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        View viewIndicator;

        public MonthDayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
        }
    }
}