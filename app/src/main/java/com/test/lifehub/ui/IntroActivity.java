package com.test.lifehub.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.test.lifehub.R;
import com.test.lifehub.core.base.BaseActivity;
import com.test.lifehub.core.util.SessionManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class IntroActivity extends BaseActivity {

    @Inject
    SessionManager sessionManager;

    private ViewPager2 viewPager;
    private Button btnAction;
    private TextView tvSkip;
    private IntroAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. KIỂM TRA: Nếu không phải lần đầu mở app -> Vào Login luôn
        if (!sessionManager.isFirstRun()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_intro);

        viewPager = findViewById(R.id.viewPager);
        btnAction = findViewById(R.id.btn_action);
        tvSkip = findViewById(R.id.tv_skip);

        setupViewPager();
        setupListeners();
    }

    private void setupViewPager() {
        List<IntroItem> items = new ArrayList<>();
        items.add(new IntroItem(
            getString(R.string.intro_welcome_title), 
            getString(R.string.intro_welcome_desc), 
            R.drawable.ic_lock
        ));
        items.add(new IntroItem(
            getString(R.string.intro_security_title), 
            getString(R.string.intro_security_desc), 
            R.drawable.ic_fingerprint
        ));
        items.add(new IntroItem(
            getString(R.string.intro_notification_title), 
            getString(R.string.intro_notification_desc), 
            R.drawable.ic_alarm
        ));

        adapter = new IntroAdapter(items);
        viewPager.setAdapter(adapter);

        // Lắng nghe sự kiện vuốt trang để đổi chữ nút bấm
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == items.size() - 1) {
                    btnAction.setText(R.string.grant_permission_and_start);
                } else {
                    btnAction.setText(R.string.continue_text);
                }
            }
        });
    }

    private void setupListeners() {
        btnAction.setOnClickListener(v -> {
            int currentItem = viewPager.getCurrentItem();
            if (currentItem < adapter.getItemCount() - 1) {
                // Chưa đến trang cuối -> Chuyển trang kế
                viewPager.setCurrentItem(currentItem + 1);
            } else {
                // Trang cuối -> Xin quyền và kết thúc
                requestNotificationPermission();
            }
        });

        tvSkip.setOnClickListener(v -> finishIntro());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                finishIntro();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        } else {
            finishIntro();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Dù người dùng đồng ý hay từ chối, ta vẫn cho vào app
        finishIntro();
    }

    private void finishIntro() {
        sessionManager.setFirstRun(false); // Đánh dấu đã xong intro
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // --- INNER CLASSES CHO GỌN ---

    static class IntroItem {
        String title, desc;
        int imageRes;
        IntroItem(String title, String desc, int imageRes) {
            this.title = title; this.desc = desc; this.imageRes = imageRes;
        }
    }

    class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.IntroViewHolder> {
        private final List<IntroItem> items;
        IntroAdapter(List<IntroItem> items) { this.items = items; }

        @NonNull @Override
        public IntroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new IntroViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_intro_slide, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull IntroViewHolder holder, int position) {
            IntroItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvDesc.setText(item.desc);
            holder.ivImage.setImageResource(item.imageRes);
        }

        @Override public int getItemCount() { return items.size(); }

        class IntroViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDesc;
            ImageView ivImage;
            IntroViewHolder(View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tv_intro_title);
                tvDesc = view.findViewById(R.id.tv_intro_description);
                ivImage = view.findViewById(R.id.iv_intro_image);
            }
        }
    }
}