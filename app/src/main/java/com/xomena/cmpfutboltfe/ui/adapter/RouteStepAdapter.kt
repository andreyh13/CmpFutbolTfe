package com.xomena.cmpfutboltfe.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.xomena.cmpfutboltfe.R
import com.xomena.cmpfutboltfe.model.RouteStep

class RouteStepAdapter(
    private val steps: MutableList<RouteStep>
) : RecyclerView.Adapter<RouteStepAdapter.ViewHolder>() {

    private var listener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(itemView: View, position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stepTextView: TextView = itemView.findViewById(R.id.route_step)

        init {
            itemView.setOnClickListener {
                listener?.onItemClick(it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_step, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val step = steps[position]
        holder.stepTextView.text = step.stepText
    }

    override fun getItemCount(): Int = steps.size

    fun addItem(step: RouteStep, position: Int) {
        steps.add(position, step)
        notifyItemInserted(position)
    }
}
