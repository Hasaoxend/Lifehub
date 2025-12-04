package com.test.lifehub.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import com.google.android.material.button.MaterialButton;
import com.test.lifehub.R;
import com.test.lifehub.core.base.BaseActivity;
import com.test.lifehub.core.util.LocaleHelper;
import com.test.lifehub.core.util.SessionManager;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Màn hình chọn ngôn ngữ - hiển thị lần đầu tiên mở ứng dụng
 */
@AndroidEntryPoint
public class LanguageSelectionActivity extends BaseActivity {

    @Inject
    SessionManager sessionManager;

    private RadioGroup radioGroupLanguage;
    private RadioButton radioEnglish;
    private RadioButton radioVietnamese;
    private MaterialButton btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Kiểm tra xem đã chọn ngôn ngữ chưa
        if (!sessionManager.isFirstRun()) {
            navigateToIntro();
            return;
        }

        setContentView(R.layout.activity_language_selection);

        findViews();
        setupListeners();
        setupBackPressedCallback();
        preselectLanguage();
    }

    private void findViews() {
        radioGroupLanguage = findViewById(R.id.radio_group_language);
        radioEnglish = findViewById(R.id.radio_english);
        radioVietnamese = findViewById(R.id.radio_vietnamese);
        btnContinue = findViewById(R.id.btn_continue);
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(v -> {
            int selectedId = radioGroupLanguage.getCheckedRadioButtonId();
            
            if (selectedId == -1) {
                Toast.makeText(this, R.string.please_select_language, Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedLanguage;
            if (selectedId == R.id.radio_english) {
                selectedLanguage = LocaleHelper.LANGUAGE_ENGLISH;
            } else {
                selectedLanguage = LocaleHelper.LANGUAGE_VIETNAMESE;
            }

            // Lưu ngôn ngữ
            LocaleHelper.saveLanguage(this, selectedLanguage);
            
            // Áp dụng ngôn ngữ ngay lập tức
            LocaleHelper.setLocale(this, selectedLanguage);
            
            // Chuyển đến màn hình Intro
            navigateToIntro();
        });
    }

    /**
     * Tự động chọn ngôn ngữ dựa trên ngôn ngữ hệ thống
     */
    private void preselectLanguage() {
        String systemLang = LocaleHelper.getLanguage(this);
        
        if (systemLang.equals(LocaleHelper.LANGUAGE_VIETNAMESE)) {
            radioVietnamese.setChecked(true);
        } else {
            radioEnglish.setChecked(true);
        }
    }

    private void navigateToIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Không cho phép quay lại từ màn hình chọn ngôn ngữ
            }
        });
    }
}
