package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*
import com.xomena.cmpfutboltfe.ui.adapter.*

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.Toast

import androidx.annotation.NonNull
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.location.places.AutocompletePrediction
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.PlaceBuffer
import com.google.android.gms.location.places.Places

class SelectPlaceActivity : AppCompatActivity(),
    GoogleApiClient.OnConnectionFailedListener,
    SelectPlacesAdapter.OnItemClickListener {

    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mAdapter: PlacesAutocompleteAdapter
    private lateinit var adapter: SelectPlacesAdapter

    private val mAutocompleteClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        /*
         Retrieve the place ID of the selected item from the Adapter.
         The adapter stores each Place suggestion in a AutocompletePrediction from which we
         read the place ID and title.
          */
        val item = mAdapter.getItem(position)
        val placeId = item?.placeId
        val primaryText = item?.getPrimaryText(null)

        if (placeId != null && primaryText != null) {
            val fm = supportFragmentManager
            val alertDialog = SavePlaceDialog.newInstance(
                getString(R.string.title_activity_select_place),
                primaryText.toString(), placeId
            )
            alertDialog.show(fm, "fragment_alert")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        self = this

        mGoogleApiClient = GoogleApiClient.Builder(this)
            .enableAutoManage(this, 0 /* clientId */, this)
            .addApi(Places.GEO_DATA_API)
            .build()

        setContentView(R.layout.activity_select_place)
        val toolbar = findViewById<Toolbar>(R.id.toolbarSelectPlace)
        setSupportActionBar(toolbar)
        try {
            val ab = supportActionBar
            ab?.setDisplayShowTitleEnabled(false)
        } catch (e: NullPointerException) {
            Log.e(LOG_TAG, "Exception", e)
        }

        val upArrow = ResourcesCompat.getDrawable(
            resources, R.drawable.arrow_left,
            applicationContext.theme
        )
        toolbar.navigationIcon = upArrow
        toolbar.setNavigationOnClickListener { onBackPressed() }

        val mAutocompleteView = findViewById<AutoCompleteTextView>(R.id.fieldFindPlace)
        mAutocompleteView.onItemClickListener = mAutocompleteClickListener
        mAdapter = PlacesAutocompleteAdapter(this, mGoogleApiClient, MainActivity.BOUNDS_TENERIFE, null)
        mAutocompleteView.setAdapter(mAdapter)

        val recyclerView = findViewById<RecyclerView>(R.id.rvSelectPlace)

        adapter = SelectPlacesAdapter(MyPlace.createPlacesList(this))
        adapter.setOnItemClickListener(this)

        recyclerView.adapter = adapter
        // Set layout manager to position the items
        recyclerView.layoutManager = LinearLayoutManager(this)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST)
        recyclerView.addItemDecoration(itemDecoration)
    }

    override fun onConnectionFailed(@NonNull connectionResult: ConnectionResult) {
        Log.e(
            LOG_TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                    + connectionResult.errorCode
        )

        Toast.makeText(
            this,
            "Could not connect to Google API Client: Error " + connectionResult.errorCode,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onItemClick(itemView: View, position: Int) {
        val place = adapter.getItem(position)
        if (place != null) {
            returnPlace(place.placeID, place.address)
        } else {
            val resultIntent = Intent()
            setResult(RESULT_CANCELED, resultIntent)
            finish()
        }
    }

    private fun savePlace(placeId: String, placeName: String) {
        val sharedPref = this.getSharedPreferences(
            MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE
        )

        val keys = sharedPref.getStringSet(STORED_KEYS, LinkedHashSet()) ?: LinkedHashSet()
        if (keys.contains(placeId)) {
            val toastMsg = String.format(getString(R.string.place_alredy_in_list), placeName)
            Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show()
        } else {
            Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId)
                .setResultCallback(object : ResultCallback<PlaceBuffer> {
                    override fun onResult(@NonNull places: PlaceBuffer) {
                        if (places.status.isSuccess) {
                            val place = places[0]

                            val placeData = LinkedHashSet<String>()
                            val val_str = place.id + "###" + place.latLng.latitude.toString() +
                                    "###" + place.latLng.longitude.toString() +
                                    "###" + place.name.toString() +
                                    "###" + place.address.toString()
                            placeData.add(val_str)

                            val newKeys = LinkedHashSet(keys)
                            newKeys.add(place.id)

                            val editor = sharedPref.edit()
                            editor.putStringSet(STORED_KEYS, newKeys)
                            editor.putStringSet(place.id, placeData)
                            editor.apply()

                            val toastMsg = String.format(getString(R.string.place_added_to_list), place.name)
                            Toast.makeText(self, toastMsg, Toast.LENGTH_LONG).show()
                        }
                        //Release the PlaceBuffer to prevent a memory leak
                        places.release()
                    }
                })
        }
    }

    private fun returnPlace(placeId: String?, placeAddress: String?) {
        val resultIntent = Intent()
        resultIntent.putExtra(MainActivity.EXTRA_PLACEID, placeId)
        resultIntent.putExtra(MainActivity.EXTRA_ADDRESS, placeAddress)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    class SavePlaceDialog : DialogFragment() {

        @NonNull
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val title = arguments?.getString("title")
            val placeName = arguments?.getString("place_name")
            val placeId = arguments?.getString("place_id")
            val alertDialogBuilder = AlertDialog.Builder(requireActivity())
            alertDialogBuilder.setTitle(title)
            alertDialogBuilder.setMessage(String.format(getString(R.string.save_this_place), placeName))
            alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                // on success
                if (placeId != null && placeName != null) {
                    self?.savePlace(placeId, placeName)
                    self?.returnPlace(placeId, placeName)
                }
                dialog.dismiss()
            }
            alertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                if (placeId != null && placeName != null) {
                    self?.returnPlace(placeId, placeName)
                }
                dialog.dismiss()
            }

            return alertDialogBuilder.create()
        }

        companion object {
            fun newInstance(title: String, placeName: String, placeId: String): SavePlaceDialog {
                val frag = SavePlaceDialog()
                val args = Bundle()
                args.putString("title", title)
                args.putString("place_name", placeName)
                args.putString("place_id", placeId)
                frag.arguments = args
                return frag
            }
        }
    }

    companion object {
        private var self: SelectPlaceActivity? = null
        private const val LOG_TAG = "SelectPlaceActivity"
        const val STORED_KEYS = "PLACE_IDS"
    }
}
