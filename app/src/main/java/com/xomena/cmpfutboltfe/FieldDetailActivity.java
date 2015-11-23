package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FieldDetailActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, WebServiceExec.OnWebServiceResult {

    private static final String LOG_TAG = "FieldDetailActivity";

    private GoogleMap mMap;
    private FootballField ff;

    protected GoogleApiClient mGoogleApiClient;
    private PlacesAutocompleteAdapter mAdapter;

    private static final LatLngBounds BOUNDS_TENERIFE = new LatLngBounds(
            new LatLng(27.9980726,-16.9259232), new LatLng(28.5893007,-16.1194386));

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            Toast.makeText(getApplicationContext(), primaryText, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), RouteActivity.class);
            intent.putExtra(MainActivity.EXTRA_ITEM, ff);
            intent.putExtra(MainActivity.EXTRA_ADDRESS, primaryText.toString());
            intent.putExtra(MainActivity.EXTRA_PLACEID, placeId);

            startActivity(intent);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();

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

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.abc_ic_ab_back_mtrl_am_alpha,
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

        setUpMapIfNeeded(ff);

        AutoCompleteTextView mAutocompleteView = (AutoCompleteTextView) findViewById(R.id.fieldDetailEnterPlace);
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);
        mAdapter = new PlacesAutocompleteAdapter(this, mGoogleApiClient, BOUNDS_TENERIFE, null);
        mAutocompleteView.setAdapter(mAdapter);
    }

    private void setUpMapIfNeeded(FootballField ff) {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.ff_map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ff.getLat(),ff.getLng())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                mMap.addMarker(new MarkerOptions().position(new LatLng(ff.getLat(),ff.getLng()))
                .title(ff.getName()).snippet(getString(R.string.phoneLabel)+" "+ff.getPhone())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));
            }
        }
    }

    public void onShowPitchStreetView(View view){
        try {
            String m_url = MainActivity.ROADS_API_BASE + "?path=" + ff.getLat() + "," + ff.getLng();
            WebServiceExec m_exec = new WebServiceExec(WebServiceExec.WS_TYPE_ROADS, m_url, this);
            m_exec.executeWS();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error processing Roads API URL", e);
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

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e(LOG_TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    private void startSV (double lat, double lng, String placeId) {
        Intent intent = new Intent(this, StreetViewActivity.class);
        intent.putExtra("SV_LAT", lat);
        intent.putExtra("SV_LNG", lng);
        intent.putExtra("SV_LAT_NEXT", ff.getLat());
        intent.putExtra("SV_LNG_NEXT", ff.getLng());
        intent.putExtra("SV_PLACEID", placeId);
        startActivity(intent);
    }
}
