package com.test.lifehub.features.one_accounts.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.test.lifehub.core.util.SessionManager;
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
 * 2. SessionManager (TOTP accounts từ EncryptedSharedPreferences)
 */
@HiltViewModel
public class UnifiedAccountViewModel extends ViewModel {

    private final AccountRepository accountRepository;
    private final SessionManager sessionManager;
    
    private final MediatorLiveData<List<UnifiedAccountItem>> unifiedAccountsLiveData;
    private final MutableLiveData<Boolean> isLoadingLiveData;

    @Inject
    public UnifiedAccountViewModel(AccountRepository accountRepository, SessionManager sessionManager) {
        this.accountRepository = accountRepository;
        this.sessionManager = sessionManager;
        
        this.unifiedAccountsLiveData = new MediatorLiveData<>();
        this.isLoadingLiveData = new MutableLiveData<>(false);
        
        // Observe password accounts from AccountRepository
        unifiedAccountsLiveData.addSource(accountRepository.getAllAccounts(), passwordAccounts -> {
            combineAccounts(passwordAccounts);
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
        // Manually trigger recombination with current password accounts
        LiveData<List<AccountEntry>> accountsLiveData = accountRepository.getAllAccounts();
        List<AccountEntry> currentAccounts = accountsLiveData.getValue();
        combineAccounts(currentAccounts != null ? currentAccounts : new ArrayList<>());
    }
    
    /**
     * Combine password accounts and TOTP accounts into unified list
     */
    private void combineAccounts(List<AccountEntry> passwordAccounts) {
        List<UnifiedAccountItem> unifiedList = new ArrayList<>();
        
        // Add password accounts (handle null safely)
        if (passwordAccounts != null && !passwordAccounts.isEmpty()) {
            for (AccountEntry account : passwordAccounts) {
                unifiedList.add(new UnifiedAccountItem(account));
            }
        }
        
        // Add TOTP accounts
        List<UnifiedAccountItem> totpAccounts = getTotpAccountsFromSession();
        unifiedList.addAll(totpAccounts);
        
        // Sort by service name
        Collections.sort(unifiedList, new Comparator<UnifiedAccountItem>() {
            @Override
            public int compare(UnifiedAccountItem o1, UnifiedAccountItem o2) {
                String name1 = o1.getServiceName() != null ? o1.getServiceName() : "";
                String name2 = o2.getServiceName() != null ? o2.getServiceName() : "";
                return name1.compareToIgnoreCase(name2);
            }
        });
        
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
     * Delete TOTP account
     * @param serviceName Actually the issuer from TOTP data
     * @param username Actually the accountName from TOTP data
     */
    public void deleteTotpAccount(String serviceName, String username) {
        try {
            String totpJson = sessionManager.getTotpAccounts();
            if (totpJson == null || totpJson.isEmpty() || totpJson.equals("[]")) {
                return;
            }
            
            JSONArray jsonArray = new JSONArray(totpJson);
            JSONArray newArray = new JSONArray();
            
            boolean found = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                // Keys in JSON: accountName, issuer, secret
                String accountName = obj.optString("accountName", "");
                String issuer = obj.optString("issuer", "");
                
                // serviceName is mapped to issuer, username is mapped to accountName
                boolean isMatch = issuer.trim().equals(serviceName.trim()) && 
                                 accountName.trim().equals(username.trim());
                
                if (!isMatch) {
                    newArray.put(obj);
                } else {
                    found = true;
                }
            }
            
            if (found) {
                sessionManager.saveTotpAccounts(newArray.toString());
                // Force refresh by manually triggering combineAccounts
                refreshAccounts();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Update password account
     */
    public void updatePasswordAccount(AccountEntry account) {
        // AccountViewModel doesn't have update method, would need to be added to repository
        // For now, this is a placeholder
    }
}
