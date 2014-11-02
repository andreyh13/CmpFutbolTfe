package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class RouteActivity extends ActionBarActivity {
    private GoogleMap mMap;

    private static final String LOG_TAG = "RouteActivity";

    private static final String DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions";
    private static final String GEOCODE_API_BASE = "https://maps.googleapis.com/maps/api/geocode";
    private static final String OUT_JSON = "/json";

    private static final String API_KEY = "AIzaSyA67JIj41Ze0lbc2KidOgQMgqLOAZOcybE";

    private FootballField ff;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        TextView title = (TextView)findViewById(R.id.routeCaption);

        Intent i = getIntent();
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        title.setText(ff.getName());

        address = i.getStringExtra(MainActivity.EXTRA_ADDRESS);
        getRoute(address, ff.getLat(), ff.getLng());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_route, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded(FootballField ff, String address) {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.route_map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ff.getLat(), ff.getLng())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                mMap.addMarker(new MarkerOptions().position(new LatLng(ff.getLat(),ff.getLng()))
                        .title(ff.getName()).snippet(getString(R.string.phoneLabel)+" "+ff.getPhone())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));
            }
        }
    }

    private void getRoute(String address, double fLat, double fLon) {
        try {
            StringBuilder sb = new StringBuilder(DIRECTIONS_API_BASE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&origin=" + URLEncoder.encode(address, "utf8"));
            sb.append("&destination=" + fLat + "," + fLon);
            sb.append("&language=es");
            sb.append("&units=metric");
            sb.append("&region=es");
            new HttpAsyncTask().execute(sb.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error processing Directions API URL", e);
        }
    }

    private void processJSONObject(JSONObject jsonRoute){
        if(jsonRoute != null){
            try {
                if (jsonRoute.has("status") && jsonRoute.getString("status").equals("OK")) {
                    if(jsonRoute.has("routes")){
                       JSONArray rts = jsonRoute.getJSONArray("routes");
                       if(rts != null && rts.length() > 0 && !rts.isNull(0)) {
                           JSONObject r = rts.getJSONObject(0);

                           //Title
                           if(r.has("summary") && !r.isNull("summary")){
                               TextView title = (TextView)findViewById(R.id.routeTitle);
                               title.setText(r.getString("summary"));
                           }

                           //Warnings
                           if(r.has("warnings") && !r.isNull("warnings")){
                               JSONArray aw = r.getJSONArray("warnings");
                               if(aw.length() > 0) {
                                   String w = "";
                                   for(int i=0; i<aw.length(); i++){
                                       if(!aw.isNull(i)){
                                           w += aw.getString(i)+"\n";
                                       }
                                   }
                                   if(!w.equals("")){
                                       TextView warn = (TextView)findViewById(R.id.routeWarnings);
                                       warn.setText(w);
                                   }
                               }
                           }

                           //Legs
                           if(r.has("legs") && !r.isNull("legs")){
                               JSONArray al = r.getJSONArray("legs");
                               for(int i=0; i<al.length(); i++){
                                   if(!al.isNull(i)){
                                       JSONObject l = al.getJSONObject(i);
                                       //Start address
                                       if(i==0 && l.has("start_address") && !l.isNull("start_address")){
                                           TextView saddr = (TextView)findViewById(R.id.routeStartAddress);
                                           saddr.setText(l.getString("start_address"));
                                       }
                                       //End address
                                       if(i==al.length()-1 && l.has("end_address") && !l.isNull("end_address")){
                                           TextView eaddr = (TextView)findViewById(R.id.routeEndAddress);
                                           eaddr.setText(l.getString("end_address"));
                                       }
                                       //Distance and duration
                                       String dd = "";
                                       if(i==0 && l.has("distance") && !l.isNull("distance")){
                                           dd += getString(R.string.distance)+": "+l.getJSONObject("distance").getString("text")+"  ";
                                       }
                                       if(i==0 && l.has("duration") && !l.isNull("duration")){
                                           dd += getString(R.string.duration)+": "+l.getJSONObject("duration").getString("text");
                                       }
                                       if(!dd.equals("")){
                                           TextView ddtv = (TextView)findViewById(R.id.routeDistanceDuration);
                                           ddtv.setText(dd);
                                       }

                                       //Steps

                                   }
                               }
                           }

                           //Copyrights
                           if(r.has("copyrights") && !r.isNull("copyrights")){
                               TextView cpr = (TextView)findViewById(R.id.routeCopyright);
                               cpr.setText(r.getString("copyrights"));
                           }
                       }
                    }
                }
            } catch(JSONException e){
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
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
                Log.e(LOG_TAG, "Error processing Directions API URL", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to Directions API", e);
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
                JSONObject res = new JSONObject(result);
                processJSONObject(res);
                setUpMapIfNeeded(ff, address);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
        }
    }

}
