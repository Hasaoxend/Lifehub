package com.test.lifehub.features.one_accounts.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.core.util.TotpManager;
import com.test.lifehub.features.authenticator.ui.AddTotpAccountActivity;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.features.one_accounts.data.UnifiedAccountItem;
import com.test.lifehub.features.one_accounts.viewmodel.UnifiedAccountViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AccountFragment extends Fragment implements UnifiedAccountAdapter.OnItemClickListener {

    private static final String TAG = "AccountFragment";
    private static final int REQUEST_ADD_TOTP_MANUAL = 1007;

    private RecyclerView recyclerView;
    private UnifiedAccountAdapter adapter;
    private TextView emptyListText;
    private SearchView searchView;
    private FloatingActionButton fab;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private UnifiedAccountViewModel viewModel;
    
    private Handler totpUpdateHandler;
    private Runnable totpUpdateRunnable;
    
    private List<UnifiedAccountItem> allAccounts = new ArrayList<>();
    private List<UnifiedAccountItem> filteredAccounts = new ArrayList<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);

        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupSearchView();
        setupFab();
        setupSwipeRefresh();
        setupTotpUpdater();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rv_unified_accounts);
        emptyListText = view.findViewById(R.id.tv_empty_list);
        searchView = view.findViewById(R.id.search_view_accounts);
        fab = view.findViewById(R.id.fab_add_account);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(UnifiedAccountViewModel.class);
        
        viewModel.getUnifiedAccounts().observe(getViewLifecycleOwner(), accounts -> {
            allAccounts = accounts != null ? accounts : new ArrayList<>();
            filterAccounts(searchView.getQuery().toString());
            updateEmptyState();
        });
    }

    private void setupRecyclerView() {
        adapter = new UnifiedAccountAdapter();
        adapter.setOnItemClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterAccounts(newText);
                return true;
            }
        });
    }

    private void setupFab() {
        fab.setOnClickListener(v -> showAddAccountBottomSheet());
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d(TAG, "Refreshing accounts...");
            viewModel.refreshAccounts();
            // Stop refreshing after a short delay
            swipeRefreshLayout.postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });
        
        // Set color scheme
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupTotpUpdater() {
        totpUpdateHandler = new Handler();
        totpUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (adapter != null) {
                    adapter.updateCodes();
                }
                totpUpdateHandler.postDelayed(this, 1000); // Update every second
            }
        };
    }

    private void filterAccounts(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredAccounts = new ArrayList<>(allAccounts);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            filteredAccounts = new ArrayList<>();
            
            for (UnifiedAccountItem item : allAccounts) {
                String serviceName = item.getServiceName() != null ? item.getServiceName().toLowerCase() : "";
                String username = item.getUsername() != null ? item.getUsername().toLowerCase() : "";
                
                if (serviceName.contains(lowerQuery) || username.contains(lowerQuery)) {
                    filteredAccounts.add(item);
                }
            }
        }
        
        adapter.submitList(filteredAccounts);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredAccounts.isEmpty();
        emptyListText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showAddAccountBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_add_account_unified, null);
        bottomSheet.setContentView(sheetView);

        MaterialCardView cardAddPassword = sheetView.findViewById(R.id.card_add_password);
        MaterialCardView cardManualInput = sheetView.findViewById(R.id.card_manual_input);

        cardAddPassword.setOnClickListener(v -> {
            bottomSheet.dismiss();
            Intent intent = new Intent(getActivity(), AddEditAccountActivity.class);
            startActivity(intent);
        });

        cardManualInput.setOnClickListener(v -> {
            bottomSheet.dismiss();
            openManualInputActivity();
        });

        bottomSheet.show();
    }


    private void openManualInputActivity() {
        Intent intent = new Intent(getContext(), AddTotpAccountActivity.class);
        startActivityForResult(intent, REQUEST_ADD_TOTP_MANUAL);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ADD_TOTP_MANUAL && resultCode == getActivity().RESULT_OK) {
            viewModel.refreshAccounts();
        }
    }

    // UnifiedAccountAdapter.OnItemClickListener implementations
    @Override
    public void onPasswordAccountClick(UnifiedAccountItem item) {
        Intent intent = new Intent(getContext(), AccountDetailActivity.class);
        intent.putExtra("account_id", item.getId());
        startActivity(intent);
    }

    @Override
    public void onTotpAccountClick(UnifiedAccountItem item) {
        // Generate TOTP code from secret using getCurrentCode
        String code = TotpManager.getCurrentCode(item.getSecret());
        
        // Copy TOTP code to clipboard
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("TOTP Code", code);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(getContext(), "Code copied: " + code, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPasswordAccountMenuClick(UnifiedAccountItem item, View anchor) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete this account?")
            .setPositiveButton("Delete", (dialog, which) -> {
                try {
                    // Convert UnifiedAccountItem back to AccountEntry for deletion
                    AccountEntry accountEntry = new AccountEntry();
                    accountEntry.documentId = item.getId();
                    accountEntry.serviceName = item.getServiceName();
                    accountEntry.username = item.getUsername();
                    accountEntry.password = item.getPassword();
                    accountEntry.notes = item.getNotes();
                    
                    viewModel.deletePasswordAccount(accountEntry);
                    
                    // Show toast only if fragment is still attached
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
                    }
                    e.printStackTrace();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onTotpAccountMenuClick(UnifiedAccountItem item, View anchor) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "Delete TOTP account clicked");
        Log.d(TAG, "Service: " + item.getServiceName());
        Log.d(TAG, "Username: " + item.getUsername());
        Log.d(TAG, "Document ID: " + item.getId());
        Log.d(TAG, "========================================");
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Authenticator")
            .setMessage("Are you sure you want to delete this authenticator account?")
            .setPositiveButton("Delete", (dialog, which) -> {
                try {
                    String documentId = item.getId();
                    if (documentId == null || documentId.isEmpty()) {
                        Log.e(TAG, "✗ Cannot delete: Document ID is null or empty");
                        Toast.makeText(getContext(), "Error: Invalid document ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Log.d(TAG, "Calling viewModel.deleteTotpAccount with ID: " + documentId);
                    viewModel.deleteTotpAccount(documentId, new com.test.lifehub.features.authenticator.repository.TotpRepository.OnCompleteListener() {
                        @Override
                        public void onSuccess(String documentId) {
                            Log.d(TAG, "✓ Delete successful: " + documentId);
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Authenticator deleted", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            Log.e(TAG, "✗ Delete failed: " + error);
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Failed to delete authenticator: " + error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "✗ Exception during delete", e);
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Failed to delete authenticator", Toast.LENGTH_SHORT).show();
                    }
                    e.printStackTrace();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        totpUpdateHandler.post(totpUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        totpUpdateHandler.removeCallbacks(totpUpdateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (totpUpdateHandler != null) {
            totpUpdateHandler.removeCallbacks(totpUpdateRunnable);
        }
    }
}