package com.test.lifehub.features.four_calendar.utils;

import java.util.Calendar;
import java.util.Date;

/**
 * Vietnamese Lunar Calendar Converter
 * Converts Gregorian (solar) dates to Vietnamese lunar dates
 * Based on Vietnamese lunar calendar algorithms with UTC+7 timezone
 */
public class LunarCalendar {
    
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
     * Converts a Gregorian date to Vietnamese lunar date
     * @param date The Gregorian date to convert
     * @return LunarDate object containing lunar day, month, year
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
     * Converts a Gregorian date to Vietnamese lunar date
     * @param solarDay Day of month (1-31)
     * @param solarMonth Month (1-12)
     * @param solarYear Year
     * @return LunarDate object containing lunar day, month, year
     */
    public static LunarDate convertSolarToLunar(int solarDay, int solarMonth, int solarYear) {
        if (solarYear < 1900 || solarYear > 2050) {
            return new LunarDate(solarDay, solarMonth, solarYear, false);
        }
        
        // Calculate the number of days from the solar new year to the given date
        int daysFromNewYear = getDaysInYear(solarYear, solarMonth, solarDay);
        
        // Get lunar year data
        int lunarYear = solarYear;
        int yearIndex = solarYear - 1900;
        
        // Adjust for Vietnamese timezone - Lunar new year typically falls in late Jan or Feb
        // If before lunar new year, belongs to previous lunar year
        int lunarNewYearDays = getLunarNewYearDays(solarYear);
        if (daysFromNewYear < lunarNewYearDays) {
            lunarYear = solarYear - 1;
            yearIndex = lunarYear - 1900;
            if (yearIndex < 0) {
                return new LunarDate(solarDay, solarMonth, solarYear, false);
            }
            // Add days from previous year
            daysFromNewYear += getTotalDaysInSolarYear(solarYear - 1);
            lunarNewYearDays += getTotalDaysInSolarYear(solarYear - 1);
        }
        
        // Calculate days since lunar new year
        int daysSinceLunarNewYear = daysFromNewYear - lunarNewYearDays;
        
        // Determine lunar month and day
        int[] monthData = LUNAR_MONTH_DAYS[yearIndex];
        int lunarMonth = 1;
        int lunarDay = 1;
        
        for (int i = 0; i < 12; i++) {
            int daysInMonth = (monthData[i] < 3) ? 29 + monthData[i] : 29;
            
            // Check for leap month
            if (monthData[i] >= 3) {
                // This is a leap month indicator
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
                    return new LunarDate(lunarDay, lunarMonth, lunarYear, true); // Leap month
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
     * Gets the day number of lunar new year in a given solar year
     */
    private static int getLunarNewYearDays(int year) {
        // Approximate lunar new year dates (typically late Jan to mid Feb)
        // This is a simplified calculation - in reality it varies by year
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
        return 31; // Default to Jan 31 if out of range
    }
    
    /**
     * Calculates the number of days from Jan 1 to the given date
     */
    private static int getDaysInYear(int year, int month, int day) {
        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year)) {
            daysInMonth[1] = 29;
        }
        
        int days = day;
        for (int i = 0; i < month - 1; i++) {
            days += daysInMonth[i];
        }
        return days;
    }
    
    /**
     * Gets total days in a solar year
     */
    private static int getTotalDaysInSolarYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }
    
    /**
     * Checks if a year is a leap year
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
    
    /**
     * Returns a formatted lunar date string for display
     * @param date The solar date to convert
     * @return Formatted string like "15/8" for lunar day 15, month 8
     */
    public static String getLunarDateString(Date date) {
        LunarDate lunar = convertSolarToLunar(date);
        if (lunar.isValid) {
            return lunar.day + "/" + lunar.month + (lunar.isLeapMonth ? " (nhuận)" : "");
        }
        return "";
    }
    
    /**
     * Lunar date result class
     */
    public static class LunarDate {
        public int day;
        public int month;
        public int year;
        public boolean isLeapMonth;
        public boolean isValid;
        
        public LunarDate(int day, int month, int year, boolean isLeapMonth) {
            this.day = day;
            this.month = month;
            this.year = year;
            this.isLeapMonth = isLeapMonth;
            this.isValid = true;
        }
    }
}
