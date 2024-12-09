package com.example.TNTSMobileApp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MembersAdapter(private val members: List<Member>) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tvUserNames)
        val profileImageView: ImageView = itemView.findViewById(R.id.ivProfilePicture)
        val roleTextView: TextView = itemView.findViewById(R.id.tvRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        val member = members[position]

        holder.nameTextView.text = member.name
        // Show "Teacher" text for the first item (position == 0)
        if (position == 0) {
            holder.roleTextView.visibility = View.VISIBLE  // Show the "Teacher" role
        } else {
            holder.roleTextView.visibility = View.GONE  // Hide the role for other items
        }

        // Load profile picture using Glide (or Picasso)
        Glide.with(holder.itemView.context)
            .load(member.profilePictureUrl)
            .placeholder(R.drawable.custom_circle_button) // Optional placeholder
            .circleCrop()
            .into(holder.profileImageView)
    }

    override fun getItemCount(): Int = members.size

    data class Member(
        val name: String,
        val profilePictureUrl: String)
}



