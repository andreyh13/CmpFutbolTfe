package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLink;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.maps.android.PolyUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AnimateRouteActivity extends AppCompatActivity
        implements OnStreetViewPanoramaReadyCallback, GoogleMap.OnMapClickListener,
        OnMapReadyCallback, StreetViewPanorama.OnStreetViewPanoramaChangeListener {

    private static final String LOG_TAG = "AnimateRouteActivity";

    private String polyline;
    private FootballField ff;
    List<LatLng> path;
    List<LatLng> interpolated;
    private StreetViewPanorama mStreetViewPanorama;
    private int position = 0;
    private Timer timer;
    private FloatingActionButton btnAnimate;
    private Marker posMarker;

    private boolean hasRuntimeData;
    private boolean wasAnimated;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animate_route);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarAnimate);
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

        btnAnimate = (FloatingActionButton)findViewById(R.id.move_position);

        Intent i = getIntent();
        polyline = i.getStringExtra("ENC_POLY");
        if(polyline!=null){
            path = PolyUtil.decode(polyline);
            interpolated = interpolatePath();
        }
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        TextView title = (TextView)findViewById(R.id.toolbar_title_animate);
        title.setText(String.format(getString(R.string.route_to),ff.getName()));

        if (savedInstanceState != null && savedInstanceState.containsKey("hasRuntimeData")) {
            hasRuntimeData = savedInstanceState.getBoolean("hasRuntimeData");
            position = savedInstanceState.getInt("currentPos");
            wasAnimated = savedInstanceState.getBoolean("wasAnimated");
        }

        MapFragment mapFrag = (MapFragment)
                getFragmentManager().findFragmentById(R.id.animate_map);
        mapFrag.getMapAsync(this);

        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.routepanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);

    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
       outState.putBoolean("hasRuntimeData", true);
       outState.putInt("currentPos", position);
       outState.putBoolean("wasAnimated", timer != null);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        mStreetViewPanorama = panorama;
        panorama.setOnStreetViewPanoramaChangeListener(this);
        if(polyline!=null && interpolated!=null && interpolated.size()>0){
            panorama.setPosition(interpolated.get(position), 20);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if(polyline != null) {
            PolylineOptions polyOptions = new PolylineOptions().addAll(path);
            googleMap.addPolyline(polyOptions);

            LatLng startPos;
            if (hasRuntimeData) {
                startPos = interpolated.get(position);
            } else {
                startPos = path.get(0);
            }

            posMarker = googleMap.addMarker(new MarkerOptions()
                    .position(startPos).anchor(0.5f, 0.5f).draggable(false)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_point)));

            googleMap.setOnMapClickListener(this);

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for(LatLng coord : path){
                builder.include(coord);
            }
            LatLngBounds m_bounds = builder.build();

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10));

            if (wasAnimated) {
                onMovePosition(findViewById(R.id.move_position));
            }
        }
    }

    public void onMovePosition(View view){
        if(timer != null){
            timer.cancel();
            timer = null;
            btnAnimate.setImageResource(R.drawable.ic_play_circle_outline_white_24dp);
        } else {
            timer = new Timer();
            AnimateRouteTimerTask myTimerTask = new AnimateRouteTimerTask();
            timer.schedule(myTimerTask, 1000, 2000);
            btnAnimate.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp);
        }
        moveCameraToNextPosition(interpolated);
    }

    //Gets bearing between current point and next point in path
    private float getTargetBearing(List<LatLng> path) {
        float targetBearing = 0f;
        if (path != null) {
            Location startingLocation = new Location("starting point");
            Location endingLocation = new Location("ending point");
            if (position < path.size() - 1) {
                startingLocation.setLatitude(path.get(position).latitude);
                startingLocation.setLongitude(path.get(position).longitude);

                //Get the target location
                endingLocation.setLatitude(path.get(position + 1).latitude);
                endingLocation.setLongitude(path.get(position + 1).longitude);
            } else {
                startingLocation.setLatitude(path.get(path.size() - 1).latitude);
                startingLocation.setLongitude(path.get(path.size() - 1).longitude);

                //Get the target location
                endingLocation.setLatitude(ff.getLat());
                endingLocation.setLongitude(ff.getLng());
            }
            //Find the Bearing from current location to next location
            targetBearing = startingLocation.bearingTo(endingLocation);
        }
        return targetBearing;
    }

    private void moveCameraToNextPosition(List<LatLng> path){
        if(path!=null && mStreetViewPanorama != null){
            //Find the Bearing from current location to next location
            float targetBearing = getTargetBearing(path);

            long duration = 100;
            mStreetViewPanorama.animateTo(new StreetViewPanoramaCamera.Builder().
                    bearing(targetBearing).build(),duration);

            if(position == path.size() -1) {
                Toast.makeText(this, getText(R.string.last_path_point), Toast.LENGTH_LONG).show();
            }
        }
    }


    private void moveToNextPosition(List<LatLng> path){
        if(path!=null){
            StreetViewPanoramaLocation location = mStreetViewPanorama.getLocation();
            if (location != null && location.links != null) {
                if(!isLocationBetweenPathPoints(location,interpolated)) {
                    position++;
                    if(!isLocationBetweenPathPoints(location,interpolated)) {
                        if (position < path.size() - 1) {
                            mStreetViewPanorama.setPosition(path.get(++position), 5);
                        }
                    } else {
                        if (position < path.size() - 1) {
                            StreetViewPanoramaLink link = findClosestLinkToBearing(location.links, getTargetBearing(interpolated));
                            mStreetViewPanorama.setPosition(link.panoId);
                        }
                    }
                } else {
                    StreetViewPanoramaLink link = findClosestLinkToBearing(location.links, getTargetBearing(interpolated));
                    mStreetViewPanorama.setPosition(link.panoId);
                }
            } else {
                if (position < path.size() - 1) {
                    mStreetViewPanorama.setPosition(path.get(++position), 5);
                }
            }
        }
    }

    private void movePositionalMarker () {
        if (posMarker != null && mStreetViewPanorama != null) {
            StreetViewPanoramaLocation loc = mStreetViewPanorama.getLocation();
            if (loc != null) {
                LatLng pos = loc.position;
                if (pos != null) {
                    posMarker.setPosition(pos);
                }
            }
        }
    }

    class AnimateRouteTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    moveToNextPosition(interpolated);
                    moveCameraToNextPosition(interpolated);
                    movePositionalMarker();

                    if(position == path.size() -1){
                        if(timer != null) {
                            timer.cancel();
                            timer = null;
                            btnAnimate.setClickable(false);
                            btnAnimate.setImageResource(R.drawable.ic_play_circle_outline_white_24dp);
                        }
                    }
                }
            });
        }

    }

    private List<LatLng> interpolatePath(){
        int counter = 0;
        float step = 10f;
        LatLng last = path.get(path.size()-1);
        List<LatLng> res = path;

        while(!(res.get(counter).latitude == last.latitude && res.get(counter).longitude == last.longitude)){
            Location s = new Location("");
            s.setLatitude(res.get(counter).latitude);
            s.setLongitude(res.get(counter).longitude);
            Location f = new Location("");
            f.setLatitude(res.get(counter+1).latitude);
            f.setLongitude(res.get(counter+1).longitude);
            float d = s.distanceTo(f);
            if(d < step){
                counter++;
            } else {
                double m_lat = (res.get(counter).latitude + res.get(counter+1).latitude)*0.5;
                double m_lng = (res.get(counter).longitude + res.get(counter+1).longitude)*0.5;
                res.add(counter+1,new LatLng(m_lat, m_lng));
            }
        }

        return res;

    }

    public static StreetViewPanoramaLink findClosestLinkToBearing(StreetViewPanoramaLink[] links,
                                                                  float bearing) {
        float minBearingDiff = 360;
        StreetViewPanoramaLink closestLink = links[0];
        for (StreetViewPanoramaLink link : links) {
            if (minBearingDiff > findNormalizedDifference(bearing, link.bearing)) {
                minBearingDiff = findNormalizedDifference(bearing, link.bearing);
                closestLink = link;
            }
        }
        return closestLink;
    }

    // Find the difference between angle a and b as a value between 0 and 180
    public static float findNormalizedDifference(float a, float b) {
        float diff = a - b;
        float normalizedDiff = diff - (360.0f * (float)Math.floor(diff / 360.0f));
        return (normalizedDiff < 180.0f) ? normalizedDiff : 360.0f - normalizedDiff;
    }

    private boolean isLocationBetweenPathPoints(StreetViewPanoramaLocation location, List<LatLng> path){
        LatLng pos1 = path.get(position);
        LatLng pos2;
        if(position<path.size()-1) {
            pos2 = path.get(position + 1);
        } else {
            pos2 = new LatLng(ff.getLat(), ff.getLng());
        }

        double llat = Math.round(location.position.latitude*100000)/100000d;
        double llng = Math.round(location.position.longitude*100000)/100000d;
        double p1lat = Math.round(pos1.latitude*100000)/100000d;
        double p1lng = Math.round(pos1.longitude*100000)/100000d;
        double p2lat = Math.round(pos2.latitude*100000)/100000d;
        double p2lng = Math.round(pos2.longitude*100000)/100000d;

        return llat >= Math.min(p1lat,p2lat) && llat <= Math.max(p1lat,p2lat) &&
                llng >= Math.min(p1lng,p2lng) && llng <= Math.max(p1lng,p2lng);
    }

    @Override
    public void onMapClick(LatLng point) {
        LatLng nearest = null;
        int nearestInd = -1;
        float mdist = 100000f;
        int index = -1;
        for(LatLng rpoint : interpolated) {
            index++;
            float d = distanceBetween(point, rpoint);
            if (d < mdist) {
                mdist = d;
                nearest = rpoint;
                nearestInd = index;
            }
        }
        if (mdist <= 50f) {
            if(timer != null){
                timer.cancel();
                timer = null;
                btnAnimate.setImageResource(R.drawable.ic_play_circle_outline_white_24dp);
            }
            posMarker.setPosition(nearest);
            position = nearestInd;
            mStreetViewPanorama.setPosition(nearest, 20);
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

    @Override
    public  void onStreetViewPanoramaChange(StreetViewPanoramaLocation location) {
        moveCameraToNextPosition(interpolated);
    }

}
