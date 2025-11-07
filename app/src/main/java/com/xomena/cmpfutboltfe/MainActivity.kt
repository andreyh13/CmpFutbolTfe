package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.ui.adapter.*
import com.xomena.cmpfutboltfe.ui.fragment.*
import com.xomena.cmpfutboltfe.ui.*

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.content.Intent

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager.widget.ViewPager

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity(), CountiesFragment.OnFragmentInteractionListener,
    MainMapFragment.OnFragmentInteractionListener, SearchFragment.OnFragmentInteractionListener {

    private lateinit var mPagerAdapter: MainPagerAdapter
    private lateinit var mViewPager: ViewPager
    private var mActionProgressItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        try {
            val ab = supportActionBar
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false)
                ab.setLogo(R.drawable.ic_stadion)
            }
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Exception", e)
        }

        // Create the adapter that will return a fragment for each of the two primary sections
        // of the app.
        mPagerAdapter = MainPagerAdapter(supportFragmentManager, this@MainActivity)

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = findViewById(R.id.mainFragmentPager)
        mViewPager.adapter = mPagerAdapter

        // Give the TabLayout the ViewPager
        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs)
        tabLayout.setupWithViewPager(mViewPager)
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        mActionProgressItem = menu.findItem(R.id.miActionProgress)

        //MenuItem item = menu.findItem(R.id.menu_search);

        //SearchView searchView = (SearchView)MenuItemCompat.getActionView(item);

        /*if(searchView!=null) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchableInfo info = searchManager.getSearchableInfo(getComponentName());
            searchView.setSearchableInfo(info);
        }*/
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_places -> {
                val intent = Intent(applicationContext, ManagePlacesActivity::class.java)
                startActivity(intent)
            }
            R.id.action_about -> {
                val fm = supportFragmentManager
                val d = AboutDialogFragment.newInstance(getString(R.string.about))
                d.show(fm, "layout_about_dialog")
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStartAsyncTask() {
        this.showProgressBar()
    }

    override fun onObtainFootballFields(ffData: Map<String, List<FootballField>>) {
        this.hideProgressBar()
        val fragments = supportFragmentManager.fragments
        for (frag in fragments) {
            if (frag is MainMapFragment) {
                frag.setFootbalFields(ffData)
                break
            }
        }
    }

    override fun onSelectCounty(county: String, ffData: Map<String, List<FootballField>>) {
        val intent = Intent(this, FieldsListActivity::class.java)
        intent.putExtra(EXTRA_COUNTY, county)
        if (ffData.containsKey(county)) {
            val ff_list = ffData[county]
            intent.putParcelableArrayListExtra(EXTRA_FIELDS, ArrayList(ff_list))
        }
        startActivity(intent)
    }

    override fun onSelectMarker() {
        //Do nothing
    }

    override fun onSearchFootballField() {

    }

    private fun showProgressBar() {
        mActionProgressItem?.isVisible = true
    }

    private fun hideProgressBar() {
        mActionProgressItem?.isVisible = false
    }

    companion object {
        private const val LOG_TAG = "MainActivity"

        const val EXTRA_COUNTY = "com.xomena.cmpfutboltfe.COUNTY"
        const val EXTRA_FIELDS = "com.xomena.cmpfutboltfe.FIELDS"
        const val EXTRA_ITEM = "com.xomena.cmpfutboltfe.ITEM"
        const val SAVED_KEYS = "com.xomena.cmpfutboltfe.KEYS"
        const val EXTRA_ADDRESS = "com.xomena.cmpfutboltfe.ADDRESS"
        const val EXTRA_PLACEID = "com.xomena.cmpfutboltfe.PLACEID"
        const val EXTRA_ENC_POLY = "com.xomena.cmpfutboltfe.ENC_POLY"
        const val SV_LAT = "com.xomena.cmpfutboltfe.SV_LAT"
        const val SV_LNG = "com.xomena.cmpfutboltfe.SV_LNG"

        const val DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions"
        const val ROADS_API_BASE = "https://roads.googleapis.com/v1/snapToRoads"
        const val GEOCODE_API_BASE = "https://maps.googleapis.com/maps/api/geocode"
        const val OUT_JSON = "/json"

        const val XOMENA_DOMAIN = "http://aux.xomena.elementfx.com"
        const val XOMENA_WS_PROXY = "/geows.php"

        val API_KEYS = arrayOf(
            "AIzaSyAf5x1KKZAiW5XIm5Nop1sD7NrGY9hAsgE",
            "AIzaSyDHK8_Oudx__GQszqPD8ukVMnrJQaMNWYk",
            "AIzaSyBwrb2-1hlA0AkiI9aiNO3kq2t_8wk11vY",
            "AIzaSyCdNXAWPRXQRnfw23M8PJoUkm_R2jFTZWs"
        )

        val BOUNDS_TENERIFE = LatLngBounds(
            LatLng(27.9980726, -16.9259232), LatLng(28.5893007, -16.1194386)
        )

        const val PLACES_SHARED_PREF = "com.xomena.cmpfutboltfe.PREFERENCE_PLACES_KEY"

        //protected static final int QPS = 10;
    }
}
