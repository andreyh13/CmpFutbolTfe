package com.xomena.cmpfutboltfe

import com.xomena.cmpfutboltfe.model.*
import com.xomena.cmpfutboltfe.util.*
import com.xomena.cmpfutboltfe.ui.adapter.*

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.gms.maps.model.LatLng

import org.json.JSONException
import org.json.JSONObject

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.LinkedList

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [RouteMapFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [RouteMapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RouteMapFragment : Fragment(), RouteStepAdapter.OnItemClickListener,
    WebServiceExec.OnWebServiceResult {

    private var jsonRoute: JSONObject? = null
    private var stepLatLng: MutableList<LatLng> = LinkedList()
    private var ff_lat: Double = 0.0
    private var ff_lng: Double = 0.0
    private var origPlaceId: String? = null
    private var address: String? = null
    private var destPlaceId: String? = null
    private var ff: FootballField? = null
    private var enc_polyline: String? = null

    private var mListener: OnFragmentInteractionListener? = null
    private var adapter: RouteStepAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val i = requireActivity().intent
        ff = i.getParcelableExtra(MainActivity.EXTRA_ITEM)

        address = i.getStringExtra(MainActivity.EXTRA_ADDRESS)
        origPlaceId = i.getStringExtra(MainActivity.EXTRA_PLACEID)
        ff_lat = ff?.lat ?: 0.0
        ff_lng = ff?.lng ?: 0.0
        destPlaceId = ff?.placeId

        getRoute(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val frameRoute = inflater.inflate(R.layout.fragment_route_map, container, false) as FrameLayout
        val rvRoute = frameRoute.findViewById<RecyclerView>(R.id.rvRoute)
        val items = emptyArray<Spanned>()
        // Create adapter passing in the sample user data
        adapter = RouteStepAdapter(RouteStep.createRouteStepsList(items).toMutableList())
        adapter?.setOnItemClickListener(this)
        // Attach the adapter to the recyclerview to populate items
        rvRoute.adapter = adapter
        // Set layout manager to position the items
        rvRoute.layoutManager = LinearLayoutManager(requireActivity())
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL_LIST)
        rvRoute.addItemDecoration(itemDecoration)

        return frameRoute
    }

    override fun onItemClick(itemView: View, position: Int) {
        val stepTextView = itemView.findViewById<TextView>(R.id.route_step)
        if (stepTextView != null) {
            val intent = Intent(requireActivity(), StreetViewRouteStepActivity::class.java)
            val location = stepLatLng[position]
            intent.putExtra(MainActivity.SV_LAT, location.latitude)
            intent.putExtra(MainActivity.SV_LNG, location.longitude)
            intent.putExtra(MainActivity.EXTRA_ITEM, ff)
            intent.putExtra(MainActivity.EXTRA_ENC_POLY, enc_polyline)
            intent.putExtra("ROUTE_STEP_DESCR", stepTextView.text.toString())

            startActivity(intent)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val a: Activity = if (context is Activity) {
            context
        } else {
            requireActivity()
        }

        try {
            mListener = a as OnFragmentInteractionListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                a.toString() + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun exchangeJSON(json: JSONObject?)
    }

    private fun processJSONObject() {
        val route = jsonRoute ?: return
        try {
            if (!route.has("status") || route.getString("status") != "OK") return
            if (!route.has("routes")) return
            
            val rts = route.getJSONArray("routes")
            if (rts.length() == 0 || rts.isNull(0)) return
            
            val r = rts.getJSONObject(0)

                            if (r.has("overview_polyline") && !r.isNull("overview_polyline")) {
                                val m_poly = r.getJSONObject("overview_polyline")
                                if (m_poly.has("points") && !m_poly.isNull("points")) {
                                    this.enc_polyline = m_poly.getString("points")
                                }
                            }

                            val listSteps = ArrayList<Spanned>()

                            //Title
                            if (r.has("summary") && !r.isNull("summary")) {
                                val v = view
                                if (v != null) {
                                    val title = view?.findViewById<TextView>(R.id.routeTitle)
                                    title?.text = r.getString("summary")
                                }
                            }

                            //Warnings
                            if (r.has("warnings") && !r.isNull("warnings")) {
                                val aw = r.getJSONArray("warnings")
                                if (aw.length() > 0) {
                                    var w = ""
                                    for (i in 0 until aw.length()) {
                                        if (!aw.isNull(i)) {
                                            w += aw.getString(i) + "\n"
                                        }
                                    }
                                    if (w != "") {
                                        val warn = view?.findViewById<TextView>(R.id.routeWarnings)
                                        warn?.text = w
                                    }
                                }
                            }

                            //Legs
                            if (r.has("legs") && !r.isNull("legs")) {
                                val al = r.getJSONArray("legs")
                                for (i in 0 until al.length()) {
                                    if (!al.isNull(i)) {
                                        val l = al.getJSONObject(i)

                                        //Distance and duration
                                        var dd = ""
                                        if (i == 0 && l.has("distance") && !l.isNull("distance")) {
                                            dd += getString(R.string.distance) + ": " + l.getJSONObject("distance")
                                                .getString("text") + "  "
                                        }
                                        if (i == 0 && l.has("duration") && !l.isNull("duration")) {
                                            dd += getString(R.string.duration) + ": " + l.getJSONObject("duration")
                                                .getString("text")
                                        }
                                        if (dd != "") {
                                            val ddtv = view?.findViewById<TextView>(R.id.routeDistanceDuration)
                                            ddtv?.text = dd
                                        }

                                        //Start address
                                        if (i == 0 && l.has("start_address") && !l.isNull("start_address")) {
                                            listSteps.add(Html.fromHtml(l.getString("start_address")))
                                            if (l.has("start_location") && !l.isNull("start_location")) {
                                                stepLatLng.add(
                                                    LatLng(
                                                        l.getJSONObject("start_location").getDouble("lat"),
                                                        l.getJSONObject("start_location").getDouble("lng")
                                                    )
                                                )
                                            }
                                        }

                                        //Steps
                                        if (l.has("steps") && !l.isNull("steps")) {
                                            val as_steps = l.getJSONArray("steps")
                                            for (k in 0 until as_steps.length()) {
                                                if (!as_steps.isNull(k)) {
                                                    val s = as_steps.getJSONObject(k)
                                                    var m_step = ""
                                                    if (s.has("html_instructions") && !s.isNull("html_instructions")) {
                                                        m_step += s.getString("html_instructions") + "<br/>"
                                                    }
                                                    if (s.has("distance") && !s.isNull("distance")) {
                                                        m_step += getString(R.string.distance) + ": " + s.getJSONObject(
                                                            "distance"
                                                        ).getString("text") + "  "
                                                    }
                                                    if (s.has("duration") && !s.isNull("duration")) {
                                                        m_step += getString(R.string.duration) + ": " + s.getJSONObject(
                                                            "duration"
                                                        ).getString("text")
                                                    }
                                                    if (m_step != "") {
                                                        if (s.has("start_location") && !s.isNull("start_location")) {
                                                            stepLatLng.add(
                                                                LatLng(
                                                                    s.getJSONObject("start_location").getDouble("lat"),
                                                                    s.getJSONObject("start_location").getDouble("lng")
                                                                )
                                                            )
                                                        }
                                                        listSteps.add(Html.fromHtml(m_step))
                                                    }
                                                }
                                            }
                                        }

                                        //End address
                                        if (i == al.length() - 1 && l.has("end_address") && !l.isNull("end_address")) {
                                            listSteps.add(Html.fromHtml(l.getString("end_address")))
                                            if (l.has("end_location") && !l.isNull("end_location")) {
                                                stepLatLng.add(
                                                    LatLng(
                                                        l.getJSONObject("end_location").getDouble("lat"),
                                                        l.getJSONObject("end_location").getDouble("lng")
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val data = listSteps.toTypedArray()
                            if (data.isNotEmpty()) {
                                for (i in data.indices) {
                                    adapter?.addItem(RouteStep(data[i]), i)
                                }
                            }

                            //Copyrights
                            if (r.has("copyrights") && !r.isNull("copyrights")) {
                                val cpr = view?.findViewById<TextView>(R.id.routeCopyright)
                                cpr?.text = r.getString("copyrights")
                            }
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "Cannot process JSON results", e)
        }
    }

    override fun onRoadsResult(res: JSONObject?) {
        //Empty method
    }

    override fun onGeocodeResult(res: JSONObject?) {
        //Empty method
    }

    override fun onDirectionsResult(res: JSONObject?) {
        // Create a JSON object hierarchy from the results
        try {
            if (res?.has("status") == true && res.getString("status") == "NOT_FOUND") {
                getRoute(true)
            } else {
                jsonRoute = res
                processJSONObject()
                mListener?.exchangeJSON(jsonRoute)
            }
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "JSON exception", e)
        }
    }

    private fun getRoute(noPlaces: Boolean) {
        try {
            val orig: String = if (noPlaces) {
                URLEncoder.encode(address, "utf8")
            } else {
                if (origPlaceId != null && origPlaceId != "") {
                    "place_id:$origPlaceId"
                } else {
                    URLEncoder.encode(address, "utf8")
                }
            }
            
            val dest: String = if (noPlaces) {
                "$ff_lat,$ff_lng"
            } else {
                if (destPlaceId != null && destPlaceId != "") {
                    "place_id:$destPlaceId"
                } else {
                    "$ff_lat,$ff_lng"
                }
            }

            val m_url = MainActivity.DIRECTIONS_API_BASE + MainActivity.OUT_JSON +
                    "?origin=" + orig + "&destination=" + dest + "&language=es&units=metric&region=es"
            val m_exec = WebServiceExec(WebServiceExec.WS_TYPE_DIRECTIONS, m_url, this)
            m_exec.executeWS()
        } catch (e: UnsupportedEncodingException) {
            Log.e(LOG_TAG, "Error processing Directions API URL", e)
        }
    }

    companion object {
        private const val LOG_TAG = "RouteMapFragment"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment RouteMapFragment.
         */
        fun newInstance(): RouteMapFragment {
            return RouteMapFragment()
        }
    }
}
