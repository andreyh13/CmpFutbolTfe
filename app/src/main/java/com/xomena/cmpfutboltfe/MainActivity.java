package com.xomena.cmpfutboltfe;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.widget.SearchView;

public class MainActivity extends FragmentActivity implements CountiesFragment.OnFragmentInteractionListener,
        MainMapFragment.OnFragmentInteractionListener, SearchFragment.OnFragmentInteractionListener,
        ActionBar.TabListener {
    private static final String LOG_TAG = "MainActivity";
    protected static final String EXTRA_COUNTY = "com.xomena.cmpfutboltfe.COUNTY";
    protected static final String EXTRA_FIELDS = "com.xomena.cmpfutboltfe.FIELDS";
    protected static final String EXTRA_ITEM = "com.xomena.cmpfutboltfe.ITEM";
    protected static final String SAVED_KEYS = "com.xomena.cmpfutboltfe.KEYS";
    protected static final String EXTRA_ADDRESS = "com.xomena.cmpfutboltfe.ADDRESS";

    protected static final String DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions";
    protected static final String OUT_JSON = "/json";
    protected static final String API_KEY = "AIzaSyA67JIj41Ze0lbc2KidOgQMgqLOAZOcybE";
    protected static final int QPS = 10;

    MainPagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three primary sections
        // of the app.
        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        // Specify that we will be displaying tabs in the action bar.
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.mainFragmentPager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                try {
                    assert actionBar != null;
                    actionBar.setSelectedNavigationItem(position);
                } catch(Exception e){
                    Log.e(LOG_TAG, "Cannot set selected item", e);
                }
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int k = 0; k < mPagerAdapter.getCount(); k++) {
            // Create a tab with text corresponding to the page title defined by the adapter.
            // Also specify this Activity object, which implements the TabListener interface, as the
            // listener for when this tab is selected.
            try {
                assert actionBar != null;
                actionBar.addTab(actionBar.newTab().setText(mPagerAdapter.getPageTitle(k))
                        .setTabListener(this));
            } catch(Exception e){
                Log.e(LOG_TAG, "Cannot create new tab", e);
            }
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.menu_search);

        SearchView searchView = (SearchView)MenuItemCompat.getActionView(item);

        if(searchView!=null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            searchView.setSearchableInfo(info);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onObtainFootballFields(Map<String,List<FootballField>> ff_data){
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for(Fragment frag: fragments){
            if(frag instanceof MainMapFragment){
                MainMapFragment mapFrag = (MainMapFragment)frag;
                mapFrag.initializeMainMap(ff_data);
                break;
            }
        }
    }

    public void onSelectCounty(String county, Map<String,List<FootballField>> ff_data){
        Intent intent = new Intent(this, FieldsListActivity.class);
        intent.putExtra(EXTRA_COUNTY, county);
        if(ff_data !=null && ff_data.containsKey(county)){
            List<FootballField> ff_list = ff_data.get(county);
            intent.putParcelableArrayListExtra(EXTRA_FIELDS, new ArrayList<FootballField>(ff_list));
        }
        startActivity(intent);
    }

    public void onSelectMarker(){

    }

    public void onSearchFootballField(){

    }

    public static class MainPagerAdapter extends FragmentPagerAdapter {

        public MainPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 2;
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
            switch (position) {
                case 0:
                    return "Counties"; //Resources.getSystem().getString(R.string.description);
                case 1:
                    return "Map"; //Resources.getSystem().getString(R.string.route_map);
                default:
                    return null;
            }
        }
    }
}
