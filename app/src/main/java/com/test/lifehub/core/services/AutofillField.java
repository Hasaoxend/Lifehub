package com.test.lifehub.core.services;

import android.view.autofill.AutofillId;

/**
 * AutofillField - Data class lưu thông tin field cần autofill
 */
public class AutofillField {
    
    public AutofillId autofillId;
    public String hint;           // Autofill hint (username, password, email)
    public boolean isUsername;    // true nếu đây là username/email field
    public boolean isPassword;    // true nếu đây là password field
    public boolean isFocused;     // true nếu field đang có focus
    public String currentText;    // Giá trị hiện tại của field
    
    public AutofillField(AutofillId autofillId) {
        this.autofillId = autofillId;
    }
    
    @Override
    public String toString() {
        return "AutofillField{" +
                "hint='" + hint + '\'' +
                ", isUsername=" + isUsername +
                ", isPassword=" + isPassword +
                '}';
    }
}
