package com.test.lifehub.features.one_accounts.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.Objects;

/**
 * Adapter cho RecyclerView hiển thị danh sách các Tài khoản (AccountEntry).
 * (Phiên bản đã VIẾT LẠI cho Firebase Firestore)
 */
public class AccountAdapter extends ListAdapter<AccountEntry, AccountAdapter.AccountViewHolder> {

    /**
     * Khởi tạo Adapter.
     */
    public AccountAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AccountEntry currentAccount = getItem(position);

        holder.tvServiceName.setText(currentAccount.serviceName);
        holder.tvUsername.setText(currentAccount.username);

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, AddEditAccountActivity.class);

            // SỬA LỖI: Dòng này giờ sẽ hoạt động
            // vì EXTRA_ACCOUNT_ID đã được định nghĩa trong AddEditAccountActivity
            intent.putExtra(AddEditAccountActivity.EXTRA_ACCOUNT_ID, currentAccount.documentId);

            context.startActivity(intent);
        });
    }

    /**
     * Lớp ViewHolder
     */
    static class AccountViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivServiceIcon;
        private final TextView tvServiceName;
        private final TextView tvUsername;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            ivServiceIcon = itemView.findViewById(R.id.iv_service_icon);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
        }
    }

    /**
     * DiffUtil Callback
     */
    private static final DiffUtil.ItemCallback<AccountEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<AccountEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull AccountEntry oldItem, @NonNull AccountEntry newItem) {
            if (oldItem.documentId == null || newItem.documentId == null) {
                return false;
            }
            return oldItem.documentId.equals(newItem.documentId);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AccountEntry oldItem, @NonNull AccountEntry newItem) {
            return Objects.equals(oldItem.serviceName, newItem.serviceName) &&
                    Objects.equals(oldItem.username, newItem.username);
        }
    };
}