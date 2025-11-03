package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import org.json.JSONException
import org.json.JSONObject

class FieldDetailActivity : AppCompatActivity(),
    WebServiceExec.OnWebServiceResult, OnMapReadyCallback {

    private var ff: FootballField? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_field_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbarDetails)
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
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM)

        val title = findViewById<TextView>(R.id.toolbar_title_details)
        title.text = ff?.name

        val mapFrag = fragmentManager.findFragmentById(R.id.ff_map) as MapFragment
        mapFrag.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        ff?.let { field ->
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(field.lat, field.lng)))
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(17f))

            googleMap.addMarker(
                MarkerOptions().position(LatLng(field.lat, field.lng))
                    .title(field.name).snippet(getString(R.string.phoneLabel) + " " + field.phone)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield))
            )

            googleMap.uiSettings.isMapToolbarEnabled = false
        }
    }

    fun onShowPitchStreetView(view: View) {
        ff?.let { field ->
            if (field.accessLat != 0.0 && field.accessLng != 0.0) {
                startSV(field.accessLat, field.accessLng, field.placeId)
            } else {
                try {
                    val m_url = MainActivity.ROADS_API_BASE + "?path=" + field.lat + "," + field.lng
                    val m_exec = WebServiceExec(WebServiceExec.WS_TYPE_ROADS, m_url, this)
                    m_exec.executeWS()
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error processing Roads API URL", e)
                }
            }
        }
    }

    override fun onRoadsResult(res: JSONObject?) {
        if (res != null) {
            if (res.has("snappedPoints") && !res.isNull("snappedPoints")) {
                try {
                    val points = res.getJSONArray("snappedPoints")
                    if (points.length() > 0) {
                        val point = points.getJSONObject(0)

                        val lat = point.getJSONObject("location").getDouble("latitude")
                        val lng = point.getJSONObject("location").getDouble("longitude")
                        val placeId = point.getString("placeId")

                        startSV(lat, lng, placeId)
                    }
                } catch (e: JSONException) {
                    Log.e(LOG_TAG, "Cannot process JSON results", e)
                }
            } else {
                //Roads API didn't work so try with the reverse geocoding
                ff?.let { field ->
                    val m_url = MainActivity.GEOCODE_API_BASE + MainActivity.OUT_JSON + "?latlng=" +
                            field.lat + "," + field.lng
                    val m_exec = WebServiceExec(WebServiceExec.WS_TYPE_GEOCODE, m_url, this)
                    m_exec.executeWS()
                }
            }
        }
    }

    override fun onGeocodeResult(res: JSONObject?) {
        if (res != null) {
            if (res.has("results") && !res.isNull("results")) {
                try {
                    val addresses = res.getJSONArray("results")
                    if (addresses.length() > 0) {
                        val address = addresses.getJSONObject(0)

                        val placeId = address.getString("place_id")

                        if (address.has("geometry") && !address.isNull("geometry")) {
                            val geom = address.getJSONObject("geometry")
                            if (geom.has("location") && !geom.isNull("location")) {
                                val loc = geom.getJSONObject("location")

                                val lat = loc.getDouble("lat")
                                val lng = loc.getDouble("lng")

                                startSV(lat, lng, placeId)
                            }
                        }
                    }
                } catch (e: JSONException) {
                    Log.e(LOG_TAG, "Cannot process JSON results", e)
                }
            }
        }
    }

    override fun onDirectionsResult(res: JSONObject?) {
        //Empty method
    }

    fun onShowPitchDirections(view: View) {
        val intent = Intent(this, SelectPlaceActivity::class.java)
        startActivityForResult(intent, REQUEST_PLACE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PLACE) {
            if (resultCode == RESULT_OK && data != null) {
                val placeId = data.getStringExtra(MainActivity.EXTRA_PLACEID)
                val address = data.getStringExtra(MainActivity.EXTRA_ADDRESS)

                Toast.makeText(applicationContext, address, Toast.LENGTH_SHORT).show()
                val intent = Intent(applicationContext, RouteActivity::class.java)
                intent.putExtra(MainActivity.EXTRA_ITEM, ff)
                intent.putExtra(MainActivity.EXTRA_ADDRESS, address)
                intent.putExtra(MainActivity.EXTRA_PLACEID, placeId)

                startActivity(intent)
            }
        }
    }

    private fun startSV(lat: Double, lng: Double, placeId: String?) {
        val intent = Intent(this, StreetViewActivity::class.java)
        intent.putExtra("SV_LAT", lat)
        intent.putExtra("SV_LNG", lng)
        ff?.let { field ->
            intent.putExtra("SV_LAT_NEXT", field.lat)
            intent.putExtra("SV_LNG_NEXT", field.lng)
            intent.putExtra("SV_TITLE", field.name)
        }
        intent.putExtra("SV_PLACEID", placeId)
        startActivity(intent)
    }

    companion object {
        private const val LOG_TAG = "FieldDetailActivity"
        private const val REQUEST_PLACE = 1
    }
}
