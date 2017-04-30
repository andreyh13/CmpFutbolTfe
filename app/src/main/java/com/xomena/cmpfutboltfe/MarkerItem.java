package com.xomena.cmpfutboltfe;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * CmpFutbolTfe
 * Created by andriy on 11/23/14.
 */
public class MarkerItem implements ClusterItem {
    private final LatLng mPosition;
    private String name;
    private String snippet;
    private FootballField ff;

    public MarkerItem(double lat, double lng) {
        mPosition = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public void setName(String  n){
        this.name = n;
    }

    public void setSnippet(String s){
        this.snippet = s;
    }

    public void setFootballField(FootballField f){
        this.ff = f;
    }

    public String getName(){
        return this.name;
    }

    public String getTitle() { return this.name; }

    public String getSnippet(){
        return this.snippet;
    }

    public FootballField getFootballField(){
        return this.ff;
    }
}
