package com.xomena.cmpfutboltfe.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.model.County

class CountyAdapter(
    private val counties: MutableList<County>
) : RecyclerView.Adapter<CountyAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(itemView: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.county_name)

        init {
            itemView.setOnClickListener {
                listener?.onItemClick(it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_county, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val county = counties[position]
        holder.nameTextView.text = county.name
    }

    override fun getItemCount(): Int = counties.size

    fun addItem(county: County, position: Int) {
        counties.add(position, county)
        notifyItemInserted(position)
    }
}
