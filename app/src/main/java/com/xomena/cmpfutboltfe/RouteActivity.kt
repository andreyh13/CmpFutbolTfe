package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*
import com.xomena.cmpfutboltfe.ui.fragment.*

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.tabs.TabLayout
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil

import org.json.JSONException
import org.json.JSONObject

class RouteActivity : AppCompatActivity(),
    RouteMapFragment.OnFragmentInteractionListener, DirectionsMapFragment.OnFragmentInteractionListener,
    OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var mPagerAdapter: SimplePagerAdapter
    private lateinit var mViewPager: ViewPager

    private var jsonRoute: JSONObject? = null
    private var ff: FootballField? = null
    private var enc_polyline: String? = null
    private var mPermissionDenied = false
    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        val toolbar = findViewById<Toolbar>(R.id.toolbarRoute)
        setSupportActionBar(toolbar)
        try {
            val ab = supportActionBar
            ab?.setDisplayShowTitleEnabled(false)
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Exception", e)
        }

        val upArrow = ResourcesCompat.getDrawable(
            resources, R.drawable.arrow_left,
            applicationContext.theme
        )
        toolbar.navigationIcon = upArrow
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Create the adapter that will return a fragment for each of the two primary sections
        // of the app.
        mPagerAdapter = SimplePagerAdapter(supportFragmentManager, this)

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = findViewById(R.id.fragmentPager)
        mViewPager.adapter = mPagerAdapter

        // Give the TabLayout the ViewPager
        val tabLayout = findViewById<TabLayout>(R.id.sliding_tabs_route)
        tabLayout.setupWithViewPager(mViewPager)

        val i = intent
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM)

        val title = findViewById<TextView>(R.id.toolbar_title_route)
        title.text = String.format(getString(R.string.route_to), ff?.name)
    }

    class SimplePagerAdapter(fragmentManager: FragmentManager, private val context: Context) :
        FragmentPagerAdapter(fragmentManager) {
        
        private val PAGE_COUNT = 2
        private val tabTitles = intArrayOf(R.string.description, R.string.route_map)

        override fun getCount(): Int {
            return PAGE_COUNT
        }

        override fun getItem(position: Int): Fragment? {
            return when (position) {
                0 -> RouteMapFragment.newInstance()
                1 -> DirectionsMapFragment.newInstance()
                else -> null
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context.getString(tabTitles[position])
        }
    }

    override fun exchangeJSON(json: JSONObject?) {
        jsonRoute = json
        val mapFrag = fragmentManager.findFragmentById(R.id.route_map) as? MapFragment
        if (mapFrag != null && jsonRoute != null) {
            val self = this
            mapFrag.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(googleMap: GoogleMap) {
                    var lastLatLng: LatLng? = null
                    try {
                        val route = jsonRoute
                        if (route != null && route.has("status") && route.getString("status") == "OK") {
                            if (route.has("routes")) {
                                val rts = route.getJSONArray("routes")
                                if (rts.length() > 0 && !rts.isNull(0)) {
                                    val r = rts.getJSONObject(0)
                                    if (r.has("overview_polyline") && !r.isNull("overview_polyline")) {
                                        val m_poly = r.getJSONObject("overview_polyline")
                                        if (m_poly.has("points") && !m_poly.isNull("points")) {
                                            val enc_points = m_poly.getString("points")
                                            self.enc_polyline = enc_points
                                            val m_path = PolyUtil.decode(enc_points)
                                            val polyOptions = PolylineOptions().addAll(m_path)
                                            googleMap.addPolyline(polyOptions)

                                            val builder = LatLngBounds.Builder()
                                            for (coord in m_path) {
                                                builder.include(coord)
                                            }
                                            val m_bounds = builder.build()
                                            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10))
                                        }
                                    }

                                    //Legs
                                    if (r.has("legs") && !r.isNull("legs")) {
                                        val al = r.getJSONArray("legs")
                                        for (i in 0 until al.length()) {
                                            if (!al.isNull(i)) {
                                                val l = al.getJSONObject(i)

                                                //Start address
                                                if (i == 0 && l.has("start_address") && !l.isNull("start_address")) {
                                                    if (l.has("start_location") && !l.isNull("start_location")) {
                                                        googleMap.addMarker(
                                                            MarkerOptions()
                                                                .position(
                                                                    LatLng(
                                                                        l.getJSONObject("start_location").getDouble("lat"),
                                                                        l.getJSONObject("start_location").getDouble("lng")
                                                                    )
                                                                )
                                                                .title(l.getString("start_address"))
                                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                                        )
                                                    }
                                                }

                                                //End address
                                                if (i == al.length() - 1 && l.has("end_address") && !l.isNull("end_address")) {
                                                    if (l.has("end_location") && !l.isNull("end_location")) {
                                                        lastLatLng = LatLng(
                                                            l.getJSONObject("end_location").getDouble("lat"),
                                                            l.getJSONObject("end_location").getDouble("lng")
                                                        )
                                                        googleMap.addMarker(
                                                            MarkerOptions()
                                                                .position(lastLatLng!!)
                                                                .title(l.getString("end_address"))
                                                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e(LOG_TAG, "Cannot process directions JSON results", e)
                    }
                    ff?.let { field ->
                        val fldLatLng = LatLng(field.lat, field.lng)
                        googleMap.addMarker(
                            MarkerOptions().position(fldLatLng)
                                .title(field.name).snippet(getString(R.string.phoneLabel) + " " + field.phone)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield))
                        )

                        lastLatLng?.let {
                            self.showCurvedPolyline(fldLatLng, it, 0.5)
                        }
                    }
                    googleMap.setOnMyLocationButtonClickListener(self)
                    self.mMap = googleMap
                    self.enableMyLocation()
                }
            })
        }
    }

    override fun onDirectionsMapClick() {

    }

    override fun onRouteAnimationClick() {
        val intent = Intent(this, AnimateRouteActivity::class.java)
        enc_polyline?.let {
            intent.putExtra("ENC_POLY", it)
        }
        ff?.let {
            intent.putExtra(MainActivity.EXTRA_ITEM, it)
        }
        startActivity(intent)
    }

    fun onShowStreetView(view: View) {
        this.onRouteAnimationClick()
    }

    override fun onMyLocationButtonClick(): Boolean {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        if (PermissionUtils.isPermissionGranted(
                permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            mPermissionDenied = false
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private fun showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
            .newInstance(true).show(supportFragmentManager, "dialog")
    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        } else {
            mMap?.let {
                // Access to the location has been granted to the app.
                it.isMyLocationEnabled = true
            }
        }
    }

    private fun showCurvedPolyline(p1: LatLng, p2: LatLng, k: Double) {
        //Calculate distance and heading between two points
        val d = SphericalUtil.computeDistanceBetween(p1, p2)
        val h = SphericalUtil.computeHeading(p1, p2)

        //Midpoint position
        val p = SphericalUtil.computeOffset(p1, d * 0.5, h)

        //Apply some mathematics to calculate position of the circle center
        val x = (1 - k * k) * d * 0.5 / (2 * k)
        val r = (1 + k * k) * d * 0.5 / (2 * k)

        val c = SphericalUtil.computeOffset(p, x, h + 90.0)

        //Polyline options
        val options = PolylineOptions()
        val pattern = listOf<PatternItem>(Dash(30f), Gap(20f))

        //Calculate heading between circle center and two points
        val h1 = SphericalUtil.computeHeading(c, p1)
        val h2 = SphericalUtil.computeHeading(c, p2)

        //Calculate positions of points on circle border and add them to polyline options
        val numpoints = 100
        val step = (h2 - h1) / numpoints

        for (i in 0 until numpoints) {
            val pi = SphericalUtil.computeOffset(c, r, h1 + i * step)
            options.add(pi)
        }

        //Draw polyline
        mMap?.addPolyline(options.width(10f).color(Color.BLUE).geodesic(false).pattern(pattern))
    }

    companion object {
        private const val LOG_TAG = "RouteActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
