package com.test.lifehub.core.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.test.lifehub.R;
import com.test.lifehub.core.receivers.AlarmReceiver;
import com.test.lifehub.core.services.ReminderService;

import java.util.Date;

/**
 * AlarmHelper - Quản lý Alarm cho Reminder (Nhắc nhở)
 * 
 * === MỤC ĐÍCH ===
 * Class tiện ích để lên lịch và hủy các Alarm cho:
 * - Note reminders (nhắc ghi chú)
 * - Task reminders (nhắc công việc)
 * - Calendar event notifications (thông báo sự kiện)
 * 
 * === ANDROID ALARM TYPES ===
 * 
 * 1. EXACT ALARMS (Dùng trong app này):
 *    - setExact(): Chính xác đến millisecond
 *    - setExactAndAllowWhileIdle(): Chính xác + chạy khi Doze mode
 *    - Yêu cầu: SCHEDULE_EXACT_ALARM permission (Android 12+)
 * 
 * 2. INEXACT ALARMS (Không dùng):
 *    - set(): Không chính xác, hệ thống batch để tiết kiệm pin
 *    - setWindow(): Chạy trong time window
 * 
 * === PERMISSION HANDLING (Android 12+) ===
 * 
 * Android 12 (API 31) bắt buộc SCHEDULE_EXACT_ALARM permission:
 * ```xml
 * <!-- AndroidManifest.xml -->
 * <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
 * ```
 * 
 * Runtime check:
 * ```java
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
 *     if (!alarmManager.canScheduleExactAlarms()) {
 *         // Mở Settings để user cấp quyền
 *         Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
 *         startActivity(intent);
 *         return;
 *     }
 * }
 * ```
 * 
 * === VÍ DỤ SỚ DỤNG ===
 * 
 * ```java
 * // 1. Lên lịch reminder cho note
 * Date reminderTime = new Date(System.currentTimeMillis() + 3600000); // +1 hour
 * int requestCode = 12345;  // Unique ID (dùng noteId hoặc taskId)
 * 
 * AlarmHelper.scheduleAlarm(
 *     context,
 *     requestCode,
 *     reminderTime,
 *     "Nhắc nhở",
 *     "Hoàn thành báo cáo tuần"
 * );
 * 
 * // 2. Hủy reminder khi xóa note
 * AlarmHelper.cancelAlarm(context, requestCode);
 * 
 * // 3. Update reminder (hủy cũ + tạo mới)
 * AlarmHelper.cancelAlarm(context, oldRequestCode);
 * AlarmHelper.scheduleAlarm(context, newRequestCode, newTime, title, content);
 * ```
 * 
 * === FLOW DIAGRAM ===
 * 
 * ```
 * scheduleAlarm()
 *    |
 *    v
 * Kiểm tra reminderTime > now?
 *    |
 *    ├─ No -> Return (không lên lịch)
 *    v
 * Kiểm tra SCHEDULE_EXACT_ALARM permission (Android 12+)?
 *    |
 *    ├─ No -> Mở Settings để cấp quyền
 *    v
 * Tạo PendingIntent với requestCode
 *    |
 *    v
 * setExactAndAllowWhileIdle(RTC_WAKEUP, triggerTime, pendingIntent)
 *    |
 *    v
 * Lưu requestCode vào Note/Task (dùng để hủy sau)
 * 
 * --- Khi đến giờ ---
 * 
 * AlarmReceiver.onReceive()
 *    |
 *    v
 * ReminderService.showNotification()
 *    |
 *    v
 * User click notification -> Mở AddEditNoteActivity
 * ```
 * 
 * === PENDING INTENT FLAGS ===
 * 
 * ```java
 * PendingIntent.getBroadcast(
 *     context,
 *     requestCode,  // Unique ID (quan trọng!)
 *     intent,
 *     FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE
 * );
 * ```
 * 
 * - FLAG_UPDATE_CURRENT: Update intent hiện có (nếu requestCode trùng)
 * - FLAG_IMMUTABLE: PendingIntent không thay đổi (bắt buộc Android 12+)
 * 
 * === DOZE MODE & APP STANDBY ===
 * 
 * setExactAndAllowWhileIdle() đảm bảo alarm chạy ngay cả khi:
 * - Device ở Doze mode (màn hình tắt lâu)
 * - App ở App Standby mode (không dùng lâu)
 * 
 * Giới hạn: Chỉ 1 alarm mỗi 9 phút trong Doze mode.
 * 
 * === REQUEST CODE STRATEGY ===
 * 
 * Để tránh conflict, dùng unique requestCode:
 * ```java
 * // Option 1: Hash documentId
 * int requestCode = note.documentId.hashCode();
 * 
 * // Option 2: Dùng alarmRequestCode field
 * note.alarmRequestCode = generateUniqueId();
 * int requestCode = note.alarmRequestCode;
 * 
 * // Option 3: Combine type + id
 * int requestCode = ("note_" + noteId).hashCode();
 * ```
 * 
 * === CANCELLATION ===
 * 
 * Để hủy alarm, PHẢI dùng đúng requestCode:
 * ```java
 * // Lưu requestCode khi tạo
 * note.alarmRequestCode = 12345;
 * scheduleAlarm(context, 12345, ...);
 * 
 * // Dùng requestCode đó để hủy
 * cancelAlarm(context, note.alarmRequestCode);
 * ```
 * 
 * === LƯU Ý QUAN TRỌNG ===
 * 
 * 1. PERMISSION:
 *    - Thêm SCHEDULE_EXACT_ALARM vào AndroidManifest.xml
 *    - Kiểm tra runtime trên Android 12+
 *    - Mở Settings nếu chưa có quyền
 * 
 * 2. REQUEST CODE:
 *    - PHẢI unique cho mỗi alarm
 *    - PHẢI lưu để hủy sau này
 *    - Tránh dùng random() (không reproduce được)
 * 
 * 3. TIME VALIDATION:
 *    - Luôn kiểm tra reminderTime > now
 *    - Không schedule alarm quá khứ
 * 
 * 4. RECEIVER:
 *    - Đăng ký AlarmReceiver trong AndroidManifest.xml
 *    - Handle intent extras đúng cách
 * 
 * === TROUBLESHOOTING ===
 * 
 * 1. Alarm không chạy:
 *    - Kiểm tra SCHEDULE_EXACT_ALARM permission
 *    - Kiểm tra battery optimization settings
 *    - Kiểm tra AlarmReceiver đã đăng ký chưa
 * 
 * 2. Alarm chạy sai giờ:
 *    - Kiểm tra timezone
 *    - Dùng System.currentTimeMillis() thay vì Calendar
 * 
 * 3. Không hủy được alarm:
 *    - Kiểm tra requestCode có đúng không
 *    - Kiểm tra PendingIntent flags
 * 
 * === TODO: TÍNH NĂNG TƯƠNG LAI ===
 * TODO: Thêm recurring alarms (lặp lại hàng ngày/tuần)
 * TODO: Thêm snooze functionality
 * TODO: Thêm notification channels customization
 * TODO: Thêm alarm history tracking
 * FIXME: Xử lý timezone changes
 * 
 * @see AlarmReceiver Nhận alarm broadcast
 * @see ReminderService Hiển thị notification
 */
public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    /**
     * Lên lịch một Alarm
     * @param context Context
     * @param requestCode ID duy nhất cho Alarm
     * @param triggerTime Thời gian kích hoạt (Date)
     * @param title Tiêu đề thông báo
     * @param content Nội dung thông báo
     */
    public static void scheduleAlarm(Context context, int requestCode, Date triggerTime, String title, String content) {
        if (triggerTime == null || triggerTime.getTime() <= System.currentTimeMillis()) {
            Log.w(TAG, "Không thể lên lịch: Thời gian đã qua hoặc null");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager không khả dụng");
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(Constants.EXTRA_REMINDER_TITLE, title);
        intent.putExtra(Constants.EXTRA_REMINDER_CONTENT, content);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            // Kiểm tra quyền SCHEDULE_EXACT_ALARM (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Không có quyền SCHEDULE_EXACT_ALARM");
                    Toast.makeText(context, R.string.permission_alarm_settings, Toast.LENGTH_LONG).show();
                    
                    // Mở cài đặt để cấp quyền
                    try {
                        Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(settingsIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Không thể mở cài đặt quyền", e);
                    }
                    return; // Dừng lại, không đặt alarm
                }
            }

            // Dùng setExactAndAllowWhileIdle để đảm bảo chính xác
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.getTime(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.getTime(),
                        pendingIntent
                );
            }
            Log.d(TAG, "✅ Đã lên lịch Alarm: " + title + " lúc " + triggerTime);

        } catch (SecurityException e) {
            Log.e(TAG, "Lỗi bảo mật khi lên lịch Alarm", e);
            Toast.makeText(context, R.string.permission_alarm_failed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Hủy một Alarm
     * @param context Context
     * @param requestCode ID của Alarm cần hủy
     */
    public static void cancelAlarm(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Log.d(TAG, "✅ Đã hủy Alarm với requestCode: " + requestCode);
    }
}