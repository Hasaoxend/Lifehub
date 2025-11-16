package com.test.lifehub.features.one_accounts.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.one_accounts.data.AccountEntry;

import java.util.Objects;

public class AccountAdapter extends ListAdapter<AccountEntry, AccountAdapter.AccountViewHolder> {

    public interface OnAccountActionListener {
        void onDelete(AccountEntry account);
    }

    private OnAccountActionListener mListener;

    public AccountAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnAccountActionListener(OnAccountActionListener listener) {
        this.mListener = listener;
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

        // Normal click - Open detail view
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, AccountDetailActivity.class);
            intent.putExtra(AccountDetailActivity.EXTRA_ACCOUNT_ID, currentAccount.documentId);
            context.startActivity(intent);
        });

        // Long click - Show menu
        holder.itemView.setOnLongClickListener(v -> {
            showPopupMenu(v, currentAccount);
            return true;
        });
    }

    private void showPopupMenu(View view, AccountEntry account) {
        PopupMenu popup = new PopupMenu(view.getContext(), view);
        popup.getMenuInflater().inflate(R.menu.account_item_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit_account) {
                // Open edit
                Context context = view.getContext();
                Intent intent = new Intent(context, AddEditAccountActivity.class);
                intent.putExtra(AddEditAccountActivity.EXTRA_ACCOUNT_ID, account.documentId);
                context.startActivity(intent);
                return true;
            } else if (id == R.id.action_delete_account) {
                // Confirm delete
                new AlertDialog.Builder(view.getContext())
                        .setTitle("Xác nhận Xóa")
                        .setMessage("Bạn có chắc chắn muốn xóa tài khoản \"" + account.serviceName + "\" không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            if (mListener != null) {
                                mListener.onDelete(account);
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .setIcon(R.drawable.ic_warning)
                        .show();
                return true;
            }
            return false;
        });

        popup.show();
    }

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