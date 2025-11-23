package com.test.lifehub.features.one_accounts.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.util.SessionManager;
import com.test.lifehub.features.authenticator.ui.AuthenticatorActivity;
import com.test.lifehub.features.authenticator.ui.TotpAccountsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment hiển thị danh sách tài khoản TOTP
 */
public class TotpAccountsFragment extends Fragment {

    private static final String PREF_TUTORIAL_SHOWN = "totp_tutorial_shown";

    private RecyclerView rvAccounts;
    private LinearLayout layoutEmpty;
    private FloatingActionButton fabAddFirst;
    private RelativeLayout tutorialOverlay;
    private Button btnGotIt;

    private TotpAccountsAdapter adapter;
    private List<AuthenticatorActivity.TotpAccountItem> accounts;
    private SessionManager sessionManager;

    private Handler handler;
    private Runnable updateRunnable;

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
        View view = inflater.inflate(R.layout.fragment_totp_accounts, container, false);

        sessionManager = new SessionManager(requireContext());
        accounts = new ArrayList<>();

        findViews(view);
        setupRecyclerView();
        setupListeners();

        loadAccounts();
        startAutoUpdate();

        // Show tutorial if first time
        checkAndShowTutorial();

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

                accounts.add(new AuthenticatorActivity.TotpAccountItem(accountName, issuer, secret));
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
            .setTitle("Xóa tài khoản")
            .setMessage("Bạn có chắc muốn xóa tài khoản \"" + account.getAccountName() + "\"?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                accounts.remove(account);
                saveAccounts();
                updateEmptyView();
                adapter.notifyDataSetChanged();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
            requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("TOTP Code", text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(getContext(), "Đã sao chép mã: " + text, Toast.LENGTH_SHORT).show();
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
