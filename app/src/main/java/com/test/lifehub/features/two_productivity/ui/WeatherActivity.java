package com.test.lifehub.features.two_productivity.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.test.lifehub.R;
import com.test.lifehub.core.util.PreferenceManager;
import com.test.lifehub.features.two_productivity.data.GeoResult;
import com.test.lifehub.features.two_productivity.data.WeatherApiService;
import com.test.lifehub.features.two_productivity.data.WeatherResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

@AndroidEntryPoint
public class WeatherActivity extends AppCompatActivity {

    // --- QUAN TRỌNG: THAY THẾ BẰNG KEY CỦA BẠN ---
    private static final String API_KEY = "REMOVED_API_KEY";
    // ------------------------------------------

    // Tags để lọc lỗi trong Logcat
    private static final String TAG_SEARCH = "WeatherSearchError";
    private static final String TAG_FETCH = "WeatherFetchError";

    @Inject
    WeatherApiService apiService;
    @Inject
    PreferenceManager preferenceManager;

    private TextView tvCityName, tvTemperature, tvCondition, tvHumidity, tvPromptCity;
    private ProgressBar progressBar;
    private LinearLayout weatherContent;
    private Button btnChangeCity;
    private MaterialToolbar toolbar;

    private AlertDialog searchDialog; // Giữ tham chiếu đến dialog
    private Call<List<GeoResult>> searchCall; // Giữ tham chiếu đến lệnh search

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Ánh xạ View
        toolbar = findViewById(R.id.toolbar_weather);
        tvCityName = findViewById(R.id.tv_city_name);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvCondition = findViewById(R.id.tv_condition);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvPromptCity = findViewById(R.id.tv_prompt_city);
        progressBar = findViewById(R.id.progress_bar_weather);
        weatherContent = findViewById(R.id.weather_content);
        btnChangeCity = findViewById(R.id.btn_change_city);

        // Cài đặt Toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Gán sự kiện
        btnChangeCity.setOnClickListener(v -> showChangeCityDialog());

        // Tải thành phố đã lưu
        loadSavedCity();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.weather_menu, menu);
        return true;
    }

    // === BƯỚC 2: THÊM HÀM NÀY ĐỂ XỬ LÝ KHI NHẤN NÚT ===
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            // Xử lý nhấn nút Làm mới
            refreshWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // === BƯỚC 3: THÊM HÀM TIỆN ÍCH NÀY ===
    private void refreshWeather() {
        // Lấy thành phố đã lưu
        String savedCity = preferenceManager.getSavedCity();

        if (savedCity != null && !savedCity.isEmpty()) {
            // Nếu đã có thành phố, gọi fetchWeather
            Toast.makeText(this, R.string.weather_refreshing, Toast.LENGTH_SHORT).show();
            fetchWeather(savedCity);
        } else {
            // Nếu chưa có thành phố (lần đầu vào app)
            Toast.makeText(this, R.string.weather_select_city_first, Toast.LENGTH_SHORT).show();
            // Bạn cũng có thể gọi showChangeCityDialog() nếu muốn
        }
    }

    private void loadSavedCity() {
        String savedCity = preferenceManager.getSavedCity();
        if (savedCity != null && !savedCity.isEmpty()) {
            fetchWeather(savedCity);
        } else {
            // Nếu chưa lưu gì, hiển thị thông báo
            tvPromptCity.setVisibility(View.VISIBLE);
            weatherContent.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            // Tự động hiện dialog
            showChangeCityDialog();
        }
    }

    private void showChangeCityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_city, null);
        builder.setView(dialogView);

        SearchView searchView = dialogView.findViewById(R.id.search_view_city);
        RecyclerView rvResults = dialogView.findViewById(R.id.rv_city_results);
        TextView tvNoResults = dialogView.findViewById(R.id.tv_no_results);

        rvResults.setLayoutManager(new LinearLayoutManager(this));

        // Adapter với listener
        CitySearchAdapter adapter = new CitySearchAdapter(new ArrayList<>(), city -> {
            // Khi người dùng nhấn vào 1 thành phố:
            fetchWeather(city.name); // Tải thời tiết
            searchDialog.dismiss(); // Đóng dialog
        });
        rvResults.setAdapter(adapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Khi nhấn enter (hoặc nút search)
                performSearch(query, adapter, tvNoResults);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    adapter.updateData(new ArrayList<>()); // Xóa kết quả
                    tvNoResults.setVisibility(View.GONE);
                }
                return true;
            }
        });

        // Hủy bỏ lệnh search cũ khi dialog đóng
        builder.setOnDismissListener(dialog -> {
            if (searchCall != null) {
                searchCall.cancel();
            }
        });

        searchDialog = builder.create();
        searchDialog.show();
    }

    // Hàm mới để thực hiện search
    private void performSearch(String query, CitySearchAdapter adapter, TextView tvNoResults) {
        if (query.length() < 2) return; // Không search nếu query quá ngắn

        // Hủy lệnh search cũ nếu đang chạy
        if (searchCall != null) {
            searchCall.cancel();
        }

        // Gọi API Geocoding
        searchCall = apiService.searchCity(query, 5, API_KEY);
        searchCall.enqueue(new Callback<List<GeoResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<GeoResult>> call, @NonNull Response<List<GeoResult>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GeoResult> results = response.body();
                    adapter.updateData(results); // Cập nhật RecyclerView
                    tvNoResults.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    // Lỗi từ phía server (ví dụ: 401 do sai key)
                    Log.e(TAG_SEARCH, "Lỗi khi tìm kiếm: Code " + response.code() + " - " + response.message());
                    adapter.updateData(new ArrayList<>());
                    tvNoResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GeoResult>> call, @NonNull Throwable t) {
                // Lỗi mạng (ví dụ: không có internet)
                Log.e(TAG_SEARCH, "LỖI TÌM KIẾM THÀNH PHỐ: " + t.getMessage(), t);

                if (!call.isCanceled()) {
                    Toast.makeText(WeatherActivity.this, getString(R.string.weather_search_error, t.getMessage()), Toast.LENGTH_SHORT).show();
                    tvNoResults.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void fetchWeather(String city) {
        progressBar.setVisibility(View.VISIBLE);
        weatherContent.setVisibility(View.GONE);
        tvPromptCity.setVisibility(View.GONE);

        apiService.getCurrentWeather(city, "metric", API_KEY)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherData = response.body();

                            // Lưu lại thành phố thành công
                            preferenceManager.saveCity(weatherData.name);

                            // Cập nhật UI
                            updateUI(weatherData);
                            weatherContent.setVisibility(View.VISIBLE);
                        } else {
                            // Lỗi server (ví dụ: không tìm thấy thành phố này)
                            Log.e(TAG_FETCH, "Lỗi khi lấy thời tiết: Code " + response.code() + " - " + response.message());
                            Toast.makeText(WeatherActivity.this, getString(R.string.weather_not_found, city), Toast.LENGTH_SHORT).show();
                            tvPromptCity.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                        // Lỗi mạng
                        Log.e(TAG_FETCH, "LỖI LẤY THỜI TIẾT: " + t.getMessage(), t);

                        progressBar.setVisibility(View.GONE);
                        tvPromptCity.setVisibility(View.VISIBLE);
                        Toast.makeText(WeatherActivity.this, getString(R.string.weather_network_error, t.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(WeatherResponse data) {
        tvCityName.setText(data.name);
        tvTemperature.setText(String.format(Locale.US, "%.0f°C", data.main.temp));
        tvHumidity.setText(getString(R.string.humidity_format, data.main.humidity));

        if (data.weather != null && !data.weather.isEmpty()) {
            tvCondition.setText(data.weather.get(0).description);
        }
    }


}