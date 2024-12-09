package com.example.TNTSMobileApp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

class ClassAdapter(private var classList: MutableList<ClassInfo>, private val onMoreClick: (String) -> Unit,
                   private val homeViewModel: HomeViewModel // Pass HomeViewModel to update LiveData
                    ) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectNameTextView: TextView = itemView.findViewById(R.id.subjectName)
        val sectionTextView: TextView = itemView.findViewById(R.id.tvSection)
        val createdByTextView: TextView = itemView.findViewById(R.id.tvTeacher)
        val moreButton: ImageView = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.create_class_cardview, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classInfo = classList[position]
        val context = holder.itemView.context

        holder.subjectNameTextView.text = classInfo.subjectName
        holder.sectionTextView.text = classInfo.section
        holder.createdByTextView.text = context.getString(R.string.teacher_name_label, classInfo.createdByUserName)

        // Handle card view click
        holder.itemView.setOnClickListener {
            // Create a new instance of SubjectDetailFragment
            val newFragment = SubjectDetailFragment()
            val bundle = Bundle().apply {
                putString("subjectName", classInfo.subjectName)
                putString("code", classInfo.code)
            }
            newFragment.arguments = bundle

            // Replace the fragment
            val activity = context as? MainActivity
            activity?.replaceFragment(newFragment)
        }
        // Set the OnClickListener for the "More" button (ImageView)
        holder.moreButton.setOnClickListener {
            onMoreClick(classInfo.code) // Trigger the callback with the classInfo
        }
    }

    override fun getItemCount(): Int = classList.size

    // New method to add, join and rejoin a class
    fun addClass(classInfo: ClassInfo) {
        classList.add(0, classInfo) // Add to the top of the list
        notifyItemInserted(0) // Notify RecyclerView about the new item
        homeViewModel.addClass(classInfo) // Update the ViewModel

    }
    // New method to remove a class by code
    fun removeClassByCode(code: String) {
        val index = classList.indexOfFirst { it.code == code }
        if (index != -1) {
            classList.removeAt(index)
            notifyItemRemoved(index)
            homeViewModel.removeClassByCode(code) // Update the ViewModel
        }
    }


    data class ClassInfo(
        val subjectName: String,
        val section: String,
        val createdByUserName: String,
        val code: String,
        val joinedDate: Date?
    )
}

