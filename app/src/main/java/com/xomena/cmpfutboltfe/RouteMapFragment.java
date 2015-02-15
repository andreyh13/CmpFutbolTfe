package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

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
import java.util.LinkedList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link RouteMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link RouteMapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RouteMapFragment extends Fragment {

    private static final String LOG_TAG = "RouteMapFragment";

    private JSONObject jsonRoute;
    private List<LatLng> stepLatLng = new LinkedList<LatLng>();
    private double ff_lat;
    private double ff_lng;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RouteMapFragment.
     */
    public static RouteMapFragment newInstance() {
        RouteMapFragment fragment = new RouteMapFragment();
        return fragment;
    }

    public RouteMapFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getActivity().getIntent();
        FootballField ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        String address = i.getStringExtra(MainActivity.EXTRA_ADDRESS);
        ff_lat = ff.getLat();
        ff_lng = ff.getLng();
        getRoute(address, ff_lat, ff_lng);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_map, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void exchangeJSON(JSONObject json);
    }

    private void getRoute(String address, double fLat, double fLon) {
        try {
            StringBuilder sb = new StringBuilder(MainActivity.DIRECTIONS_API_BASE).append(MainActivity.OUT_JSON)
                    .append("?key=").append(MainActivity.API_KEY)
                    .append("&origin=").append(URLEncoder.encode(address, "utf8"))
                    .append("&destination=").append(fLat).append(",").append(fLon)
                    .append("&language=es&units=metric&region=es");
            new HttpAsyncTask().execute(sb.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error processing Directions API URL", e);
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
                jsonRoute = new JSONObject(result);
                processJSONObject();
                mListener.exchangeJSON(jsonRoute);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Cannot process JSON results", e);
            }
        }
    }

    private void processJSONObject(){
        if(jsonRoute != null){
            try {
                if (jsonRoute.has("status") && jsonRoute.getString("status").equals("OK")) {
                    if(jsonRoute.has("routes")){
                        JSONArray rts = jsonRoute.getJSONArray("routes");
                        if(rts != null && rts.length() > 0 && !rts.isNull(0)) {
                            JSONObject r = rts.getJSONObject(0);
                            List<Spanned> listSteps = new ArrayList<Spanned>();

                            //Title
                            if(r.has("summary") && !r.isNull("summary")){
                                TextView title = (TextView)getView().findViewById(R.id.routeTitle);
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
                                        TextView warn = (TextView)getView().findViewById(R.id.routeWarnings);
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

                                        //Distance and duration
                                        String dd = "";
                                        if(i==0 && l.has("distance") && !l.isNull("distance")){
                                            dd += getString(R.string.distance)+": "+l.getJSONObject("distance").getString("text")+"  ";
                                        }
                                        if(i==0 && l.has("duration") && !l.isNull("duration")){
                                            dd += getString(R.string.duration)+": "+l.getJSONObject("duration").getString("text");
                                        }
                                        if(!dd.equals("")){
                                            TextView ddtv = (TextView)getView().findViewById(R.id.routeDistanceDuration);
                                            ddtv.setText(dd);
                                        }

                                        //Start address
                                        if(i==0 && l.has("start_address") && !l.isNull("start_address")){
                                            listSteps.add(Html.fromHtml(l.getString("start_address")));
                                            if (l.has("start_location") && !l.isNull("start_location")) {
                                                stepLatLng.add(new LatLng(l.getJSONObject("start_location").getDouble("lat"), l.getJSONObject("start_location").getDouble("lng")));
                                            }
                                        }

                                        //Steps
                                        if(l.has("steps") && !l.isNull("steps")){
                                            JSONArray as = l.getJSONArray("steps");
                                            for(int k=0; k<as.length(); k++){
                                                if(!as.isNull(k)){
                                                    JSONObject s = as.getJSONObject(k);
                                                    String m_step = "";
                                                    if(s.has("html_instructions") && !s.isNull("html_instructions")) {
                                                        m_step += s.getString("html_instructions") + "<br/>";
                                                    }
                                                    if(s.has("distance") && !s.isNull("distance")){
                                                        m_step += getString(R.string.distance)+": "+s.getJSONObject("distance").getString("text")+"  ";
                                                    }
                                                    if(s.has("duration") && !s.isNull("duration")){
                                                        m_step += getString(R.string.duration)+": "+s.getJSONObject("duration").getString("text");
                                                    }
                                                    if(!m_step.equals("")) {
                                                        if (s.has("start_location") && !s.isNull("start_location")) {
                                                            stepLatLng.add(new LatLng(s.getJSONObject("start_location").getDouble("lat"), s.getJSONObject("start_location").getDouble("lng")));
                                                        }
                                                        listSteps.add(Html.fromHtml(m_step));
                                                    }
                                                }
                                            }
                                        }

                                        //End address
                                        if(i==al.length()-1 && l.has("end_address") && !l.isNull("end_address")){
                                            listSteps.add(Html.fromHtml(l.getString("end_address")));
                                            if (l.has("end_location") && !l.isNull("end_location")) {
                                                stepLatLng.add(new LatLng(l.getJSONObject("end_location").getDouble("lat"), l.getJSONObject("end_location").getDouble("lng")));
                                            }
                                        }
                                    }
                                }
                            }

                            Spanned[] data = listSteps.toArray(new Spanned[listSteps.size()]);
                            ArrayAdapter<Spanned> adapter = new ArrayAdapter<Spanned>(this.getActivity(),
                                    android.R.layout.simple_list_item_1, data);
                            ListView listView = (ListView) getActivity().findViewById(R.id.routeListView);
                            listView.setAdapter(adapter);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                                    Intent intent = new Intent(getActivity(), StreetViewActivity.class);
                                    LatLng location = stepLatLng.get(position);
                                    intent.putExtra("SV_LAT", location.latitude);
                                    intent.putExtra("SV_LNG", location.longitude);
                                    if(position==stepLatLng.size()-1){
                                        intent.putExtra("SV_LAT_NEXT", stepLatLng.get(position+1).latitude);
                                        intent.putExtra("SV_LNG_NEXT", stepLatLng.get(position+1).longitude);
                                    } else {
                                        intent.putExtra("SV_LAT_NEXT", ff_lat);
                                        intent.putExtra("SV_LNG_NEXT", ff_lng);
                                    }
                                    startActivity(intent);
                                }
                            });

                            //Copyrights
                            if(r.has("copyrights") && !r.isNull("copyrights")){
                                TextView cpr = (TextView)getView().findViewById(R.id.routeCopyright);
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
}
