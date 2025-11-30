package com.test.lifehub.features.one_accounts.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.test.lifehub.R;
import com.test.lifehub.features.one_accounts.data.AccountEntry;
import com.test.lifehub.core.util.ServiceIconHelper;
import com.test.lifehub.core.util.TotpManager;
import com.test.lifehub.features.one_accounts.data.UnifiedAccountItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified adapter for both password and TOTP accounts
 */
public class UnifiedAccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_PASSWORD = 0;
    private static final int VIEW_TYPE_TOTP = 1;

    private List<UnifiedAccountItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public UnifiedAccountAdapter() {
        // Default constructor
    }

    public interface OnItemClickListener {
        void onPasswordAccountClick(UnifiedAccountItem item);
        void onTotpAccountClick(UnifiedAccountItem item);
        void onPasswordAccountMenuClick(UnifiedAccountItem item, View anchor);
        void onTotpAccountMenuClick(UnifiedAccountItem item, View anchor);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<UnifiedAccountItem> newItems) {
        this.items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateCodes() {
        // Only update TOTP items
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType() == UnifiedAccountItem.AccountType.PASSWORD 
            ? VIEW_TYPE_PASSWORD 
            : VIEW_TYPE_TOTP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_PASSWORD) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unified_account_password, parent, false);
            return new PasswordViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unified_account_totp, parent, false);
            return new TotpViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UnifiedAccountItem item = items.get(position);
        
        if (holder instanceof PasswordViewHolder) {
            ((PasswordViewHolder) holder).bind(item, listener);
        } else if (holder instanceof TotpViewHolder) {
            ((TotpViewHolder) holder).bind(item, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Password Account ViewHolder
    static class PasswordViewHolder extends RecyclerView.ViewHolder {
        ImageView ivServiceIcon;
        TextView tvServiceName;
        TextView tvUsername;
        ImageButton btnMenu;

        PasswordViewHolder(View itemView) {
            super(itemView);
            // Find ImageView inside FrameLayout
            ivServiceIcon = itemView.findViewById(R.id.iv_service_icon_image);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            btnMenu = itemView.findViewById(R.id.btn_menu);
        }

        void bind(UnifiedAccountItem item, OnItemClickListener listener) {
            tvServiceName.setText(item.getServiceName());
            tvUsername.setText(item.getUsername());
            
            // Set service icon
            ivServiceIcon.setImageDrawable(
                ServiceIconHelper.getServiceIcon(itemView.getContext(), item.getServiceName())
            );

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPasswordAccountClick(item);
                }
            });

            btnMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPasswordAccountMenuClick(item, v);
                }
            });
        }
    }

    // TOTP Account ViewHolder
    static class TotpViewHolder extends RecyclerView.ViewHolder {
        ImageView ivServiceIcon;
        TextView tvServiceName;
        TextView tvUsername;
        TextView tvTotpCode;
        ProgressBar progressTimeRemaining;
        ImageButton btnMenu;

        TotpViewHolder(View itemView) {
            super(itemView);
            // Find ImageView inside FrameLayout
            ivServiceIcon = itemView.findViewById(R.id.iv_service_icon_image);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTotpCode = itemView.findViewById(R.id.tv_totp_code);
            progressTimeRemaining = itemView.findViewById(R.id.progress_time_remaining);
            btnMenu = itemView.findViewById(R.id.btn_menu);
        }

        void bind(UnifiedAccountItem item, OnItemClickListener listener) {
            String serviceName = item.getIssuer() != null && !item.getIssuer().isEmpty() 
                ? item.getIssuer() 
                : item.getServiceName();
            
            tvServiceName.setText(serviceName);
            tvUsername.setText(item.getUsername());
            
            // Generate TOTP code
            String code = TotpManager.getCurrentCode(item.getSecret());
            tvTotpCode.setText(formatCode(code));
            
            // Update progress
            int timeRemaining = TotpManager.getTimeRemaining();
            progressTimeRemaining.setMax(30);
            progressTimeRemaining.setProgress(timeRemaining);
            
            // Change color when time is running out
            if (timeRemaining <= 5) {
                tvTotpCode.setTextColor(Color.parseColor("#F44336")); // Red
                progressTimeRemaining.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336"))
                );
            } else {
                tvTotpCode.setTextColor(Color.parseColor("#4CAF50")); // Green
                progressTimeRemaining.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
                );
            }
            
            // Set service icon
            ivServiceIcon.setImageDrawable(
                ServiceIconHelper.getServiceIcon(itemView.getContext(), serviceName)
            );

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTotpAccountClick(item);
                }
            });

            btnMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTotpAccountMenuClick(item, v);
                }
            });
        }

        private String formatCode(String code) {
            if (code.length() == 6) {
                return code.substring(0, 3) + " " + code.substring(3);
            }
            return code;
        }
    }
}
