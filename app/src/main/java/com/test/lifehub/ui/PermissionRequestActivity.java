package com.test.lifehub.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.test.lifehub.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình yêu cầu quyền lần đầu sử dụng ứng dụng
 */
public class PermissionRequestActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final String EXTRA_FROM_INTRO = "from_intro";

    private RecyclerView recyclerPermissions;
    private MaterialButton btnGrantAll;
    private MaterialButton btnContinue;
    private TextView tvStatus;
    
    private PermissionAdapter adapter;
    private List<PermissionItem> permissionItems;
    private boolean isFromIntro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_request);

        isFromIntro = getIntent().getBooleanExtra(EXTRA_FROM_INTRO, true);

        recyclerPermissions = findViewById(R.id.recycler_permissions);
        btnGrantAll = findViewById(R.id.btn_grant_all);
        btnContinue = findViewById(R.id.btn_continue);
        tvStatus = findViewById(R.id.tv_status);

        setupPermissionsList();
        setupRecyclerView();
        setupButtons();
        updateUI();
    }

    private void setupPermissionsList() {
        permissionItems = new ArrayList<>();

        // 1. Camera (bắt buộc cho scan QR code TOTP)
        permissionItems.add(new PermissionItem(
            "Camera",
            "Quét mã QR để thêm xác thực 2 bước (TOTP)",
            Manifest.permission.CAMERA,
            R.drawable.ic_camera,
            true
        ));

        // 2. Thông báo (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionItems.add(new PermissionItem(
                "Thông báo",
                "Nhận thông báo nhắc nhở sự kiện và nhiệm vụ",
                Manifest.permission.POST_NOTIFICATIONS,
                R.drawable.ic_notifications,
                true
            ));
        }

        // 3. Báo thức chính xác (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionItems.add(new PermissionItem(
                "Báo thức",
                "Đặt nhắc nhở chính xác cho sự kiện",
                "SCHEDULE_EXACT_ALARM",
                R.drawable.ic_alarm,
                true
            ));
        }

        // 4. Lưu trữ (BẮT BUỘC - xuất/nhập dữ liệu sao lưu) - Android 12 trở xuống
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionItems.add(new PermissionItem(
                "Lưu trữ",
                "Xuất và nhập dữ liệu sao lưu (bắt buộc để bảo vệ dữ liệu)",
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                R.drawable.ic_storage,
                true
            ));
        }

        // 5. Vị trí (tùy chọn - cho tính năng thời tiết)
        permissionItems.add(new PermissionItem(
            "Vị trí",
            "Lấy thông tin thời tiết dựa trên vị trí của bạn",
            Manifest.permission.ACCESS_FINE_LOCATION,
            R.drawable.ic_location,
            false
        ));

        checkAllPermissions();
    }

    private void setupRecyclerView() {
        adapter = new PermissionAdapter(permissionItems);
        recyclerPermissions.setLayoutManager(new LinearLayoutManager(this));
        recyclerPermissions.setAdapter(adapter);
    }

    private void setupButtons() {
        btnGrantAll.setOnClickListener(v -> requestAllPermissions());
        btnContinue.setOnClickListener(v -> continueToApp());
    }

    private void checkAllPermissions() {
        for (PermissionItem item : permissionItems) {
            if (item.permission.equals("SCHEDULE_EXACT_ALARM")) {
                // Kiểm tra quyền báo thức
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
                    item.isGranted = alarmManager != null && alarmManager.canScheduleExactAlarms();
                } else {
                    item.isGranted = true;
                }
            } else {
                // Kiểm tra quyền runtime thông thường
                item.isGranted = ContextCompat.checkSelfPermission(this, item.permission) 
                    == PackageManager.PERMISSION_GRANTED;
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        for (PermissionItem item : permissionItems) {
            if (!item.isGranted && !item.permission.equals("SCHEDULE_EXACT_ALARM")) {
                permissionsToRequest.add(item.permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
            );
        }

        // Xử lý quyền báo thức riêng
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                    Toast.makeText(this, "Vui lòng bật quyền 'Alarms & reminders'", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            checkAllPermissions();
            updateUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions khi quay lại (từ Settings)
        checkAllPermissions();
        updateUI();
    }

    private void updateUI() {
        int granted = 0;
        int required = 0;
        
        for (PermissionItem item : permissionItems) {
            if (item.isGranted) granted++;
            if (item.isRequired) required++;
        }

        int total = permissionItems.size();
        tvStatus.setText(String.format("Đã cấp: %d/%d quyền", granted, total));

        // Chỉ cho phép tiếp tục nếu tất cả quyền bắt buộc đã được cấp
        boolean allRequiredGranted = true;
        for (PermissionItem item : permissionItems) {
            if (item.isRequired && !item.isGranted) {
                allRequiredGranted = false;
                break;
            }
        }

        btnContinue.setEnabled(allRequiredGranted);
        if (allRequiredGranted) {
            btnContinue.setText("Tiếp tục");
            btnGrantAll.setVisibility(View.GONE);
        } else {
            btnContinue.setText("Vui lòng cấp quyền bắt buộc");
            btnGrantAll.setVisibility(View.VISIBLE);
        }
    }

    private void continueToApp() {
        if (isFromIntro) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // Không cho phép back nếu từ intro
        if (!isFromIntro) {
            super.onBackPressed();
        }
    }

    // ===== ADAPTER =====
    class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.ViewHolder> {
        private List<PermissionItem> items;

        PermissionAdapter(List<PermissionItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permission_request, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PermissionItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvDescription, tvStatus, tvRequired;
            MaterialCardView cardView;

            ViewHolder(View view) {
                super(view);
                ivIcon = view.findViewById(R.id.iv_permission_icon);
                tvName = view.findViewById(R.id.tv_permission_name);
                tvDescription = view.findViewById(R.id.tv_permission_description);
                tvStatus = view.findViewById(R.id.tv_permission_status);
                tvRequired = view.findViewById(R.id.tv_required);
                cardView = (MaterialCardView) view;
            }

            void bind(PermissionItem item) {
                ivIcon.setImageResource(item.iconRes);
                tvName.setText(item.name);
                tvDescription.setText(item.description);
                
                if (item.isGranted) {
                    tvStatus.setText("✓ Đã cấp");
                    tvStatus.setTextColor(getColor(R.color.status_success));
                    cardView.setStrokeColor(getColor(R.color.status_success));
                } else {
                    tvStatus.setText("✗ Chưa cấp");
                    tvStatus.setTextColor(getColor(R.color.status_error));
                    cardView.setStrokeColor(getColor(R.color.status_error));
                }

                tvRequired.setVisibility(item.isRequired ? View.VISIBLE : View.GONE);
            }
        }
    }

    // ===== MODEL =====
    static class PermissionItem {
        String name;
        String description;
        String permission;
        int iconRes;
        boolean isGranted;
        boolean isRequired;

        PermissionItem(String name, String description, String permission, int iconRes, boolean isRequired) {
            this.name = name;
            this.description = description;
            this.permission = permission;
            this.iconRes = iconRes;
            this.isRequired = isRequired;
            this.isGranted = false;
        }
    }
}
