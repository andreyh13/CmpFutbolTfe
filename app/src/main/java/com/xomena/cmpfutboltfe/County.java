package com.xomena.cmpfutboltfe;

import java.util.ArrayList;
import java.util.List;

public class County {
    private String mName;

    public County(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    private static int lastCountyId = 0;

    public static List<County> createCountiesList(String[] data) {
        List<County> counties = new ArrayList<County>();

        for (int i = 0; i < data.length; i++) {
            counties.add(new County(data[i]));
        }

        return counties;
    }
}
