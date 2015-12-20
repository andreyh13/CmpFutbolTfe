package com.xomena.cmpfutboltfe;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MyPlace {
    private static final String LOG_TAG = "MyPlace";

    private String placeID;
    private LatLng latLng;
    private String mName;
    private String address;

    public MyPlace(Set<String> place) {
        Iterator<String> it = place.iterator();

        try {
            String val = it.next();
            String[] data = val.split("###");

            placeID = data[0];
            latLng = new LatLng(Double.valueOf(data[1]), Double.valueOf(data[2]));
            mName = data[3];
            address = data[4];
        } catch (Exception e) {
            Log.e(LOG_TAG, "Bad place data", e);
        }
    }

    public String getName() {
        return mName;
    }

    public String getPlaceID() {
        return placeID;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public String getAddress() {
        return address;
    }

    public static List<MyPlace> createPlacesList(Context context) {
        List<MyPlace> places = new ArrayList<>();

        SharedPreferences sharedPref = context.getSharedPreferences(
                MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE);
        Set<String> keys = sharedPref.getStringSet(ManagePlacesActivity.STORED_KEYS, new LinkedHashSet<String>());

        for (String key : keys) {
            Set<String> pdata = sharedPref.getStringSet(key, new LinkedHashSet<String>());
            if (pdata.size() == 1) {
                places.add(new MyPlace(pdata));
            }
        }

        return places;
    }
}

