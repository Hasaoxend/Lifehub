package com.test.lifehub.core.util;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.autofill.AutofillId;

import androidx.annotation.RequiresApi;

import com.test.lifehub.core.services.AutofillField;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * AutofillHelper - Helper class cho Android Autofill Framework
 * 
 * === MỤC ĐÍCH ===
 * 1. Parse AssistStructure để tìm username/password fields
 * 2. Extract domain từ web page
 * 3. Match accounts với domain/package
 * 
 * === HEURISTICS ===
 * Sử dụng nhiều cách để phát hiện fields:
 * - android:autofillHints (chuẩn nhất)
 * - android:hint text
 * - android:inputType
 * - View ID name
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillHelper {

    private static final String TAG = "AutofillHelper";
    
    // Common hints for username fields
    private static final String[] USERNAME_HINTS = {
            View.AUTOFILL_HINT_USERNAME,
            View.AUTOFILL_HINT_EMAIL_ADDRESS,
            "email", "username", "login", "user", "userid", "account",
            "emailaddress", "email_address", "user_name",
            "tên đăng nhập", "ten dang nhap", "số điện thoại", "so dien thoai", "phone",
            "mã sinh viên", "ma sinh vien", "mssv", "msv", "masv", "sinh viên", "sinh vien", 
            "tài khoản", "tai khoan", "mã số", "ma so", "tendangnhap", "taikhoan", "masinhvien", "masố",
            "số định danh", "so dinh danh", "cccd", "định danh", "dinh danh", "số cccd", "so cccd",
            "tên", "ten", "địa chỉ", "dia chi", "mật danh", "mat danh", "id_", "_id", "user_", "_user"
    };
    
    // Common hints for password fields  
    private static final String[] PASSWORD_HINTS = {
            View.AUTOFILL_HINT_PASSWORD,
            "password", "pass", "pwd", "passcode", "pin",
            "mật khẩu", "mat khau", "mk", "mật mã", "mat ma", "matkhau", "matma",
            "password_", "_password", "pwd_", "_pwd", "pass_", "_pass", "mật", "mat"
    };

    /**
     * Parse AssistStructure để tìm tất cả autofill fields
     */
    public static List<AutofillField> parseStructure(AssistStructure structure) {
        List<AutofillField> fields = new ArrayList<>();
        if (structure == null) return fields;
        
        int windowCount = structure.getWindowNodeCount();
        for (int i = 0; i < windowCount; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            if (windowNode == null) continue;
            AssistStructure.ViewNode rootNode = windowNode.getRootViewNode();
            parseViewNode(rootNode, fields);
        }
        
        Log.d(TAG, "Parsed total " + fields.size() + " potential autofill fields");
        return fields;
    }
    
    /**
     * Đệ quy parse ViewNode tree
     */
    private static void parseViewNode(AssistStructure.ViewNode node, List<AutofillField> fields) {
        if (node == null) return;
        
        AutofillId autofillId = node.getAutofillId();
        
        // Log verbose details for potential nodes (even if not strictly editable yet)
        if (autofillId != null) {
            String className = node.getClassName() != null ? node.getClassName() : "";
            if (className.toLowerCase().contains("edit") || 
                className.toLowerCase().contains("text") || 
                className.toLowerCase().contains("view")) {
                Log.v(TAG, "Node candidate: class=" + node.getClassName() + 
                            ", text=" + node.getText() + 
                            ", hint=" + node.getHint() + 
                            ", id=" + node.getIdEntry());
            }
        }

        // Chỉ xử lý các view editable HOẶC có hint quan trọng
        if (autofillId != null && (isEditableField(node) || hasImportantHints(node))) {
            AutofillField field = new AutofillField(autofillId);
            field.currentText = node.getText() != null ? node.getText().toString() : "";
            field.isFocused = node.isFocused();
            
            // Phát hiện loại field (cố gắng phân loại nếu được)
            detectFieldType(node, field);
            
            // LUÔN ADD vào danh sách candidates nếu nó là editable/focusable
            fields.add(field);
            
            if (field.isFocused || field.isUsername || field.isPassword) {
                Log.d(TAG, "Detected relevant field: id=" + node.getIdEntry() + 
                           ", type=" + (field.isUsername ? "User" : (field.isPassword ? "Pass" : "Generic")) +
                           ", focused=" + field.isFocused);
            }
        }
        
        // Đệ quy xử lý children
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            parseViewNode(node.getChildAt(i), fields);
        }
    }

    private static boolean hasImportantHints(AssistStructure.ViewNode node) {
        String[] hints = node.getAutofillHints();
        if (hints != null) {
            for (String h : hints) {
                if (h == null) continue;
                String lh = h.toLowerCase();
                if (containsAny(lh, USERNAME_HINTS) || containsAny(lh, PASSWORD_HINTS)) return true;
            }
        }
        // Also check combined info as some web browsers don't report hints separately
        String combined = getCombinedNodeInfo(node);
        return containsAny(combined, USERNAME_HINTS) || containsAny(combined, PASSWORD_HINTS);
    }

    private static String getCombinedNodeInfo(AssistStructure.ViewNode node) {
        StringBuilder sb = new StringBuilder();
        if (node.getHint() != null) sb.append(node.getHint()).append(" ");
        if (node.getContentDescription() != null) sb.append(node.getContentDescription()).append(" ");
        if (node.getText() != null) sb.append(node.getText()).append(" ");
        if (node.getIdEntry() != null) sb.append(node.getIdEntry()).append(" ");
        if (node.getClassName() != null) sb.append(node.getClassName()).append(" ");
        return sb.toString().toLowerCase();
    }
    
    /**
     * Kiểm tra view có phải editable không
     */
    private static boolean isEditableField(AssistStructure.ViewNode node) {
        if (node.getAutofillId() == null) return false;

        // BẤT KỲ node nào có thể focus hoặc được hệ thống đánh dấu là Text/Input
        // thì đều là ứng viên để hiện popup autofill.
        // Không bắt buộc phải có Hint hay Text vì Games thường để trống.
        if (node.isFocusable() || node.getAutofillType() == View.AUTOFILL_TYPE_TEXT || node.getInputType() != 0) {
            return true;
        }

        // Trường hợp đặc biệt: Clickable node nhưng có hints (thường là label của custom view)
        if (node.isClickable() && hasImportantHints(node)) {
            return true;
        }

        return false;
    }
    
    /**
     * Phát hiện loại field (username/password)
     */
    private static void detectFieldType(AssistStructure.ViewNode node, AutofillField field) {
        // 1. Check autofillHints (ưu tiên cao nhất)
        String[] hints = node.getAutofillHints();
        if (hints != null && hints.length > 0) {
            for (String hint : hints) {
                if (hint == null) continue;
                String lh = hint.toLowerCase();
                if (containsAny(lh, PASSWORD_HINTS)) {
                    field.isPassword = true;
                    return;
                }
                if (containsAny(lh, USERNAME_HINTS)) {
                    field.isUsername = true;
                    return;
                }
            }
        }
        
        // 2. Check hint text, ContentDescription, and Text
        StringBuilder combined = new StringBuilder();
        if (node.getHint() != null) combined.append(node.getHint()).append(" ");
        if (node.getContentDescription() != null) combined.append(node.getContentDescription()).append(" ");
        if (node.getText() != null) combined.append(node.getText()).append(" ");
        if (node.getIdEntry() != null) combined.append(node.getIdEntry());
        
        String searchStr = combined.toString().toLowerCase();
        
        if (containsAny(searchStr, PASSWORD_HINTS)) {
            field.isPassword = true;
            return;
        }
        if (containsAny(searchStr, USERNAME_HINTS)) {
            field.isUsername = true;
            return;
        }
        
        // 3. Check inputType
        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
            (inputType & android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
            (inputType & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0) {
            field.isPassword = true;
            return;
        }
        if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
            (inputType & android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) != 0) {
            field.isUsername = true;
            return;
        }
    }
    
    /**
     * Helper: Kiểm tra string có chứa bất kỳ keyword nào không
     */
    private static boolean containsAny(String text, String[] keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract web domain từ AssistStructure
     */
    public static String extractWebDomain(AssistStructure structure) {
        int windowCount = structure.getWindowNodeCount();
        for (int i = 0; i < windowCount; i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            AssistStructure.ViewNode rootNode = windowNode.getRootViewNode();
            String domain = findWebDomain(rootNode);
            if (domain != null) {
                return domain;
            }
        }
        return null;
    }
    
    /**
     * Đệ quy tìm web domain trong ViewNode tree
     */
    private static String findWebDomain(AssistStructure.ViewNode node) {
        if (node == null) return null;
        
        // Check webDomain attribute
        String webDomain = node.getWebDomain();
        if (!TextUtils.isEmpty(webDomain)) {
            return webDomain;
        }
        
        // Check idEntry cho URL bar
        String idEntry = node.getIdEntry();
        if (idEntry != null && (idEntry.contains("url") || idEntry.contains("address"))) {
            CharSequence text = node.getText();
            if (text != null) {
                return extractDomainFromUrl(text.toString());
            }
        }
        
        // Đệ quy
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            String domain = findWebDomain(node.getChildAt(i));
            if (domain != null) {
                return domain;
            }
        }
        
        return null;
    }
    
    /**
     * Extract domain từ URL string
     */
    private static String extractDomainFromUrl(String url) {
        if (url == null) return null;
        
        // Remove protocol
        url = url.replaceFirst("^(https?://)?", "");
        // Remove path
        int slashIndex = url.indexOf('/');
        if (slashIndex > 0) {
            url = url.substring(0, slashIndex);
        }
        // Remove port
        int colonIndex = url.indexOf(':');
        if (colonIndex > 0) {
            url = url.substring(0, colonIndex);
        }
        // Remove www.
        url = url.replaceFirst("^www\\.", "");
        
        return url.toLowerCase();
    }
    
    /**
     * Tìm accounts phù hợp với domain hoặc package
     */
    public static List<AccountEntry> findMatchingAccounts(List<AccountEntry> allAccounts, 
                                                          String webDomain, 
                                                          String packageName) {
        List<AccountEntry> matching = new ArrayList<>();
        
        if (allAccounts == null || allAccounts.isEmpty()) {
            return matching;
        }
        
        for (AccountEntry account : allAccounts) {
            // 1. Match by websiteUrl (which stores domain for web OR package name for native apps)
            if (!TextUtils.isEmpty(account.websiteUrl)) {
                String accountDomain = extractDomainFromUrl(account.websiteUrl);
                if (accountDomain != null) {
                    // Match with web domain if available
                    if (webDomain != null && (accountDomain.contains(webDomain) || webDomain.contains(accountDomain))) {
                        matching.add(account);
                        continue;
                    }
                    // Match with native package name
                    if (packageName != null && (accountDomain.contains(packageName) || packageName.contains(accountDomain))) {
                        matching.add(account);
                        continue;
                    }
                }
            }
            
            // 2. Match by serviceName
            if (!TextUtils.isEmpty(account.serviceName)) {
                String serviceLower = account.serviceName.toLowerCase();
                // Match with web domain
                if (webDomain != null && webDomain.contains(serviceLower)) {
                    matching.add(account);
                    continue;
                }
                // Match with native package name
                if (packageName != null && packageName.toLowerCase().contains(serviceLower)) {
                    matching.add(account);
                }
            }
        }
        
        Log.d(TAG, "Found " + matching.size() + " matching accounts for domain: " + webDomain);
        return matching;
    }
}
