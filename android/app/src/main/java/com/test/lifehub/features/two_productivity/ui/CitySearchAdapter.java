package com.test.lifehub.features.two_productivity.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.GeoResult;
import java.util.List;

public class CitySearchAdapter extends RecyclerView.Adapter<CitySearchAdapter.CityViewHolder> {

    private List<GeoResult> results;
    private OnCityClickListener listener;

    public interface OnCityClickListener {
        void onCityClick(GeoResult city);
    }

    public CitySearchAdapter(List<GeoResult> results, OnCityClickListener listener) {
        this.results = results;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city_result, parent, false);
        return new CityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        GeoResult city = results.get(position);
        holder.tvCityName.setText(city.getDisplayName());
        holder.itemView.setOnClickListener(v -> listener.onCityClick(city));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class CityViewHolder extends RecyclerView.ViewHolder {
        TextView tvCityName;
        public CityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCityName = itemView.findViewById(R.id.tv_city_name);
        }
    }

    public void updateData(List<GeoResult> newResults) {
        this.results.clear();
        this.results.addAll(newResults);
        notifyDataSetChanged();
    }
}