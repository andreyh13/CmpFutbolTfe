package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaCamera;
import com.google.maps.android.PolyUtil;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class AnimateRouteActivity extends ActionBarActivity implements OnStreetViewPanoramaReadyCallback {

    private String polyline;
    private FootballField ff;
    List<LatLng> path;
    private StreetViewPanorama mStreetViewPanorama;
    private int position = 0;
    private Timer timer;
    private AnimateRouteTimerTask myTimerTask;
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
            myTimerTask = new AnimateRouteTimerTask();
            timer.schedule(myTimerTask, 1000, 1000);
            btnAnimate.setText(R.string.stop);
        }
        moveCameraToNextPosition();
    }

    private void moveCameraToNextPosition(){
        if(path!=null){
            Location startingLocation = new Location("starting point");
            Location endingLocation = new Location("ending point");
            if(position < path.size()-1){
                startingLocation.setLatitude(path.get(position).latitude);
                startingLocation.setLongitude(path.get(position).longitude);

                //Get the target location
                endingLocation.setLatitude(path.get(position+1).latitude);
                endingLocation.setLongitude(path.get(position+1).longitude);
            } else {
                startingLocation.setLatitude(path.get(path.size()-1).latitude);
                startingLocation.setLongitude(path.get(path.size()-1).longitude);

                //Get the target location
                endingLocation.setLatitude(ff.getLat());
                endingLocation.setLongitude(ff.getLng());
            }
            //Find the Bearing from current location to next location
            float targetBearing = startingLocation.bearingTo(endingLocation);

            long duration = 100;
            mStreetViewPanorama.animateTo(new StreetViewPanoramaCamera.Builder().
                    bearing(targetBearing).build(),duration);

        }
    }

    private void moveToNextPosition(){
        if(path!=null){
            if(position < path.size()-1){
                mStreetViewPanorama.setPosition(path.get(++position), 20);
            } else {
                Toast.makeText(this, getText(R.string.last_path_point),Toast.LENGTH_LONG).show();
                if(timer != null) {
                    timer.cancel();
                    timer = null;
                    btnAnimate.setClickable(false);
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
                    moveToNextPosition();
                    moveCameraToNextPosition();
                }
            });
        }

    }

}
