package com.test.lifehub.features.one_accounts.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.test.lifehub.R;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Hộp thoại (Dialog) để tạo mật khẩu mạnh, giống Bitwarden.
 */
public class PasswordGeneratorDialog extends DialogFragment {

    // Interface để gửi mật khẩu đã tạo trở lại Activity
    public interface PasswordGeneratedListener {
        void onPasswordGenerated(String password);
    }

    private PasswordGeneratedListener mListener;

    // --- Các bộ ký tự ---
    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String OTHER_CHAR = "!@#$%&*_-.()[]{}";

    // --- Views ---
    private TextInputEditText etGeneratedPassword;
    private TextInputLayout layoutGeneratedPassword;
    private Slider mSlider;
    private TextView tvLengthLabel;
    private MaterialCheckBox cbUppercase, cbNumbers, cbSymbols;
    private Button btnRegenerate, btnSelect;

    private int mLength = 16;
    private boolean mUseUppercase = true;
    private boolean mUseNumbers = true;
    private boolean mUseSymbols = true;

    // Gán Listener khi Dialog được khởi tạo
    public void setListener(PasswordGeneratedListener listener) {
        this.mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_generate_password, container, false);

        // Ánh xạ Views
        etGeneratedPassword = view.findViewById(R.id.et_generated_password);
        layoutGeneratedPassword = view.findViewById(R.id.layout_generated_password);
        mSlider = view.findViewById(R.id.slider_length);
        tvLengthLabel = view.findViewById(R.id.tv_length_label);
        cbUppercase = view.findViewById(R.id.cb_uppercase);
        cbNumbers = view.findViewById(R.id.cb_numbers);
        cbSymbols = view.findViewById(R.id.cb_symbols);
        btnRegenerate = view.findViewById(R.id.btn_regenerate);
        btnSelect = view.findViewById(R.id.btn_select_password);

        // Thiết lập Listeners
        setupListeners();

        // Tạo mật khẩu lần đầu
        generatePassword();

        return view;
    }

    private void setupListeners() {
        // Thanh trượt
        mSlider.addOnChangeListener((slider, value, fromUser) -> {
            mLength = (int) value;
            tvLengthLabel.setText("Độ dài: " + mLength);
            generatePassword(); // Tự động tạo lại khi trượt
        });

        // Checkboxes
        cbUppercase.setOnCheckedChangeListener((v, isChecked) -> {
            mUseUppercase = isChecked;
            generatePassword();
        });
        cbNumbers.setOnCheckedChangeListener((v, isChecked) -> {
            mUseNumbers = isChecked;
            generatePassword();
        });
        cbSymbols.setOnCheckedChangeListener((v, isChecked) -> {
            mUseSymbols = isChecked;
            generatePassword();
        });

        // Nút Tạo lại
        btnRegenerate.setOnClickListener(v -> generatePassword());

        // Nút Sao chép
        layoutGeneratedPassword.setEndIconOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Password", etGeneratedPassword.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Đã sao chép mật khẩu!", Toast.LENGTH_SHORT).show();
        });

        // Nút CHỌN
        btnSelect.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPasswordGenerated(etGeneratedPassword.getText().toString());
            }
            dismiss();
        });
    }

    /**
     * Hàm logic chính để tạo mật khẩu dựa trên các tùy chọn
     */
    private void generatePassword() {
        // 1. Xây dựng bộ ký tự (character pool)
        StringBuilder charPool = new StringBuilder(CHAR_LOWER);
        List<String> guaranteedChars = new ArrayList<>();
        guaranteedChars.add(CHAR_LOWER); // Luôn có chữ thường

        if (mUseUppercase) {
            charPool.append(CHAR_UPPER);
            guaranteedChars.add(CHAR_UPPER);
        }
        if (mUseNumbers) {
            charPool.append(NUMBER);
            guaranteedChars.add(NUMBER);
        }
        if (mUseSymbols) {
            charPool.append(OTHER_CHAR);
            guaranteedChars.add(OTHER_CHAR);
        }

        // Đảm bảo các checkbox không bị vô hiệu hóa
        checkMinimumRequirements();

        String allChars = charPool.toString();
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(mLength);

        // 2. Thêm các ký tự BẮT BUỘC (để đảm bảo tuân thủ)
        for (String charSet : guaranteedChars) {
            password.append(charSet.charAt(random.nextInt(charSet.length())));
        }

        // 3. Điền các ký tự còn lại
        for (int i = guaranteedChars.size(); i < mLength; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // 4. Xáo trộn (Shuffle) mật khẩu
        // (Chúng ta không muốn 4 ký tự đầu tiên luôn là [a, A, 1, !])
        String shuffledPassword = shuffleString(password.toString());
        etGeneratedPassword.setText(shuffledPassword);
    }

    /**
     * Đảm bảo người dùng không thể bỏ chọn tất cả các tùy chọn.
     */
    private void checkMinimumRequirements() {
        // Luôn bật chữ thường (không có checkbox)
        // Nếu tất cả các checkbox khác đều tắt, không cho phép tắt cái cuối cùng
        cbUppercase.setEnabled(mUseNumbers || mUseSymbols);
        cbNumbers.setEnabled(mUseUppercase || mUseSymbols);
        cbSymbols.setEnabled(mUseUppercase || mUseNumbers);
    }

    /**
     * Tiện ích xáo trộn một chuỗi
     */
    private String shuffleString(String string) {
        List<Character> chars = new ArrayList<>();
        for (char c : string.toCharArray()) {
            chars.add(c);
        }
        java.util.Collections.shuffle(chars, new SecureRandom());
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Cấu hình kích thước Dialog
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}