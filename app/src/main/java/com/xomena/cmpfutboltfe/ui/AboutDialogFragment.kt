package com.xomena.cmpfutboltfe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.xomena.cmpfutboltfe.R

/**
 * About dialog showing app information.
 * Converted from Java to Kotlin.
 */
class AboutDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_TITLE = "title"

        @JvmStatic
        fun newInstance(title: String): AboutDialogFragment {
            return AboutDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_about_dialog, container, false)

        view.findViewById<View>(R.id.about_button).setOnClickListener {
            dialog?.dismiss()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fetch arguments from bundle and set title
        val title = arguments?.getString(ARG_TITLE)
        dialog?.setTitle(title)
    }

    override fun onResume() {
        // Get existing layout params for the window
        val params = dialog?.window?.attributes
        // Assign window properties to fill the parent
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog?.window?.attributes = params as WindowManager.LayoutParams
        // Call super onResume after sizing
        super.onResume()
    }
}
