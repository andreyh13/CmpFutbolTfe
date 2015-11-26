package com.xomena.cmpfutboltfe;

import android.text.Spanned;

import java.util.ArrayList;
import java.util.List;

public class RouteStep {
    private Spanned stepText;

    public RouteStep(Spanned txt) {
        stepText = txt;
    }

    public Spanned getStepText() {
        return stepText;
    }

    public static List<RouteStep> createRouteStepsList(Spanned[] data) {
        List<RouteStep> steps = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            steps.add(new RouteStep(data[i]));
        }

        return steps;
    }
}
