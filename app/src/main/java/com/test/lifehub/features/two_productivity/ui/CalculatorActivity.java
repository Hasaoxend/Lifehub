package com.test.lifehub.features.two_productivity.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.test.lifehub.R;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class CalculatorActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvDisplay;
    private TextView tvFormula; // Hiển thị công thức
    private Button btnClear; // Nút AC/C

    private double operand1 = Double.NaN;
    private double operand2 = Double.NaN;
    private String pendingOperation = null;

    private boolean isUserTyping = false;
    private boolean isErrorState = false; // Trạng thái "Lỗi"

    private DecimalFormat decimalFormat;
    private StringBuilder formulaBuilder = new StringBuilder(); // Xây dựng chuỗi công thức

    // Lịch sử
    private ArrayList<String> calculationHistory = new ArrayList<>();
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        // --- CÀI ĐẶT TOOLBAR ---
        Toolbar toolbar = findViewById(R.id.toolbar_calculator);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Ẩn tiêu đề
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed()); // Nút quay về
        // ------------------------

        tvDisplay = findViewById(R.id.tv_display);
        tvFormula = findViewById(R.id.tv_formula); // Ánh xạ TextView công thức
        btnClear = findViewById(R.id.btn_clear); // Nút AC/C

        // Định dạng số
        decimalFormat = new DecimalFormat("#.##########");

        // Adapter cho lịch sử
        historyAdapter = new HistoryAdapter(calculationHistory);

        // Gán listener cho các nút số
        findViewById(R.id.btn_0).setOnClickListener(this);
        findViewById(R.id.btn_1).setOnClickListener(this);
        findViewById(R.id.btn_2).setOnClickListener(this);
        findViewById(R.id.btn_3).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_5).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_8).setOnClickListener(this);
        findViewById(R.id.btn_9).setOnClickListener(this);
        findViewById(R.id.btn_decimal).setOnClickListener(this);

        // Gán listener cho các nút toán tử
        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.btn_subtract).setOnClickListener(this);
        findViewById(R.id.btn_multiply).setOnClickListener(this);
        findViewById(R.id.btn_divide).setOnClickListener(this);
        findViewById(R.id.btn_equals).setOnClickListener(this);

        // Gán listener cho các nút chức năng
        btnClear.setOnClickListener(this); // Nút AC/C
        findViewById(R.id.btn_toggle_sign).setOnClickListener(this);
        findViewById(R.id.btn_percent).setOnClickListener(this);
        findViewById(R.id.btn_history).setOnClickListener(this); // Nút Lịch sử
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        String buttonText = "";
        if (v instanceof Button) {
            buttonText = ((Button) v).getText().toString();
        }

        // Nếu đang lỗi, chỉ cho phép nhấn "AC" hoặc nhập số mới
        if (isErrorState) {
            if (id == R.id.btn_clear) {
                handleClearInput();
            } else if (id == R.id.btn_0 || id == R.id.btn_1 || id == R.id.btn_2 || id == R.id.btn_3 ||
                    id == R.id.btn_4 || id == R.id.btn_5 || id == R.id.btn_6 || id == R.id.btn_7 ||
                    id == R.id.btn_8 || id == R.id.btn_9) {

                isErrorState = false; // Xóa lỗi khi bắt đầu nhập số mới
                handleNumberInput(buttonText);
            }
            // Nếu là các nút khác thì không làm gì
            return;
        }

        // Nếu không lỗi, xử lý bình thường
        if (id == R.id.btn_0 || id == R.id.btn_1 || id == R.id.btn_2 || id == R.id.btn_3 ||
                id == R.id.btn_4 || id == R.id.btn_5 || id == R.id.btn_6 || id == R.id.btn_7 ||
                id == R.id.btn_8 || id == R.id.btn_9) {
            handleNumberInput(buttonText);
        } else if (id == R.id.btn_decimal) {
            handleDecimalInput();
        } else if (id == R.id.btn_add || id == R.id.btn_subtract || id == R.id.btn_multiply || id == R.id.btn_divide) {
            handleOperationInput(buttonText);
        } else if (id == R.id.btn_equals) {
            handleEqualsInput();
        } else if (id == R.id.btn_clear) {
            handleClearInput();
        } else if (id == R.id.btn_toggle_sign) {
            handleToggleSignInput();
        } else if (id == R.id.btn_percent) {
            handlePercentInput();
        } else if (id == R.id.btn_history) {
            showHistoryDialog();
        }
    }

    private void handleNumberInput(String number) {
        String currentText = tvDisplay.getText().toString();
        if (!isUserTyping) {
            tvDisplay.setText(number);
            isUserTyping = true;
        } else {
            if (currentText.equals("0")) {
                tvDisplay.setText(number);
            } else {
                // Giới hạn độ dài nhập
                if (currentText.length() < 15) {
                    tvDisplay.setText(currentText + number);
                }
            }
        }
        btnClear.setText(R.string.calc_button_c); // Đổi thành nút "C"
    }

    private void handleDecimalInput() {
        if (!isUserTyping) {
            tvDisplay.setText("0.");
            isUserTyping = true;
        } else if (!tvDisplay.getText().toString().contains(".")) {
            tvDisplay.setText(tvDisplay.getText().toString() + ".");
        }
        btnClear.setText(R.string.calc_button_c); // Đổi thành nút "C"
    }

    private void handleOperationInput(String operation) {
        if (isUserTyping || !Double.isNaN(operand1)) {
            if (!Double.isNaN(operand1) && isUserTyping) {
                // Xử lý chuỗi phép tính (ví dụ: 5 + 5 +)
                handleEqualsInput();
            }

            operand1 = getDisplayValue();
            pendingOperation = operation;
            isUserTyping = false;

            // Cập nhật logic formula
            formulaBuilder.setLength(0);
            formulaBuilder.append(formatResult(operand1)).append(" ").append(operation);
            tvFormula.setText(formulaBuilder.toString());
        }
        btnClear.setText(R.string.calc_button_ac); // Đổi về nút "AC"
    }

    private void handleEqualsInput() {
        // Chỉ tính khi có phép tính chờ và người dùng đang gõ
        // Hoặc khi operand2 chưa được gán (trường hợp 5 + =)

        if (pendingOperation == null) {
            return;
        }

        if (isUserTyping) {
            operand2 = getDisplayValue();
        } else if (Double.isNaN(operand2)) {
            // Trường hợp 5 + =, dùng operand1 làm operand2
            operand2 = Double.isNaN(operand1) ? 0 : operand1;
        }

        // Nếu operand1 hoặc operand2 không hợp lệ (ví dụ: nhấn = liên tục)
        if (Double.isNaN(operand1) || Double.isNaN(operand2)) {
            return;
        }

        double result = Double.NaN;
        String op1Str = formatResult(operand1);
        String op2Str = formatResult(operand2);

        // Cập nhật logic formula
        formulaBuilder.setLength(0); // Xóa formula cũ (ví dụ: "5 +")
        formulaBuilder.append(op1Str).append(" ").append(pendingOperation).append(" ").append(op2Str).append(" =");
        tvFormula.setText(formulaBuilder.toString());

        switch (pendingOperation) {
            case "+":
                result = operand1 + operand2;
                break;
            case "-":
                result = operand1 - operand2;
                break;
            case "×":
                result = operand1 * operand2;
                break;
            case "÷":
                if (operand2 == 0) {
                    tvDisplay.setText(R.string.calc_error);
                    tvFormula.setText(""); // Xóa formula khi lỗi
                    isErrorState = true;
                    isUserTyping = false;
                    btnClear.setText(R.string.calc_button_ac);
                    resetCalculatorState(); // Chỉ reset state, không reset hiển thị
                    return;
                }
                result = operand1 / operand2;
                break;
        }

        String resultStr = formatResult(result);
        tvDisplay.setText(resultStr);

        // Lưu lịch sử
        String historyEntry = formulaBuilder.toString() + " " + resultStr;
        calculationHistory.add(0, historyEntry); // Thêm vào đầu danh sách
        historyAdapter.notifyDataSetChanged();

        operand1 = result; // Kết quả trở thành operand1 cho phép tính tiếp theo
        isUserTyping = false;
        btnClear.setText(R.string.calc_button_ac);

        // Yêu cầu 2: Tắt tính năng lặp lại dấu =
        // Reset pendingOperation và operand2 để buộc người dùng nhập lại
        pendingOperation = null;
        operand2 = Double.NaN;
    }

    private void handleClearInput() {
        if (isErrorState) {
            // Nếu đang lỗi, "AC" sẽ reset mọi thứ
            tvDisplay.setText("0");
            resetCalculatorState();
            isErrorState = false;
        } else if (isUserTyping && !tvDisplay.getText().toString().equals("0")) {
            // Đây là chức năng "C" (Clear)
            tvDisplay.setText("0");
            // Không reset isUserTyping vì người dùng có thể muốn nhập số khác
        } else {
            // Đây là chức năng "AC" (All Clear)
            tvDisplay.setText("0");
            resetCalculatorState();
        }
        btnClear.setText(R.string.calc_button_ac);
    }

    // Reset logic tính toán
    private void resetCalculatorState() {
        operand1 = Double.NaN;
        operand2 = Double.NaN;
        pendingOperation = null;
        isUserTyping = false;

        // Xóa cả formula
        formulaBuilder.setLength(0);
        tvFormula.setText("");
    }

    private void handleToggleSignInput() {
        if (isUserTyping && !tvDisplay.getText().toString().equals("0")) {
            double value = getDisplayValue();
            tvDisplay.setText(formatResult(value * -1));
        } else if (!Double.isNaN(operand1)) {
            operand1 *= -1;
            tvDisplay.setText(formatResult(operand1));
        }
    }

    private void handlePercentInput() {
        double value = getDisplayValue();
        double result = value / 100.0;

        // Nâng cấp: Nếu đang tính (100 + 10%), nó sẽ tính 10% của 100
        if (!Double.isNaN(operand1) && pendingOperation != null && isUserTyping) {
            result = (operand1 * value) / 100.0;
        }

        tvDisplay.setText(formatResult(result));
        isUserTyping = false;
    }

    private double getDisplayValue() {
        try {
            return Double.parseDouble(tvDisplay.getText().toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatResult(double value) {
        return decimalFormat.format(value);
    }

    // --- LOGIC LỊCH SỬ ---
    private void showHistoryDialog() {
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_history, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(bottomSheetView);

        RecyclerView rvHistory = bottomSheetView.findViewById(R.id.rv_history);
        TextView tvClearHistory = bottomSheetView.findViewById(R.id.tv_clear_history);
        TextView tvEmptyHistory = bottomSheetView.findViewById(R.id.tv_empty_history);

        if (calculationHistory.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            tvClearHistory.setVisibility(View.GONE);
            tvEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            tvClearHistory.setVisibility(View.VISIBLE);
            tvEmptyHistory.setVisibility(View.GONE);

            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            rvHistory.setAdapter(historyAdapter);
        }

        tvClearHistory.setOnClickListener(v -> {
            calculationHistory.clear();
            historyAdapter.notifyDataSetChanged();
            dialog.dismiss();
            Toast.makeText(this, R.string.calc_history_cleared, Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}