package com.xomena.cmpfutboltfe.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.xomena.cmpfutboltfe.R
import org.json.JSONObject

/**
 * A simple Fragment for displaying directions map.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface to handle interaction events.
 */
class DirectionsMapFragment : Fragment() {

    private var map: GoogleMap? = null
    private var jsonObject: JSONObject? = null
    private var listener: OnFragmentInteractionListener? = null

    fun setJSON(json: JSONObject) {
        this.jsonObject = json
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_directions_map, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnFragmentInteractionListener
            ?: throw ClassCastException("$context must implement OnFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that activity.
     */
    interface OnFragmentInteractionListener {
        fun onDirectionsMapClick()
        fun onRouteAnimationClick()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of this fragment.
         *
         * @return A new instance of fragment DirectionsMapFragment.
         */
        @JvmStatic
        fun newInstance() = DirectionsMapFragment()
    }
}
