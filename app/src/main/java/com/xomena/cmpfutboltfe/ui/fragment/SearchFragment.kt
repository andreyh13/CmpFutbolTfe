package com.xomena.cmpfutboltfe.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.xomena.cmpfutboltfe.R

/**
 * A simple Fragment for search functionality.
 * Activities that contain this fragment must implement the
 * [OnFragmentInteractionListener] interface to handle interaction events.
 */
class SearchFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    fun onFieldFound() {
        listener?.onSearchFootballField()
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
        fun onSearchFootballField()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of this fragment.
         *
         * @return A new instance of fragment SearchFragment.
         */
        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}
