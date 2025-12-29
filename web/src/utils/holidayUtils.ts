import { convertSolarToLunar } from './lunarUtils';

export interface HolidayInfo {
  name: string;
  isLunar: boolean;
}

/**
 * Identifies Vietnamese holidays (Solar and Lunar)
 * Ported from Android's MonthViewFragment.java
 */
export function getVietnameseHoliday(date: Date, lang: 'vi' | 'en' = 'vi'): string | null {
  const day = date.getDate();
  const month = date.getMonth(); // 0-indexed

  // Solar Holidays
  if (month === 0 && day === 1) return lang === 'vi' ? 'Tết Dương lịch' : 'New Year\'s Day';
  if (month === 1 && day === 14) return 'Valentine';
  if (month === 2 && day === 8) return lang === 'vi' ? 'Quốc tế Phụ nữ' : 'Intl. Women\'s Day';
  if (month === 3 && day === 30) return lang === 'vi' ? 'Ngày Chiến thắng' : 'Reunification Day';
  if (month === 4 && day === 1) return lang === 'vi' ? 'Quốc tế Lao động' : 'Intl. Workers\' Day';
  if (month === 5 && day === 1) return lang === 'vi' ? 'Quốc tế Thiếu nhi' : 'Intl. Children\'s Day';
  if (month === 8 && day === 2) return lang === 'vi' ? 'Quốc khánh' : 'National Day';
  if (month === 9 && day === 20) return lang === 'vi' ? 'Ngày Phụ nữ VN' : 'Vietnamese Women\'s Day';
  if (month === 10 && day === 20) return lang === 'vi' ? 'Ngày Nhà giáo VN' : 'Vietnamese Teachers\' Day';
  if (month === 11 && day === 25) return lang === 'vi' ? 'Giáng sinh' : 'Christmas';

  // Lunar Holidays
  const lunar = convertSolarToLunar(date);
  const { day: lDay, month: lMonth, isLeapMonth } = lunar;

  if (!isLeapMonth) {
    // Tết Nguyên Đán
    if (lMonth === 1 && lDay >= 1 && lDay <= 3) {
      if (lDay === 1) return lang === 'vi' ? 'Tết Nguyên Đán' : 'Lunar New Year';
      return lang === 'vi' ? `Tết (Mùng ${lDay})` : `Tet (Day ${lDay})`;
    }
    if (lMonth === 1 && lDay === 15) return lang === 'vi' ? 'Tết Nguyên Tiêu' : 'Lantern Festival';
    if (lMonth === 3 && lDay === 3) return lang === 'vi' ? 'Tết Hàn Thực' : 'Cold Food Festival';
    if (lMonth === 3 && lDay === 10) return lang === 'vi' ? 'Giỗ Tổ Hùng Vương' : 'Hung Kings Commemoration';
    if (lMonth === 4 && lDay === 15) return lang === 'vi' ? 'Phật Đản' : 'Vesak Day';
    if (lMonth === 5 && lDay === 5) return lang === 'vi' ? 'Tết Đoan Ngọ' : 'Mid-year Festival';
    if (lMonth === 7 && lDay === 15) return lang === 'vi' ? 'Vu Lan' : 'Hungry Ghost Festival';
    if (lMonth === 8 && lDay === 15) return lang === 'vi' ? 'Tết Trung Thu' : 'Mid-Autumn Festival';
    if (lMonth === 9 && lDay === 9) return lang === 'vi' ? 'Tết Trùng Cửu' : 'Double Ninth Festival';
    if (lMonth === 12 && lDay === 23) return lang === 'vi' ? 'Tết Ông Công Ông Táo' : 'Kitchen Gods Day';
    
    // Giao Thừa
    if (lMonth === 12 && (lDay === 29 || lDay === 30)) {
      const tomorrow = new Date(date);
      tomorrow.setDate(tomorrow.getDate() + 1);
      const lunarTomorrow = convertSolarToLunar(tomorrow);
      if (lunarTomorrow.month === 1 && lunarTomorrow.day === 1 && !lunarTomorrow.isLeapMonth) {
        return lang === 'vi' ? 'Giao Thừa' : 'Lunar New Year\'s Eve';
      }
    }
  }

  return null;
}

