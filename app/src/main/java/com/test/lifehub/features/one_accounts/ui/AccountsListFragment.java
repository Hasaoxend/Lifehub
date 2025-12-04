package com.test.lifehub.features.one_accounts.ui;

import android.content.Intent;
import android.os.Bundle;
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

import com.test.lifehub.R;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment hiển thị danh sách tài khoản (passwords)
 */
@AndroidEntryPoint
public class AccountsListFragment extends Fragment {

    private AccountViewModel mAccountViewModel;
    private AccountAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private TextView mEmptyView;

    private List<AccountEntry> mAllAccounts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts_list, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view_accounts);
        mSearchView = view.findViewById(R.id.search_view_accounts);
        mEmptyView = view.findViewById(R.id.tv_empty_list);

        mAdapter = new AccountAdapter();
        mAdapter.setOnAccountActionListener(account -> {
            mAccountViewModel.delete(account);
            Toast.makeText(getContext(), R.string.account_deleted_msg, Toast.LENGTH_SHORT).show();
        });

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupSearchView();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAccountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        mAccountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            mAllAccounts = accounts != null ? accounts : new ArrayList<>();
            filterAccounts(mSearchView.getQuery().toString());
        });
    }

    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

    private void filterAccounts(String query) {
        List<AccountEntry> filteredList = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filteredList = mAllAccounts;
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (AccountEntry account : mAllAccounts) {
                if (account.serviceName.toLowerCase().contains(lowerCaseQuery) ||
                        account.username.toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(account);
                }
            }
        }

        mAdapter.submitList(filteredList);
        updateEmptyView(filteredList);
    }

    private void updateEmptyView(List<AccountEntry> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }
}
