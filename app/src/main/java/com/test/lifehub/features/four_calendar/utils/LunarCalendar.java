package com.test.lifehub.features.four_calendar.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Công cụ chuyển đổi Lịch Âm Việt Nam (Sử dụng thuật toán thiên văn)
 * Fix: Lỗi nhảy ngày và sai lịch Tết do dữ liệu hardcode không chính xác.
 */
public class LunarCalendar {

    // Các hằng số thiên văn
    private static final double PI = Math.PI;

    /**
     * Chuyển đổi ngày dương lịch sang ngày âm lịch Việt Nam
     * @param date Ngày dương lịch (java.util.Date)
     * @return Đối tượng LunarDate
     */
    public static LunarDate convertSolarToLunar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH chạy từ 0-11
        int year = cal.get(Calendar.YEAR);

        return convertSolar2Lunar(day, month, year, 7.0); // Múi giờ +7 cho Việt Nam
    }

    /**
     * Hàm chính thực hiện chuyển đổi (Thuật toán Hồ Ngọc Đức)
     */
    public static LunarDate convertSolar2Lunar(int dd, int mm, int yy, double timeZone) {
        int k, dayNumber, monthStart, a11, b11, lunarYear, lunarMonth, lunarDay;
        long leapMonthDiff;

        dayNumber = jdFromDate(dd, mm, yy);
        k = (int) ((dayNumber - 2415021.076998695) / 29.530588853);
        monthStart = getNewMoonDay(k + 1, timeZone);

        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone);
        }

        a11 = getLunarMonth11(yy, timeZone);
        b11 = a11;

        if (a11 >= monthStart) {
            lunarYear = yy;
            a11 = getLunarMonth11(yy - 1, timeZone);
        } else {
            lunarYear = yy + 1;
            b11 = getLunarMonth11(yy + 1, timeZone);
        }

        lunarDay = dayNumber - monthStart + 1;
        int diff = (int) ((dayNumber - a11) / 29);
        lunarMonth = diff + 11;
        leapMonthDiff = b11 - a11;

        boolean isLeap = false;

        // Kiểm tra tháng nhuận
        if (leapMonthDiff > 365) {
            int leapMonthIndex = (int) ((getNewMoonDay(getLunarMonth11Index(yy - 1, timeZone) + 3, timeZone) - a11) / 29);
            if (diff >= leapMonthIndex) {
                lunarMonth = diff + 10;
            }
            if (diff == leapMonthIndex) {
                isLeap = true;
            }
        }

        if (lunarMonth > 12) {
            lunarMonth = lunarMonth - 12;
        }

        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1;
        }

        return new LunarDate(lunarDay, lunarMonth, lunarYear, isLeap);
    }

    // --- CÁC HÀM TÍNH TOÁN THIÊN VĂN (PRIVATE) ---

    private static int jdFromDate(int dd, int mm, int yy) {
        int a = (14 - mm) / 12;
        int y = yy + 4800 - a;
        int m = mm + 12 * a - 3;
        return dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    private static int getNewMoonDay(int k, double timeZone) {
        double T = k / 1236.85;
        double dr = PI / 180;
        double ga = dr * (359.2242 + 29.10535608 * k - 0.0000333 * T * T - 0.00000347 * T * T * T);
        double gc = dr * (306.0253 + 385.81691806 * k + 0.0107306 * T * T + 0.00001236 * T * T * T);
        double gf = dr * (21.2964 + 390.67050646 * k - 0.0016528 * T * T - 0.00000239 * T * T * T);

        double julianDay = 2415020.75933 + 29.53058868 * k + 0.0001178 * T * T - 0.000000155 * T * T * T
                + (0.1734 - 0.000393 * T) * Math.sin(ga) + 0.0021 * Math.sin(2 * ga) - 0.4068 * Math.sin(gc)
                + 0.0161 * Math.sin(2 * gc) - 0.0004 * Math.sin(3 * gc) + 0.0104 * Math.sin(2 * gf)
                - 0.0051 * Math.sin(ga + gc) - 0.0074 * Math.sin(ga - gc) + 0.0004 * Math.sin(2 * gf + ga)
                - 0.0004 * Math.sin(2 * gf - ga) - 0.0006 * Math.sin(2 * gf + gc) + 0.0010 * Math.sin(2 * gf - gc)
                + 0.0005 * Math.sin(ga + 2 * gc);

        return (int) (julianDay + 0.5 + timeZone / 24);
    }

    private static int getSunLongitude(int dayNumber, double timeZone) {
        double T = (dayNumber - 0.5 - timeZone / 24 - 2451545.0) / 36525;
        double dr = PI / 180;
        double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T * T;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T * T - 0.00000048 * T * T * T;
        double C = (1.914600 - 0.004817 * T - 0.000014 * T * T) * Math.sin(M * dr)
                + (0.019993 - 0.000101 * T) * Math.sin(2 * M * dr) + 0.000290 * Math.sin(3 * M * dr);
        double theta = L0 + C;

        theta = theta - 360 * (int) (theta / 360);
        if (theta < 0) theta += 360; // Chuẩn hóa về 0-360

        return (int) theta;
    }

    private static int getLunarMonth11(int yy, double timeZone) {
        double off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) (off / 29.530588853);
        int m = getNewMoonDay(k, timeZone);
        int sunLong = getSunLongitude(m, timeZone);

        if (sunLong >= 270) {
            m = getNewMoonDay(k - 1, timeZone);
        }
        return m;
    }

    private static int getLunarMonth11Index(int yy, double timeZone) {
        double off = jdFromDate(31, 12, yy) - 2415021;
        int k = (int) (off / 29.530588853);
        int m = getNewMoonDay(k, timeZone);
        int sunLong = getSunLongitude(m, timeZone);
        if (sunLong >= 270) {
            return k - 1;
        }
        return k;
    }

    /**
     * Chuyển đổi ngày dương lịch thành chuỗi hiển thị
     */
    public static String getLunarDateString(Date date) {
        LunarDate lunar = convertSolarToLunar(date);
        return lunar.toString();
    }

    /**
     * Class DTO chứa kết quả
     */
    public static class LunarDate {
        public int day;
        public int month;
        public int year;
        public boolean isLeapMonth; // Có phải tháng nhuận không
        public boolean isValid;

        public LunarDate(int day, int month, int year, boolean isLeapMonth) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.isLeapMonth = isLeapMonth;
            this.isValid = true;
        }

        @Override
        public String toString() {
            return day + "/" + month + (isLeapMonth ? " (nhuận)" : "") + "/" + year;
        }
    }
}