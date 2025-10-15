package com.xomena.cmpfutboltfe.model

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException

@Parcelize
data class FootballField(
    val county: String = "",
    val name: String = "",
    val address: String = "",
    val description: String = "",
    val phone: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val type: String = "",
    val placeId: String = "",
    val accessLat: Double = 0.0,
    val accessLng: Double = 0.0
) : Parcelable {

    companion object {
        private const val LOG_TAG = "[JSON Array]"

        @JvmStatic
        fun fromJSONArray(jsVal: JSONArray): FootballField {
            var county = ""
            var name = ""
            var address = ""
            var description = ""
            var phone = ""
            var lat = 0.0
            var lng = 0.0
            var type = ""
            var placeId = ""
            var accessLat = 0.0
            var accessLng = 0.0

            for (i in 0 until jsVal.length()) {
                if (!jsVal.isNull(i)) {
                    try {
                        when (i) {
                            0 -> county = jsVal.getString(i)
                            1 -> name = jsVal.getString(i)
                            2 -> address = jsVal.getString(i)
                            3 -> description = jsVal.getString(i)
                            4 -> phone = jsVal.getString(i)
                            5 -> lat = jsVal.getDouble(i)
                            6 -> lng = jsVal.getDouble(i)
                            7 -> type = jsVal.getString(i)
                            8 -> placeId = jsVal.getString(i)
                            9 -> {
                                if (!jsVal.isNull(i) && jsVal.getString(i).isNotEmpty()) {
                                    accessLat = jsVal.getDouble(i)
                                }
                            }
                            10 -> {
                                if (!jsVal.isNull(i) && jsVal.getString(i).isNotEmpty()) {
                                    accessLng = jsVal.getDouble(i)
                                }
                            }
                        }
                    } catch (ex: JSONException) {
                        Log.e(LOG_TAG, "Football field constructor", ex)
                    }
                }
            }

            return FootballField(
                county, name, address, description, phone,
                lat, lng, type, placeId, accessLat, accessLng
            )
        }
    }
}
