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

/**
 * Fragment (màn hình) chính cho tab "Tài khoản".
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 *
 * Lỗi crash "AppDatabase_Impl does not exist" của bạn
 * đã được SỬA LỖI trong file này.
 */
public class AccountFragment extends Fragment {

    private AccountViewModel mAccountViewModel;
    private AccountAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFab;

    // (Chúng ta cũng cần file AccountAdapter.java phiên bản Firestore)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);

        // --- Thiết lập RecyclerView ---
        mRecyclerView = view.findViewById(R.id.recycler_view_accounts);

        // (Adapter sẽ được cung cấp ở file tiếp theo)
        mAdapter = new AccountAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // --- Thiết lập Nút FAB (Thêm mới) ---
        mFab = view.findViewById(R.id.fab_add_account);
        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditAccountActivity.class);
            // (Không gửi ID, màn hình AddEdit sẽ hiểu đây là "Thêm mới")
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy ViewModel

        // ----- SỬA LỖI CRASH (CỦA BẠN) NẰM Ở ĐÂY -----
        // Dòng này (dòng 63 cũ) đã từng gây crash.
        // Bây giờ nó sẽ gọi AccountViewModel (phiên bản Firestore),
        // và sẽ KHÔNG GÂY CRASH nữa.
        mAccountViewModel = new ViewModelProvider(this).get(AccountViewModel.class);
        // ------------------------------------------

        // "Quan sát" (Observe) LiveData từ ViewModel
        mAccountViewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            // Cập nhật danh sách mới cho Adapter
            mAdapter.submitList(accounts);
        });
    }
}