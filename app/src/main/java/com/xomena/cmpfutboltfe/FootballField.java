package com.xomena.cmpfutboltfe;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by andriy on 9/14/14.
 */
public class FootballField implements Parcelable {
    private String county;
    private String name;
    private String address;
    private String description;
    private String phone;
    private double lat;
    private double lng;
    private String type;
    private String placeId;
    private double accessLat;
    private double accessLng;

    /**
     * Constructor
     * @param js_val JSONArray from data source
     */
    public FootballField(JSONArray js_val){
      for(int i=0; i<js_val.length(); i++){
        if(!js_val.isNull(i)){
          try {
              switch (i) {
                  case 0:
                      this.county = js_val.getString(i);
                      break;
                  case 1:
                      this.name = js_val.getString(i);
                      break;
                  case 2:
                      this.address = js_val.getString(i);
                      break;
                  case 3:
                      this.description = js_val.getString(i);
                      break;
                  case 4:
                      this.phone = js_val.getString(i);
                      break;
                  case 5:
                      this.lat = js_val.getDouble(i);
                      break;
                  case 6:
                      this.lng = js_val.getDouble(i);
                      break;
                  case 7:
                      this.type = js_val.getString(i);
                      break;
                  case 8:
                      this.placeId = js_val.getString(i);
                      break;
                  case 9:
                      if (!js_val.isNull(i) && !"".equals(js_val.getString(i))){
                          this.accessLat = js_val.getDouble(i);
                      }
                      break;
                  case 10:
                      if (!js_val.isNull(i) && !"".equals(js_val.getString(i))){
                          this.accessLng = js_val.getDouble(i);
                      }
                      break;
                  default:
                      break;
              }
          } catch (JSONException ex){
              Log.e("[JSON Array]", "Football field constructor", ex);
          }
        }
      }
    }

    public FootballField(Parcel source) {
        this.county = source.readString();
        this.name = source.readString();
        this.address = source.readString();
        this.description = source.readString();
        this.phone = source.readString();
        this.lat = source.readDouble();
        this.lng = source.readDouble();
        this.type = source.readString();
        this.placeId = source.readString();
        this.accessLat = source.readDouble();
        this.accessLng = source.readDouble();
    }

    /**
     * Getters
     */
    public String getCounty(){
        return this.county;
    }

    public String getName(){
        return this.name;
    }

    public String getAddress(){
        return this.address;
    }

    public String getDescription(){
        return this.description;
    }

    public String getPhone(){
        return this.phone;
    }

    public double getLat(){
        return this.lat;
    }

    public double getLng(){
        return this.lng;
    }

    public String getType(){
        return this.type;
    }

    public String getPlaceId() {
        return this.placeId;
    }

    public double getAccessLat() {
        return this.accessLat;
    }

    public double getAccessLng() {
        return this.accessLng;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeString(this.county);
        p.writeString(this.name);
        p.writeString(this.address);
        p.writeString(this.description);
        p.writeString(this.phone);
        p.writeDouble(this.lat);
        p.writeDouble(this.lng);
        p.writeString(this.type);
        p.writeString(this.placeId);
        p.writeDouble(this.accessLat);
        p.writeDouble(this.accessLng);
    }

    public static final Parcelable.Creator<FootballField> CREATOR = new Parcelable.Creator<FootballField>() {

        @Override
        public FootballField createFromParcel(Parcel source) {
            return new FootballField(source);
        }

        @Override
        public FootballField[] newArray(int size) {
            return new FootballField[size];
        }

    };

}
