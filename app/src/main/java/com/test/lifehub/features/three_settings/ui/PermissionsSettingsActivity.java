package com.test.lifehub.features.three_settings.ui;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.test.lifehub.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity quản lý quyền ứng dụng
 * Hiển thị danh sách các quyền và cho phép người dùng bật/tắt
 */
public class PermissionsSettingsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private RecyclerView recyclerView;
    private PermissionAdapter adapter;
    private List<PermissionItem> permissionItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_manage_permissions);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_permissions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        initPermissionsList();
        adapter = new PermissionAdapter(permissionItems);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật trạng thái quyền khi quay lại màn hình
        updatePermissionsStatus();
        adapter.notifyDataSetChanged();
    }

    private void initPermissionsList() {
        permissionItems = new ArrayList<>();

        // Camera
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionItems.add(new PermissionItem(
                "Camera",
                "Cho phép chụp ảnh QR code để thêm tài khoản xác thực 2 bước",
                Manifest.permission.CAMERA,
                false
            ));
        }

        // Thông báo (POST_NOTIFICATIONS - Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionItems.add(new PermissionItem(
                "Thông báo",
                "Hiển thị thông báo nhắc nhở và cảnh báo bảo mật",
                Manifest.permission.POST_NOTIFICATIONS,
                false
            ));
        }

        // Vị trí (nếu ứng dụng cần)
        permissionItems.add(new PermissionItem(
            "Vị trí",
            "Lấy thông tin thời tiết dựa trên vị trí hiện tại",
            Manifest.permission.ACCESS_FINE_LOCATION,
            false
        ));

        // Lưu trữ (READ/WRITE - Android 12 trở xuống)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionItems.add(new PermissionItem(
                "Lưu trữ",
                "Đọc và ghi file để xuất/nhập dữ liệu",
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                false
            ));
        }

        // Biometric (không phải runtime permission, chỉ hiển thị thông tin)
        permissionItems.add(new PermissionItem(
            "Sinh trắc học",
            "Sử dụng vân tay hoặc khuôn mặt để xác thực",
            "android.permission.USE_BIOMETRIC",
            true // Là quyền đặc biệt, không cần request
        ));

        updatePermissionsStatus();
    }

    private void updatePermissionsStatus() {
        for (PermissionItem item : permissionItems) {
            if (item.isSpecialPermission) {
                // Quyền đặc biệt không cần check runtime
                item.isGranted = true;
            } else {
                item.isGranted = ContextCompat.checkSelfPermission(this, item.permission) 
                    == PackageManager.PERMISSION_GRANTED;
            }
        }
    }

    private void requestPermission(PermissionItem item) {
        if (item.isSpecialPermission) {
            Toast.makeText(this, R.string.permission_system_managed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, item.permission)) {
            // Hiển thị giải thích tại sao cần quyền
            new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.title_grant_permission, item.name))
                .setMessage(item.description)
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    ActivityCompat.requestPermissions(
                        PermissionsSettingsActivity.this,
                        new String[]{item.permission},
                        PERMISSION_REQUEST_CODE
                    );
                })
                .setNegativeButton("Hủy", null)
                .show();
        } else {
            ActivityCompat.requestPermissions(
                this,
                new String[]{item.permission},
                PERMISSION_REQUEST_CODE
            );
        }
    }

    private void revokePermission(PermissionItem item) {
        if (item.isSpecialPermission) {
            return;
        }

        // Android không cho phép ứng dụng tự thu hồi quyền
        // Cần hướng dẫn người dùng vào cài đặt hệ thống
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.title_revoke_permission)
            .setMessage(R.string.msg_revoke_permission_instructions)
            .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            updatePermissionsStatus();
            adapter.notifyDataSetChanged();
            
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted_short, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ===== ADAPTER =====
    class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder> {

        private List<PermissionItem> items;

        public PermissionAdapter(List<PermissionItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permission, parent, false);
            return new PermissionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
            PermissionItem item = items.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class PermissionViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDescription, tvStatus;
            MaterialSwitch switchPermission;

            public PermissionViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_permission_name);
                tvDescription = itemView.findViewById(R.id.tv_permission_description);
                tvStatus = itemView.findViewById(R.id.tv_permission_status);
                switchPermission = itemView.findViewById(R.id.switch_permission);
            }

            public void bind(PermissionItem item) {
                tvName.setText(item.name);
                tvDescription.setText(item.description);
                
                // Cập nhật trạng thái
                if (item.isGranted) {
                    tvStatus.setText(R.string.permission_granted);
                    tvStatus.setTextColor(getColor(R.color.green_500));
                } else {
                    tvStatus.setText(R.string.permission_not_granted);
                    tvStatus.setTextColor(getColor(R.color.orange_500));
                }

                // Tắt listener tạm thời để tránh trigger khi set checked
                switchPermission.setOnCheckedChangeListener(null);
                switchPermission.setChecked(item.isGranted);

                // Vô hiệu hóa switch cho quyền đặc biệt
                switchPermission.setEnabled(!item.isSpecialPermission);

                // Gắn listener
                switchPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked && !item.isGranted) {
                        // Yêu cầu cấp quyền
                        requestPermission(item);
                        // Reset lại switch vì permission chưa được cấp ngay lập tức
                        buttonView.setChecked(false);
                    } else if (!isChecked && item.isGranted) {
                        // Thu hồi quyền
                        revokePermission(item);
                        // Giữ nguyên trạng thái granted
                        buttonView.setChecked(true);
                    }
                });
            }
        }
    }

    // ===== MODEL =====
    static class PermissionItem {
        String name;
        String description;
        String permission;
        boolean isGranted;
        boolean isSpecialPermission; // Quyền đặc biệt không cần runtime request

        public PermissionItem(String name, String description, String permission, boolean isSpecialPermission) {
            this.name = name;
            this.description = description;
            this.permission = permission;
            this.isSpecialPermission = isSpecialPermission;
            this.isGranted = false;
        }
    }
}
