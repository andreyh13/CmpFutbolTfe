package com.xomena.cmpfutboltfe;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.GeocodingApiRequest;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;


public class FieldDetailActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private GoogleMap mMap;

    private static final String LOG_TAG = "FieldDetailActivity";

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String ROADS_API_BASE = "https://roads.googleapis.com/v1/snapToRoads";
    private static final String REVGEO_API_BASE = "https://maps.googleapis.com/maps/api/geocode";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    private static final String API_KEY = "AIzaSyA67JIj41Ze0lbc2KidOgQMgqLOAZOcybE";
    private static final int QPS = 10;

    private FootballField ff;
    private JSONObject snappedPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_detail);

        TextView title = (TextView)findViewById(R.id.fieldDetailCaption);

        Intent i = getIntent();
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        title.setText(ff.getName());

        setUpMapIfNeeded(ff);

        AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.fieldDetailEnterPlace);
        autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
        autoCompView.setOnItemClickListener(this);
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String str = (String) adapterView.getItemAtPosition(position);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, RouteActivity.class);
        intent.putExtra(MainActivity.EXTRA_ITEM, ff);
        intent.putExtra(MainActivity.EXTRA_ADDRESS,str);
        startActivity(intent);
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

    private ArrayList<String> autocomplete(String input) {
        ArrayList<String> resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:es");
            sb.append("&location=28.2915637,-16.6291304&radius=70000");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

    private class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }

    public void onShowPitchStreetView(View view){
        //The client library doesn't work on old versions of Android. Will comment this block and implement another one.
        /*GeoApiContext context = new GeoApiContext().setApiKey(API_KEY).setQueryRateLimit(QPS);
        try {
            GeocodingApiRequest req = GeocodingApi.newRequest(context);
            GeocodingResult[] results = req.latlng(new com.google.maps.model.LatLng(ff.getLat(),ff.getLng())).await();
            if(results!=null && results.length>0){
                GeocodingResult r = results[0];
                if(r!=null && r.geometry!=null && r.geometry.location!=null){
                    Intent intent = new Intent(this, StreetViewActivity.class);
                    intent.putExtra("SV_LAT", r.geometry.location.lat);
                    intent.putExtra("SV_LNG", r.geometry.location.lng);
                    intent.putExtra("SV_LAT_NEXT", ff.getLat());
                    intent.putExtra("SV_LNG_NEXT", ff.getLng());
                    startActivity(intent);
                }
            }
        } catch(ApiException e){
            Log.e(LOG_TAG, e.getMessage());
        } catch(Exception e){
            Log.e(LOG_TAG, e.getMessage());
        }*/
        try {
            StringBuilder sb = new StringBuilder(ROADS_API_BASE).append("?key=").append(API_KEY)
                    .append("&path=").append(ff.getLat()).append(",").append(ff.getLng());
            new HttpAsyncTask().execute(sb.toString());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error processing Roads API URL", e);
        }
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder jsonResults = new StringBuilder();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error processing Roads API URL", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to Roads API", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return jsonResults.toString();
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            try {
                // Create a JSON object hierarchy from the results
                snappedPoints = new JSONObject(result);
                processJSONObject();
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
        }
    }

    private class HttpAsyncTaskRevGeo extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            StringBuilder jsonResults = new StringBuilder();
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urls[0]);
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "Error processing Geocoding API URL", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to Geocoding API", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return jsonResults.toString();
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            try {
                // Create a JSON object hierarchy from the results
                JSONObject m_revgeo = new JSONObject(result);
                processJSONObjectRevGeo(m_revgeo);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
        }
    }

    private void processJSONObject(){
        if(snappedPoints !=null){
            if(snappedPoints.has("snappedPoints") && !snappedPoints.isNull("snappedPoints")){
                try {
                    JSONArray points = snappedPoints.getJSONArray("snappedPoints");
                    if(points.length()>0){
                        JSONObject point = points.getJSONObject(0);

                        double lat =  point.getJSONObject("location").getDouble("latitude");
                        double lng =  point.getJSONObject("location").getDouble("longitude");

                        Intent intent = new Intent(this, StreetViewActivity.class);
                        intent.putExtra("SV_LAT", lat);
                        intent.putExtra("SV_LNG", lng);
                        intent.putExtra("SV_LAT_NEXT", ff.getLat());
                        intent.putExtra("SV_LNG_NEXT", ff.getLng());
                        startActivity(intent);

                    }
                } catch(JSONException e){
                    Log.e(LOG_TAG, "Cannot process JSON results", e);
                }
            } else {
                //Roads API didn't work so try with the reverse geocoding
                StringBuilder sb = new StringBuilder(REVGEO_API_BASE).append(OUT_JSON).append("?key=").append(API_KEY)
                        .append("&latlng=").append(ff.getLat()).append(",").append(ff.getLng());
                new HttpAsyncTaskRevGeo().execute(sb.toString());
            }
        }
    }

    private void processJSONObjectRevGeo(JSONObject res) {
        if (res != null) {
            if(res.has("results") && !res.isNull("results")) {
                try {
                    JSONArray addresses = res.getJSONArray("results");
                    if (addresses.length() > 0) {
                        JSONObject address = addresses.getJSONObject(0);

                        if (address.has("geometry") && !address.isNull("geometry")) {
                            JSONObject geom = address.getJSONObject("geometry");
                            if (geom.has("location") && !geom.isNull("location")) {
                                JSONObject loc = geom.getJSONObject("location");

                                double lat = loc.getDouble("lat");
                                double lng = loc.getDouble("lng");

                                Intent intent = new Intent(this, StreetViewActivity.class);
                                intent.putExtra("SV_LAT", lat);
                                intent.putExtra("SV_LNG", lng);
                                intent.putExtra("SV_LAT_NEXT", ff.getLat());
                                intent.putExtra("SV_LNG_NEXT", ff.getLng());
                                startActivity(intent);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Cannot process JSON results", e);
                }
            }
        }
    }
}
