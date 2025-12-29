package com.test.lifehub.features.one_accounts.ui;

import android.animation.ObjectAnimator;
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
 * Unified adapter for both password and TOTP accounts with collapsible sections
 */
public class UnifiedAccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER_TOTP = 0;
    private static final int VIEW_TYPE_HEADER_PASSWORD = 1;
    private static final int VIEW_TYPE_PASSWORD = 2;
    private static final int VIEW_TYPE_TOTP = 3;

    private List<UnifiedAccountItem> allItems = new ArrayList<>();
    private List<Object> displayList = new ArrayList<>();
    private OnItemClickListener listener;
    
    // Section expansion state
    private boolean isTotpExpanded = true;
    private boolean isPasswordExpanded = true;
    
    // Section counts
    private int totpCount = 0;
    private int passwordCount = 0;

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
        this.allItems = newItems != null ? newItems : new ArrayList<>();
        rebuildDisplayList();
    }
    
    /**
     * Toggle section expansion state
     */
    public void toggleSection(boolean isTotp) {
        if (isTotp) {
            isTotpExpanded = !isTotpExpanded;
        } else {
            isPasswordExpanded = !isPasswordExpanded;
        }
        rebuildDisplayList();
    }
    
    /**
     * Rebuild the display list based on current expansion state
     */
    private void rebuildDisplayList() {
        displayList.clear();
        
        // Separate items by type
        List<UnifiedAccountItem> totpItems = new ArrayList<>();
        List<UnifiedAccountItem> passwordItems = new ArrayList<>();
        
        for (UnifiedAccountItem item : allItems) {
            if (item.getType() == UnifiedAccountItem.AccountType.TOTP) {
                totpItems.add(item);
            } else {
                passwordItems.add(item);
            }
        }
        
        totpCount = totpItems.size();
        passwordCount = passwordItems.size();
        
        // Add TOTP section (only if there are TOTP items)
        if (totpCount > 0) {
            displayList.add(new SectionHeader(true, isTotpExpanded, totpCount));
            if (isTotpExpanded) {
                displayList.addAll(totpItems);
            }
        }
        
        // Add Password section (only if there are password items)
        if (passwordCount > 0) {
            displayList.add(new SectionHeader(false, isPasswordExpanded, passwordCount));
            if (isPasswordExpanded) {
                displayList.addAll(passwordItems);
            }
        }
        
        notifyDataSetChanged();
    }

    public void updateCodes() {
        // Only update TOTP items - find their positions and update
        notifyDataSetChanged();
    }
    
    public boolean isTotpExpanded() {
        return isTotpExpanded;
    }
    
    public boolean isPasswordExpanded() {
        return isPasswordExpanded;
    }
    
    public void setExpansionState(boolean totpExpanded, boolean passwordExpanded) {
        this.isTotpExpanded = totpExpanded;
        this.isPasswordExpanded = passwordExpanded;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayList.get(position);
        
        if (item instanceof SectionHeader) {
            SectionHeader header = (SectionHeader) item;
            return header.isTotp ? VIEW_TYPE_HEADER_TOTP : VIEW_TYPE_HEADER_PASSWORD;
        } else {
            UnifiedAccountItem accountItem = (UnifiedAccountItem) item;
            return accountItem.getType() == UnifiedAccountItem.AccountType.PASSWORD 
                ? VIEW_TYPE_PASSWORD 
                : VIEW_TYPE_TOTP;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER_TOTP:
            case VIEW_TYPE_HEADER_PASSWORD:
                View headerView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
                return new SectionHeaderViewHolder(headerView);
                
            case VIEW_TYPE_PASSWORD:
                View passwordView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_unified_account_password, parent, false);
                return new PasswordViewHolder(passwordView);
                
            case VIEW_TYPE_TOTP:
            default:
                View totpView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_unified_account_totp, parent, false);
                return new TotpViewHolder(totpView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = displayList.get(position);
        
        if (holder instanceof SectionHeaderViewHolder) {
            ((SectionHeaderViewHolder) holder).bind((SectionHeader) item, this);
        } else if (holder instanceof PasswordViewHolder) {
            ((PasswordViewHolder) holder).bind((UnifiedAccountItem) item, listener);
        } else if (holder instanceof TotpViewHolder) {
            ((TotpViewHolder) holder).bind((UnifiedAccountItem) item, listener);
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }
    
    // Section Header Data Class
    static class SectionHeader {
        boolean isTotp;
        boolean isExpanded;
        int count;
        
        SectionHeader(boolean isTotp, boolean isExpanded, int count) {
            this.isTotp = isTotp;
            this.isExpanded = isExpanded;
            this.count = count;
        }
    }
    
    // Section Header ViewHolder
    static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSectionIcon;
        TextView tvSectionTitle;
        TextView tvSectionCount;
        ImageView ivExpandArrow;
        View cardHeader;
        
        SectionHeaderViewHolder(View itemView) {
            super(itemView);
            cardHeader = itemView.findViewById(R.id.card_section_header);
            ivSectionIcon = itemView.findViewById(R.id.iv_section_icon);
            tvSectionTitle = itemView.findViewById(R.id.tv_section_title);
            tvSectionCount = itemView.findViewById(R.id.tv_section_count);
            ivExpandArrow = itemView.findViewById(R.id.iv_expand_arrow);
        }
        
        void bind(SectionHeader header, UnifiedAccountAdapter adapter) {
            if (header.isTotp) {
                ivSectionIcon.setImageResource(R.drawable.ic_shield);
                tvSectionTitle.setText("Authenticator (2FA)");
            } else {
                ivSectionIcon.setImageResource(R.drawable.ic_key);
                tvSectionTitle.setText("Tài khoản");
            }
            
            tvSectionCount.setText(header.count + " mục");
            
            // Rotate arrow based on expansion state
            ivExpandArrow.setRotation(header.isExpanded ? 0 : -90);
            
            cardHeader.setOnClickListener(v -> {
                // Animate arrow rotation
                float startRotation = header.isExpanded ? 0 : -90;
                float endRotation = header.isExpanded ? -90 : 0;
                ObjectAnimator animator = ObjectAnimator.ofFloat(ivExpandArrow, "rotation", startRotation, endRotation);
                animator.setDuration(200);
                animator.start();
                
                // Toggle section
                adapter.toggleSection(header.isTotp);
            });
        }
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
