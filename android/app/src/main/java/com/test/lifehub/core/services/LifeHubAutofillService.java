package com.test.lifehub.core.services;

import android.app.PendingIntent;
import android.app.assist.AssistStructure;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveInfo;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.test.lifehub.R;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.core.util.AutofillHelper;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.ui.AutofillAuthActivity;
import com.test.lifehub.ui.AutofillPickerActivity;
import com.test.lifehub.ui.SaveCredentialActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * LifeHubAutofillService - Android Autofill Framework Service
 * 
 * === MỤC ĐÍCH ===
 * Cho phép LifeHub tự động điền username/password vào:
 * - Browser (Chrome, Firefox, ...)
 * - Các ứng dụng khác (Facebook, Instagram, ...)
 * 
 * === WORKFLOW ===
 * 1. User focus vào field username/password
 * 2. Android System gọi onFillRequest()
 * 3. Service parse AssistStructure để tìm fields
 * 4. Service query accounts từ local cache
 * 5. Service trả về FillResponse với danh sách accounts
 * 6. User chọn account → Biometric → Autofill
 * 
 * === ENABLE SERVICE ===
 * Settings > System > Languages & input > Autofill service > LifeHub
 * 
 * === BẢO MẬT ===
 * - Yêu cầu Biometric trước khi điền mật khẩu
 * - Chỉ hiển thị accounts match với domain/app
 * - Không log thông tin nhạy cảm
 * 
 * @see AutofillHelper Helper parse AssistStructure
 * @see AutofillAuthActivity UI xác thực trước autofill
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class LifeHubAutofillService extends AutofillService {

    private static final String TAG = "LifeHubAutofill";
    
    // ⚠️ PHẢI DÙNG PLAIN PREFS VÌ SERVICE CHẠY TRONG PROCESS RIÊNG
    // EncryptedSharedPreferences không thể đọc từ process khác
    private static final String AUTOFILL_PREF_NAME = "lifehub_autofill_prefs";
    private static final String KEY_AUTOFILL_BIOMETRIC = "autofill_biometric_enabled";
    private static final String KEY_AUTOFILL_SERVICE = "autofill_service_enabled";
    
    // Cache accounts để query nhanh (được load từ Firestore)
    private static List<AccountEntry> cachedAccounts = null;
    private static EncryptionHelper encryptionHelper = null;
    
    // Static flag dự phòng (sẽ ưu tiên đọc từ SharedPreferences)
    private static boolean biometricEnabled = false;
    
    /**
     * Getter cho cached accounts (dùng bởi AutofillPickerActivity)
     */
    public static List<AccountEntry> getCachedAccounts() {
        return cachedAccounts;
    }
    
    /**
     * Cập nhật cache accounts từ bên ngoài (gọi từ MainActivity hoặc AccountRepository)
     */
    public static void updateAccountsCache(List<AccountEntry> accounts) {
        cachedAccounts = accounts;
        Log.d(TAG, "Accounts cache updated: " + (accounts != null ? accounts.size() : 0) + " accounts");
    }
    
    /**
     * Cập nhật EncryptionHelper reference
     */
    public static void setEncryptionHelper(EncryptionHelper helper) {
        encryptionHelper = helper;
    }
    
    /**
     * Cập nhật trạng thái Biometric enabled
     * ⚠️ QUAN TRỌNG: Phải gọi method này khi user bật/tắt Biometric trong Settings
     */
    public static void setBiometricEnabled(boolean enabled) {
        biometricEnabled = enabled;
        Log.d(TAG, "Biometric enabled (static): " + enabled);
    }
    
    /**
     * Đọc trạng thái autofill enabled trực tiếp từ SharedPreferences
     * Dùng PLAIN prefs (không encrypted) để service đọc được từ process riêng
     * 
     * ⚠️ CHỈ LƯU BOOLEAN FLAGS - KHÔNG PHẢI DATA NHẠY CẢM
     */
    private boolean isAutofillEnabledFromPrefs() {
        try {
            SharedPreferences prefs = getSharedPreferences(AUTOFILL_PREF_NAME, MODE_PRIVATE);
            boolean biometric = prefs.getBoolean(KEY_AUTOFILL_BIOMETRIC, false);
            boolean autofill = prefs.getBoolean(KEY_AUTOFILL_SERVICE, false);
            Log.d(TAG, "Prefs check - Biometric: " + biometric + ", Autofill: " + autofill);
            return biometric && autofill;
        } catch (Exception e) {
            Log.e(TAG, "Error reading prefs, using static fallback", e);
            return biometricEnabled;
        }
    }
    
    @Override
    public void onConnected() {
        super.onConnected();
        Log.d(TAG, "AutofillService connected");
    }
    
    @Override
    public void onDisconnected() {
        super.onDisconnected();
        Log.d(TAG, "AutofillService disconnected");
    }
    
    @Override
    public void onFillRequest(@NonNull FillRequest request, 
                              @NonNull CancellationSignal cancellationSignal, 
                              @NonNull FillCallback callback) {
        
        Log.d(TAG, "onFillRequest received");
        
        try {
            // ⚠️ RÀNG BUỘC BẢO MẬT: Đọc trực tiếp từ SharedPreferences
            Log.d(TAG, "Checking prefs...");
            boolean autofillAllowed = isAutofillEnabledFromPrefs();
            Log.d(TAG, "Autofill allowed: " + autofillAllowed);
            
            if (!autofillAllowed) {
                Log.d(TAG, "Autofill disabled - check biometric and autofill settings in LifeHub");
                callback.onSuccess(null);
                return;
            }
            
            handleFillRequest(request, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "ERROR in onFillRequest: " + e.getMessage(), e);
            callback.onSuccess(null);
        }
    }
    
    /**
     * Xử lý fill request sau khi đã verify permissions
     */
    private void handleFillRequest(FillRequest request, FillCallback callback) {
        Log.d(TAG, "handleFillRequest started");
        
        try {
            // 1. Parse AssistStructure để tìm autofill fields
            // Duyệt qua TỐI ĐA 2 FillContext cuối cùng để tránh quá tải và lỗi partition (giới hạn 10 của Android)
            List<AutofillField> fields = new ArrayList<>();
            List<android.service.autofill.FillContext> contexts = request.getFillContexts();
            int contextSize = contexts.size();
            Log.d(TAG, "Total fill contexts available: " + contextSize + ". Parsing last 2.");
            
            for (int i = Math.max(0, contextSize - 2); i < contextSize; i++) {
                List<AutofillField> contextFields = AutofillHelper.parseStructure(contexts.get(i).getStructure());
                if (contextFields != null && !contextFields.isEmpty()) {
                    fields.addAll(contextFields);
                }
            }
            Log.d(TAG, "Parsed total " + fields.size() + " candidates for this update");
            
            // Lấy flags để kiểm tra xem có phải Manual Request không
            int flags = request.getFlags();
            boolean isManual = (flags & FillRequest.FLAG_MANUAL_REQUEST) != 0;
            
            // Nhận diện App đặc biệt (VNeID, Facebook, Garena)
            String targetPackage = "";
            AssistStructure latestStructure = contexts.get(contextSize - 1).getStructure();
            try {
                targetPackage = latestStructure.getActivityComponent().getPackageName();
            } catch (Exception e) {}
            
            // 3. Extract domain/package name
            String webDomain = AutofillHelper.extractWebDomain(latestStructure);

            boolean isHighPriorityApp = targetPackage.toLowerCase().contains("vn.gov.vneid") || 
                                      targetPackage.toLowerCase().contains("facebook") ||
                                      targetPackage.toLowerCase().contains("garena") ||
                                      targetPackage.toLowerCase().contains("freefire") ||
                                      targetPackage.toLowerCase().contains("browser") ||
                                      targetPackage.toLowerCase().contains("chrome");

            if (isManual) Log.d(TAG, "☆ MANUAL request for: " + targetPackage);

            // 3.5. Tìm accounts khớp
            List<AccountEntry> matchingAccounts = AutofillHelper.findMatchingAccounts(
                    cachedAccounts, webDomain, targetPackage
            );
            int matchCount = matchingAccounts.size();

            if (fields.isEmpty() && !isManual && !isHighPriorityApp) {
                callback.onSuccess(null);
                return;
            }
            
            // 2. Phân loại fields: Ưu tiên Focused -> User -> Pass
            AutofillField usernameField = null;
            AutofillField passwordField = null;
            AutofillField focusedField = null;
            
            for (AutofillField field : fields) {
                if (field.isFocused && focusedField == null) focusedField = field;
                
                // Phân loại dựa trên hints/heuristics
                if (field.isPassword && passwordField == null) {
                    passwordField = field;
                } else if (field.isUsername && usernameField == null) {
                    usernameField = field;
                }
            }
            
            // TÌM KIẾM BỔ SUNG: Nếu có Password nhưng chưa có Username (thường gặp ở Garena/Game)
            if (passwordField != null && usernameField == null) {
                for (AutofillField field : fields) {
                    if (field != passwordField) {
                        // Ưu tiên field đang được focus hoặc field đứng trên nó
                        usernameField = field;
                        Log.d(TAG, "Assumed username field from candidate list (fallback)");
                        break;
                    }
                }
            }
            
            // Chọn Anchor (mỏ neo) để hiện trigger: Focused > Username > Password > Root
            AutofillField anchorField = focusedField != null ? focusedField : 
                                      (usernameField != null ? usernameField : passwordField);

            // CƯỠNG ÉP (FORCE) cho Priority App hoặc Manual request
            if (anchorField == null && (isManual || isHighPriorityApp)) {
                if (!fields.isEmpty()) {
                    anchorField = fields.get(0);
                    Log.d(TAG, "Anchor: First focusable field (fallback)");
                } else {
                    try {
                        AutofillId rootId = latestStructure.getWindowNodeAt(0).getRootViewNode().getAutofillId();
                        if (rootId != null) {
                            anchorField = new AutofillField(rootId);
                            Log.d(TAG, "Anchor: Root Node (last resort)");
                        }
                    } catch (Exception e) {}
                }
            }
            
            if (anchorField == null) {
                Log.d(TAG, "No anchor found. Cannot trigger autofill.");
                callback.onSuccess(null);
                return;
            }

            // Đảm bảo usernameField và passwordField được gán giá trị hợp lệ để Picker hoạt động
            if (usernameField == null && anchorField != passwordField) usernameField = anchorField;
            if (passwordField == null && anchorField != usernameField) passwordField = anchorField;
            
            Log.d(TAG, "Triggering on: " + anchorField.autofillId + 
                       ", UserField: " + (usernameField != null) + 
                       ", PassField: " + (passwordField != null));
            
            // 4. Kiểm tra cache
            int accountCount = (cachedAccounts != null) ? cachedAccounts.size() : 0;
            
            // 5. Build FillResponse
            FillResponse.Builder responseBuilder = new FillResponse.Builder();
            
            // Dataset duy nhất mở Picker
            Dataset triggerDataset = buildSingleAutofillTrigger(anchorField, passwordField, matchCount, targetPackage, webDomain);
            if (triggerDataset != null) {
                responseBuilder.addDataset(triggerDataset);
            }
            
            // Thêm SaveInfo nếu muốn lưu credentials mới
            if (usernameField != null || passwordField != null) {
                // Xác định Required IDs (Mật khẩu là bắt buộc nhất)
                java.util.Set<AutofillId> requiredIdSet = new java.util.HashSet<>();
                java.util.Set<AutofillId> optionalIdSet = new java.util.HashSet<>();
                
                if (passwordField != null) {
                    requiredIdSet.add(passwordField.autofillId);
                    if (usernameField != null && usernameField != passwordField) {
                        optionalIdSet.add(usernameField.autofillId);
                    }
                } else if (usernameField != null) {
                    requiredIdSet.add(usernameField.autofillId);
                }

                if (!requiredIdSet.isEmpty()) {
                    AutofillId[] requiredIds = requiredIdSet.toArray(new AutofillId[0]);
                    SaveInfo.Builder saveInfoBuilder = new SaveInfo.Builder(
                            SaveInfo.SAVE_DATA_TYPE_USERNAME | SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            requiredIds
                    );
                    
                    if (!optionalIdSet.isEmpty()) {
                        saveInfoBuilder.setOptionalIds(optionalIdSet.toArray(new AutofillId[0]));
                    }
                    
                    // CỰC KỲ QUAN TRỌNG CHO FB/GAMES: 
                    // FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE giúp hiện popup khi các ô này biến mất (đăng nhập xong)
                    saveInfoBuilder.setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE);
                    
                    responseBuilder.setSaveInfo(saveInfoBuilder.build());
                    Log.d(TAG, "SaveInfo configured: Required=" + requiredIdSet.size() + ", Optional=" + optionalIdSet.size());
                }
            }
            
            FillResponse response = responseBuilder.build();
            callback.onSuccess(response);
            Log.d(TAG, "FillResponse sent with " + accountCount + " datasets + 1 trigger");
            
        } catch (Exception e) {
            Log.e(TAG, "ERROR in handleFillRequest: " + e.getMessage(), e);
            callback.onSuccess(null);
        }
    }
    
    /**
     * Tạo Dataset hiển thị thông tin 1 tài khoản cụ thể
     */
    private Dataset buildDatasetForAccount(AutofillField usernameField, AutofillField passwordField, 
                                          AccountEntry account) {
        RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_item);
        presentation.setTextViewText(R.id.autofill_service_name, account.serviceName);
        presentation.setTextViewText(R.id.autofill_username, account.username);
        presentation.setTextViewText(R.id.autofill_password_hint, "••••••••");
        
        // Intent mở AutofillPickerActivity cho account này
        Intent authIntent = new Intent(this, AutofillPickerActivity.class);
        authIntent.putExtra("ACCOUNT_ID", account.documentId); // Truyền ID để Picker biết chọn luôn
        
        if (usernameField != null) {
            authIntent.putExtra(AutofillPickerActivity.EXTRA_USERNAME_AUTOFILL_ID, usernameField.autofillId);
        }
        if (passwordField != null) {
            authIntent.putExtra(AutofillPickerActivity.EXTRA_PASSWORD_AUTOFILL_ID, passwordField.autofillId);
        }

        PendingIntent authPendingIntent = PendingIntent.getActivity(
                this,
                (account.documentId != null ? account.documentId.hashCode() : 0),
                authIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
        datasetBuilder.setAuthentication(authPendingIntent.getIntentSender());

        // Sử dụng username làm giá trị hiển thị (nhưng sẽ được replace sau auth)
        if (usernameField != null) {
            datasetBuilder.setValue(usernameField.autofillId, 
                    AutofillValue.forText(account.username), presentation);
        }
        if (passwordField != null) {
            datasetBuilder.setValue(passwordField.autofillId, 
                    AutofillValue.forText(""), presentation); // Trống cho đến khi verify
        }

        return datasetBuilder.build();
    }

    /**
     * Tạo 1 Dataset duy nhất hiển thị "Tự động điền với LifeHub"
     */
    private Dataset buildSingleAutofillTrigger(AutofillField usernameField, AutofillField passwordField, 
                                               int matchCount, String packageName, String domain) {
        // Presentation cho popup
        RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_item);
        presentation.setTextViewText(R.id.autofill_service_name, getString(R.string.autofill_title));
        
        String subText;
        if (matchCount > 0) {
            subText = "Có " + matchCount + " tài khoản cho " + (domain != null ? domain : getAppNameFromPackage(packageName));
        } else {
            subText = "Chạm để chọn tài khoản LifeHub";
        }
        
        presentation.setTextViewText(R.id.autofill_username, subText);
        presentation.setTextViewText(R.id.autofill_password_hint, "Xác thực vân tay để điền");
        
        // Intent mở AutofillPickerActivity (biometric + chọn account)
        Intent authIntent = new Intent(this, AutofillPickerActivity.class);
        
        if (usernameField != null) {
            authIntent.putExtra(AutofillPickerActivity.EXTRA_USERNAME_AUTOFILL_ID, usernameField.autofillId);
        }
        if (passwordField != null) {
            authIntent.putExtra(AutofillPickerActivity.EXTRA_PASSWORD_AUTOFILL_ID, passwordField.autofillId);
        }
        
        PendingIntent authPendingIntent = PendingIntent.getActivity(
                this,
                "lifehub_autofill".hashCode(),
                authIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
        datasetBuilder.setAuthentication(authPendingIntent.getIntentSender());
        
        // Placeholder - sẽ được thay thế sau authentication
        if (usernameField != null) {
            datasetBuilder.setValue(usernameField.autofillId, 
                    AutofillValue.forText(""), presentation);
        }
        if (passwordField != null) {
            datasetBuilder.setValue(passwordField.autofillId, 
                    AutofillValue.forText(""), presentation);
        }
        
        return datasetBuilder.build();
    }
    /**
     * Build Dataset với data trực tiếp (đã decrypt)
     * NOTE: Tạm thời bỏ authentication để debug
     */
    private Dataset buildDatasetWithAuth(AccountEntry account, 
                                         AutofillField usernameField, 
                                         AutofillField passwordField) {
        
        // Tạo presentation (hiển thị trong popup)
        RemoteViews presentation = new RemoteViews(getPackageName(), R.layout.autofill_item);
        presentation.setTextViewText(R.id.autofill_service_name, account.serviceName);
        presentation.setTextViewText(R.id.autofill_username, account.username);
        presentation.setTextViewText(R.id.autofill_password_hint, "••••••••");
        
        // Password đã được decrypt trong MainActivity trước khi cache
        // Nên ở đây dùng trực tiếp
        String password = account.password != null ? account.password : "";
        
        Log.d(TAG, "Building dataset for: " + account.serviceName + ", has password: " + (password.length() > 0));
        
        // Build Dataset với values thật (đã decrypt)
        Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
        
        if (usernameField != null) {
            datasetBuilder.setValue(usernameField.autofillId, 
                    AutofillValue.forText(account.username), presentation);
        }
        if (passwordField != null) {
            datasetBuilder.setValue(passwordField.autofillId, 
                    AutofillValue.forText(password), presentation);
        }
        
        return datasetBuilder.build();
    }
    
    @Override
    public void onSaveRequest(@NonNull SaveRequest request, 
                              @NonNull SaveCallback callback) {
        
        Log.d(TAG, "onSaveRequest received");
        
        try {
            // Kiểm tra biometric enabled
            if (!isAutofillEnabledFromPrefs()) {
                Log.d(TAG, "Save disabled - autofill not enabled");
                callback.onSuccess();
                return;
            }
            
            // Parse TẤT CẢ FillContext để lấy data (rất quan trọng cho FB vì nó chia nhiều bước)
            List<android.service.autofill.FillContext> contexts = request.getFillContexts();
            if (contexts.isEmpty()) {
                Log.d(TAG, "onSaveRequest: No contexts found");
                callback.onSuccess();
                return;
            }
            
            String username = null;
            String password = null;
            String packageName = "";
            String webDomain = null;
            
            // Duyệt từ cũ nhất đến mới nhất để "nhặt" dần data
            for (android.service.autofill.FillContext context : contexts) {
                AssistStructure structure = context.getStructure();
                if (packageName.isEmpty()) {
                    try { packageName = structure.getActivityComponent().getPackageName(); } catch (Exception e) {}
                }
                
                // Trích xuất domain nếu là trình duyệt (quan trọng để lưu đúng web thay vì lưu "Chrome")
                if (webDomain == null) {
                    webDomain = AutofillHelper.extractWebDomain(structure);
                }
                
                int windowCount = structure.getWindowNodeCount();
                for (int w = 0; w < windowCount; w++) {
                    AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(w);
                    UsernamePasswordPair pair = findCredentials(windowNode.getRootViewNode());
                    if (pair.username != null) {
                        username = pair.username;
                        Log.d(TAG, "Found username candidate: " + username);
                    }
                    if (pair.password != null) {
                        password = pair.password;
                        Log.d(TAG, "Found password candidate");
                    }
                }
            }
            
            // Ưu tiên dùng Web Domain nếu có (cho Browser), nếu không dùng Package Name (cho Native App)
            String finalTarget = (webDomain != null && !webDomain.isEmpty()) ? webDomain : packageName;
            
            Log.d(TAG, "Final Save Data - Target: " + finalTarget + 
                       ", User: " + (username != null) + ", Pass: " + (password != null));
            
            if (username == null || password == null) {
                Log.w(TAG, "Incomplete credentials. Abortion save.");
                callback.onSuccess();
                return;
            }
            
            // Mở activity để xác nhận lưu
            Intent saveIntent = new Intent(this, SaveCredentialActivity.class);
            saveIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            saveIntent.putExtra(SaveCredentialActivity.EXTRA_SAVE_MODE, true);
            saveIntent.putExtra(SaveCredentialActivity.EXTRA_USERNAME, username);
            saveIntent.putExtra(SaveCredentialActivity.EXTRA_PASSWORD, password);
            saveIntent.putExtra(SaveCredentialActivity.EXTRA_PACKAGE, finalTarget); // Lưu domain hoặc package
            
            Log.d(TAG, ">>> STARTING SaveCredentialActivity for target: " + finalTarget);
            startActivity(saveIntent);
            
            callback.onSuccess();
            
        } catch (Exception e) {
            Log.e(TAG, "Error in onSaveRequest: " + e.getMessage(), e);
            callback.onSuccess();
        }
    }
    
    /**
     * Helper class để chứa username/password pair
     */
    private static class UsernamePasswordPair {
        String username;
        String password;
    }
    
    /**
     * Đệ quy tìm credentials trong ViewNode tree
     */
    private UsernamePasswordPair findCredentials(AssistStructure.ViewNode node) {
        UsernamePasswordPair pair = new UsernamePasswordPair();
        findCredentialsRecursive(node, pair);
        return pair;
    }
    
    private void findCredentialsRecursive(AssistStructure.ViewNode node, UsernamePasswordPair pair) {
        if (node == null) return;
        
        // 1. Lấy text từ AutofillValue HOẶC getText() (Fallback quan trọng cho Browser)
        String nodeText = null;
        AutofillValue value = node.getAutofillValue();
        if (value != null && value.isText()) {
            nodeText = value.getTextValue().toString().trim();
        } else if (node.getText() != null) {
            nodeText = node.getText().toString().trim();
        }
        
        if (nodeText != null && !nodeText.isEmpty()) {
            // 2. Phân loại node (Password vs Username)
            int inputType = node.getInputType();
            boolean looksLikePassword = (inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                                        (inputType & android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                                        (inputType & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0 ||
                                        (inputType & android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0;
            
            // Check hints & ID
            String idEntry = node.getIdEntry();
            String[] hints = node.getAutofillHints();
            String combinedInfo = (idEntry != null ? idEntry : "") + " " + (node.getHint() != null ? node.getHint() : "");
            combinedInfo = combinedInfo.toLowerCase();
            
            if (hints != null) {
                for (String hint : hints) {
                    if (hint != null && (hint.toLowerCase().contains("password") || hint.toLowerCase().contains("mật khẩu"))) {
                        looksLikePassword = true;
                        break;
                    }
                }
            }
            if (!looksLikePassword && (combinedInfo.contains("password") || combinedInfo.contains("matkhau") || combinedInfo.contains("pass"))) {
                looksLikePassword = true;
            }
            
            Log.d(TAG, "  > Detected Text: " + (looksLikePassword ? "[SECURE]" : nodeText) + 
                       " | Class: " + node.getClassName() + 
                       " | ID: " + idEntry);
            
            if (looksLikePassword) {
                pair.password = nodeText;
                Log.d(TAG, "    => Assigned as Password");
            } else {
                // Heuristic: Ưu tiên chọn các node là ô nhập liệu (EditText) làm Username.
                boolean isEditField = node.getClassName() != null && node.getClassName().toString().toLowerCase().contains("edit");
                
                if (isEditField) {
                    pair.username = nodeText;
                    Log.d(TAG, "    => Assigned as Username (EditText priority)");
                } else if (pair.username == null && nodeText.length() < 30) {
                    // Cấp độ 1: Chưa có gì -> Coi là username
                    pair.username = nodeText;
                    Log.d(TAG, "    => Assigned as Username candidate (First text found)");
                } else if (pair.username != null && pair.password == null && !nodeText.equals(pair.username)) {
                    // Cấp độ 2: Đã có username nhưng chưa có password.
                    // Nếu node này khác username và có class là "null" hoặc generic (thường là Browser data),
                    // thì khả năng cực cao đây là Mật khẩu bị lộ dưới dạng plaintext trong structure.
                    if (node.getClassName() == null || node.getClassName().toString().contains("View")) {
                        if (nodeText.length() < 40) {
                            pair.password = nodeText;
                            Log.d(TAG, "    => Assigned as Password candidate (Sequential discovery)");
                        }
                    }
                }
            }
        }
        
        // Recurse children
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            findCredentialsRecursive(node.getChildAt(i), pair);
        }
    }

    private String getAppNameFromPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return "ứng dụng";
        try {
            return getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(packageName, 0)).toString();
        } catch (Exception e) {
            String[] parts = packageName.split("\\.");
            return parts[parts.length - 1];
        }
    }
}
