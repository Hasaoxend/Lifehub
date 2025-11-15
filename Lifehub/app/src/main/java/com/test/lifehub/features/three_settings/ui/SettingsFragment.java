package com.test.lifehub.features.three_settings.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.test.lifehub.R;
import com.test.lifehub.core.security.BiometricHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.ui.LoginActivity;

/**
 * Fragment (màn hình) cho tab "Cài đặt".
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 */
public class SettingsFragment extends Fragment implements BiometricHelper.BiometricAuthListener {

    private static final String TAG = "SettingsFragment";

    private MaterialSwitch mSwitchBiometric;
    private TextView mTvBiometricDesc;
    private Button mBtnLogout;
    private LinearLayout mBtnChangeTheme;
    private TextView mTvCurrentTheme;

    private SessionManager mSessionManager;
    private boolean mPendingBiometricState = false;

    // (Không cần ExecutorService nữa)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mSessionManager = new SessionManager(requireContext());

        // Ánh xạ View
        mSwitchBiometric = view.findViewById(R.id.switch_biometric);
        mTvBiometricDesc = view.findViewById(R.id.tv_biometric_description);
        mBtnLogout = view.findViewById(R.id.btn_logout);
        mBtnChangeTheme = view.findViewById(R.id.btn_change_theme);
        mTvCurrentTheme = view.findViewById(R.id.tv_current_theme);

        // Cài đặt
        setupBiometricSwitch();
        setupLogoutButton();
        setupThemeButton();

        return view;
    }


    private void setupLogoutButton() {
        mBtnLogout.setOnClickListener(v -> {
            // SỬA LỖI: Cập nhật thông báo Cảnh báo (bỏ chữ "Xóa Dữ liệu")
            new AlertDialog.Builder(requireContext())
                    .setTitle("Xác nhận Đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi LifeHub?")
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        performLogout(); // Gọi hàm đăng xuất
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    /**
     * VIẾT LẠI: Hàm Đăng xuất
     * Xóa sạch CSDL (clearAllTables) KHÔNG CÒN NỮA.
     */
    private void performLogout() {
        Toast.makeText(getContext(), "Đang đăng xuất...", Toast.LENGTH_SHORT).show();

        // 1. Đăng xuất khỏi Firebase
        FirebaseAuth.getInstance().signOut();
        Log.d(TAG, "Đã đăng xuất Firebase.");

        // 2. Xóa session local (Chỉ xóa login token, giữ lại cài đặt)
        mSessionManager.logoutUser();
        Log.d(TAG, "Đã xóa Session local.");

        // 3. Quay về LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    //
    // ----- (CÁC HÀM CÒN LẠI GIỮ NGUYÊN) -----
    // (setupBiometricSwitch, setupThemeButton, showThemeChooserDialog,
    //  updateThemeSummary, onBiometricAuthSuccess,
    //  onBiometricAuthError, onBiometricAuthFailed)
    //

    private void setupThemeButton() {
        updateThemeSummary(mSessionManager.getThemeMode());
        mBtnChangeTheme.setOnClickListener(v -> showThemeChooserDialog());
    }

    private void showThemeChooserDialog() {
        final String[] themes = {"Sáng", "Tối", "Tự động theo Hệ thống"};
        int currentMode = mSessionManager.getThemeMode();
        int currentChoice;
        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) currentChoice = 0;
        else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) currentChoice = 1;
        else currentChoice = 2;

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn Giao diện")
                .setSingleChoiceItems(themes, currentChoice, (dialog, which) -> {
                    int selectedMode;
                    switch (which) {
                        case 0: selectedMode = AppCompatDelegate.MODE_NIGHT_NO; break;
                        case 1: selectedMode = AppCompatDelegate.MODE_NIGHT_YES; break;
                        default: selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
                    }
                    mSessionManager.setThemeMode(selectedMode);
                    AppCompatDelegate.setDefaultNightMode(selectedMode);
                    updateThemeSummary(selectedMode);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateThemeSummary(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) mTvCurrentTheme.setText("Sáng");
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES) mTvCurrentTheme.setText("Tối");
        else mTvCurrentTheme.setText("Tự động theo Hệ thống");
    }

    private void setupBiometricSwitch() {
        if (BiometricHelper.isBiometricAvailable(requireContext())) {
            mSwitchBiometric.setEnabled(true);
            mSwitchBiometric.setChecked(mSessionManager.isBiometricEnabled());
            mSwitchBiometric.setOnClickListener(v -> {
                boolean newState = mSwitchBiometric.isChecked();
                mPendingBiometricState = newState;
                BiometricHelper.showBiometricPrompt((AppCompatActivity) requireActivity(), this);
            });
        } else {
            mSwitchBiometric.setChecked(false);
            mSwitchBiometric.setEnabled(false);
            mTvBiometricDesc.setText("Thiết bị không hỗ trợ hoặc chưa cài đặt vân tay.");
        }
    }

    @Override
    public void onBiometricAuthSuccess() {
        mSessionManager.setBiometricEnabled(mPendingBiometricState);
        Log.d(TAG, "Vân tay HỢP LỆ. Đã lưu cài đặt mới: " + mPendingBiometricState);
        Toast.makeText(getContext(), "Đã lưu cài đặt vân tay", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBiometricAuthError(String errorMessage) {
        Log.w(TAG, "Xác thực vân tay bị hủy/lỗi: " + errorMessage);
        Toast.makeText(getContext(), "Xác thực bị hủy. Cài đặt không thay đổi.", Toast.LENGTH_SHORT).show();
        mSwitchBiometric.setChecked(mSessionManager.isBiometricEnabled());
    }

    @Override
    public void onBiometricAuthFailed() {
        Log.w(TAG, "Vân tay không khớp.");
        Toast.makeText(getContext(), "Vân tay không khớp. Cài đặt không thay đổi.", Toast.LENGTH_SHORT).show();
        mSwitchBiometric.setChecked(mSessionManager.isBiometricEnabled());
    }
}