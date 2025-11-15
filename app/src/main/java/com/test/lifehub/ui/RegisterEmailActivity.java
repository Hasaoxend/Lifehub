package com.test.lifehub.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // <-- THÊM IMPORT NÀY
import com.test.lifehub.R;

import java.util.HashMap; // <-- THÊM IMPORT NÀY
import java.util.Map; // <-- THÊM IMPORT NÀY

/**
 * Activity xử lý việc Đăng ký (Register) một tài khoản mới bằng Email và Mật khẩu
 * (Phiên bản đã SỬA LỖI: Tự động tạo Document User trong Firestore)
 */
public class RegisterEmailActivity extends AppCompatActivity {

    private static final String TAG = "RegisterEmailActivity";

    // --- Views ---
    private TextInputEditText etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar pbRegister;

    // --- Firebase ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb; // <-- THÊM FIRESTORE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_email);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance(); // <-- KHỞI TẠO FIRESTORE

        // Ánh xạ (Find) Views
        findViews();

        // Thiết lập Listeners
        setupListeners();
    }

    private void findViews() {
        etEmail = findViewById(R.id.et_register_email);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);
        pbRegister = findViewById(R.id.pb_register);
    }

    private void setupListeners() {
        // Nút Đăng ký
        btnRegister.setOnClickListener(v -> attemptRegistration());

        // Nút Quay lại Đăng nhập
        tvGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    /**
     * Xử lý khi người dùng nhấn nút "Tạo tài khoản".
     */
    private void attemptRegistration() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // --- 1. Kiểm tra (Validate) dữ liệu đầu vào ---
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email không được để trống.");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ.");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Mật khẩu không được để trống.");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự.");
            etPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu không khớp.");
            etConfirmPassword.requestFocus();
            return;
        }

        // --- 2. Gọi API Firebase ---
        setLoading(true); // Bật ProgressBar

        // Bước 2a: Tạo người dùng trong Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "createUserWithEmail:success");
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {

                        // ----- SỬA LỖI Ở ĐÂY -----
                        // Bước 2b: Tạo tài liệu (document) trong Firestore
                        createUserDocumentInFirestore(user, email);

                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Tạo tài khoản thất bại (user null).", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Đăng ký tài khoản THẤT BẠI
                    setLoading(false);
                    Log.w(TAG, "createUserWithEmail:failure", e);
                    Toast.makeText(this, "Đăng ký thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * HÀM MỚI:
     * Tạo một tài liệu (document) trong collection "users"
     * để khớp với rule bảo mật của Firestore.
     */
    private void createUserDocumentInFirestore(FirebaseUser user, String email) {
        // Lấy ID người dùng từ Auth
        String uid = user.getUid();

        // Tạo một đối tượng (Map) dữ liệu cơ bản
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("createdAt", System.currentTimeMillis());
        // (Bạn có thể thêm bất cứ trường nào khác ở đây, ví dụ: "displayName")

        // Thực hiện lệnh "set" (tạo) tài liệu
        // Đường dẫn: /users/{uid}
        mDb.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Tạo document THÀNH CÔNG
                    Log.d(TAG, "Tạo User Document trong Firestore thành công!");
                    // Bước 2c: Gửi email xác thực (logic cũ)
                    sendVerificationEmail(user);
                })
                .addOnFailureListener(e -> {
                    // Tạo document THẤT BẠI
                    setLoading(false);
                    Log.e(TAG, "Lỗi khi tạo User Document trong Firestore", e);
                    Toast.makeText(this, "Đăng ký thành công, nhưng không thể tạo CSDL người dùng.", Toast.LENGTH_LONG).show();
                    // (Lúc này bạn có thể cân nhắc xóa user Auth đã tạo để đồng bộ)
                });
    }


    /**
     * Gửi email xác thực đến người dùng.
     * (Hàm này giữ nguyên)
     */
    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnSuccessListener(aVoid -> {
                    // Gửi email THÀNH CÔNG
                    setLoading(false);
                    showSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    // Gửi email THẤT BẠI
                    setLoading(false);
                    Log.w(TAG, "sendEmailVerification:failure", e);
                    Toast.makeText(this, "Đăng ký thành công, nhưng gửi email xác thực thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Hiển thị hộp thoại thông báo thành công và hướng dẫn người dùng.
     * (Hàm này giữ nguyên)
     */
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng ký Thành công!")
                .setMessage("Một email xác thực đã được gửi đến địa chỉ email của bạn. Vui lòng kiểm tra hộp thư (kể cả spam) và xác thực tài khoản trước khi đăng nhập.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Đóng Activity Đăng ký và quay lại Đăng nhập
                    finish();
                })
                .setCancelable(false) // Không cho đóng dialog bằng cách nhấn bên ngoài
                .show();
    }

    /**
     * Bật/Tắt ProgressBar và ẩn/hiện form đăng ký.
     * (Hàm này giữ nguyên)
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            pbRegister.setVisibility(View.VISIBLE);
            btnRegister.setEnabled(false);
            etEmail.setEnabled(false);
            etPassword.setEnabled(false);
            etConfirmPassword.setEnabled(false);
        } else {
            pbRegister.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            etEmail.setEnabled(true);
            etPassword.setEnabled(true);
            etConfirmPassword.setEnabled(true);
        }
    }
}