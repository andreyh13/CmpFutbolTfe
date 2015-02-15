package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;

import android.location.Location;


public class StreetViewActivity extends ActionBarActivity implements OnStreetViewPanoramaReadyCallback {

    private double m_lat;
    private double m_lng;
    private double m_lat_next;
    private double m_lng_next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);

        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.streetviewpanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);

        Intent i = getIntent();
        m_lat = i.getDoubleExtra("SV_LAT", 0);
        m_lng = i.getDoubleExtra("SV_LNG", 0);
        m_lat_next = i.getDoubleExtra("SV_LAT_NEXT", 0);
        m_lng_next = i.getDoubleExtra("SV_LNG_NEXT", 0);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        if(m_lat!=0 && m_lng!=0) {
            panorama.setPosition(new LatLng(m_lat, m_lng));
            if(m_lat_next!=0 && m_lng_next!=0){
                //Get the current location
                Location startingLocation = new Location("starting point");
                startingLocation.setLatitude(m_lat);
                startingLocation.setLongitude(m_lng);

                //Get the target location
                Location endingLocation = new Location("ending point");
                endingLocation.setLatitude(m_lat_next);
                endingLocation.setLongitude(m_lng_next);

                //Find the Bearing from current location to next location
                float targetBearing = startingLocation.bearingTo(endingLocation);

                long duration = 0;
                panorama.animateTo(StreetViewPanoramaCamera.builder().bearing(targetBearing).build(),duration);
            }
        }
        panorama.setUserNavigationEnabled(true);
    }
}