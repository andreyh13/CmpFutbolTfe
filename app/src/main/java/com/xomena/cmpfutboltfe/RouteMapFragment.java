package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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
public class RouteMapFragment extends Fragment implements RouteStepAdapter.OnItemClickListener,
        WebServiceExec.OnWebServiceResult {

    private static final String LOG_TAG = "RouteMapFragment";

    private JSONObject jsonRoute;
    private List<LatLng> stepLatLng = new LinkedList<>();
    private double ff_lat;
    private double ff_lng;
    private String origPlaceId;
    private String address;
    private String destPlaceId;
    private FootballField ff;
    private String enc_polyline;

    private OnFragmentInteractionListener mListener;
    private RouteStepAdapter adapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RouteMapFragment.
     */
    public static RouteMapFragment newInstance() {
        return new RouteMapFragment();
    }

    public RouteMapFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getActivity().getIntent();
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        address = i.getStringExtra(MainActivity.EXTRA_ADDRESS);
        origPlaceId = i.getStringExtra(MainActivity.EXTRA_PLACEID);
        ff_lat = ff.getLat();
        ff_lng = ff.getLng();
        destPlaceId = ff.getPlaceId();

        getRoute(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FrameLayout frameRoute = (FrameLayout) inflater.inflate(R.layout.fragment_route_map, container, false);
        RecyclerView rvRoute = (RecyclerView) frameRoute.findViewById(R.id.rvRoute);
        Spanned[] items = new Spanned[] {};
        // Create adapter passing in the sample user data
        adapter = new RouteStepAdapter(RouteStep.createRouteStepsList(items));
        adapter.setOnItemClickListener(this);
        // Attach the adapter to the recyclerview to populate items
        rvRoute.setAdapter(adapter);
        // Set layout manager to position the items
        rvRoute.setLayoutManager(new LinearLayoutManager(getActivity()));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        rvRoute.addItemDecoration(itemDecoration);

        return frameRoute;
    }

    @Override
    public void onItemClick(View itemView, int position) {
        TextView stepTextView = (TextView) itemView.findViewById(R.id.route_step);
        if (stepTextView != null) {
            Intent intent = new Intent(getActivity(), StreetViewRouteStepActivity.class);
            LatLng location = stepLatLng.get(position);
            intent.putExtra(MainActivity.SV_LAT, location.latitude);
            intent.putExtra(MainActivity.SV_LNG, location.longitude);
            intent.putExtra(MainActivity.EXTRA_ITEM, ff);
            intent.putExtra(MainActivity.EXTRA_ENC_POLY, enc_polyline);
            intent.putExtra("ROUTE_STEP_DESCR", stepTextView.getText().toString());

            startActivity(intent);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity a;
        if (context instanceof Activity){
            a= (Activity) context;
        } else {
            a = getActivity();
        }

        try {
            mListener = (OnFragmentInteractionListener) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString()
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
        void exchangeJSON(JSONObject json);
    }

    private void processJSONObject(){
        if(jsonRoute != null){
            try {
                if (jsonRoute.has("status") && jsonRoute.getString("status").equals("OK")) {
                    if(jsonRoute.has("routes")){
                        JSONArray rts = jsonRoute.getJSONArray("routes");
                        if(rts != null && rts.length() > 0 && !rts.isNull(0)) {
                            JSONObject r = rts.getJSONObject(0);

                            if (r.has("overview_polyline") && !r.isNull("overview_polyline")) {
                                JSONObject m_poly = r.getJSONObject("overview_polyline");
                                if (m_poly.has("points") && !m_poly.isNull("points")) {
                                    this.enc_polyline = m_poly.getString("points");
                                }
                            }

                            List<Spanned> listSteps = new ArrayList<>();

                            //Title
                            if(r.has("summary") && !r.isNull("summary")){
                                View v = getView();
                                if (v != null) {
                                    TextView title = (TextView) getView().findViewById(R.id.routeTitle);
                                    if (title != null) {
                                        title.setText(r.getString("summary"));
                                    }
                                }
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
                            if (data.length > 0) {
                                for (int i = 0; i < data.length; i++) {
                                    adapter.addItem(new RouteStep(data[i]), i);
                                }
                            }

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

    @Override
    public void onRoadsResult(JSONObject res) {
        //Empty method
    }

    @Override
    public void onGeocodeResult(JSONObject res) {
        //Empty method
    }

    @Override
    public void onDirectionsResult(JSONObject res) {
        // Create a JSON object hierarchy from the results
        try {
            if (res.has("status") && res.getString("status").equals("NOT_FOUND")) {
                getRoute(true);
            } else {
                jsonRoute = res;
                processJSONObject();
                mListener.exchangeJSON(jsonRoute);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON exception", e);
        }
    }

    private void getRoute(boolean noPlaces) {
        String orig;
        String dest;
        try {
            if (noPlaces) {
                orig = URLEncoder.encode(address, "utf8");
                dest = ff_lat + "," + ff_lng;
            } else {
                orig = (origPlaceId != null && !"".equals(origPlaceId)) ? "place_id:" + origPlaceId :
                        URLEncoder.encode(address, "utf8");
                dest = (destPlaceId != null && !"".equals(destPlaceId)) ?
                        "place_id:" + destPlaceId : ff_lat + "," + ff_lng;
            }

            String m_url = MainActivity.DIRECTIONS_API_BASE + MainActivity.OUT_JSON +
                    "?origin=" + orig + "&destination=" + dest + "&language=es&units=metric&region=es";
            WebServiceExec m_exec = new WebServiceExec(WebServiceExec.WS_TYPE_DIRECTIONS, m_url, this);
            m_exec.executeWS();
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "Error processing Directions API URL", e);
        }
    }
}
