package com.test.lifehub.core.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.test.lifehub.core.receivers.AlarmReceiver;
import java.util.Date;

/**
 * Helper class để lên lịch và hủy Alarm cho Nhắc nhở
 * (Dựa trên giải pháp của bạn)
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
            // Kiểm tra quyền (mặc dù đã check ở MainActivity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Không có quyền SCHEDULE_EXACT_ALARM");
                    Toast.makeText(context, "Cần cấp quyền Báo thức để nhắc nhở hoạt động", Toast.LENGTH_LONG).show();
                    // Không return, vẫn cố gắng đặt lịch (có thể thất bại)
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
            Toast.makeText(context, "Không thể đặt báo thức do thiếu quyền", Toast.LENGTH_LONG).show();
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