package com.xomena.cmpfutboltfe.ui.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.xomena.cmpfutboltfe.MainMapFragment
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.ui.fragment.CountiesFragment

@Suppress("DEPRECATION")
class MainPagerAdapter(
    fragmentManager: FragmentManager,
    private val context: Context
) : FragmentPagerAdapter(fragmentManager) {

    private val tabTitles = intArrayOf(R.string.select_county, R.string.route_map)

    override fun getCount(): Int = PAGE_COUNT

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> CountiesFragment.newInstance()
            1 -> MainMapFragment.newInstance()
            else -> CountiesFragment.newInstance() // Default fallback
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return context.getString(tabTitles[position])
    }

    companion object {
        private const val PAGE_COUNT = 2
    }
}
