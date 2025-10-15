package com.xomena.cmpfutboltfe;
import com.xomena.cmpfutboltfe.model.*;
import com.xomena.cmpfutboltfe.util.*;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FieldDetailActivity extends AppCompatActivity
        implements WebServiceExec.OnWebServiceResult, OnMapReadyCallback {

    private static final String LOG_TAG = "FieldDetailActivity";
    private static final int REQUEST_PLACE = 1;

    private FootballField ff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_field_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarDetails);
        setSupportActionBar(toolbar);
        try {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Exception", e);
        }

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_left,
                getApplicationContext().getTheme());
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent i = getIntent();
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        TextView title = (TextView)findViewById(R.id.toolbar_title_details);
        title.setText(ff.getName());

        MapFragment mapFrag = (MapFragment) getFragmentManager().findFragmentById(R.id.ff_map);
        mapFrag.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ff != null) {
            googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ff.getLat(), ff.getLng())));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));

            googleMap.addMarker(new MarkerOptions().position(new LatLng(ff.getLat(), ff.getLng()))
                    .title(ff.getName()).snippet(getString(R.string.phoneLabel) + " " + ff.getPhone())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));

            googleMap.getUiSettings().setMapToolbarEnabled(false);
        }
    }

    public void onShowPitchStreetView(View view){
        if (ff.getAccessLat() != 0 && ff.getAccessLng() != 0) {
            startSV(ff.getAccessLat(), ff.getAccessLng(), ff.getPlaceId());
        } else {
            try {
                String m_url = MainActivity.ROADS_API_BASE + "?path=" + ff.getLat() + "," + ff.getLng();
                WebServiceExec m_exec = new WebServiceExec(WebServiceExec.WS_TYPE_ROADS, m_url, this);
                m_exec.executeWS();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error processing Roads API URL", e);
            }
        }
    }

    @Override
    public void onRoadsResult(JSONObject res) {
        if(res != null) {
            if(res.has("snappedPoints") && !res.isNull("snappedPoints")){
                try {
                    JSONArray points = res.getJSONArray("snappedPoints");
                    if(points.length() > 0) {
                        JSONObject point = points.getJSONObject(0);

                        double lat =  point.getJSONObject("location").getDouble("latitude");
                        double lng =  point.getJSONObject("location").getDouble("longitude");
                        String placeId = point.getString("placeId");

                        startSV(lat, lng, placeId);
                    }
                } catch(JSONException e){
                    Log.e(LOG_TAG, "Cannot process JSON results", e);
                }
            } else {
                //Roads API didn't work so try with the reverse geocoding
                String m_url = MainActivity.GEOCODE_API_BASE + MainActivity.OUT_JSON + "?latlng=" +
                        ff.getLat() + "," + ff.getLng();
                WebServiceExec m_exec = new WebServiceExec(WebServiceExec.WS_TYPE_GEOCODE, m_url, this);
                m_exec.executeWS();
            }
        }
    }

    @Override
    public void onGeocodeResult(JSONObject res) {
        if (res != null) {
            if(res.has("results") && !res.isNull("results")) {
                try {
                    JSONArray addresses = res.getJSONArray("results");
                    if (addresses.length() > 0) {
                        JSONObject address = addresses.getJSONObject(0);

                        String placeId = address.getString("place_id");

                        if (address.has("geometry") && !address.isNull("geometry")) {
                            JSONObject geom = address.getJSONObject("geometry");
                            if (geom.has("location") && !geom.isNull("location")) {
                                JSONObject loc = geom.getJSONObject("location");

                                double lat = loc.getDouble("lat");
                                double lng = loc.getDouble("lng");

                                startSV(lat, lng, placeId);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Cannot process JSON results", e);
                }
            }
        }
    }

    @Override
    public void onDirectionsResult(JSONObject res) {
        //Empty method
    }

    public void onShowPitchDirections(View view) {
        Intent intent = new Intent(this, SelectPlaceActivity.class);
        startActivityForResult(intent, REQUEST_PLACE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PLACE) {
            if(resultCode == RESULT_OK) {
                String placeId = data.getStringExtra(MainActivity.EXTRA_PLACEID);
                String address = data.getStringExtra(MainActivity.EXTRA_ADDRESS);

                Toast.makeText(getApplicationContext(), address, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), RouteActivity.class);
                intent.putExtra(MainActivity.EXTRA_ITEM, ff);
                intent.putExtra(MainActivity.EXTRA_ADDRESS, address);
                intent.putExtra(MainActivity.EXTRA_PLACEID, placeId);

                startActivity(intent);
            }
        }
    }

    private void startSV (double lat, double lng, String placeId) {
        Intent intent = new Intent(this, StreetViewActivity.class);
        intent.putExtra("SV_LAT", lat);
        intent.putExtra("SV_LNG", lng);
        intent.putExtra("SV_LAT_NEXT", ff.getLat());
        intent.putExtra("SV_LNG_NEXT", ff.getLng());
        intent.putExtra("SV_PLACEID", placeId);
        intent.putExtra("SV_TITLE", ff.getName());
        startActivity(intent);
    }
}
