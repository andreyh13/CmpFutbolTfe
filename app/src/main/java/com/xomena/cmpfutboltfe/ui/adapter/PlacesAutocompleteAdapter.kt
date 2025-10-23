package com.xomena.cmpfutboltfe.ui.adapter

/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.content.Context
import android.graphics.Typeface
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.data.DataBufferUtils
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.AutocompletePrediction
import com.google.android.gms.location.places.Places
import com.google.android.gms.maps.model.LatLngBounds
import java.util.concurrent.TimeUnit

/**
 * Adapter that handles Autocomplete requests from the Places Geo Data API.
 * [AutocompletePrediction] results from the API are frozen and stored directly in this adapter.
 *
 * Note that this adapter requires a valid [GoogleApiClient].
 * The API client must be maintained in the encapsulating Activity, including all lifecycle and
 * connection states. The API client must be connected with the [Places.GEO_DATA_API] API.
 */
@Suppress("DEPRECATION")
class PlacesAutocompleteAdapter(
    context: Context,
    private val googleApiClient: GoogleApiClient,
    private var bounds: LatLngBounds?,
    private val placeFilter: AutocompleteFilter?
) : ArrayAdapter<AutocompletePrediction>(
    context,
    android.R.layout.simple_expandable_list_item_2,
    android.R.id.text1
), Filterable {

    /**
     * Current results returned by this adapter.
     */
    private var resultList: ArrayList<AutocompletePrediction>? = null

    /**
     * Sets the bounds for all subsequent queries.
     */
    fun setBounds(bounds: LatLngBounds?) {
        this.bounds = bounds
    }

    /**
     * Returns the number of results received in the last autocomplete query.
     */
    override fun getCount(): Int {
        return resultList?.size ?: 0
    }

    /**
     * Returns an item from the last autocomplete query.
     */
    override fun getItem(position: Int): AutocompletePrediction? {
        return resultList?.getOrNull(position)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = super.getView(position, convertView, parent)

        // Sets the primary and secondary text for a row.
        // Note that getPrimaryText() and getSecondaryText() return a CharSequence that may contain
        // styling based on the given CharacterStyle.
        val item = getItem(position)

        val textView1 = row.findViewById<TextView>(android.R.id.text1)
        val textView2 = row.findViewById<TextView>(android.R.id.text2)
        
        textView1.text = item?.getPrimaryText(STYLE_BOLD)
        textView1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        textView2.text = item?.getSecondaryText(STYLE_BOLD)

        return row
    }

    /**
     * Returns the filter for the current set of autocomplete results.
     */
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                // Skip the autocomplete query if no constraints are given.
                if (constraint != null) {
                    // Query the autocomplete API for the (constraint) search string.
                    resultList = getAutocomplete(constraint)
                    if (resultList != null) {
                        // The API successfully returned results.
                        results.values = resultList
                        results.count = resultList!!.size
                    }
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    // The API returned at least one result, update the data.
                    notifyDataSetChanged()
                } else {
                    // The API did not return any results, invalidate the data set.
                    notifyDataSetInvalidated()
                }
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                // Override this method to display a readable result in the AutocompleteTextView
                // when clicked.
                return if (resultValue is AutocompletePrediction) {
                    resultValue.getFullText(null)
                } else {
                    super.convertResultToString(resultValue)
                }
            }
        }
    }

    /**
     * Submits an autocomplete query to the Places Geo Data Autocomplete API.
     * Results are returned as frozen AutocompletePrediction objects, ready to be cached.
     * Returns an empty list if no results were found.
     * Returns null if the API client is not available or the query did not complete successfully.
     * This method MUST be called off the main UI thread, as it will block until data is returned
     * from the API, which may include a network request.
     *
     * @param constraint Autocomplete query string
     * @return Results from the autocomplete API or null if the query was not successful.
     */
    private fun getAutocomplete(constraint: CharSequence): ArrayList<AutocompletePrediction>? {
        if (googleApiClient.isConnected) {
            Log.i(TAG, "Starting autocomplete query for: $constraint")

            // Submit the query to the autocomplete API and retrieve a PendingResult that will
            // contain the results when the query completes.
            val results = Places.GeoDataApi.getAutocompletePredictions(
                googleApiClient,
                constraint.toString(),
                bounds,
                placeFilter
            )

            // This method should have been called off the main UI thread. Block and wait for at most 60s
            // for a result from the API.
            val autocompletePredictions = results.await(60, TimeUnit.SECONDS)

            // Confirm that the query completed successfully, otherwise return null
            val status = autocompletePredictions.status
            if (!status.isSuccess) {
                Toast.makeText(
                    context,
                    "Error contacting API: $status",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Error getting autocomplete prediction API call: $status")
                autocompletePredictions.release()
                return null
            }

            Log.i(
                TAG,
                "Query completed. Received ${autocompletePredictions.count} predictions."
            )

            // Freeze the results immutable representation that can be stored safely.
            return DataBufferUtils.freezeAndClose(autocompletePredictions)
        }
        Log.e(TAG, "Google API client is not connected for autocomplete query.")
        return null
    }

    companion object {
        private const val TAG = "AutocompleteAdapter"
        private val STYLE_BOLD = StyleSpan(Typeface.BOLD)
    }
}
