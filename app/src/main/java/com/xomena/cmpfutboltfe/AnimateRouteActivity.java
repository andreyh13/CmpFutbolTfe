package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.android.gms.maps.model.StreetViewPanoramaLink;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;
import com.google.maps.android.PolyUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AnimateRouteActivity extends ActionBarActivity implements OnStreetViewPanoramaReadyCallback {

    private String polyline;
    private FootballField ff;
    List<LatLng> path;
    List<LatLng> interpolated;
    private StreetViewPanorama mStreetViewPanorama;
    private int position = 0;
    private Timer timer;
    private Button btnAnimate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animate_route);

        StreetViewPanoramaFragment streetViewPanoramaFragment =
                (StreetViewPanoramaFragment) getFragmentManager()
                        .findFragmentById(R.id.routepanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);

        btnAnimate = (Button)findViewById(R.id.move_position);

        Intent i = getIntent();
        polyline = i.getStringExtra("ENC_POLY");
        if(polyline!=null){
            path = PolyUtil.decode(polyline);
            interpolated = interpolatePath();
        }
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        mStreetViewPanorama = panorama;
        if(polyline!=null && path!=null && path.size()>0){
            panorama.setPosition(path.get(0), 20);
        }
    }

    public void onMovePosition(View view){
        if(timer != null){
            timer.cancel();
            timer = null;
            btnAnimate.setText(R.string.start);
        } else {
            timer = new Timer();
            AnimateRouteTimerTask myTimerTask = new AnimateRouteTimerTask();
            timer.schedule(myTimerTask, 1000, 2000);
            btnAnimate.setText(R.string.stop);
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
        if(path!=null){
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

    class AnimateRouteTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    moveToNextPosition(interpolated);
                    moveCameraToNextPosition(interpolated);

                    if(position == path.size() -1){
                        if(timer != null) {
                            timer.cancel();
                            timer = null;
                            btnAnimate.setClickable(false);
                            btnAnimate.setText(R.string.finished);
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

}
