package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*

import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaLocation
import com.google.maps.android.PolyUtil

class StreetViewRouteStepActivity : AppCompatActivity(),
    OnStreetViewPanoramaReadyCallback, OnMapReadyCallback,
    StreetViewPanorama.OnStreetViewPanoramaChangeListener {

    private var polyline: String? = null
    private var path: List<LatLng>? = null
    private var mStreetViewPanorama: StreetViewPanorama? = null
    private var posLatLng: LatLng? = null
    private var ff: FootballField? = null
    private var map: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_street_view_route_step)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSVStep)
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

        val i = intent
        val m_lat = i.getDoubleExtra(MainActivity.SV_LAT, 0.0)
        val m_lng = i.getDoubleExtra(MainActivity.SV_LNG, 0.0)
        posLatLng = LatLng(m_lat, m_lng)

        polyline = i.getStringExtra(MainActivity.EXTRA_ENC_POLY)
        polyline?.let {
            path = PolyUtil.decode(it)
        }
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM)
        val descr = i.getStringExtra("ROUTE_STEP_DESCR")
        val textDescr = findViewById<TextView>(R.id.sv_step_descr)
        textDescr.text = descr

        val mapFrag = fragmentManager.findFragmentById(R.id.sv_step_map) as MapFragment
        mapFrag.getMapAsync(this)

        val streetViewPanoramaFragment =
            fragmentManager.findFragmentById(R.id.sv_step_panorama) as StreetViewPanoramaFragment
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this)
    }

    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama) {
        mStreetViewPanorama = panorama
        panorama.setOnStreetViewPanoramaChangeListener(this)
        posLatLng?.let {
            panorama.setPosition(it, 5)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.uiSettings.isMapToolbarEnabled = false
        val currentPath = path
        if (polyline != null && currentPath != null && currentPath.isNotEmpty()) {
            val polyOptions = PolylineOptions().addAll(currentPath)
            googleMap.addPolyline(polyOptions)

            posLatLng?.let {
                googleMap.addMarker(
                    MarkerOptions()
                        .position(it).anchor(0.5f, 0.5f).draggable(false)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_point))
                )
            }

            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                val builder = LatLngBounds.Builder()
                for (coord in currentPath) {
                    builder.include(coord)
                }
                val m_bounds = builder.build()

                map?.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10))
            }, 1000)
        }
    }

    override fun onStreetViewPanoramaChange(location: StreetViewPanoramaLocation) {
        var nearest: LatLng? = null
        var nearestInd = -1
        var mdist = 100000f
        var index = -1
        
        val currentPos = posLatLng ?: return
        
        for (rpoint in path ?: emptyList()) {
            index++
            val d = distanceBetween(currentPos, rpoint)
            if (d < mdist) {
                mdist = d
                nearest = rpoint
                nearestInd = index
            }
        }
        if (nearest != null && nearestInd != -1) {
            val nextLatLng: LatLng = if (nearestInd + 1 < (path?.size ?: 0) - 1) {
                path!![nearestInd + 1]
            } else {
                LatLng(ff?.lat ?: 0.0, ff?.lng ?: 0.0)
            }
            //Find the Bearing from current location to next location
            val targetBearing = getTargetBearing(nextLatLng)

            val duration: Long = 100
            mStreetViewPanorama?.animateTo(
                StreetViewPanoramaCamera.Builder()
                    .bearing(targetBearing).build(), duration
            )
        }
    }

    private fun distanceBetween(latLng1: LatLng, latLng2: LatLng): Float {
        val loc1 = Location(LocationManager.GPS_PROVIDER)
        val loc2 = Location(LocationManager.GPS_PROVIDER)

        loc1.latitude = latLng1.latitude
        loc1.longitude = latLng1.longitude

        loc2.latitude = latLng2.latitude
        loc2.longitude = latLng2.longitude

        return loc1.distanceTo(loc2)
    }

    //Gets bearing between current point and next point in path
    private fun getTargetBearing(next: LatLng?): Float {
        var targetBearing = 0f
        val currentPos = posLatLng
        if (next != null && currentPos != null) {
            val startLocation = Location("starting point")
            val endLocation = Location("ending point")
            startLocation.latitude = currentPos.latitude
            startLocation.longitude = currentPos.longitude
            endLocation.latitude = next.latitude
            endLocation.longitude = next.longitude
            //Find the Bearing from current location to next location
            targetBearing = startLocation.bearingTo(endLocation)
        }
        return targetBearing
    }

    companion object {
        private const val LOG_TAG = "StreetViewRouteStep"
    }
}
