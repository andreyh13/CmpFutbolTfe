package com.xomena.cmpfutboltfe;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
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

import android.content.Intent;

public class MainActivity extends ActionBarActivity {
    private static final String TAG = "MainActivity";
    public static final String EXTRA_COUNTY = "com.xomena.cmpfutboltfe.COUNTY";
    public static final String EXTRA_FIELDS = "com.xomena.cmpfutboltfe.FIELDS";
    public static final String EXTRA_ITEM = "com.xomena.cmpfutboltfe.ITEM";

    private Map<String,List<FootballField>> ff_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // call AsyncTask to perform network operation on separate thread
        String DATA_SERVICE_URL = "https://script.google.com/macros/s/AKfycbyxqfsV0zdCKFRxgYYWPVO1PMshyhiuvTbvuKkkHjEGimPcdlpd/exec?jsonp=?";
        new HttpAsyncTask().execute(DATA_SERVICE_URL);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    // check network connection
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(MainActivity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
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
                        res = MainActivity.convertInputStreamToString(content);
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

    private void showCounties(String[] data){
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, data);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                TextView selectedView = (TextView) v;
                gotoSelectFields(selectedView.getText().toString());
            }
        });
    }

    private void gotoSelectFields(String county){
        Intent intent = new Intent(this, FieldsListActivity.class);
        intent.putExtra(EXTRA_COUNTY, county);
        if(ff_data !=null && ff_data.containsKey(county)){
            List<FootballField> ff_list = ff_data.get(county);
            intent.putParcelableArrayListExtra(EXTRA_FIELDS, new ArrayList<FootballField>(ff_list));
        }
        startActivity(intent);
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
}
