package com.xomena.cmpfutboltfe

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle

import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaFragment
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaLocation

import android.location.Location
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat

import java.util.Timer
import java.util.TimerTask


class StreetViewActivity : AppCompatActivity(), OnStreetViewPanoramaReadyCallback {

    private var m_lat: Double = 0.0
    private var m_lng: Double = 0.0
    private var m_lat_next: Double = 0.0
    private var m_lng_next: Double = 0.0
    private var mStreetViewPanorama: StreetViewPanorama? = null
    private var timer: Timer? = null
    private var radius = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_street_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbarSV)
        setSupportActionBar(toolbar)
        try {
            val ab = supportActionBar
            ab?.setDisplayShowTitleEnabled(false)
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Exception", e)
        }

        val upArrow = ResourcesCompat.getDrawable(
            resources, R.drawable.abc_ic_ab_back_material,
            applicationContext.theme
        )
        toolbar.navigationIcon = upArrow
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val i = intent
        m_lat = i.getDoubleExtra("SV_LAT", 0.0)
        m_lng = i.getDoubleExtra("SV_LNG", 0.0)
        m_lat_next = i.getDoubleExtra("SV_LAT_NEXT", 0.0)
        m_lng_next = i.getDoubleExtra("SV_LNG_NEXT", 0.0)

        val title = findViewById<TextView>(R.id.toolbar_title_sv)
        val sv_title = i.getStringExtra("SV_TITLE")
        if (sv_title != null && sv_title != "") {
            title.text = sv_title
        }

        val streetViewPanoramaFragment =
            fragmentManager.findFragmentById(R.id.streetviewpanorama) as StreetViewPanoramaFragment
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this)
    }

    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama) {
        panorama.isUserNavigationEnabled = true
        mStreetViewPanorama = panorama

        initPanorama(0)
    }

    private fun initPanorama(step: Int) {
        if (radius >= MAX_RADIUS) {
            timer?.let {
                it.cancel()
                timer = null
            }
            Toast.makeText(this, getString(R.string.no_panorama), Toast.LENGTH_LONG).show()
        } else {
            if (m_lat != 0.0 && m_lng != 0.0) {
                if (step == 0) {
                    mStreetViewPanorama?.setPosition(LatLng(m_lat, m_lng))

                    timer = Timer()
                    val myTimerTask = PanoramaTimerTask()
                    timer?.schedule(myTimerTask, 2000, 1000)
                } else {
                    radius += step
                    mStreetViewPanorama?.setPosition(LatLng(m_lat, m_lng), radius)
                }
            }
        }
    }

    inner class PanoramaTimerTask : TimerTask() {

        override fun run() {
            runOnUiThread {
                val svLoc = mStreetViewPanorama?.location
                if (svLoc != null && svLoc.panoId != null && "" != svLoc.panoId) {
                    val cam_build = StreetViewPanoramaCamera.builder()
                    if (svLoc.position != null && m_lat_next != 0.0 && m_lng_next != 0.0) {
                        //Get the current location
                        val startingLocation = Location("starting point")
                        startingLocation.latitude = svLoc.position.latitude
                        startingLocation.longitude = svLoc.position.longitude

                        //Get the target location
                        val endingLocation = Location("ending point")
                        endingLocation.latitude = m_lat_next
                        endingLocation.longitude = m_lng_next

                        //Find the Bearing from current location to next location
                        val targetBearing = startingLocation.bearingTo(endingLocation)

                        val duration: Long = 100
                        mStreetViewPanorama?.animateTo(cam_build.bearing(targetBearing).build(), duration)
                    }

                    timer?.let {
                        it.cancel()
                        timer = null
                    }
                } else {
                    initPanorama(10)
                }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "StreetViewActivity"
        private const val MAX_RADIUS = 50
    }
}
