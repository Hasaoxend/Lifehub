package com.test.lifehub.features.authenticator.ui;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.core.util.TotpManager;

import java.util.List;

/**
 * Adapter cho danh sách tài khoản TOTP
 */
public class TotpAccountsAdapter extends RecyclerView.Adapter<TotpAccountsAdapter.ViewHolder> {
    
    private static final String TAG = "TotpAccountsAdapter";

    private List<AuthenticatorActivity.TotpAccountItem> accounts;
    private OnAccountActionListener listener;

    public interface OnAccountActionListener {
        void onCopyCode(AuthenticatorActivity.TotpAccountItem account);
        void onDeleteAccount(AuthenticatorActivity.TotpAccountItem account);
    }

    public TotpAccountsAdapter(List<AuthenticatorActivity.TotpAccountItem> accounts, 
                              OnAccountActionListener listener) {
        this.accounts = accounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_totp_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder called for position: " + position);
        AuthenticatorActivity.TotpAccountItem account = accounts.get(position);
        Log.d(TAG, "Binding account: " + account.getIssuer() + " / " + account.getAccountName());
        
        holder.tvAccountName.setText(account.getAccountName());
        holder.tvIssuer.setText(account.getIssuer());
        holder.tvCode.setText(formatCode(account.getCurrentCode()));
        
        int timeRemaining = account.getTimeRemaining();
        holder.progressBar.setMax(30);
        holder.progressBar.setProgress(timeRemaining);
        
        // Change color when time is running out
        if (timeRemaining <= 5) {
            holder.tvCode.setTextColor(Color.parseColor("#F44336")); // Red
        } else {
            holder.tvCode.setTextColor(Color.parseColor("#4CAF50")); // Green
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCopyCode(account);
            }
        });
        
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteAccount(account);
            }
        });
    }

    @Override
    public int getItemCount() {
        int count = accounts.size();
        Log.d(TAG, "getItemCount() returning: " + count);
        return count;
    }

    /**
     * Format mã TOTP thành dạng 3 3 (ví dụ: 123 456)
     */
    private String formatCode(String code) {
        if (code.length() == 6) {
            return code.substring(0, 3) + " " + code.substring(3);
        }
        return code;
    }

    /**
     * Cập nhật mã OTP và thanh progress
     */
    public void updateCodes() {
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName;
        TextView tvIssuer;
        TextView tvCode;
        ProgressBar progressBar;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvAccountName = itemView.findViewById(R.id.tv_account_name);
            tvIssuer = itemView.findViewById(R.id.tv_issuer);
            tvCode = itemView.findViewById(R.id.tv_totp_code);
            progressBar = itemView.findViewById(R.id.progress_time_remaining);
            btnDelete = itemView.findViewById(R.id.btn_delete_account);
        }
    }
}
