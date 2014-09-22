package com.xomena.cmpfutboltfe;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class FieldDetailActivity extends ActionBarActivity {
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_detail);

        TextView title = (TextView)findViewById(R.id.fieldDetailCaption);

        Intent i = getIntent();
        FootballField ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM);

        title.setText(ff.getName());

        setUpMapIfNeeded(ff);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.field_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded(FootballField ff) {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.ff_map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                // The Map is verified. It is now safe to manipulate the map.
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ff.getLat(),ff.getLng())));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                mMap.addMarker(new MarkerOptions().position(new LatLng(ff.getLat(),ff.getLng()))
                .title(ff.getName()).snippet(getString(R.string.phoneLabel)+" "+ff.getPhone())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_soccerfield)));
            }
        }
    }
}
