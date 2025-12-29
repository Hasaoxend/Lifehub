package com.test.lifehub.core.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.test.lifehub.R;
import com.test.lifehub.ui.MainActivity;

/**
 * Service (Dịch vụ) chạy nền để hiển thị thông báo nhắc nhở.
 * Service này được khởi chạy bởi AlarmManager.
 */
public class ReminderService extends Service {

    private static final String CHANNEL_ID = "LifeHubReminderChannel";
    private static final String CHANNEL_NAME = "Nhắc nhở LifeHub";
    private static final String CHANNEL_DESC = "Kênh thông báo cho các nhắc nhở công việc và ghi chú";

    public static final String EXTRA_REMINDER_TITLE = "REMINDER_TITLE";
    public static final String EXTRA_REMINDER_CONTENT = "REMINDER_CONTENT";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // Lấy dữ liệu (Tiêu đề, Nội dung) từ Intent đã được gửi bởi AlarmManager
            String title = intent.getStringExtra(EXTRA_REMINDER_TITLE);
            String content = intent.getStringExtra(EXTRA_REMINDER_CONTENT);

            if (title == null) title = "Bạn có một nhắc nhở!";
            if (content == null) content = "Hãy kiểm tra công việc/ghi chú của bạn.";

            // Tạo hành động khi người dùng nhấn vào thông báo (mở lại app)
            Intent mainIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Xây dựng thông báo
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_alarm) // TODO: Thay bằng icon báo thức của bạn
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true) // Tự động xóa thông báo khi người dùng nhấn
                    .setContentIntent(pendingIntent); // Gán hành động

            // Hiển thị thông báo
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            int notificationId = (int) System.currentTimeMillis(); // ID duy nhất

            // Cần kiểm tra quyền trước khi gửi thông báo (cho Android 13+)
            // Logic kiểm tra quyền nên ở Activity, ở đây ta giả định đã có quyền
            try {
                notificationManager.notify(notificationId, builder.build());
            } catch (SecurityException e) {
                // Xử lý trường hợp không có quyền POST_NOTIFICATIONS
                e.printStackTrace();
            }
        }

        // Tự động dừng Service sau khi hoàn thành nhiệm vụ
        stopSelf(startId);

        return START_NOT_STICKY;
    }

    /**
     * Tạo Kênh Thông báo (Notification Channel).
     * Bắt buộc phải có cho Android 8.0 (API 26) trở lên.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Không dùng (vì đây là Started Service, không phải Bound Service)
        return null;
    }
}