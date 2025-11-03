package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*
import com.xomena.cmpfutboltfe.ui.adapter.*

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
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

import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ManagePlacesActivity : AppCompatActivity(),
    com.xomena.cmpfutboltfe.ui.adapter.ManagePlacesAdapter.OnItemClickListener {

    private val PLACE_PICKER_REQUEST = 1
    private lateinit var self: Activity
    private lateinit var adapter: ManagePlacesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_places)
        val toolbar = findViewById<Toolbar>(R.id.toolbarMngPlaces)
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

        val recyclerView = findViewById<RecyclerView>(R.id.rvManagePlaces)

        adapter = ManagePlacesAdapter(MyPlace.createPlacesList(this).toMutableList())
        adapter.setOnItemClickListener(this)

        recyclerView.adapter = adapter
        // Set layout manager to position the items
        recyclerView.layoutManager = LinearLayoutManager(this)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST)
        recyclerView.addItemDecoration(itemDecoration)

        self = this

        val fab = findViewById<FloatingActionButton>(R.id.fabAddPlace)
        fab.setOnClickListener {
            val builder = PlacePicker.IntentBuilder()
            try {
                startActivityForResult(builder.build(self), PLACE_PICKER_REQUEST)
            } catch (e: GooglePlayServicesRepairableException) {
                Log.e(LOG_TAG, "Play Service Exception", e)
            } catch (e: GooglePlayServicesNotAvailableException) {
                Log.e(LOG_TAG, "Play Service Not Available Exception", e)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                val place = PlacePicker.getPlace(this, data)

                val sharedPref = this.getSharedPreferences(
                    MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE
                )

                val keys = sharedPref.getStringSet(STORED_KEYS, LinkedHashSet()) ?: LinkedHashSet()
                val placeId = place.id
                if (keys.contains(placeId)) {
                    val toastMsg = String.format(getString(R.string.place_alredy_in_list), place.name)
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show()
                } else {
                    val placeData = LinkedHashSet<String>()
                    val val_str = placeId + "###" + place.latLng.latitude.toString() +
                            "###" + place.latLng.longitude.toString() +
                            "###" + place.name.toString() +
                            "###" + place.address.toString()
                    placeData.add(val_str)

                    val newKeys = LinkedHashSet(keys)
                    newKeys.add(placeId)

                    val editor = sharedPref.edit()
                    editor.putStringSet(STORED_KEYS, newKeys)
                    editor.putStringSet(placeId, placeData)
                    editor.apply()

                    val myPlace = MyPlace.fromStringSet(placeData)
                    if (myPlace != null) {
                        adapter.addItem(myPlace, adapter.itemCount)
                    }

                    val toastMsg = String.format(getString(R.string.place_added_to_list), place.name)
                    Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onRemovePlace(itemView: View, position: Int) {
        val mPlace = adapter.getItem(position)
        if (mPlace != null) {
            val fm = supportFragmentManager
            val alertDialog = RemovePlaceDialog.newInstance(
                getString(R.string.remove_place),
                mPlace.name, position
            )
            alertDialog.show(fm, "fragment_alert")
        }
    }

    fun onRemovePlaceConfirmed(position: Int) {
        val mPlace = adapter.getItem(position)
        if (mPlace != null) {
            val sharedPref = this.getSharedPreferences(
                MainActivity.PLACES_SHARED_PREF, Context.MODE_PRIVATE
            )
            val keys = sharedPref.getStringSet(STORED_KEYS, LinkedHashSet()) ?: LinkedHashSet()
            val newKeys = LinkedHashSet(keys)
            if (newKeys.contains(mPlace.placeID)) {
                newKeys.remove(mPlace.placeID)
            }

            val editor = sharedPref.edit()
            editor.putStringSet(STORED_KEYS, newKeys)
            editor.remove(mPlace.placeID)
            editor.apply()

            adapter.removeItem(position)
        }
    }

    class RemovePlaceDialog : DialogFragment() {

        @NonNull
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val title = arguments?.getString("title")
            val placeName = arguments?.getString("place_name")
            val position = arguments?.getInt("position") ?: 0
            val alertDialogBuilder = AlertDialog.Builder(requireActivity())
            alertDialogBuilder.setTitle(title)
            alertDialogBuilder.setMessage(String.format(getString(R.string.are_you_sure), placeName))
            alertDialogBuilder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                // on success
                (activity as? ManagePlacesActivity)?.onRemovePlaceConfirmed(position)
                dialog.dismiss()
            }
            alertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            return alertDialogBuilder.create()
        }

        companion object {
            fun newInstance(title: String, placeName: String, position: Int): RemovePlaceDialog {
                val frag = RemovePlaceDialog()
                val args = Bundle()
                args.putString("title", title)
                args.putString("place_name", placeName)
                args.putInt("position", position)
                frag.arguments = args
                return frag
            }
        }
    }

    companion object {
        private const val LOG_TAG = "MyPlaces"
        const val STORED_KEYS = "PLACE_IDS"
    }
}
