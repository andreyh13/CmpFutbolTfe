package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import com.google.android.gms.maps.model.StreetViewPanoramaLink
import com.google.android.gms.maps.model.StreetViewPanoramaLocation
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.maps.android.PolyUtil

import java.util.Timer
import java.util.TimerTask
import kotlin.math.floor

class AnimateRouteActivity : AppCompatActivity(),
    OnStreetViewPanoramaReadyCallback, GoogleMap.OnMapClickListener,
    OnMapReadyCallback, StreetViewPanorama.OnStreetViewPanoramaChangeListener,
    OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private var polyline: String? = null
    private var ff: FootballField? = null
    private var path: List<LatLng>? = null
    private var interpolated: List<LatLng>? = null
    private var mStreetViewPanorama: StreetViewPanorama? = null
    private var position = 0
    private var timer: Timer? = null
    private var btnAnimate: FloatingActionButton? = null
    private var posMarker: Marker? = null

    private var hasRuntimeData = false
    private var wasAnimated = false
    private var mPermissionDenied = false
    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animate_route)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAnimate)
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

        btnAnimate = findViewById(R.id.move_position)

        val i = intent
        polyline = i.getStringExtra("ENC_POLY")
        polyline?.let {
            path = PolyUtil.decode(it)
            interpolated = interpolatePath()
        }
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM)

        val title = findViewById<TextView>(R.id.toolbar_title_animate)
        title.text = String.format(getString(R.string.route_to), ff?.name)

        if (savedInstanceState != null && savedInstanceState.containsKey("hasRuntimeData")) {
            hasRuntimeData = savedInstanceState.getBoolean("hasRuntimeData")
            position = savedInstanceState.getInt("currentPos")
            wasAnimated = savedInstanceState.getBoolean("wasAnimated")
        }

        val mapFrag = fragmentManager.findFragmentById(R.id.animate_map) as MapFragment
        mapFrag.getMapAsync(this)

        val streetViewPanoramaFragment =
            fragmentManager.findFragmentById(R.id.routepanorama) as StreetViewPanoramaFragment
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("hasRuntimeData", true)
        outState.putInt("currentPos", position)
        outState.putBoolean("wasAnimated", timer != null)
        super.onSaveInstanceState(outState)
    }

    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama) {
        mStreetViewPanorama = panorama
        panorama.setOnStreetViewPanoramaChangeListener(this)
        if (polyline != null && interpolated != null && interpolated!!.isNotEmpty()) {
            panorama.setPosition(interpolated!![position], 20)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        googleMap.uiSettings.isMapToolbarEnabled = false
        val currentPath = path
        if (polyline != null && currentPath != null) {
            val polyOptions = PolylineOptions().addAll(currentPath)
            googleMap.addPolyline(polyOptions)

            val startPos: LatLng = if (hasRuntimeData) {
                interpolated!![position]
            } else {
                currentPath[0]
            }

            posMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(startPos).anchor(0.5f, 0.5f).draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_point))
            )

            googleMap.setOnMapClickListener(this)

            val builder = LatLngBounds.Builder()
            for (coord in currentPath) {
                builder.include(coord)
            }
            val m_bounds = builder.build()

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10))

            if (wasAnimated) {
                onMovePosition(findViewById(R.id.move_position))
            }
        }
        googleMap.setOnMyLocationButtonClickListener(this)
        this.mMap = googleMap
        this.enableMyLocation()
    }

    fun onMovePosition(view: View) {
        if (timer != null) {
            timer?.cancel()
            timer = null
            btnAnimate?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
        } else {
            timer = Timer()
            val myTimerTask = AnimateRouteTimerTask()
            timer?.schedule(myTimerTask, 1000, 2000)
            btnAnimate?.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp)
        }
        moveCameraToNextPosition(interpolated)
    }

    //Gets bearing between current point and next point in path
    private fun getTargetBearing(path: List<LatLng>?): Float {
        var targetBearing = 0f
        if (path != null) {
            val startingLocation = Location("starting point")
            val endingLocation = Location("ending point")
            if (position < path.size - 1) {
                startingLocation.latitude = path[position].latitude
                startingLocation.longitude = path[position].longitude

                //Get the target location
                endingLocation.latitude = path[position + 1].latitude
                endingLocation.longitude = path[position + 1].longitude
            } else {
                startingLocation.latitude = path[path.size - 1].latitude
                startingLocation.longitude = path[path.size - 1].longitude

                //Get the target location
                endingLocation.latitude = ff?.lat ?: 0.0
                endingLocation.longitude = ff?.lng ?: 0.0
            }
            //Find the Bearing from current location to next location
            targetBearing = startingLocation.bearingTo(endingLocation)
        }
        return targetBearing
    }

    private fun moveCameraToNextPosition(path: List<LatLng>?) {
        if (path != null && mStreetViewPanorama != null) {
            //Find the Bearing from current location to next location
            val targetBearing = getTargetBearing(path)

            val duration: Long = 100
            mStreetViewPanorama?.animateTo(
                StreetViewPanoramaCamera.Builder()
                    .bearing(targetBearing).build(), duration
            )

            if (position == path.size - 1) {
                Toast.makeText(this, getText(R.string.last_path_point), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun moveToNextPosition(path: List<LatLng>?) {
        if (path != null) {
            val location = mStreetViewPanorama?.location
            if (location != null && location.links != null) {
                if (!isLocationBetweenPathPoints(location, interpolated)) {
                    position++
                    if (!isLocationBetweenPathPoints(location, interpolated)) {
                        if (position < path.size - 1) {
                            mStreetViewPanorama?.setPosition(path[++position], 5)
                        }
                    } else {
                        if (position < path.size - 1) {
                            val link = findClosestLinkToBearing(location.links, getTargetBearing(interpolated))
                            mStreetViewPanorama?.setPosition(link.panoId)
                        }
                    }
                } else {
                    val link = findClosestLinkToBearing(location.links, getTargetBearing(interpolated))
                    mStreetViewPanorama?.setPosition(link.panoId)
                }
            } else {
                if (position < path.size - 1) {
                    mStreetViewPanorama?.setPosition(path[++position], 5)
                }
            }
        }
    }

    private fun movePositionalMarker() {
        if (posMarker != null && mStreetViewPanorama != null) {
            val loc = mStreetViewPanorama?.location
            if (loc != null) {
                val pos = loc.position
                if (pos != null) {
                    posMarker?.position = pos
                }
            }
        }
    }

    inner class AnimateRouteTimerTask : TimerTask() {
        override fun run() {
            runOnUiThread {
                moveToNextPosition(interpolated)
                moveCameraToNextPosition(interpolated)
                movePositionalMarker()

                if (position == path!!.size - 1) {
                    timer?.let {
                        it.cancel()
                        timer = null
                        btnAnimate?.isClickable = false
                        btnAnimate?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
                    }
                }
            }
        }
    }

    private fun interpolatePath(): List<LatLng> {
        var counter = 0
        val step = 10f
        val currentPath = path ?: return emptyList()
        val last = currentPath[currentPath.size - 1]
        val res = currentPath.toMutableList()

        while (!(res[counter].latitude == last.latitude && res[counter].longitude == last.longitude)) {
            val s = Location("")
            s.latitude = res[counter].latitude
            s.longitude = res[counter].longitude
            val f = Location("")
            f.latitude = res[counter + 1].latitude
            f.longitude = res[counter + 1].longitude
            val d = s.distanceTo(f)
            if (d < step) {
                counter++
            } else {
                val m_lat = (res[counter].latitude + res[counter + 1].latitude) * 0.5
                val m_lng = (res[counter].longitude + res[counter + 1].longitude) * 0.5
                res.add(counter + 1, LatLng(m_lat, m_lng))
            }
        }

        return res
    }

    private fun isLocationBetweenPathPoints(location: StreetViewPanoramaLocation, path: List<LatLng>?): Boolean {
        if (path == null || position >= path.size) return false
        
        val pos1 = path[position]
        val pos2: LatLng = if (position < path.size - 1) {
            path[position + 1]
        } else {
            LatLng(ff?.lat ?: 0.0, ff?.lng ?: 0.0)
        }

        val llat = Math.round(location.position.latitude * 100000) / 100000.0
        val llng = Math.round(location.position.longitude * 100000) / 100000.0
        val p1lat = Math.round(pos1.latitude * 100000) / 100000.0
        val p1lng = Math.round(pos1.longitude * 100000) / 100000.0
        val p2lat = Math.round(pos2.latitude * 100000) / 100000.0
        val p2lng = Math.round(pos2.longitude * 100000) / 100000.0

        return llat >= Math.min(p1lat, p2lat) && llat <= Math.max(p1lat, p2lat) &&
                llng >= Math.min(p1lng, p2lng) && llng <= Math.max(p1lng, p2lng)
    }

    override fun onMapClick(point: LatLng) {
        var nearest: LatLng? = null
        var nearestInd = -1
        var mdist = 100000f
        var index = -1
        for (rpoint in interpolated ?: emptyList()) {
            index++
            val d = distanceBetween(point, rpoint)
            if (d < mdist) {
                mdist = d
                nearest = rpoint
                nearestInd = index
            }
        }
        if (mdist <= 50f) {
            timer?.let {
                it.cancel()
                timer = null
                btnAnimate?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
            }
            posMarker?.position = nearest
            position = nearestInd
            mStreetViewPanorama?.setPosition(nearest, 20)
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

    override fun onStreetViewPanoramaChange(location: StreetViewPanoramaLocation) {
        moveCameraToNextPosition(interpolated)
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

    companion object {
        private const val LOG_TAG = "AnimateRouteActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        fun findClosestLinkToBearing(
            links: Array<StreetViewPanoramaLink>,
            bearing: Float
        ): StreetViewPanoramaLink {
            var minBearingDiff = 360f
            var closestLink = links[0]
            for (link in links) {
                if (minBearingDiff > findNormalizedDifference(bearing, link.bearing)) {
                    minBearingDiff = findNormalizedDifference(bearing, link.bearing)
                    closestLink = link
                }
            }
            return closestLink
        }

        // Find the difference between angle a and b as a value between 0 and 180
        fun findNormalizedDifference(a: Float, b: Float): Float {
            val diff = a - b
            val normalizedDiff = diff - (360.0f * floor(diff / 360.0f))
            return if (normalizedDiff < 180.0f) normalizedDiff else 360.0f - normalizedDiff
        }
    }
}
