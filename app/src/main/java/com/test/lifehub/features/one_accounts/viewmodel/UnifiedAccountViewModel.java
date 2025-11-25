package com.test.lifehub.features.one_accounts.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.data.TotpAccount;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.data.UnifiedAccountItem;
import com.test.lifehub.features.one_accounts.repository.AccountRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel thống nhất kết hợp dữ liệu từ:
 * 1. AccountRepository (password accounts từ Firebase)
 * 2. TotpRepository (TOTP accounts từ Firestore với encryption)
 */
@HiltViewModel
public class UnifiedAccountViewModel extends ViewModel {

    private static final String TAG = "UnifiedAccountViewModel";

    private final AccountRepository accountRepository;
    private final TotpRepository totpRepository;
    private final EncryptionHelper encryptionHelper;
    private final SessionManager sessionManager;
    
    private final MediatorLiveData<List<UnifiedAccountItem>> unifiedAccountsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;

    @Inject
    public UnifiedAccountViewModel(
            AccountRepository accountRepository, 
            TotpRepository totpRepository,
            EncryptionHelper encryptionHelper,
            SessionManager sessionManager) {
        Log.d(TAG, "UnifiedAccountViewModel created");
        this.accountRepository = accountRepository;
        this.totpRepository = totpRepository;
        this.encryptionHelper = encryptionHelper;
        this.sessionManager = sessionManager;
        
        this.unifiedAccountsLiveData = new MediatorLiveData<>();
        this.isLoadingLiveData = new MutableLiveData<>(false);
        
        // Observe password accounts from AccountRepository
        unifiedAccountsLiveData.addSource(accountRepository.getAllAccounts(), passwordAccounts -> {
            Log.d(TAG, "Password accounts updated: " + (passwordAccounts != null ? passwordAccounts.size() : 0));
            combineAccounts(passwordAccounts, totpRepository.getAllAccounts().getValue());
        });
        
        // Observe TOTP accounts from TotpRepository
        unifiedAccountsLiveData.addSource(totpRepository.getAllAccounts(), totpAccounts -> {
            Log.d(TAG, "TOTP accounts updated: " + (totpAccounts != null ? totpAccounts.size() : 0));
            combineAccounts(accountRepository.getAllAccounts().getValue(), totpAccounts);
        });
    }
    
    public LiveData<List<UnifiedAccountItem>> getUnifiedAccounts() {
        return unifiedAccountsLiveData;
    }
    
    public LiveData<Boolean> isLoading() {
        return isLoadingLiveData;
    }
    
    /**
     * Refresh accounts data
     */
    public void refreshAccounts() {
        Log.d(TAG, "refreshAccounts() called");
        // Data will auto-refresh via LiveData observers
    }
    
    /**
     * Combine password accounts and TOTP accounts into unified list
     */
    private void combineAccounts(List<AccountEntry> passwordAccounts, List<TotpAccount> totpAccounts) {
        Log.d(TAG, "Combining accounts - Password: " + 
            (passwordAccounts != null ? passwordAccounts.size() : 0) + 
            ", TOTP: " + (totpAccounts != null ? totpAccounts.size() : 0));
        
        List<UnifiedAccountItem> unifiedList = new ArrayList<>();
        
        // Add password accounts (handle null safely)
        if (passwordAccounts != null && !passwordAccounts.isEmpty()) {
            for (AccountEntry account : passwordAccounts) {
                unifiedList.add(new UnifiedAccountItem(account));
            }
        }
        
        // Add TOTP accounts from Firestore
        if (totpAccounts != null && !totpAccounts.isEmpty()) {
            for (TotpAccount account : totpAccounts) {
                try {
                    // Decrypt secret key
                    String decryptedSecret = encryptionHelper.decrypt(account.getSecretKey());
                    
                    if (decryptedSecret != null && !decryptedSecret.isEmpty()) {
                        // UnifiedAccountItem(documentId, serviceName, username, secret, issuer)
                        unifiedList.add(new UnifiedAccountItem(
                            account.getDocumentId(),
                            account.getIssuer(),
                            account.getAccountName(),
                            decryptedSecret,
                            account.getIssuer()
                        ));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error decrypting TOTP account: " + account.getIssuer(), e);
                }
            }
        }
        
        // Sort by service name
        Collections.sort(unifiedList, new Comparator<UnifiedAccountItem>() {
            @Override
            public int compare(UnifiedAccountItem o1, UnifiedAccountItem o2) {
                String name1 = o1.getServiceName() != null ? o1.getServiceName() : "";
                String name2 = o2.getServiceName() != null ? o2.getServiceName() : "";
                return name1.compareToIgnoreCase(name2);
            }
        });
        
        Log.d(TAG, "Total unified accounts: " + unifiedList.size());
        unifiedAccountsLiveData.setValue(unifiedList);
    }
    
    /**
     * Load TOTP accounts from SessionManager
     * JSON structure: {"accountName": "user@example.com", "issuer": "Google", "secret": "ABCD1234..."}
     */
    private List<UnifiedAccountItem> getTotpAccountsFromSession() {
        List<UnifiedAccountItem> totpList = new ArrayList<>();
        
        try {
            String totpJson = sessionManager.getTotpAccounts();
            if (totpJson != null && !totpJson.isEmpty()) {
                JSONArray jsonArray = new JSONArray(totpJson);
                
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    
                    // Keys in JSON: accountName, issuer, secret
                    String accountName = obj.optString("accountName", "");
                    String issuer = obj.optString("issuer", "Unknown");
                    String secret = obj.optString("secret", "");
                    
                    if (!secret.isEmpty()) {
                        // UnifiedAccountItem(serviceName, username, secret, issuer)
                        // Map: issuer -> serviceName, accountName -> username
                        totpList.add(new UnifiedAccountItem(issuer, accountName, secret, issuer));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return totpList;
    }
    
    /**
     * Delete password account
     */
    public void deletePasswordAccount(AccountEntry account) {
        accountRepository.delete(account);
    }
    
    /**
     * Delete TOTP account from Firestore
     */
    public void deleteTotpAccount(String documentId, TotpRepository.OnCompleteListener listener) {
        Log.d(TAG, "Deleting TOTP account: " + documentId);
        totpRepository.delete(documentId, listener);
    }
    
    /**
     * Update password account
     */
    public void updatePasswordAccount(AccountEntry account) {
        // AccountViewModel doesn't have update method, would need to be added to repository
        // For now, this is a placeholder
    }
}
