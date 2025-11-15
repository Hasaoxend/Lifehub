package com.test.lifehub.features.two_productivity.data;

import java.util.List;

// Lớp chính chứa toàn bộ phản hồi
public class WeatherResponse {
    public Main main;
    public List<Weather> weather;
    public String name; // Tên thành phố
}