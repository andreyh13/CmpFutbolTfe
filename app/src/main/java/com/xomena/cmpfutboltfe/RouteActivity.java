package com.xomena.cmpfutboltfe;
import com.xomena.cmpfutboltfe.model.*;
import com.xomena.cmpfutboltfe.util.*;
import com.xomena.cmpfutboltfe.ui.adapter.*;
import com.xomena.cmpfutboltfe.ui.fragment.*;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;


public class RouteActivity extends AppCompatActivity implements
        RouteMapFragment.OnFragmentInteractionListener, DirectionsMapFragment.OnFragmentInteractionListener,
        OnMyLocationButtonClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String LOG_TAG = "RouteActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    SimplePagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    private JSONObject jsonRoute = null;
    private FootballField ff;
    private String enc_polyline = null;
    private boolean mPermissionDenied = false;
    private GoogleMap mMap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarRoute);
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

        // Create the adapter that will return a fragment for each of the two primary sections
        // of the app.
        mPagerAdapter = new SimplePagerAdapter(getSupportFragmentManager(), this);

        // Set up the ViewPager, attaching the adapter and setting up a listener for when the
        // user swipes between sections.
        mViewPager = (ViewPager) findViewById(R.id.fragmentPager);
        mViewPager.setAdapter(mPagerAdapter);

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs_route);
        tabLayout.setupWithViewPager(mViewPager);

        Intent i = getIntent();
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        TextView title = (TextView)findViewById(R.id.toolbar_title_route);
        title.setText(String.format(getString(R.string.route_to), ff.getName()));
    }

    public static class SimplePagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 2;
        private int tabTitles[] = new int[] { R.string.description, R.string.route_map };
        private Context context;

        public SimplePagerAdapter(FragmentManager fragmentManager, Context context) {
            super(fragmentManager);
            this.context = context;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return RouteMapFragment.newInstance();
                case 1:
                    return DirectionsMapFragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return context.getString(tabTitles[position]);
        }
    }

    public void exchangeJSON(JSONObject json){
        jsonRoute = json;
        MapFragment mapFrag = (MapFragment)
                getFragmentManager().findFragmentById(R.id.route_map);
        if(mapFrag!=null && jsonRoute != null){
            final RouteActivity self = this;
            mapFrag.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if(googleMap!=null) {
                        LatLng lastLatLng = null;
                        try {
                            if (jsonRoute.has("status") && jsonRoute.getString("status").equals("OK")) {
                                if (jsonRoute.has("routes")) {
                                    JSONArray rts = jsonRoute.getJSONArray("routes");
                                    if (rts != null && rts.length() > 0 && !rts.isNull(0)) {
                                        JSONObject r = rts.getJSONObject(0);
                                        if (r.has("overview_polyline") && !r.isNull("overview_polyline")) {
                                            JSONObject m_poly = r.getJSONObject("overview_polyline");
                                            if (m_poly.has("points") && !m_poly.isNull("points")) {
                                                String enc_points = m_poly.getString("points");
                                                self.enc_polyline = enc_points;
                                                List<LatLng> m_path = PolyUtil.decode(enc_points);
                                                PolylineOptions polyOptions = new PolylineOptions().addAll(m_path);
                                                googleMap.addPolyline(polyOptions);

                                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                                for(LatLng coord : m_path){
                                                    builder.include(coord);
                                                }
                                                LatLngBounds m_bounds = builder.build();
                                                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10));
                                            }
                                        }

                                        //Legs
                                        if(r.has("legs") && !r.isNull("legs")) {
                                            JSONArray al = r.getJSONArray("legs");
                                            for (int i = 0; i < al.length(); i++) {
                                                if (!al.isNull(i)) {
                                                    JSONObject l = al.getJSONObject(i);

                                                    //Start address
                                                    if(i==0 && l.has("start_address") && !l.isNull("start_address")){
                                                        if (l.has("start_location") && !l.isNull("start_location")) {
                                                            googleMap.addMarker(new MarkerOptions()
                                                                    .position(new LatLng(l.getJSONObject("start_location").getDouble("lat"), l.getJSONObject("start_location").getDouble("lng")))
                                                                    .title(l.getString("start_address"))
                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                                        }
                                                    }

                                                    //End address
                                                    if(i==al.length()-1 && l.has("end_address") && !l.isNull("end_address")){
                                                        if (l.has("end_location") && !l.isNull("end_location")) {
                                                            lastLatLng = new LatLng(l.getJSONObject("end_location").getDouble("lat"), l.getJSONObject("end_location").getDouble("lng"));
                                                            googleMap.addMarker(new MarkerOptions()
                                                                    .position(lastLatLng)
                                                                    .title(l.getString("end_address"))
                                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Cannot process directions JSON results", e);
                        }
                        if(ff!=null) {
                            LatLng fldLatLng = new LatLng(ff.getLat(), ff.getLng());
                            googleMap.addMarker(new MarkerOptions().position(fldLatLng)
                                    .title(ff.getName()).snippet(getString(R.string.phoneLabel) + " " + ff.getPhone())
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));

                            if (lastLatLng != null) {
                                self.showCurvedPolyline(fldLatLng,lastLatLng,0.5);
                            }
                        }
                        googleMap.setOnMyLocationButtonClickListener(self);
                        self.mMap = googleMap;
                        self.enableMyLocation();
                    }
                }
            });
        }
    }

    public void onDirectionsMapClick(){

    }

    public void onRouteAnimationClick() {
        Intent intent = new Intent(this, AnimateRouteActivity.class);
        if(jsonRoute!=null && enc_polyline!=null){
            intent.putExtra("ENC_POLY", enc_polyline);
        }
        if(ff!=null){
            intent.putExtra(MainActivity.EXTRA_ITEM,ff);
        }
        startActivity(intent);
    }

    public void onShowStreetView(View view){
        this.onRouteAnimationClick();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }


    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    private void showCurvedPolyline (LatLng p1, LatLng p2, double k) {
        //Calculate distance and heading between two points
        double d = SphericalUtil.computeDistanceBetween(p1,p2);
        double h = SphericalUtil.computeHeading(p1, p2);

        //Midpoint position
        LatLng p = SphericalUtil.computeOffset(p1, d*0.5, h);

        //Apply some mathematics to calculate position of the circle center
        double x = (1-k*k)*d*0.5/(2*k);
        double r = (1+k*k)*d*0.5/(2*k);

        LatLng c = SphericalUtil.computeOffset(p, x, h + 90.0);

        //Polyline options
        PolylineOptions options = new PolylineOptions();
        List<PatternItem> pattern = Arrays.<PatternItem>asList(new Dash(30), new Gap(20));

        //Calculate heading between circle center and two points
        double h1 = SphericalUtil.computeHeading(c, p1);
        double h2 = SphericalUtil.computeHeading(c, p2);

        //Calculate positions of points on circle border and add them to polyline options
        int numpoints = 100;
        double step = (h2 -h1) / numpoints;

        for (int i=0; i < numpoints; i++) {
            LatLng pi = SphericalUtil.computeOffset(c, r, h1 + i * step);
            options.add(pi);
        }

        //Draw polyline
        mMap.addPolyline(options.width(10).color(Color.BLUE).geodesic(false).pattern(pattern));
    }

}
