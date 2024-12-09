package com.example.TNTSMobileApp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ActivityAdapter(
    private var activityList: MutableList<ActivityInfo>,
    private val auth: FirebaseAuth,
    private val onMoreClick: (String, String) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    // Remove an activity from the list by activityId
    fun removeActivity(activityId: String) {
        val index = activityList.indexOfFirst { it.activityId == activityId }
        if (index != -1) {
            activityList.removeAt(index) // Remove the item
            notifyItemRemoved(index) // Notify adapter of the removal
        }
    }

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val activityNameTextView: TextView = itemView.findViewById(R.id.tvActivityName)
        val postedTextView: TextView = itemView.findViewById(R.id.tvPosted)
        val dueTextView: TextView = itemView.findViewById(R.id.tvDueDate)
        val moreButton: ImageView = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.create_activity_cardview, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activityInfo = activityList[position]
        val context = holder.itemView.context

        holder.activityNameTextView.text = context.getString(R.string.activity_name_label,activityInfo.activityName)
        holder.postedTextView.text = context.getString(R.string.created_date_label,activityInfo.formattedCreatedDate)
        holder.dueTextView.text = context.getString(R.string.due_date_label,activityInfo.formattedDueDate)

        // Handle card view click
        holder.itemView.setOnClickListener {
            // Create a new instance of ActivityDetailFragment
            val newFragment = ActivityDetailFragment()

            val bundle = Bundle().apply {
                putString("activityName", activityInfo.activityName)
                putString("activityDesc", activityInfo.activityDesc)
                putString("subjectName", activityInfo.subjectName)
                putString("code", activityInfo.code)
            }
            newFragment.arguments = bundle

            // Replace the fragment
            val activity = context as? MainActivity
            activity?.replaceFragment(newFragment)
        }

        // Set button visibility based on Firestorm data
        val currentUser = auth.currentUser?.uid
        val createdBy = activityInfo.classCreator
        holder.moreButton.visibility = if (createdBy == currentUser) View.VISIBLE else View.GONE

        // Set the OnClickListener for the "More" button (ImageView)
        holder.moreButton.setOnClickListener {
            onMoreClick(activityInfo.code,activityInfo.activityId) // Trigger the callback with the activityInfo
        }
    }

    override fun getItemCount(): Int = activityList.size

    // New method to add, join and rejoin a class
    fun addClass(classInfo: ActivityInfo) {
        activityList.add(0, classInfo) // Add to the top of the list
        notifyItemInserted(0) // Notify RecyclerView about the new item
        //homeViewModel.addClass(classInfo) // Update the ViewModel

    }
    // Update the list and notify the adapter
    fun updateData(newActivityList: MutableList<ActivityInfo>) {
        activityList = newActivityList
        notifyDataSetChanged()  // Notify that data has changed
    }

    data class ActivityInfo(
        val activityName: String,
        val formattedCreatedDate: String,
        val formattedDueDate: String,
        val activityDesc: String,
        val code: String,
        val activityId: String,
        val subjectName: String,
        val classCreator: String
    )

}
