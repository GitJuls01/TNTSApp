package com.example.TNTSMobileApp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class SkeletonAdapter(private val itemCount: Int) :
    RecyclerView.Adapter<SkeletonAdapter.SkeletonViewHolder>() {

    class SkeletonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skeleton_cardview_item, parent, false)
        return SkeletonViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {
        // No binding required for static skeleton views
    }

    override fun getItemCount(): Int = itemCount
}
