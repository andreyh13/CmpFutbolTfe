package com.xomena.cmpfutboltfe;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


public class RouteActivity extends AppCompatActivity implements
        RouteMapFragment.OnFragmentInteractionListener, DirectionsMapFragment.OnFragmentInteractionListener {

    private static final String LOG_TAG = "RouteActivity";

    SimplePagerAdapter mPagerAdapter;
    ViewPager mViewPager;

    private JSONObject jsonRoute = null;
    private FootballField ff;
    private String enc_polyline = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarRoute);
        setSupportActionBar(toolbar);
        try {
            android.support.v7.app.ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Exception", e);
        }

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.abc_ic_ab_back_mtrl_am_alpha,
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
        title.setText(getString(R.string.route_to) + " " + ff.getName());
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
            GoogleMap map = mapFrag.getMap();
            if(map!=null) {
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
                                        this.enc_polyline = enc_points;
                                        List<LatLng> m_path = PolyUtil.decode(enc_points);
                                        PolylineOptions polyOptions = new PolylineOptions().addAll(m_path);
                                        Polyline polyline = map.addPolyline(polyOptions);

                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        for(LatLng coord : m_path){
                                            builder.include(coord);
                                        }
                                        LatLngBounds m_bounds = builder.build();
                                        map.moveCamera(CameraUpdateFactory.newLatLngBounds(m_bounds, 10));
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
                                                    map.addMarker(new MarkerOptions()
                                                            .position(new LatLng(l.getJSONObject("start_location").getDouble("lat"), l.getJSONObject("start_location").getDouble("lng")))
                                                            .title(l.getString("start_address"))
                                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                                }
                                            }

                                            //End address
                                            if(i==al.length()-1 && l.has("end_address") && !l.isNull("end_address")){
                                                if (l.has("end_location") && !l.isNull("end_location")) {
                                                    map.addMarker(new MarkerOptions()
                                                            .position(new LatLng(l.getJSONObject("end_location").getDouble("lat"), l.getJSONObject("end_location").getDouble("lng")))
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
                    map.addMarker(new MarkerOptions().position(new LatLng(ff.getLat(), ff.getLng()))
                            .title(ff.getName()).snippet(getString(R.string.phoneLabel) + " " + ff.getPhone())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));
                }
            }
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
}
