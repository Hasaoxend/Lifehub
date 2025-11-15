package com.test.lifehub.features.one_accounts.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;

import dagger.hilt.android.AndroidEntryPoint; // <-- THÊM IMPORT NÀY

/**
 * Fragment (màn hình) chính cho tab "Tài khoản".
 * (Phiên bản đã refactor để dùng Hilt)
 */
@AndroidEntryPoint // <-- THÊM CHÚ THÍCH NÀY
public class AccountFragment extends Fragment {

    private AccountViewModel mAccountViewModel;
    private AccountAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);

        // --- Thiết lập RecyclerView ---
        mRecyclerView = view.findViewById(R.id.recycler_view_accounts);

        // (Adapter sẽ được cung cấp ở file tiếp theo - Giả sử bạn đã có
        // file AccountAdapter.java phiên bản Firestore)
        mAdapter = new AccountAdapter(); // (Đảm bảo Adapter này dùng DiffUtil/submitList)
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- Thiết lập Nút FAB (Thêm mới) ---
        mFab = view.findViewById(R.id.fab_add_account);
        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditAccountActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy ViewModel (Hilt sẽ tự động cung cấp)
        mAccountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);

        // "Quan sát" (Observe) LiveData từ ViewModel
        mAccountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            // Cập nhật danh sách mới cho Adapter
            // Đảm bảo AccountAdapter của bạn có hàm submitList
            // Nếu không, hãy dùng mAdapter.setAccounts(accounts);
            mAdapter.submitList(accounts);
        });
    }
}