package com.test.lifehub.features.two_productivity.data;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    /**
     * Lấy thời tiết hiện tại.
     * Đường dẫn đầy đủ: [baseUrl] + "data/2.5/weather"
     */
    @GET("data/2.5/weather") // <-- Đường dẫn con chính xác
    Call<WeatherResponse> getCurrentWeather(
            @Query("q") String city,
            @Query("units") String units,
            @Query("appid") String apiKey
    );

    /**
     * Tìm kiếm thành phố (Geocoding).
     * Đường dẫn đầy đủ: [baseUrl] + "geo/1.0/direct"
     */
    @GET("geo/1.0/direct") // <-- Đường dẫn con chính xác
    Call<List<GeoResult>> searchCity(
            @Query("q") String query,
            @Query("limit") int limit,
            @Query("appid") String apiKey
    );
}