package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
public class CountiesFragment extends Fragment implements CountyAdapter.OnItemClickListener {

    private static final String TAG = "CountiesFragment";
    private static final String DATA_SERVICE_URL = "https://script.google.com/macros/s/AKfycbyxqfsV0zdCKFRxgYYWPVO1PMshyhiuvTbvuKkkHjEGimPcdlpd/exec?jsonp=?";
    private Map<String,List<FootballField>> ff_data;
    private OnFragmentInteractionListener mListener;
    private CountyAdapter adapter;
    private boolean inProgressAsync = false;

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
                ArrayList<FootballField> af = new ArrayList<>(ff_data.get(key));
                savedInstanceState.putParcelableArrayList(key, af);
            }
        }
    }

    private void setupRecyclerView(RecyclerView recyclerView, Bundle savedInstanceState) {
        String[] items = new String[] {};
        // Create adapter passing in the sample user data
        adapter = new CountyAdapter(County.createCountiesList(items));
        adapter.setOnItemClickListener(this);
        // Attach the adapter to the recyclerview to populate items
        recyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        if(savedInstanceState!=null && savedInstanceState.containsKey(MainActivity.SAVED_KEYS)){
            String[] keys = savedInstanceState.getStringArray(MainActivity.SAVED_KEYS);
            ff_data = new LinkedHashMap<>();
            if (keys != null && keys.length > 0) {
                for (String key : keys) {
                    ArrayList<Parcelable> al = savedInstanceState.getParcelableArrayList(key);
                    ArrayList<FootballField> af = new ArrayList<>();
                    if (al != null) {
                        for (Parcelable p : al) {
                            af.add((FootballField) p);
                        }
                    }
                    ff_data.put(key, af);
                }
                showCounties(keys);
            }
        } else {
            // call AsyncTask to perform network operation on separate thread
            inProgressAsync = true;
            new HttpAsyncTask().execute(DATA_SERVICE_URL);
        }
    }

    @Override
    public void onItemClick(View itemView, int position) {
        TextView nameTextView = (TextView) itemView.findViewById(R.id.county_name);
        if (nameTextView != null) {
            onCountyPressed(nameTextView.getText().toString(), ff_data);
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
            if (inProgressAsync) {
                mListener.onStartAsyncTask();
            }
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
        void onObtainFootballFields(Map<String,List<FootballField>> ff_data);
        void onSelectCounty(String county, Map<String,List<FootballField>> ff_data);
        void onStartAsyncTask();
    }

    private String getFromUrl(String url) {
        String res = "";
        // check if you are connected or not
        if(isConnected()) {
            HttpURLConnection conn = null;
            try {
                URL urlobj = new URL(url);
                conn = (HttpURLConnection) urlobj.openConnection();
                conn.connect();
                InputStream in = conn.getInputStream();
                StringBuilder stringBuilder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                res = stringBuilder.toString();
            } catch (MalformedURLException ex) {
                Log.e(TAG, "Malformed URL", ex);
            } catch (IOException ex) {
                Log.e(TAG, "IO error", ex);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } else {
            Log.d(TAG, "Network is not connected");
            Toast.makeText(getActivity(), "Network is not connected", Toast.LENGTH_SHORT).show();
        }
        return res;
    }

    // check network connection
    private boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return getFromUrl(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Map<String,List<FootballField>> res = new LinkedHashMap<>();
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
            inProgressAsync = false;
        }
    }

    private void showCounties(String[] data) {
        if (data != null && data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                adapter.addItem(new County(data[i]), i);
            }
        }

        onInitializeFootballFields(ff_data);
    }
}
