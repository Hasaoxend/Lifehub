package com.test.lifehub.core.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.test.lifehub.core.services.ReminderService;
import com.test.lifehub.core.util.Constants;

/**
 * BroadcastReceiver để "bắt" Alarm đã được lên lịch.
 * Nhiệm vụ duy nhất của nó là khởi động ReminderService.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm đã reo!");

        if (intent != null) {
            String title = intent.getStringExtra(Constants.EXTRA_REMINDER_TITLE);
            String content = intent.getStringExtra(Constants.EXTRA_REMINDER_CONTENT);

            // Tạo Intent mới để khởi động Service
            Intent serviceIntent = new Intent(context, ReminderService.class);
            serviceIntent.putExtra(Constants.EXTRA_REMINDER_TITLE, title);
            serviceIntent.putExtra(Constants.EXTRA_REMINDER_CONTENT, content);

            // Khởi động service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}