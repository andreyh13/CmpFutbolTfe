package com.xomena.cmpfutboltfe;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import java.util.LinkedHashSet;
import java.util.Set;

public class SelectPlaceActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
            SelectPlacesAdapter.OnItemClickListener {

    protected GoogleApiClient mGoogleApiClient;
    private PlacesAutocompleteAdapter mAdapter;
    private SelectPlacesAdapter adapter;
    private static SelectPlaceActivity self;
    private final static String LOG_TAG = "SelectPlaceActivity";
    protected static final String STORED_KEYS = "PLACE_IDS";

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            FragmentManager fm = getSupportFragmentManager();
            SavePlaceDialog alertDialog = SavePlaceDialog.newInstance(getString(R.string.title_activity_select_place),
                    primaryText.toString(), placeId);
            alertDialog.show(fm, "fragment_alert");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        self = this;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();

        setContentView(R.layout.activity_select_place);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarSelectPlace);
        setSupportActionBar(toolbar);
        try {
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setDisplayShowTitleEnabled(false);
            }
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, "Exception", e);
        }

        final Drawable upArrow = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_left,
                getApplicationContext().getTheme());
        toolbar.setNavigationIcon(upArrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        AutoCompleteTextView mAutocompleteView = (AutoCompleteTextView) findViewById(R.id.fieldFindPlace);
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);
        mAdapter = new PlacesAutocompleteAdapter(this, mGoogleApiClient, MainActivity.BOUNDS_TENERIFE, null);
        mAutocompleteView.setAdapter(mAdapter);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rvSelectPlace);

        adapter = new SelectPlacesAdapter(MyPlace.createPlacesList(this));
        adapter.setOnItemClickListener(this);

        recyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.e(LOG_TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onItemClick(View itemView, int position) {
        MyPlace place = adapter.getItem(position);
        if (place != null) {
            returnPlace(place.getPlaceID(), place.getAddress());
        } else {
            Intent resultIntent = new Intent();
            setResult(RESULT_CANCELED, resultIntent);
            finish();
        }
    }

    private void savePlace (String placeId, String placeName) {
        final SharedPreferences sharedPref = this.getSharedPreferences(
                MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE);

        final Set<String> keys = sharedPref.getStringSet(STORED_KEYS, new LinkedHashSet<String>());
        if (keys.contains(placeId)) {
            String toastMsg = String.format(getString(R.string.place_alredy_in_list), placeName);
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
        } else {
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
                @Override
                public void onResult(@NonNull PlaceBuffer places) {
                    if (places.getStatus().isSuccess()) {
                        Place place = places.get(0);

                        Set<String> placeData = new LinkedHashSet<>();
                        String val = place.getId() + "###" + String.valueOf(place.getLatLng().latitude) +
                                "###" + String.valueOf(place.getLatLng().longitude) +
                                "###" + place.getName().toString() +
                                "###" + place.getAddress().toString();
                        placeData.add(val);

                        keys.add(place.getId());

                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putStringSet(STORED_KEYS, keys);
                        editor.putStringSet(place.getId(), placeData);
                        editor.apply();

                        String toastMsg = String.format(getString(R.string.place_added_to_list), place.getName());
                        Toast.makeText(self, toastMsg, Toast.LENGTH_LONG).show();
                    }
                    //Release the PlaceBuffer to prevent a memory leak
                    places.release();
                }
            });
        }
    }

    private void returnPlace (String placeId, String placeAddress) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(MainActivity.EXTRA_PLACEID, placeId);
        resultIntent.putExtra(MainActivity.EXTRA_ADDRESS, placeAddress);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    public static class SavePlaceDialog extends DialogFragment {
        public SavePlaceDialog() {
            // Empty constructor required for DialogFragment
        }

        public static SavePlaceDialog newInstance(String title, String placeName, String placeId) {
            SavePlaceDialog frag = new SavePlaceDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("place_name", placeName);
            args.putString("place_id", placeId);
            frag.setArguments(args);
            return frag;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            final String placeName = getArguments().getString("place_name");
            final String placeId = getArguments().getString("place_id");
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(title);
            alertDialogBuilder.setMessage(String.format(getString(R.string.save_this_place), placeName));
            alertDialogBuilder.setPositiveButton(getString(R.string.ok),  new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // on success
                    self.savePlace(placeId, placeName);
                    self.returnPlace(placeId, placeName);
                    dialog.dismiss();
                }
            });
            alertDialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    self.returnPlace(placeId, placeName);
                    dialog.dismiss();
                }
            });

            return alertDialogBuilder.create();
        }
    }

}
