package com.test.lifehub.core.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.content.Context;

import androidx.core.content.ContextCompat;

import com.test.lifehub.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Class trợ giúp tạo icon cho các dịch vụ
 * 
 * Chức năng chính:
 * 1. Tạo "letter avatar" (icon chữ cái đầu) cho dịch vụ không xác định
 * 2. Cung cấp màu sắc nhất quán dựa trên tên dịch vụ (hash-based)
 * 3. Hỗ trợ icon branded cho các dịch vụ phổ biến (nếu có)
 * 
 * Ví dụ:
 * - "Google" → Chữ "G" màu trắng trên nền đỏ Google (#DB4437)
 * - "Unknown Service" → Chữ "U" màu trắng trên nền màu ngẫu nhiên (nhưng nhất quán)
 */
public class ServiceIconHelper {
    
    private static final Map<String, Integer> SERVICE_ICONS = new HashMap<>();
    
    static {
        // Add known service icons here
        // SERVICE_ICONS.put("google", R.drawable.ic_google);
        // SERVICE_ICONS.put("facebook", R.drawable.ic_facebook);
        // SERVICE_ICONS.put("github", R.drawable.ic_github);
        // SERVICE_ICONS.put("twitter", R.drawable.ic_twitter);
        // etc...
    }
    
    /**
     * Get icon for a service
     * @param context Context
     * @param serviceName Name of service
     * @return Drawable icon or generated letter avatar
     */
    public static Drawable getServiceIcon(Context context, String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) {
            return generateLetterAvatar(context, "?", Color.GRAY);
        }
        
        String serviceKey = serviceName.toLowerCase().trim();
        
        // Check if we have a predefined icon
        if (SERVICE_ICONS.containsKey(serviceKey)) {
            return ContextCompat.getDrawable(context, SERVICE_ICONS.get(serviceKey));
        }
        
        // Generate letter avatar
        String letter = serviceName.substring(0, 1).toUpperCase();
        int color = getColorForService(serviceName);
        
        return generateLetterAvatar(context, letter, color);
    }
    
    /**
     * Generate a letter avatar drawable
     */
    private static Drawable generateLetterAvatar(Context context, String letter, int color) {
        int size = 120; // Avatar size in pixels
        
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // Draw circle background
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint);
        
        // Draw letter
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size * 0.5f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float textY = size / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2f;
        
        canvas.drawText(letter, size / 2f, textY, textPaint);
        
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    
    /**
     * Generate consistent color for service name
     */
    private static int getColorForService(String serviceName) {
        // Predefined colors for popular services
        Map<String, Integer> serviceColors = new HashMap<>();
        serviceColors.put("google", Color.parseColor("#DB4437"));
        serviceColors.put("facebook", Color.parseColor("#1877F2"));
        serviceColors.put("github", Color.parseColor("#333333"));
        serviceColors.put("twitter", Color.parseColor("#1DA1F2"));
        serviceColors.put("instagram", Color.parseColor("#E4405F"));
        serviceColors.put("linkedin", Color.parseColor("#0A66C2"));
        serviceColors.put("microsoft", Color.parseColor("#00A4EF"));
        serviceColors.put("amazon", Color.parseColor("#FF9900"));
        serviceColors.put("apple", Color.parseColor("#000000"));
        serviceColors.put("discord", Color.parseColor("#5865F2"));
        serviceColors.put("reddit", Color.parseColor("#FF4500"));
        serviceColors.put("dropbox", Color.parseColor("#0061FF"));
        
        String serviceKey = serviceName.toLowerCase().trim();
        if (serviceColors.containsKey(serviceKey)) {
            return serviceColors.get(serviceKey);
        }
        
        // Generate color based on hash
        int hash = serviceName.hashCode();
        Random random = new Random(hash);
        
        // Generate pleasant colors (avoid too dark or too light)
        int r = random.nextInt(156) + 50;  // 50-205
        int g = random.nextInt(156) + 50;
        int b = random.nextInt(156) + 50;
        
        return Color.rgb(r, g, b);
    }
}
