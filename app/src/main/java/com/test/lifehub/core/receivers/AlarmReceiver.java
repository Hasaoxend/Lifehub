package com.test.lifehub.core.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.test.lifehub.R;
import com.test.lifehub.core.util.Constants;
import com.test.lifehub.ui.MainActivity;

/**
 * BroadcastReceiver để "bắt" Alarm đã được lên lịch.
 * ✅ SỬA LỖI: Hiển thị notification trực tiếp thay vì start service
 * để tránh lỗi ForegroundServiceDidNotStartInTimeException
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "LifeHubReminderChannel";
    private static final String CHANNEL_NAME = "Nhắc nhở LifeHub";
    private static final String CHANNEL_DESC = "Kênh thông báo cho các nhắc nhở công việc và ghi chú";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "⏰ Alarm đã reo!");

        if (intent != null) {
            String title = intent.getStringExtra(Constants.EXTRA_REMINDER_TITLE);
            String content = intent.getStringExtra(Constants.EXTRA_REMINDER_CONTENT);

            if (title == null) title = "Bạn có một nhắc nhở!";
            if (content == null) content = "Hãy kiểm tra công việc/ghi chú của bạn.";

            // Tạo notification channel (nếu chưa có)
            createNotificationChannel(context);

            // Hiển thị notification trực tiếp
            showNotification(context, title, content);
        }
    }

    /**
     * Hiển thị notification
     */
    private void showNotification(Context context, String title, String content) {
        // Tạo intent mở app khi click notification
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Xây dựng thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = (int) System.currentTimeMillis(); // ID duy nhất

        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "✅ Notification displayed: " + title);
        } catch (SecurityException e) {
            Log.e(TAG, "❌ No notification permission", e);
        }
    }

    /**
     * Tạo Notification Channel (bắt buộc cho Android 8.0+)
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}