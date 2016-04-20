package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

import android.location.Location;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


public class StreetViewActivity extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback {

    private static final String LOG_TAG = "StreetViewActivity";
    private static final int MAX_RADIUS = 50;

    private double m_lat;
    private double m_lng;
    private double m_lat_next;
    private double m_lng_next;
    private StreetViewPanorama mStreetViewPanorama;
    private Timer timer;
    private int radius = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarSV);
        setSupportActionBar(toolbar);
        try {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Exception", e);
        }

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.abc_ic_ab_back_material,
                getApplicationContext().getTheme());
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Intent i = getIntent();
        m_lat = i.getDoubleExtra("SV_LAT", 0);
        m_lng = i.getDoubleExtra("SV_LNG", 0);
        m_lat_next = i.getDoubleExtra("SV_LAT_NEXT", 0);
        m_lng_next = i.getDoubleExtra("SV_LNG_NEXT", 0);

        TextView title = (TextView)findViewById(R.id.toolbar_title_sv);
        String sv_title = i.getStringExtra("SV_TITLE");
        if (sv_title != null && !sv_title.equals("")) {
            title.setText(sv_title);
        }

        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.streetviewpanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        panorama.setUserNavigationEnabled(true);
        mStreetViewPanorama = panorama;

        initPanorama(0);
    }

    private void initPanorama(int step) {
        if (radius >= MAX_RADIUS) {
            if(timer != null) {
                timer.cancel();
                timer = null;
            }
            Toast.makeText(this, getString(R.string.no_panorama), Toast.LENGTH_LONG).show();
        } else {
            if(m_lat!=0 && m_lng!=0) {
                if (step == 0) {
                    mStreetViewPanorama.setPosition(new LatLng(m_lat, m_lng));

                    timer = new Timer();
                    PanoramaTimerTask myTimerTask = new PanoramaTimerTask();
                    timer.schedule(myTimerTask, 2000, 1000);
                } else {
                    radius += step;
                    mStreetViewPanorama.setPosition(new LatLng(m_lat, m_lng), radius);
                }
            }
        }
    }

    class PanoramaTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    StreetViewPanoramaLocation svLoc = mStreetViewPanorama.getLocation();
                    if (svLoc != null && svLoc.panoId != null && !"".equals(svLoc.panoId)) {
                        StreetViewPanoramaCamera.Builder cam_build = StreetViewPanoramaCamera.builder();
                        if (svLoc.position != null && m_lat_next != 0 && m_lng_next != 0) {
                            //Get the current location
                            Location startingLocation = new Location("starting point");
                            startingLocation.setLatitude(svLoc.position.latitude);
                            startingLocation.setLongitude(svLoc.position.longitude);

                            //Get the target location
                            Location endingLocation = new Location("ending point");
                            endingLocation.setLatitude(m_lat_next);
                            endingLocation.setLongitude(m_lng_next);

                            //Find the Bearing from current location to next location
                            float targetBearing = startingLocation.bearingTo(endingLocation);

                            long duration = 100;
                            mStreetViewPanorama.animateTo(cam_build.bearing(targetBearing).build(), duration);
                        }

                        if(timer != null) {
                            timer.cancel();
                            timer = null;
                        }
                    } else {
                        initPanorama(10);
                    }
                }
            });
        }

    }
}