package com.xomena.cmpfutboltfe;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.content.Context;

public class MainPagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 2;
    private int tabTitles[] = new int[] { R.string.description, R.string.route_map };
    private Context context;

    public MainPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager);
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return CountiesFragment.newInstance();
            case 1:
                return MainMapFragment.newInstance();
            default:
                return null;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return context.getString(tabTitles[position]);
    }
}
