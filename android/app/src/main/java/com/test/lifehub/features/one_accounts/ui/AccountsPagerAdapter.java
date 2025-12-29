package com.test.lifehub.features.one_accounts.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter cho ViewPager2 trong AccountFragment
 */
public class AccountsPagerAdapter extends FragmentStateAdapter {

    public AccountsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public AccountsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new AccountsListFragment();
        } else {
            return new TotpAccountsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // 2 tabs: Tài khoản và Authenticator
    }
}
