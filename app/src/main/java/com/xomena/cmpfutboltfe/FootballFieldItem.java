package com.xomena.cmpfutboltfe;

import java.util.ArrayList;
import java.util.List;

public class FootballFieldItem {
    private String name;
    private String address;
    private String phone;

    public FootballFieldItem(String n, String a, String p){
        this.name = n;
        this.address = a;
        this.phone = p;
    }

    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

    public String getPhone() {
        return this.phone;
    }

    public static List<FootballFieldItem> createPitchesList(List<FootballField> ff_data) {
        List<FootballFieldItem> pitches = new ArrayList<>(ff_data.size());

        if (ff_data.size() > 0) {
            for (FootballField f : ff_data) {
                pitches.add(new FootballFieldItem(f.getName(), f.getAddress(), f.getPhone()));
            }
        }

        return pitches;
    }

}
