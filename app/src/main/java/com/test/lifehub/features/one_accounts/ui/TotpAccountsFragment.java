package com.test.lifehub.features.one_accounts.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.security.EncryptionHelper;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.data.TotpAccount;
import com.test.lifehub.features.authenticator.repository.TotpRepository;
import com.test.lifehub.features.authenticator.ui.AuthenticatorActivity;
import com.test.lifehub.features.authenticator.ui.TotpAccountsAdapter;
import com.test.lifehub.features.authenticator.viewmodel.AuthenticatorViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment hiển thị danh sách tài khoản TOTP
 * Sử dụng Firestore để đồng bộ dữ liệu
 */
@AndroidEntryPoint
public class TotpAccountsFragment extends Fragment {

    private static final String TAG = "TotpAccountsFragment";
    private static final String PREF_TUTORIAL_SHOWN = "totp_tutorial_shown";

    private RecyclerView rvAccounts;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabAddFirst;
    private RelativeLayout tutorialOverlay;
    private Button btnGotIt;

    private TotpAccountsAdapter adapter;
    private List<AuthenticatorActivity.TotpAccountItem> accounts;
    private SessionManager sessionManager;
    private AuthenticatorViewModel viewModel;

    private Handler handler;
    private Runnable updateRunnable;

    @Inject
    EncryptionHelper encryptionHelper;

    // Listener to notify parent fragment
    private OnFabClickListener fabClickListener;

    public interface OnFabClickListener {
        void onFabClick();
    }

    public void setOnFabClickListener(OnFabClickListener listener) {
        this.fabClickListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "==================== onCreateView START ====================");
        View view = inflater.inflate(R.layout.fragment_totp_accounts, container, false);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(this).get(AuthenticatorViewModel.class);
        Log.d(TAG, "ViewModel created: " + viewModel.hashCode());
        
        accounts = new ArrayList<>();

        findViews(view);
        setupRecyclerView();
        setupListeners();

        // Load từ Firestore thay vì SessionManager
        observeAccounts();
        startAutoUpdate();

        // Show tutorial if first time
        checkAndShowTutorial();

        Log.d(TAG, "==================== onCreateView END ====================");
        return view;
    }

    private void findViews(View view) {
        rvAccounts = view.findViewById(R.id.rv_totp_accounts);
        layoutEmpty = view.findViewById(R.id.layout_empty_totp);
        fabAddFirst = view.findViewById(R.id.fab_add_first_totp);
        tutorialOverlay = view.findViewById(R.id.tutorial_overlay);
        btnGotIt = view.findViewById(R.id.btn_got_it);
    }

    private void setupRecyclerView() {
        adapter = new TotpAccountsAdapter(accounts, new TotpAccountsAdapter.OnAccountActionListener() {
            @Override
            public void onCopyCode(AuthenticatorActivity.TotpAccountItem account) {
                copyToClipboard(account.getCurrentCode());
            }

            @Override
            public void onDeleteAccount(AuthenticatorActivity.TotpAccountItem account) {
                showDeleteConfirmDialog(account);
            }
        });

        rvAccounts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAccounts.setAdapter(adapter);
    }

    private void setupListeners() {
        fabAddFirst.setOnClickListener(v -> {
            if (fabClickListener != null) {
                fabClickListener.onFabClick();
            }
        });

        btnGotIt.setOnClickListener(v -> {
            tutorialOverlay.setVisibility(View.GONE);
            // Save that tutorial was shown
            sessionManager.setFirstRun(false);
        });
    }

    /**
     * Observe TOTP accounts từ Firestore
     */
    private void observeAccounts() {
        Log.d(TAG, "Setting up Firestore observer...");
        viewModel.getAllAccounts().observe(getViewLifecycleOwner(), totpAccounts -> {
            Log.d(TAG, "Observer triggered with " + (totpAccounts != null ? totpAccounts.size() : 0) + " accounts");
            
            if (totpAccounts != null) {
                accounts.clear();
                
                // Kiểm tra EncryptionHelper
                if (encryptionHelper == null) {
                    Log.e(TAG, "EncryptionHelper is NULL! Cannot decrypt secrets.");
                    updateEmptyView();
                    return;
                }
                
                // Convert TotpAccount to TotpAccountItem
                for (TotpAccount account : totpAccounts) {
                    try {
                        Log.d(TAG, "Processing account: " + account.getIssuer() + " / " + account.getAccountName());
                        
                        // Giải mã secret key
                        String decryptedSecret = encryptionHelper.decrypt(account.getSecretKey());
                        
                        if (decryptedSecret == null || decryptedSecret.isEmpty()) {
                            Log.e(TAG, "Failed to decrypt secret for: " + account.getIssuer());
                            continue;
                        }
                        
                        accounts.add(new AuthenticatorActivity.TotpAccountItem(
                            account.getDocumentId(),
                            account.getAccountName(),
                            account.getIssuer(),
                            decryptedSecret
                        ));
                        
                        Log.d(TAG, "Successfully added account: " + account.getIssuer());
                    } catch (Exception e) {
                        Log.e(TAG, "Error decrypting secret for account: " + account.getIssuer(), e);
                    }
                }
                
                Log.d(TAG, "Final account count in UI: " + accounts.size());
                updateEmptyView();
                adapter.notifyDataSetChanged();
            } else {
                Log.w(TAG, "totpAccounts is NULL");
            }
        });
    }

    /**
     * DEPRECATED: Load từ SessionManager (local JSON)
     * Giữ lại để migrate data cũ nếu cần
     */
    private void loadAccounts() {
        accounts.clear();
        try {
            String accountsJson = sessionManager.getTotpAccounts();
            JSONArray jsonArray = new JSONArray(accountsJson);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String accountName = obj.getString("accountName");
                String issuer = obj.getString("issuer");
                String secret = obj.getString("secret");

                // Sử dụng empty string cho documentId vì đây là local data
                accounts.add(new AuthenticatorActivity.TotpAccountItem("", accountName, issuer, secret));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        updateEmptyView();
        adapter.notifyDataSetChanged();
    }

    private void saveAccounts() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (AuthenticatorActivity.TotpAccountItem account : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("accountName", account.getAccountName());
                obj.put("issuer", account.getIssuer());
                obj.put("secret", account.getSecret());
                jsonArray.put(obj);
            }
            sessionManager.saveTotpAccounts(jsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateEmptyView() {
        if (accounts.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvAccounts.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvAccounts.setVisibility(View.VISIBLE);
        }
    }

    private void startAutoUpdate() {
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null) {
                    adapter.updateCodes();
                }
                if (handler != null) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateRunnable);
    }

    private void checkAndShowTutorial() {
        // Show tutorial only if:
        // 1. First time (isFirstRun)
        // 2. No accounts yet
        if (sessionManager.isFirstRun() && accounts.isEmpty()) {
            tutorialOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteConfirmDialog(AuthenticatorActivity.TotpAccountItem account) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_delete_account)
            .setMessage(getString(R.string.msg_delete_account_confirm, account.getAccountName()))
            .setPositiveButton("Xóa", (dialog, which) -> {
                // Xóa từ Firestore
                viewModel.delete(account.getDocumentId(), new TotpRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess(String documentId) {
                        Toast.makeText(requireContext(), R.string.account_deleted_msg, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(requireContext(), getString(R.string.authenticator_add_error, error), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("TOTP Code", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), getString(R.string.code_copied, text), Toast.LENGTH_SHORT).show();
    }

    public void refreshAccounts() {
        loadAccounts();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }
}
