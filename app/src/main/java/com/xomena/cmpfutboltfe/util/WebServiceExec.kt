package com.xomena.cmpfutboltfe.util

import android.util.Log
import com.turbomanage.httpclient.AsyncCallback
import com.turbomanage.httpclient.HttpResponse
import com.turbomanage.httpclient.android.AndroidHttpClient
import com.xomena.cmpfutboltfe.MainActivity
import org.json.JSONException
import org.json.JSONObject

class WebServiceExec(
    private val type: Int,
    private val url: String,
    private val listener: OnWebServiceResult
) {
    private var attempt = 0

    fun executeWS() {
        if (attempt < MainActivity.API_KEYS.size) {
            val fullUrl = "$url&key=${MainActivity.API_KEYS[attempt]}"
            val httpClient = AndroidHttpClient(MainActivity.XOMENA_DOMAIN)
            httpClient.setMaxRetries(5)
            val params = httpClient.newParams()
                .add("uri", fullUrl)
                .add("version", "free")
                .add("output", "json")

            httpClient.post(MainActivity.XOMENA_WS_PROXY, params, object : AsyncCallback() {
                override fun onComplete(httpResponse: HttpResponse?) {
                    var res: JSONObject? = null
                    try {
                        // Create a JSON object hierarchy from the results
                        if (httpResponse != null) {
                            res = JSONObject(httpResponse.bodyAsString)
                        }
                        if (attempt == MainActivity.API_KEYS.size - 1) {
                            notifyListener(res)
                        } else {
                            if (res != null && res.has("status") && 
                                res.getString("status") == "OVER_QUERY_LIMIT") {
                                attempt++
                                executeWS()
                            } else {
                                notifyListener(res)
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e(LOG_TAG, "Cannot process JSON results", e)
                        notifyListener(res)
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Error", e)
                        notifyListener(res)
                    }
                }

                override fun onError(e: Exception?) {
                    Log.e(LOG_TAG, "Error in HTTP POST", e)
                }
            })
        }
    }

    interface OnWebServiceResult {
        fun onRoadsResult(res: JSONObject?)
        fun onGeocodeResult(res: JSONObject?)
        fun onDirectionsResult(res: JSONObject?)
    }

    private fun notifyListener(res: JSONObject?) {
        when (type) {
            WS_TYPE_GEOCODE -> listener.onGeocodeResult(res)
            WS_TYPE_DIRECTIONS -> listener.onDirectionsResult(res)
            WS_TYPE_ROADS -> listener.onRoadsResult(res)
        }
    }

    companion object {
        const val WS_TYPE_GEOCODE = 1
        const val WS_TYPE_DIRECTIONS = 2
        const val WS_TYPE_ROADS = 3

        private const val LOG_TAG = "WebServiceExec"
    }
}
