package com.xomena.cmpfutboltfe.model

import android.content.Context
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.xomena.cmpfutboltfe.MainActivity
import com.xomena.cmpfutboltfe.ManagePlacesActivity

data class MyPlace(
    val placeID: String,
    val latLng: LatLng,
    val name: String,
    val address: String
) {
    companion object {
        private const val LOG_TAG = "MyPlace"

        @JvmStatic
        fun fromStringSet(place: Set<String>): MyPlace? {
            return try {
                val data = place.first().split("###")
                MyPlace(
                    placeID = data[0],
                    latLng = LatLng(data[1].toDouble(), data[2].toDouble()),
                    name = data[3],
                    address = data[4]
                )
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Bad place data", e)
                null
            }
        }

        @JvmStatic
        fun createPlacesList(context: Context): List<MyPlace> {
            val sharedPref = context.getSharedPreferences(
                MainActivity.PLACES_SHARED_PREF,
                Context.MODE_PRIVATE
            )
            val keys = sharedPref.getStringSet(
                ManagePlacesActivity.STORED_KEYS,
                linkedSetOf()
            ) ?: emptySet()

            return keys.mapNotNull { key ->
                val pdata = sharedPref.getStringSet(key, linkedSetOf()) ?: emptySet()
                if (pdata.size == 1) {
                    fromStringSet(pdata)
                } else {
                    null
                }
            }
        }
    }
}
