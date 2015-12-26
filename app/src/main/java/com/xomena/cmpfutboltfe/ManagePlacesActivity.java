package com.xomena.cmpfutboltfe;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.LinkedHashSet;
import java.util.Set;

public class ManagePlacesActivity extends AppCompatActivity
            implements ManagePlacesAdapter.OnItemClickListener {
    private static final String LOG_TAG = "MyPlaces";

    protected static final String STORED_KEYS = "PLACE_IDS";

    private int PLACE_PICKER_REQUEST = 1;
    private Activity self;
    ManagePlacesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_places);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarMngPlaces);
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

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rvManagePlaces);

        adapter = new ManagePlacesAdapter(MyPlace.createPlacesList(this));
        adapter.setOnItemClickListener(this);

        recyclerView.setAdapter(adapter);
        // Set layout manager to position the items
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        self = this;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fabAddPlace);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(builder.build(self), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.e(LOG_TAG, "Play Service Exception", e);
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.e(LOG_TAG, "Play Service Not Available Exception", e);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);

                SharedPreferences sharedPref = this.getSharedPreferences(
                        MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE);

                Set<String> keys = sharedPref.getStringSet(STORED_KEYS, new LinkedHashSet<String>());
                String placeId = place.getId();
                if (keys.contains(placeId)) {
                    String toastMsg = String.format(getString(R.string.place_alredy_in_list), place.getName());
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                } else {
                    Set<String> placeData = new LinkedHashSet<>();
                    String val = placeId + "###" + String.valueOf(place.getLatLng().latitude) +
                            "###" + String.valueOf(place.getLatLng().longitude) +
                            "###" + place.getName().toString() +
                            "###" + place.getAddress().toString();
                    placeData.add(val);

                    keys.add(placeId);

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putStringSet(STORED_KEYS, keys);
                    editor.putStringSet(placeId, placeData);
                    editor.apply();

                    adapter.addItem(new MyPlace(placeData), adapter.getItemCount());

                    String toastMsg = String.format(getString(R.string.place_added_to_list), place.getName());
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onRemovePlace(View itemView, int position) {
        MyPlace mPlace = adapter.getItem(position);
        if (mPlace != null) {
            FragmentManager fm = getSupportFragmentManager();
            RemovePlaceDialog alertDialog = RemovePlaceDialog.newInstance(getString(R.string.remove_place),
                    mPlace.getName(), position);
            alertDialog.show(fm, "fragment_alert");
        }
    }

    public void onRemovePlaceConfirmed(int position) {
        MyPlace mPlace = adapter.getItem(position);
        if (mPlace != null) {
            SharedPreferences sharedPref = this.getSharedPreferences(
                    MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE);
            Set<String> keys = sharedPref.getStringSet(STORED_KEYS, new LinkedHashSet<String>());
            if (keys.contains(mPlace.getPlaceID())) {
                keys.remove(mPlace.getPlaceID());
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putStringSet(STORED_KEYS, keys);
            editor.remove(mPlace.getPlaceID());
            editor.apply();

            adapter.removeItem(position);
        }
    }

    public static class RemovePlaceDialog extends DialogFragment {
        public RemovePlaceDialog() {
            // Empty constructor required for DialogFragment
        }

        public static RemovePlaceDialog newInstance(String title, String placeName, int position) {
            RemovePlaceDialog frag = new RemovePlaceDialog();
            Bundle args = new Bundle();
            args.putString("title", title);
            args.putString("place_name", placeName);
            args.putInt("position", position);
            frag.setArguments(args);
            return frag;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String title = getArguments().getString("title");
            String placeName = getArguments().getString("place_name");
            final int position = getArguments().getInt("position");
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(title);
            alertDialogBuilder.setMessage(String.format(getString(R.string.are_you_sure), placeName));
            alertDialogBuilder.setPositiveButton(getString(R.string.ok),  new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // on success
                    ((ManagePlacesActivity)getActivity()).onRemovePlaceConfirmed(position);
                    dialog.dismiss();
                }
            });
            alertDialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            return alertDialogBuilder.create();
        }
    }
}
