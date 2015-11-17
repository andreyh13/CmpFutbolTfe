package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CountiesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CountiesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CountiesFragment extends Fragment {

    private static final String TAG = "CountiesFragment";
    private Map<String,List<FootballField>> ff_data;
    private OnFragmentInteractionListener mListener;
    private RecyclerView mRecycler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CountiesFragment.
     */
    public static CountiesFragment newInstance() {
        return new CountiesFragment();
    }

    public CountiesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        RecyclerView recyclerView = (RecyclerView) inflater.inflate(
                R.layout.fragment_counties, container, false);
        setupRecyclerView(recyclerView, savedInstanceState);
        return recyclerView;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if(ff_data != null){
            savedInstanceState.putStringArray(MainActivity.SAVED_KEYS, ff_data.keySet().toArray(new String[ff_data.keySet().size()]));
            for(String key : ff_data.keySet()){
                ArrayList<FootballField> af = new ArrayList<FootballField>(ff_data.get(key));
                savedInstanceState.putParcelableArrayList(key, af);
            }
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView, Bundle savedInstanceState) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);
        mRecycler = recyclerView;

        if(savedInstanceState!=null && savedInstanceState.containsKey(MainActivity.SAVED_KEYS)){
            String[] keys = savedInstanceState.getStringArray(MainActivity.SAVED_KEYS);
            ff_data = new LinkedHashMap<String,List<FootballField>>();
            for (String key : keys) {
                ArrayList<Parcelable> al = savedInstanceState.getParcelableArrayList(key);
                ArrayList<FootballField> af = new ArrayList<FootballField>();
                for (Parcelable p : al) {
                    af.add((FootballField) p);
                }
                ff_data.put(key, af);
            }
            showCounties(keys);
        } else {
            // call AsyncTask to perform network operation on separate thread
            String DATA_SERVICE_URL = "https://script.google.com/macros/s/AKfycbyxqfsV0zdCKFRxgYYWPVO1PMshyhiuvTbvuKkkHjEGimPcdlpd/exec?jsonp=?";
            new HttpAsyncTask().execute(DATA_SERVICE_URL);
        }
    }

    public void onCountyPressed(String county, Map<String,List<FootballField>> ff_data) {
        if (mListener != null) {
            mListener.onSelectCounty(county, ff_data);
        }
    }

    public void onInitializeFootballFields(Map<String,List<FootballField>> ff_data) {
        if (mListener != null) {
            mListener.onObtainFootballFields(ff_data);
        }
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
        public void onObtainFootballFields(Map<String,List<FootballField>> ff_data);
        public void onSelectCounty(String county, Map<String,List<FootballField>> ff_data);
    }

    private String getFromUrl(String url) {
        String res = "";
        InputStream content;
        // check if you are connected or not
        if(isConnected()) {
            HttpClient httpclient = new DefaultHttpClient();
            try {
                HttpResponse response = httpclient.execute(new HttpGet(url));
                content = response.getEntity().getContent();
                if(content !=null){
                    try {
                        res = CountiesFragment.convertInputStreamToString(content);
                    } catch (IOException ex){
                        Log.e(TAG, "IO exception", ex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Network exception", e);
            } finally {
                httpclient.getConnectionManager().shutdown();
            }
        } else {
            Log.d(TAG, "Network not connected");
        }
        return res;
    }

    // check network connection
    private boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    // convert input stream to String
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getFromUrl(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Map<String,List<FootballField>> res = new LinkedHashMap<String, List<FootballField>>();
            String json = (result.substring(0,result.length()-1)).substring(2);

            JSONArray js_arr;
            try {
                js_arr = new JSONArray(json);
                for(int i=1; i<js_arr.length(); i++){
                    if(!js_arr.isNull(i)){
                        JSONArray js_val = js_arr.getJSONArray(i);
                        FootballField ff = new FootballField(js_val);
                        String key = ff.getCounty();
                        if(!res.containsKey(key)){
                            res.put(key, new LinkedList<FootballField>());
                        }
                        res.get(key).add(ff);
                    }
                }
            } catch (JSONException ex){
                Log.e(TAG, "JSON Exception", ex);
            }

            ff_data = res;
            String[] data = res.keySet().toArray(new String[res.keySet().size()]);
            showCounties(data);
        }
    }

    private void showCounties(String[] data){
        CountyAdapter adapter = new CountyAdapter(County.createCountiesList(data));
        // Attach the adapter to the recyclerview to populate items
        mRecycler.setAdapter(adapter);

        /*ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(),
                android.R.layout.simple_list_item_1, data);
        ListView listView = (ListView) getActivity().findViewById(R.id.listView);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                TextView selectedView = (TextView) v;
                onCountyPressed(selectedView.getText().toString(), ff_data);
            }
        });*/

        onInitializeFootballFields(ff_data);
    }
}
