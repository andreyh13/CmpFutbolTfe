package com.xomena.cmpfutboltfe;

import android.util.Log;

import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public class WebServiceExec {
    protected static final int WS_TYPE_GEOCODE = 1;
    protected static final int WS_TYPE_DIRECTIONS = 2;
    protected static final int WS_TYPE_ROADS = 3;

    private static final String LOG_TAG = "WebServiceExec";

    private int type;
    private String url;
    private int attempt;
    private OnWebServiceResult mListener;

    public WebServiceExec (int type, String url, OnWebServiceResult listener) {
        this.type = type;
        this.url = url;
        this.attempt = 0;
        this.mListener = listener;
    }

    public void executeWS() {
        if (this.attempt < MainActivity.API_KEYS.length) {
            String m_url = this.url + "&key=" + MainActivity.API_KEYS[this.attempt];
            AndroidHttpClient httpClient = new AndroidHttpClient(MainActivity.XOMENA_DOMAIN);
            httpClient.setMaxRetries(5);
            ParameterMap params = httpClient.newParams()
                    .add("uri", m_url)
                    .add("version", "free")
                    .add("output", "json");
            httpClient.post(MainActivity.XOMENA_WS_PROXY, params, new AsyncCallback() {
                @Override
                public void onComplete(HttpResponse httpResponse) {
                    JSONObject res = null;
                    try {
                        // Create a JSON object hierarchy from the results
                        if (httpResponse != null) {
                            res = new JSONObject(httpResponse.getBodyAsString());
                        }
                        if (attempt == MainActivity.API_KEYS.length - 1) {
                            notifyListener(res);
                        } else {
                            if (res != null && res.has("status") && res.getString("status").equals("OVER_QUERY_LIMIT")) {
                                attempt++;
                                executeWS();
                            } else {
                                notifyListener(res);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Cannot process JSON results", e);
                        notifyListener(res);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error", e);
                        notifyListener(res);
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(LOG_TAG, "Error in HTTP POST", e);
                }
            });
        }
    }

    public interface OnWebServiceResult {
        void onRoadsResult(JSONObject res);
        void onGeocodeResult(JSONObject res);
        void onDirectionsResult(JSONObject res);
    }

    private void notifyListener (JSONObject res) {
        switch (this.type) {
            case WS_TYPE_GEOCODE:
                this.mListener.onGeocodeResult(res);
                break;
            case WS_TYPE_DIRECTIONS:
                this.mListener.onDirectionsResult(res);
                break;
            case WS_TYPE_ROADS:
                this.mListener.onRoadsResult(res);
                break;
        }
    }
}
