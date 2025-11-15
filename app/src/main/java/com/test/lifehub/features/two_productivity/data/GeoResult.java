package com.test.lifehub.features.two_productivity.data;

// Lớp này định nghĩa 1 kết quả tìm kiếm thành phố
public class GeoResult {
    public String name; // Tên thành phố (ví dụ: "Hanoi")
    public String country; // Mã quốc gia (ví dụ: "VN")
    public String state; // (Tùy chọn, ví dụ: "Texas")

    // Hàm tiện ích để hiển thị "Hanoi, VN"
    public String getDisplayName() {
        if (state != null && !state.isEmpty()) {
            return name + ", " + state + ", " + country;
        }
        return name + ", " + country;
    }
}