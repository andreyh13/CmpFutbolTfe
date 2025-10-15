package com.xomena.cmpfutboltfe;
import com.xomena.cmpfutboltfe.model.*;
import com.xomena.cmpfutboltfe.util.*;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.maps.android.PolyUtil;

import java.util.List;

public class StreetViewRouteStepActivity extends AppCompatActivity
        implements OnStreetViewPanoramaReadyCallback, OnMapReadyCallback,
        StreetViewPanorama.OnStreetViewPanoramaChangeListener {

    private static final String LOG_TAG = "StreetViewRouteStep";

    private String polyline;
    List<LatLng> path;
    private StreetViewPanorama mStreetViewPanorama;
    private LatLng posLatLng;
    private FootballField ff;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view_route_step);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarSVStep);
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
        double m_lat = i.getDoubleExtra(MainActivity.SV_LAT, 0);
        double m_lng = i.getDoubleExtra(MainActivity.SV_LNG, 0);
        posLatLng = new LatLng(m_lat, m_lng);

        polyline = i.getStringExtra(MainActivity.EXTRA_ENC_POLY);
        if(polyline!=null){
            path = PolyUtil.decode(polyline);
        }
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);
        String descr = i.getStringExtra("ROUTE_STEP_DESCR");
        TextView textDescr = (TextView) findViewById(R.id.sv_step_descr);
        textDescr.setText(descr);

        MapFragment mapFrag = (MapFragment)
                getFragmentManager().findFragmentById(R.id.sv_step_map);
        mapFrag.getMapAsync(this);

        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.sv_step_panorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        mStreetViewPanorama = panorama;
        panorama.setOnStreetViewPanoramaChangeListener(this);
        panorama.setPosition(posLatLng, 5);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        if(polyline != null && path!=null && path.size()>0) {
            PolylineOptions polyOptions = new PolylineOptions().addAll(path);
            googleMap.addPolyline(polyOptions);

            googleMap.addMarker(new MarkerOptions()
                    .position(posLatLng).anchor(0.5f, 0.5f).draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_point)));

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for(LatLng coord : path){
                        builder.include(coord);
                    }
                    LatLngBounds m_bounds = builder.build();

                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10));
                }
            }, 1000);
        }
    }

    @Override
    public void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
        LatLng nearest = null;
        int nearestInd = -1;
        float mdist = 100000f;
        int index = -1;
        for(LatLng rpoint : path) {
            index++;
            float d = distanceBetween(posLatLng, rpoint);
            if (d < mdist) {
                mdist = d;
                nearest = rpoint;
                nearestInd = index;
            }
        }
        if (nearest != null && nearestInd != -1) {
            LatLng nextLatLng;
            if (nearestInd + 1 < path.size()-1) {
                nextLatLng = path.get(nearestInd + 1);
            } else {
                nextLatLng = new LatLng(ff.getLat(), ff.getLng());
            }
            //Find the Bearing from current location to next location
            float targetBearing = getTargetBearing(nextLatLng);

            long duration = 100;
            mStreetViewPanorama.animateTo(new StreetViewPanoramaCamera.Builder().
                    bearing(targetBearing).build(),duration);
        }
    }

    private float distanceBetween(LatLng latLng1, LatLng latLng2) {

        Location loc1 = new Location(LocationManager.GPS_PROVIDER);
        Location loc2 = new Location(LocationManager.GPS_PROVIDER);

        loc1.setLatitude(latLng1.latitude);
        loc1.setLongitude(latLng1.longitude);

        loc2.setLatitude(latLng2.latitude);
        loc2.setLongitude(latLng2.longitude);


        return loc1.distanceTo(loc2);
    }

    //Gets bearing between current point and next point in path
    private float getTargetBearing(LatLng next) {
        float targetBearing = 0f;
        if (next != null) {
            Location startLocation = new Location("starting point");
            Location endLocation = new Location("ending point");
            startLocation.setLatitude(posLatLng.latitude);
            startLocation.setLongitude(posLatLng.longitude);
            endLocation.setLatitude(next.latitude);
            endLocation.setLongitude(next.longitude);
            //Find the Bearing from current location to next location
            targetBearing = startLocation.bearingTo(endLocation);
        }
        return targetBearing;
    }

}
