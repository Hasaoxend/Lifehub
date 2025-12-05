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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.test.lifehub.BuildConfig;
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

/**
 * Activity hiển thị thông tin thời tiết
 * 
 * Chức năng:
 * - Lấy dữ liệu thời tiết từ OpenWeatherMap API
 * - Hiển thị nhiệt độ, độ ẩm, tình trạng thời tiết
 * - Cho phép chọn thành phố từ danh sách 14 thành phố phổ biến VN
 * - Tự động lưu thành phố đã chọn vào SharedPreferences
 * - Làm mới dữ liệu mỗi 10-30 phút hoặc khi người dùng bấm nút refresh
 * 
 * Dependencies injection:
 * - WeatherApiService: Gọi API thời tiết qua Retrofit
 * - PreferenceManager: Lưu/đọc thành phố đã chọn
 */
@AndroidEntryPoint
public class WeatherActivity extends AppCompatActivity {

    // --- BẢO MẬT: API KEY ---
    // ✅ API key được đọc từ BuildConfig (local.properties)
    // ⚠️ KHÔNG HARDCODE API KEY trong source code!
    // Đăng ký key mới tại: https://openweathermap.org/api
    private static final String API_KEY = BuildConfig.OPENWEATHER_API_KEY;
    // ------------------------------------------

    // Tags để lọc log lỗi trong Logcat (dùng khi debug)
    private static final String TAG_SEARCH = "WeatherSearchError";
    private static final String TAG_FETCH = "WeatherFetchError";

    // --- DEPENDENCY INJECTION (Hilt tự động inject) ---
    @Inject
    WeatherApiService apiService;  // Service để gọi OpenWeatherMap API
    @Inject
    PreferenceManager preferenceManager;  // Quản lý SharedPreferences

    // --- UI COMPONENTS ---
    private TextView tvCityName;       // Hiển thị tên thành phố
    private TextView tvTemperature;    // Hiển thị nhiệt độ (°C)
    private TextView tvCondition;      // Hiển thị tình trạng (Sunny, Rainy, ...)
    private TextView tvHumidity;       // Hiển thị độ ẩm (%)
    private TextView tvPromptCity;     // Hiển thị lời nhắc "Vui lòng chọn thành phố"
    private ProgressBar progressBar;   // Hiển thị loading khi đang tải dữ liệu
    private LinearLayout weatherContent;  // Layout chứa thông tin thời tiết
    private Button btnChangeCity;      // Nút "Thay đổi thành phố"
    private MaterialToolbar toolbar;   // Thanh công cụ phía trên

    // --- STATE VARIABLES ---
    private AlertDialog searchDialog;  // Dialog chọn thành phố (giữ reference để dismiss khi cần)
    private Call<List<GeoResult>> searchCall;  // API call tìm kiếm thành phố (giữ để cancel nếu cần)

    /**
     * Khởi tạo Activity
     * 
     * Flow:
     * 1. Ánh xạ các View từ layout
     * 2. Setup toolbar với nút back
     * 3. Đọc thành phố đã lưu từ SharedPreferences
     * 4. Nếu có thành phố đã lưu -> tự động load thời tiết
     * 5. Nếu chưa có -> hiển thị prompt "Vui lòng chọn thành phố"
     * 6. Setup các listener cho buttons
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // === BƯỚC 1: ÁNH XẠ CÁC VIEW TỪ LAYOUT ===
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

    /**
     * Làm mới dữ liệu thời tiết
     * 
     * Được gọi khi:
     * - Người dùng bấm nút Refresh trên menu
     * - Cần cập nhật dữ liệu thời tiết mới nhất
     */
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

    /**
     * Hiển thị dialog chọn thành phố
     * 
     * Hiển thị danh sách 14 thành phố phổ biến tại Việt Nam:
     * Hà Nội, TP.HCM, Đà Nẵng, Hải Phòng, Cần Thơ, Biên Hòa, Huế, 
     * Nha Trang, Buôn Ma Thuột, Quy Nhơn, Vũng Tàu, Thái Nguyên, Nam Định, Vinh
     * 
     * Khi người dùng chọn thành phố:
     * 1. Gọi fetchWeather() để lấy thông tin thời tiết
     * 2. Đóng dialog
     */
    private void showChangeCityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_city, null);
        builder.setView(dialogView);

        // Lấy các view từ dialog
        RecyclerView rvResults = dialogView.findViewById(R.id.rv_city_results);
        Button btnSearchCity = dialogView.findViewById(R.id.btn_search_city);
        LinearLayout layoutSearch = dialogView.findViewById(R.id.layout_search);
        androidx.appcompat.widget.SearchView searchView = dialogView.findViewById(R.id.search_view_city);
        TextView tvNoResults = dialogView.findViewById(R.id.tv_no_results);

        rvResults.setLayoutManager(new LinearLayoutManager(this));

        // Danh sách các thành phố phổ biến tại Việt Nam
        List<GeoResult> popularCities = new ArrayList<>();
        popularCities.add(createCity("Hanoi"));
        popularCities.add(createCity("Ho Chi Minh City"));
        popularCities.add(createCity("Da Nang"));
        popularCities.add(createCity("Hai Phong"));
        popularCities.add(createCity("Can Tho"));
        popularCities.add(createCity("Bien Hoa"));
        popularCities.add(createCity("Hue"));
        popularCities.add(createCity("Nha Trang"));
        popularCities.add(createCity("Buon Ma Thuot"));
        popularCities.add(createCity("Quy Nhon"));
        popularCities.add(createCity("Vung Tau"));
        popularCities.add(createCity("Thai Nguyen"));
        popularCities.add(createCity("Nam Dinh"));
        popularCities.add(createCity("Vinh"));

        // Adapter với listener
        CitySearchAdapter adapter = new CitySearchAdapter(popularCities, city -> {
            // Khi người dùng nhấn vào 1 thành phố:
            fetchWeather(city.name); // Tải thời tiết
            searchDialog.dismiss(); // Đóng dialog
        });
        rvResults.setAdapter(adapter);

        // Xử lý nút "Tìm thành phố khác"
        btnSearchCity.setOnClickListener(v -> {
            // Toggle hiển thị thanh tìm kiếm
            if (layoutSearch.getVisibility() == View.GONE) {
                layoutSearch.setVisibility(View.VISIBLE);
                btnSearchCity.setText(R.string.hide_search);
                searchView.requestFocus();
            } else {
                layoutSearch.setVisibility(View.GONE);
                btnSearchCity.setText(R.string.search_other_city);
                // Reset về danh sách thành phố phổ biến
                adapter.updateData(popularCities);
                tvNoResults.setVisibility(View.GONE);
            }
        });

        // Xử lý tìm kiếm
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query, adapter, tvNoResults);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    adapter.updateData(new ArrayList<>()); // Xóa kết quả tìm kiếm
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

    /**
     * Thực hiện tìm kiếm thành phố qua API
     */
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
                    // Lỗi từ phía server
                    Log.e(TAG_SEARCH, "Lỗi khi tìm kiếm: Code " + response.code() + " - " + response.message());
                    adapter.updateData(new ArrayList<>());
                    tvNoResults.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<GeoResult>> call, @NonNull Throwable t) {
                // Lỗi mạng
                Log.e(TAG_SEARCH, "LỖI TÌM KIẾM THÀNH PHỐ: " + t.getMessage(), t);

                if (!call.isCanceled()) {
                    Toast.makeText(WeatherActivity.this, getString(R.string.weather_search_error, t.getMessage()), Toast.LENGTH_SHORT).show();
                    tvNoResults.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /**
     * Tạo object GeoResult cho thành phố
     * 
     * @param name Tên thành phố (tiếng Anh, ví dụ: "Hanoi", "Ho Chi Minh City")
     * @return GeoResult object với name và country = "VN"
     */
    private GeoResult createCity(String name) {
        GeoResult city = new GeoResult();
        city.name = name;
        city.country = "VN";
        return city;
    }

    /**
     * Lấy thông tin thời tiết cho thành phố
     * 
     * Flow:
     * 1. Hiển thị progress bar (loading)
     * 2. Gọi OpenWeatherMap API với:
     *    - Tên thành phố
     *    - Đơn vị: metric (độ C)
     *    - Ngôn ngữ: vi (tiếng Việt)
     *    - API key
     * 3. Nếu thành công:
     *    - Lưu tên thành phố vào SharedPreferences
     *    - Hiển thị dữ liệu trên UI
     * 4. Nếu lỗi:
     *    - Hiển thị thông báo lỗi
     *    - Ghi log để debug
     * 
     * @param city Tên thành phố (tiếng Anh)
     */
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