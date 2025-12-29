package com.test.lifehub.core.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.FrameLayout;

import com.test.lifehub.R;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.List;

/**
 * LifeHubAccessibilityService - Accessibility Service cho Browser Autofill
 */
public class LifeHubAccessibilityService extends AccessibilityService {

    private static final String TAG = "LifeHubA11y";
    
    // Password field indicators
    private static final String[] PASSWORD_KEYWORDS = {
            View.AUTOFILL_HINT_PASSWORD,
            "password", "pass", "pwd", "passcode", "pin",
            "mật khẩu", "mat khau", "mk", "mật mã", "mat ma", "matkhau", "matma"
    };
    
    // Username field indicators
    private static final String[] USERNAME_KEYWORDS = {
            View.AUTOFILL_HINT_USERNAME,
            View.AUTOFILL_HINT_EMAIL_ADDRESS,
            "tên đăng nhập", "ten dang nhap", "số điện thoại", "so dien thoai", "phone",
            "mã sinh viên", "ma sinh vien", "mssv", "msv", "masv", "sinh viên", "sinh vien", 
            "tài khoản", "tai khoan", "mã số", "ma so", "tendangnhap", "taikhoan", "masinhvien", "masố",
            "số định danh", "so dinh danh", "cccd", "định danh", "dinh danh", "số cccd", "so cccd"
    };
    
    private WindowManager windowManager;
    private View floatingView;
    private boolean isFloatingViewShown = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int lastNodeHash = 0;
    
    // Static instance for Activity to callback
    private static LifeHubAccessibilityService instance;
    
    public static LifeHubAccessibilityService getInstance() {
        return instance;
    }
    
    // Current focused field info
    private static AccessibilityNodeInfo currentPasswordField;
    private static AccessibilityNodeInfo currentUsernameField;
    private String currentPackage;
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "★★★ Accessibility Service CONNECTED ★★★");
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // Log service info for debugging
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            Log.d(TAG, "Service Info - EventTypes: " + info.eventTypes);
            Log.d(TAG, "Service Info - Flags: " + info.flags);
        }
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        
        int eventType = event.getEventType();
        CharSequence packageName = event.getPackageName();
        
        // Log basic events to see activity
        if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || 
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "Event: " + AccessibilityEvent.eventTypeToString(eventType) + " from " + packageName);
        }
        
        if (packageName == null) return;
        currentPackage = packageName.toString();
        
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                handleFocusEvent(event);
                break;
                
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                // Only hide on major window changes (app switch)
                if (packageName != null && !packageName.equals(currentPackage)) {
                    hideFloatingView();
                }
                break;
                
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                // Scrolled - might need to reposition but for now just hide to avoid floating in wrong place
                hideFloatingView();
                break;
        }
    }
    
    private void handleFocusEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null) return;
        
        // Anti-flicker: if this is the same node as before and button is shown, do nothing
        int nodeHash = source.hashCode();
        if (isFloatingViewShown && nodeHash == lastNodeHash) {
            source.recycle();
            return;
        }
        
        // Log basic info about the node
        Log.d(TAG, "Node: class=" + source.getClassName() + 
                   ", text=" + source.getText() + 
                   ", hint=" + source.getHintText() +
                   ", editable=" + source.isEditable() + 
                   ", password=" + source.isPassword());
        
        // Check if this is an editable field
        if (!source.isEditable()) {
            // Hide if we move to a non-editable element in the same app
            hideFloatingView(); 
            source.recycle();
            return;
        }
        
        // Detect field type
        boolean isPasswordField = isPasswordField(source);
        boolean isUsernameField = isUsernameField(source);
        
        if (isPasswordField || isUsernameField) {
            Log.d(TAG, "☆ Detected " + (isPasswordField ? "password" : "username") + " field in " + currentPackage);
            
            lastNodeHash = nodeHash;
            if (isPasswordField) {
                if (currentPasswordField != null) {
                    try { currentPasswordField.recycle(); } catch (Exception e) {}
                }
                currentPasswordField = AccessibilityNodeInfo.obtain(source); 
            } else {
                if (currentUsernameField != null) {
                    try { currentUsernameField.recycle(); } catch (Exception e) {}
                }
                currentUsernameField = AccessibilityNodeInfo.obtain(source); 
            }
            
            // Show floating button near the field
            showFloatingView(source);
        } else {
            // Not a login field but editable - hide to be clean
            hideFloatingView();
            source.recycle();
        }
    }
    
    private boolean isPasswordField(AccessibilityNodeInfo node) {
        if (node.isPassword()) return true;
        
        String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
        String hint = node.getHintText() != null ? node.getHintText().toString().toLowerCase() : "";
        String desc = node.getContentDescription() != null ? 
                node.getContentDescription().toString().toLowerCase() : "";
        String viewId = node.getViewIdResourceName() != null ? 
                node.getViewIdResourceName().toLowerCase() : "";
        
        String combined = text + " " + hint + " " + desc + " " + viewId;
        for (String keyword : PASSWORD_KEYWORDS) {
            if (combined.contains(keyword)) return true;
        }
        return false;
    }
    
    private boolean isUsernameField(AccessibilityNodeInfo node) {
        String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
        String hint = node.getHintText() != null ? node.getHintText().toString().toLowerCase() : "";
        String desc = node.getContentDescription() != null ? 
                node.getContentDescription().toString().toLowerCase() : "";
        String viewId = node.getViewIdResourceName() != null ? 
                node.getViewIdResourceName().toLowerCase() : "";
        
        String combined = text + " " + hint + " " + desc + " " + viewId;
        for (String keyword : USERNAME_KEYWORDS) {
            if (combined.contains(keyword)) return true;
        }
        return false;
    }
    
    private void showFloatingView(AccessibilityNodeInfo node) {
        // [REMOVED] Người dùng muốn bỏ button. 
        // Chúng ta chỉ âm thầm ghi nhớ node để sẵn sàng điền khi AutofillPickerActivity yêu cầu.
        Log.d(TAG, "Not showing floating button (disabled per user request)");
    }
    
    private void hideFloatingView() {
        // [DISABLED] No floating view to hide
    }
    
    public void performDirectFill(String username, String password) {
        Log.d(TAG, "performDirectFill called. Username: " + (username != null) + ", Password: " + (password != null));
        
        AccessibilityNodeInfo focusedNode = findFocusedNode();
        if (!isNodeValid(focusedNode)) {
            Log.w(TAG, "No valid focused node to start filling.");
            return;
        }

        boolean focusedIsPassword = isPasswordField(focusedNode);
        Log.d(TAG, "Focused node is password: " + focusedIsPassword);

        // CHIẾN THUẬT:
        // Case A: Đang focus ô Password -> Điền Password vào chính nó, điền Username vào láng giềng phía trước
        if (focusedIsPassword) {
            if (password != null) fillNode(focusedNode, password, "Password (focused)");
            if (username != null) {
                AccessibilityNodeInfo userTarget = isNodeValid(currentUsernameField) ? currentUsernameField : findNeighborNode(focusedNode, true);
                if (userTarget != null) fillNode(userTarget, username, "Username (neighbor-prev)");
            }
        } 
        // Case B: Đang focus ô bất kỳ (thường là Username) -> Điền Username vào chính nó, điền Password vào láng giềng phía sau
        else {
            if (username != null) fillNode(focusedNode, username, "Username (focused)");
            if (password != null) {
                AccessibilityNodeInfo passTarget = isNodeValid(currentPasswordField) ? currentPasswordField : findNeighborNode(focusedNode, false);
                if (passTarget != null) fillNode(passTarget, password, "Password (neighbor-next)");
            }
        }

        if (focusedNode != currentUsernameField && focusedNode != currentPasswordField) {
            focusedNode.recycle();
        }
    }

    /**
     * Tìm node lân cận (trước hoặc sau) có tính chất editable
     */
    private AccessibilityNodeInfo findNeighborNode(AccessibilityNodeInfo anchor, boolean searchBefore) {
        if (anchor == null || anchor.getParent() == null) return null;
        
        AccessibilityNodeInfo parent = anchor.getParent();
        int count = parent.getChildCount();
        int anchorIndex = -1;
        
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = parent.getChild(i);
            if (child != null && child.equals(anchor)) {
                anchorIndex = i;
                child.recycle();
                break;
            }
            if (child != null) child.recycle();
        }
        
        if (anchorIndex != -1) {
            if (searchBefore) {
                for (int i = anchorIndex - 1; i >= 0; i--) {
                    AccessibilityNodeInfo node = parent.getChild(i);
                    if (node != null && (node.isEditable() || node.isFocusable())) return node;
                    if (node != null) node.recycle();
                }
            } else {
                for (int i = anchorIndex + 1; i < count; i++) {
                    AccessibilityNodeInfo node = parent.getChild(i);
                    if (node != null && (node.isEditable() || node.isFocusable())) return node;
                    if (node != null) node.recycle();
                }
            }
        }
        
        parent.recycle();
        return null;
    }

    private AccessibilityNodeInfo findFocusedNode() {
        List<AccessibilityWindowInfo> windows = getWindows();
        Log.d(TAG, "Scanning " + windows.size() + " windows for focus");
        
        // Luôn thử active window trước vì nó nhanh và thường chính xác
        AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        if (activeRoot != null) {
            AccessibilityNodeInfo focused = activeRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focused != null) {
                activeRoot.recycle();
                return focused;
            }
            activeRoot.recycle();
        }

        // Lượt 1: Tìm node CÓ tiêu điểm (isFocused) xuyên qua các cửa sổ
        for (AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            
            // Bỏ qua package chính mình
            if (root.getPackageName() != null && root.getPackageName().toString().equals(getPackageName())) {
                root.recycle();
                continue;
            }
            
            AccessibilityNodeInfo focused = findFocusedRecursive(root);
            root.recycle();
            if (focused != null) return focused;
        }
        
        // Lượt 2: (FALLBACK) Nếu không thấy node focused, tìm node "có vẻ" là ô nhập login (cho Facebook)
        Log.d(TAG, "No focused node found, attempting fallback candidate search...");
        for (AccessibilityWindowInfo window : windows) {
            AccessibilityNodeInfo root = window.getRoot();
            if (root == null) continue;
            if (root.getPackageName() != null && root.getPackageName().toString().equals(getPackageName())) {
                root.recycle();
                continue;
            }
            AccessibilityNodeInfo candidate = findCandidatesRecursive(root);
            root.recycle();
            if (candidate != null) return candidate;
        }
        
        return null;
    }

    private AccessibilityNodeInfo findFocusedRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isFocused()) return AccessibilityNodeInfo.obtain(node);
        
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findFocusedRecursive(child);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private AccessibilityNodeInfo findCandidatesRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        // Nếu là ô password hoặc username và có thể focus/edit
        if ((node.isFocusable() || node.isEditable()) && (isPasswordField(node) || isUsernameField(node))) {
            Log.d(TAG, "☆ Fallback candidate found: " + node.getClassName());
            return AccessibilityNodeInfo.obtain(node);
        }
        
        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findCandidatesRecursive(child);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private boolean isNodeValid(AccessibilityNodeInfo node) {
        if (node == null) return false;
        try {
            node.getClassName();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void fillNode(AccessibilityNodeInfo node, String text, String label) {
        if (node == null) return;
        
        try {
            Log.d(TAG, "Step 1: Attempting to fill " + label);
            
            // Đảm bảo node được focus và CLICK (Wake-up) để dán
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK); 
            
            final AccessibilityNodeInfo nodeCopy = AccessibilityNodeInfo.obtain(node);
            
            // Xử lý điền sau một khoảng trễ nhỏ
            handler.postDelayed(() -> {
                try {
                    nodeCopy.refresh();
                    
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                    boolean success = nodeCopy.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    Log.d(TAG, "SET_TEXT for " + label + " success: " + success);
                    
                    if (!success) {
                        Log.d(TAG, "Attempting PASTE fallback for " + label);
                        nodeCopy.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in delayed fill", e);
                } finally {
                    nodeCopy.recycle();
                }
            }, 300);
            
        } catch (Exception e) {
            Log.e(TAG, "Error initiating fill", e);
        }
    }

    private void openAutofillPicker() {
        Intent intent = new Intent(this, com.test.lifehub.ui.AccessibilityAutofillActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("PACKAGE_NAME", currentPackage);
        startActivity(intent);
    }
    
    @Override
    public void onInterrupt() { }
    
    @Override
    public void onDestroy() { super.onDestroy(); }
}
