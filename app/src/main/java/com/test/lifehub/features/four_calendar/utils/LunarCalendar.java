package com.test.lifehub.features.four_calendar.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Công cụ chuyển đổi Lịch Âm Việt Nam
 * 
 * Class này giúp chuyển đổi ngày từ lịch dương (Gregorian) sang lịch âm Việt Nam.
 * Dữ liệu lịch được tính toán dựa trên múi giờ UTC+7 (giờ Việt Nam) và hỗ trợ
 * từ năm 1900 đến 2050.
 * 
 * Cách hoạt động:
 * - Lưu trữ bảng dữ liệu số ngày của mỗi tháng âm lịch theo năm
 * - Tính số ngày từ đầu năm dương lịch đến ngày cần chuyển đổi
 * - So sánh với ngày Tết Nguyên Đán để xác định năm âm lịch
 * - Duyệt qua các tháng âm lịch để tìm tháng và ngày tương ứng
 */
public class LunarCalendar {
    
    // Bảng dữ liệu số ngày trong mỗi tháng âm lịch từ năm 1900-2050
    // Mỗi số đại diện cho loại tháng:
    // - 1: tháng thiếu (29 ngày)
    // - 2: tháng đủ (30 ngày)  
    // - 3-6: tháng nhuận (tháng thứ mấy trong năm có thêm 1 tháng nhuận)
    private static final int[][] LUNAR_MONTH_DAYS = {
        {1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2},  // 1900
        {2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1},
        {2, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 1},
        {2, 2, 1, 2, 2, 1, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1},
        {2, 1, 2, 1, 2, 1, 5, 2, 2, 1, 2, 1},  // 1905
        {2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1, 2},
        {1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1},
        {2, 1, 2, 1, 1, 5, 1, 2, 2, 1, 2, 2},
        {2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2},
        {2, 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2},  // 1910
        {2, 2, 1, 2, 5, 1, 2, 1, 2, 1, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1},
        {2, 3, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1},
        {2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1, 2},  // 1915
        {1, 2, 1, 1, 2, 1, 5, 2, 2, 1, 2, 2},
        {1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2, 2},
        {2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2},
        {2, 1, 2, 2, 3, 2, 1, 1, 2, 1, 2, 2},
        {1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 1, 2},  // 1920
        {2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1, 1},
        {2, 1, 2, 5, 2, 1, 2, 2, 1, 2, 1, 2},
        {1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1},
        {2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 5, 1, 2, 1, 1, 2, 2, 1, 2, 2, 2},  // 1925
        {1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2},
        {1, 2, 2, 1, 1, 5, 1, 2, 1, 2, 2, 1},
        {2, 2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2},
        {1, 2, 2, 1, 6, 1, 2, 1, 2, 1, 1, 2},  // 1930
        {1, 2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 2},
        {1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1},
        {2, 1, 4, 1, 2, 1, 2, 1, 2, 2, 2, 1},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1},
        {2, 2, 1, 1, 2, 1, 4, 1, 2, 2, 1, 2},  // 1935
        {2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 2, 4, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 2, 1, 2, 2, 1, 2, 1, 1, 2},
        {1, 2, 1, 2, 1, 2, 5, 2, 2, 1, 2, 1},  // 1940
        {1, 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2},
        {2, 1, 1, 2, 3, 2, 1, 2, 2, 1, 2, 2},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2},
        {2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},  // 1945
        {2, 2, 1, 5, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},
        {1, 5, 2, 1, 2, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},  // 1950
        {1, 1, 2, 1, 1, 5, 2, 2, 1, 2, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2},
        {1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2},
        {2, 1, 2, 1, 5, 1, 2, 1, 2, 1, 2, 1},
        {2, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},  // 1955
        {1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1},
        {2, 1, 5, 2, 2, 1, 2, 1, 2, 1, 2, 1},
        {2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2},
        {1, 2, 1, 1, 2, 1, 2, 5, 2, 2, 1, 2},
        {1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1},  // 1960
        {2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2},
        {1, 2, 1, 2, 1, 4, 1, 2, 1, 2, 2, 1},
        {2, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2, 1},
        {2, 2, 1, 2, 2, 1, 1, 2, 1, 2, 1, 2},
        {1, 2, 2, 4, 1, 2, 1, 2, 1, 2, 1, 2},  // 1965
        {1, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1},
        {2, 1, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2},
        {1, 2, 1, 1, 5, 2, 1, 2, 2, 2, 1, 2},
        {1, 2, 1, 1, 2, 1, 2, 1, 2, 2, 2, 1},
        {2, 1, 2, 1, 1, 2, 1, 1, 2, 2, 2, 1},  // 1970
        {2, 2, 1, 5, 1, 2, 1, 1, 2, 2, 1, 2},
        {2, 2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2},
        {2, 2, 1, 2, 1, 2, 1, 5, 2, 1, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 1},
        {2, 2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1},  // 1975
        {2, 1, 1, 2, 1, 6, 1, 2, 2, 1, 2, 1},
        {2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 2, 1, 1, 2, 1, 1, 2, 2, 1, 2, 2},
        {2, 1, 2, 3, 2, 1, 1, 2, 2, 1, 2, 2},
        {2, 1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2},  // 1980
        {2, 1, 2, 2, 1, 1, 2, 1, 1, 5, 2, 2},
        {1, 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2},
        {1, 2, 2, 1, 2, 2, 1, 2, 1, 2, 1, 1},
        {2, 1, 2, 1, 2, 5, 2, 2, 1, 2, 1, 2},
        {1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1},  // 1985
        {2, 1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 2, 1, 1, 5, 1, 2, 2, 1, 2, 2, 2},
        {1, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2},
        {1, 2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2},
        {1, 2, 5, 2, 1, 2, 1, 1, 2, 1, 2, 1},  // 1990
        {2, 2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2},
        {1, 2, 2, 1, 2, 2, 1, 5, 2, 1, 1, 2},
        {1, 2, 1, 2, 2, 1, 2, 1, 2, 2, 1, 2},
        {1, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1},
        {2, 1, 1, 2, 3, 2, 2, 1, 2, 2, 2, 1},  // 1995
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1},
        {2, 2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1},
        {2, 2, 2, 3, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2},  // 2000
        {1, 5, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 1, 2, 2, 1, 2, 2, 1, 1},
        {2, 1, 2, 1, 2, 1, 5, 2, 2, 1, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2},  // 2005
        {2, 2, 1, 1, 5, 1, 2, 1, 2, 1, 2, 2},
        {2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 6, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},  // 2010
        {1, 2, 1, 2, 1, 2, 1, 2, 5, 2, 1, 2},
        {1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2},
        {2, 1, 1, 2, 3, 2, 1, 2, 1, 2, 2, 2},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2},  // 2015
        {2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 2, 1, 5, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},
        {1, 5, 2, 1, 2, 2, 1, 2, 1, 2, 1, 2},  // 2020
        {1, 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2},
        {2, 1, 1, 2, 3, 2, 1, 2, 2, 1, 2, 2},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2},
        {2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},  // 2025
        {2, 2, 1, 2, 1, 5, 1, 2, 1, 2, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 4, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 1, 2, 2, 1, 2, 2, 1, 2, 2},  // 2030
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2},
        {2, 1, 1, 2, 1, 1, 5, 2, 1, 2, 2, 2},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 1, 2, 2},
        {2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 2, 1, 2, 1, 2, 3, 2, 1, 2, 1, 2},  // 2035
        {2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 2, 1, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 1, 2, 5, 2, 1, 2, 1, 2},
        {1, 2, 1, 1, 2, 2, 1, 2, 2, 1, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2},  // 2040
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2},
        {2, 2, 1, 1, 5, 1, 2, 1, 2, 1, 2, 2},
        {2, 1, 2, 1, 2, 1, 1, 2, 1, 2, 1, 2},
        {2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 1},
        {2, 1, 2, 5, 2, 2, 1, 1, 2, 1, 2, 1},  // 2045
        {2, 1, 2, 1, 2, 2, 1, 2, 1, 2, 1, 2},
        {1, 2, 1, 2, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 1, 2, 4, 1, 2, 1, 2, 2, 1, 2, 2},
        {1, 1, 2, 1, 1, 2, 1, 2, 2, 2, 1, 2},
        {2, 1, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2},  // 2050
    };

    /**
     * Chuyển đổi ngày dương lịch sang ngày âm lịch Việt Nam
     * 
     * @param date Ngày dương lịch cần chuyển đổi (kiểu Date)
     * @return Đối tượng LunarDate chứa ngày, tháng, năm âm lịch
     */
    public static LunarDate convertSolarToLunar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int solarYear = cal.get(Calendar.YEAR);
        int solarMonth = cal.get(Calendar.MONTH) + 1;
        int solarDay = cal.get(Calendar.DAY_OF_MONTH);
        
        return convertSolarToLunar(solarDay, solarMonth, solarYear);
    }
    
    /**
     * Chuyển đổi ngày dương lịch sang ngày âm lịch Việt Nam
     * 
     * @param solarDay Ngày trong tháng (1-31)
     * @param solarMonth Tháng (1-12)
     * @param solarYear Năm
     * @return Đối tượng LunarDate chứa ngày, tháng, năm âm lịch
     */
    public static LunarDate convertSolarToLunar(int solarDay, int solarMonth, int solarYear) {
        if (solarYear < 1900 || solarYear > 2050) {
            return new LunarDate(solarDay, solarMonth, solarYear, false);
        }
        
        // Tính số ngày từ đầu năm dương lịch đến ngày hiện tại
        int daysFromNewYear = getDaysInYear(solarYear, solarMonth, solarDay);
        
        // Lấy dữ liệu năm âm lịch
        int lunarYear = solarYear;
        int yearIndex = solarYear - 1900;
        
        // Điều chỉnh theo múi giờ Việt Nam - Tết Nguyên Đán thường rơi vào cuối tháng 1 hoặc tháng 2
        // Nếu trước ngày Tết thì thuộc năm âm lịch trước
        int lunarNewYearDays = getLunarNewYearDays(solarYear);
        if (daysFromNewYear < lunarNewYearDays) {
            lunarYear = solarYear - 1;
            yearIndex = lunarYear - 1900;
            if (yearIndex < 0) {
                return new LunarDate(solarDay, solarMonth, solarYear, false);
            }
            // Thêm số ngày của năm trước
            daysFromNewYear += getTotalDaysInSolarYear(solarYear - 1);
            lunarNewYearDays += getTotalDaysInSolarYear(solarYear - 1);
        }
        
        // Tính số ngày từ Tết Nguyên Đán đến ngày hiện tại
        int daysSinceLunarNewYear = daysFromNewYear - lunarNewYearDays;
        
        // Xác định tháng và ngày âm lịch
        int[] monthData = LUNAR_MONTH_DAYS[yearIndex];
        int lunarMonth = 1;
        int lunarDay = 1;
        
        for (int i = 0; i < 12; i++) {
            int daysInMonth = (monthData[i] < 3) ? 29 + monthData[i] : 29;
            
            // Kiểm tra tháng nhuận (tháng 13 trong năm)
            if (monthData[i] >= 3) {
                // Đây là chỉ số tháng nhuận
                int leapMonthNum = i + 1;
                int regularDays = 29;
                int leapDays = 30;
                
                if (daysSinceLunarNewYear < regularDays) {
                    lunarMonth = i + 1;
                    lunarDay = daysSinceLunarNewYear + 1;
                    break;
                } else if (daysSinceLunarNewYear < regularDays + leapDays) {
                    lunarMonth = i + 1;
                    lunarDay = daysSinceLunarNewYear - regularDays + 1;
                    return new LunarDate(lunarDay, lunarMonth, lunarYear, true); // Tháng nhuận
                } else {
                    daysSinceLunarNewYear -= (regularDays + leapDays);
                }
            } else {
                if (daysSinceLunarNewYear < daysInMonth) {
                    lunarMonth = i + 1;
                    lunarDay = daysSinceLunarNewYear + 1;
                    break;
                }
                daysSinceLunarNewYear -= daysInMonth;
            }
        }
        
        return new LunarDate(lunarDay, lunarMonth, lunarYear, false);
    }
    
    /**
     * Lấy số thứ tự ngày của Tết Nguyên Đán trong năm dương lịch
     * Ví dụ: Tết 2025 rơi vào ngày 29/1 -> trả về 29
     */
    private static int getLunarNewYearDays(int year) {
        // Ngày Tết thường rơi vào cuối tháng 1 hoặc giữa tháng 2
        // Dữ liệu này đã được tính toán sẵn cho từng năm
        if (year >= 1900 && year <= 2050) {
            int[] lunarNewYear = {
                31, 19, 8, 29, 16, 5, 25, 13, 2, 22,      // 1900-1909
                10, 30, 18, 6, 26, 14, 3, 23, 11, 1,      // 1910-1919
                20, 8, 28, 16, 5, 24, 13, 2, 21, 9,       // 1920-1929
                30, 17, 6, 26, 14, 4, 24, 12, 31, 19,     // 1930-1939
                8, 27, 15, 4, 24, 13, 2, 21, 9, 29,       // 1940-1949
                17, 6, 27, 14, 3, 23, 11, 31, 19, 8,      // 1950-1959
                28, 15, 4, 24, 12, 1, 21, 9, 29, 17,      // 1960-1969
                6, 27, 15, 3, 23, 11, 31, 19, 7, 27,      // 1970-1979
                16, 5, 25, 13, 2, 20, 9, 29, 17, 6,       // 1980-1989
                27, 15, 3, 23, 11, 31, 19, 7, 28, 16,     // 1990-1999
                5, 24, 12, 1, 22, 9, 29, 18, 7, 26,       // 2000-2009
                14, 3, 23, 10, 31, 19, 8, 28, 16, 4,      // 2010-2019
                25, 12, 1, 22, 10, 29, 17, 6, 26, 14,     // 2020-2029
                3, 21, 9, 29, 17, 6, 26, 14, 2, 22,       // 2030-2039
                10, 30, 18, 7, 27, 15, 4, 23, 11, 31,     // 2040-2049
                20                                         // 2050
            };
            return lunarNewYear[year - 1900];
        }
        return 31; // Mặc định trả về ngày 31/1 nếu năm nằm ngoài phạm vi hỗ trợ
    }
    
    /**
     * Tính số ngày từ đầu năm (1/1) đến ngày chỉ định
     * Ví dụ: 15/2/2025 -> trả về 46 (31 ngày tháng 1 + 15 ngày)
     */
    private static int getDaysInYear(int year, int month, int day) {
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year)) {
            daysInMonth[1] = 29; // Tháng 2 có 29 ngày nếu là năm nhuận
        }
        
        int days = day;
        for (int i = 0; i < month - 1; i++) {
            days += daysInMonth[i];
        }
        return days;
    }
    
    /**
     * Lấy tổng số ngày trong một năm dương lịch
     * @return 366 nếu là năm nhuận, 365 nếu là năm thường
     */
    private static int getTotalDaysInSolarYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }
    
    /**
     * Kiểm tra xem một năm có phải là năm nhuận không
     * Quy tắc: Chia hết cho 4 NHƯNG không chia hết cho 100, HOẶC chia hết cho 400
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
    
    /**
     * Chuyển đổi ngày dương lịch thành chuỗi ngày âm lịch để hiển thị
     * @param date Ngày dương lịch cần chuyển đổi
     * @return Chuỗi định dạng như "15/8" cho ngày 15 tháng 8 âm lịch,
     *         hoặc "15/8 (nhuận)" nếu là tháng nhuận
     */
    public static String getLunarDateString(Date date) {
        LunarDate lunar = convertSolarToLunar(date);
        if (lunar.isValid) {
            return lunar.day + "/" + lunar.month + (lunar.isLeapMonth ? " (nhuận)" : "");
        }
        return "";
    }
    
    /**
     * Class chứa kết quả chuyển đổi ngày âm lịch
     * Bao gồm ngày, tháng, năm và các thông tin bổ sung
     */
    public static class LunarDate {
        public int day;           // Ngày trong tháng (1-30)
        public int month;         // Tháng trong năm (1-12)
        public int year;          // Năm âm lịch
        public boolean isLeapMonth; // Có phải tháng nhuận không
        public boolean isValid;     // Kết quả chuyển đổi có hợp lệ không
        
        public LunarDate(int day, int month, int year, boolean isLeapMonth) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.isLeapMonth = isLeapMonth;
            this.isValid = true;
        }
    }
}
