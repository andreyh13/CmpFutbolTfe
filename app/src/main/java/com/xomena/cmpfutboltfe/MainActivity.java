package com.xomena.cmpfutboltfe;

import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Intent;

public class MainActivity extends AppCompatActivity implements CountiesFragment.OnFragmentInteractionListener,
        MainMapFragment.OnFragmentInteractionListener, SearchFragment.OnFragmentInteractionListener {
    private static final String LOG_TAG = "MainActivity";

    protected static final String EXTRA_COUNTY = "com.xomena.cmpfutboltfe.COUNTY";
    protected static final String EXTRA_FIELDS = "com.xomena.cmpfutboltfe.FIELDS";
    protected static final String EXTRA_ITEM = "com.xomena.cmpfutboltfe.ITEM";
    protected static final String SAVED_KEYS = "com.xomena.cmpfutboltfe.KEYS";
    protected static final String EXTRA_ADDRESS = "com.xomena.cmpfutboltfe.ADDRESS";
    protected static final String EXTRA_PLACEID = "com.xomena.cmpfutboltfe.PLACEID";

    protected static final String DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions";
    protected static final String ROADS_API_BASE = "https://roads.googleapis.com/v1/snapToRoads";
    protected static final String GEOCODE_API_BASE = "https://maps.googleapis.com/maps/api/geocode";
    protected static final String OUT_JSON = "/json";

    protected static final String XOMENA_DOMAIN = "http://aux.xomena.elementfx.com";
    protected static final String XOMENA_WS_PROXY = "/geows.php";

    protected static final String[] API_KEYS = new String[] {
            "AIzaSyAf5x1KKZAiW5XIm5Nop1sD7NrGY9hAsgE",
            "AIzaSyDHK8_Oudx__GQszqPD8ukVMnrJQaMNWYk",
            "AIzaSyBwrb2-1hlA0AkiI9aiNO3kq2t_8wk11vY",
            "AIzaSyCdNXAWPRXQRnfw23M8PJoUkm_R2jFTZWs"
    };

    protected static final String API_KEY = "AIzaSyA67JIj41Ze0lbc2KidOgQMgqLOAZOcybE";
    //protected static final int QPS = 10;

    MainPagerAdapter mPagerAdapter;
    ViewPager mViewPager;
    MenuItem mActionProgressItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false);
                ab.setLogo(R.drawable.ic_stadion);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Exception", e);
        }

        // Create the adapter that will return a fragment for each of the two primary sections
        // of the app.
        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), MainActivity.this);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.mainFragmentPager);
        mViewPager.setAdapter(mPagerAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mActionProgressItem = menu.findItem(R.id.miActionProgress);

        //MenuItem item = menu.findItem(R.id.menu_search);

        //SearchView searchView = (SearchView)MenuItemCompat.getActionView(item);

        /*if(searchView!=null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            searchView.setSearchableInfo(info);
        }*/
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

    public void onStartAsyncTask() {
        this.showProgressBar();
    }

    public void onObtainFootballFields(Map<String,List<FootballField>> ff_data){
        this.hideProgressBar();
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
            intent.putParcelableArrayListExtra(EXTRA_FIELDS, new ArrayList<>(ff_list));
        }
        startActivity(intent);
    }

    public void onSelectMarker(){

    }

    public void onSearchFootballField(){

    }

    private void showProgressBar () {
        if (mActionProgressItem != null) {
            mActionProgressItem.setVisible(true);
        }
    }

    private void hideProgressBar() {
        if (mActionProgressItem != null) {
            mActionProgressItem.setVisible(false);
        }
    }
}
