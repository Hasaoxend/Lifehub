package com.test.lifehub.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * EncryptionManager - Quản lý mã hóa đa nền tảng
 */
@Singleton
public class EncryptionManager {

    private static final String TAG = "EncryptionManager";
    private static final String PREFS_NAME = "lifehub_encryption_manager";
    private static final String KEY_ENCRYPTION_VERSION = "encryption_version";
    
    private static final int CURRENT_VERSION = 2;

    private final Context context;
    private final EncryptionHelper legacyHelper;
    private final CrossPlatformEncryptionHelper crossPlatformHelper;
    private final SharedPreferences prefs;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final ExecutorService executor;
    
    private boolean isInitialized = false;
    private int currentVersion = 1;

    @Inject
    public EncryptionManager(
            @ApplicationContext Context context,
            EncryptionHelper legacyHelper,
            CrossPlatformEncryptionHelper crossPlatformHelper,
            FirebaseFirestore db,
            FirebaseAuth auth
    ) {
        this.context = context;
        this.legacyHelper = legacyHelper;
        this.crossPlatformHelper = crossPlatformHelper;
        this.db = db;
        this.auth = auth;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
        this.currentVersion = prefs.getInt(KEY_ENCRYPTION_VERSION, 1);
    }
    
    public enum InitResult {
        SUCCESS,
        FAILURE,
        NEEDS_SETUP
    }

    public interface InitCallback {
        void onComplete(InitResult result);
    }

    /**
     * Khởi tạo encryption (thử unlock bằng passcode/password)
     */
    public void initialize(String secret, InitCallback callback) {
        if (secret == null || secret.isEmpty()) {
            Log.e(TAG, "Secret is empty");
            if (callback != null) callback.onComplete(InitResult.FAILURE);
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No authenticated user");
            if (callback != null) callback.onComplete(InitResult.FAILURE);
            return;
        }

        String userId = user.getUid();
        Log.d(TAG, "Initializing encryption for user: " + userId);

        executor.execute(() -> {
            InitResult result = initializeSynchronously(userId, secret);
            if (callback != null) {
                // Callback on main thread
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onComplete(result));
            }
        });
    }

    /**
     * Kiểm tra trạng thái setup (không cần secret)
     */
    public void checkSetupStatus(InitCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (callback != null) callback.onComplete(InitResult.FAILURE);
            return;
        }

        executor.execute(() -> {
            try {
                DocumentSnapshot snapshot = Tasks.await(db.collection("users").document(user.getUid()).get());
                String salt = snapshot.getString("encryptionSalt");
                String verification = snapshot.getString("encryptionVerification");

                InitResult result;
                if (salt == null || salt.isEmpty() || verification == null || verification.isEmpty()) {
                    result = InitResult.NEEDS_SETUP;
                } else {
                    result = InitResult.FAILURE; // Salt exists, but we don't have the secret to succeed
                }

                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onComplete(result));
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onComplete(InitResult.FAILURE));
                }
            }
        });
    }


    // Maintain for backward compatibility during transition
    public void initializeWithLoginPassword(String password, boolean isNewUser, EncryptionManager.InitCallback callback) {
        initialize(password, callback);
    }

    private InitResult initializeSynchronously(String userId, String secret) {
        try {
            DocumentReference userDoc = db.collection("users").document(userId);
            
            // BLOCKING call
            DocumentSnapshot snapshot = Tasks.await(userDoc.get());
            
            String remoteSalt = null;
            String remoteVerification = null;
            if (snapshot.exists()) {
                remoteSalt = snapshot.getString("encryptionSalt");
                remoteVerification = snapshot.getString("encryptionVerification");
            }

            if (remoteSalt == null || remoteSalt.isEmpty()) {
                // CHƯA CÓ SETUP PIN -> Yêu cầu setup
                Log.d(TAG, "No encryption setup found on Firestore");
                return finalizeInit(InitResult.NEEDS_SETUP);
            } else {
                Log.d(TAG, "Using existing salt: " + remoteSalt);
                crossPlatformHelper.setSaltFromBase64(remoteSalt);
                
                if (remoteVerification != null) {
                    boolean isValid = crossPlatformHelper.verifyMasterPassword(secret, remoteVerification);
                    if (!isValid) {
                        Log.e(TAG, "Invalid secret (verification failed)");
                        return finalizeInit(InitResult.FAILURE);
                    }
                    // Verification success -> Unlock key
                    boolean success = crossPlatformHelper.initializeWithMasterPassword(secret, false);
                    return finalizeInit(success ? InitResult.SUCCESS : InitResult.FAILURE);
                } else {
                    // Legacy case: Salt exists but no verification string
                    // This happens during the very first transition of a user to the cross-platform system
                    // We'll treat this as NEEDS_SETUP to be safe and force them into the new PIN flow
                    Log.w(TAG, "Salt exists but verification missing. Forcing PIN setup.");
                    return finalizeInit(InitResult.NEEDS_SETUP);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in initializeSynchronously", e);
            return finalizeInit(InitResult.FAILURE);
        }
    }

    private InitResult finalizeInit(InitResult result) {
        if (result == InitResult.SUCCESS) {
            isInitialized = true;
            prefs.edit().putInt(KEY_ENCRYPTION_VERSION, CURRENT_VERSION).apply();
            currentVersion = CURRENT_VERSION;
            Log.d(TAG, "✅ ENCRYPTION READY!");
        } else {
            Log.e(TAG, "❌ Encryption init state: " + result);
        }
        return result;
    }


    public boolean isUnlocked() {
        return isInitialized && crossPlatformHelper.isUnlocked();
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return "";
        
        if (crossPlatformHelper.isUnlocked()) {
            String result = crossPlatformHelper.encrypt(plainText);
            Log.d(TAG, "✅ Encrypted with CROSS-PLATFORM");
            return result;
        } else {
            Log.w(TAG, "⚠️ Using LEGACY encryption! crossPlatform.isUnlocked=" + crossPlatformHelper.isUnlocked());
            return legacyHelper.encrypt(plainText);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) return "";
        
        if (crossPlatformHelper.isUnlocked()) {
            String result = crossPlatformHelper.decrypt(encryptedText);
            if (!result.equals(encryptedText)) {
                return result;
            }
        }
        
        return legacyHelper.decrypt(encryptedText);
    }

    public void lock() {
        crossPlatformHelper.lock();
        isInitialized = false;
    }
    
    public int getCurrentVersion() {
        return currentVersion;
    }
}
