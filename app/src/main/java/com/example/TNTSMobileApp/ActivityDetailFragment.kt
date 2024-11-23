package com.example.TNTSMobileApp

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class ActivityDetailFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var circleButton: ConstraintLayout
    private lateinit var profilePicture: ImageView
    private lateinit var activityNameTextView: TextView
    private lateinit var teacherNameTextView: TextView
    private lateinit var descriptionTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_activity_detail, container, false)

        val btnBack: View = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            val code = arguments?.getString("code") ?: "N/A"
            val subjectName = arguments?.getString("subjectName") ?: "N/A"
            val newFragment = SubjectDetailFragment()
            val bundle = Bundle().apply {
                putString("code", code)
                putString("subjectName", subjectName)
            }
            newFragment.arguments = bundle
            (activity as MainActivity).replaceFragment(newFragment)
        }

        return view
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        circleButton = view.findViewById(R.id.circleButton)
        profilePicture = view.findViewById(R.id.profilePicture)
        activityNameTextView = view.findViewById(R.id.classText)
        teacherNameTextView = view.findViewById(R.id.teacher)
        descriptionTextView = view.findViewById(R.id.description)

        // Retrieve data from arguments and set to TextView
        activityNameTextView.text = arguments?.getString("activityName") ?: "N/A"
        teacherNameTextView.text = String.format("Teacher: %s", arguments?.getString("teacherName") ?: "N/A")
        descriptionTextView.text = String.format("Description: %s", arguments?.getString("activityDesc") ?: "N/A")


        displayUserProfile()

    }

    private fun displayUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userProfilePictureUrl = currentUser.photoUrl

            // Load the user's profile picture into the button
            if (userProfilePictureUrl != null) {
                Glide.with(requireContext())
                    .load(userProfilePictureUrl)
                    .circleCrop()
                    .into(profilePicture)

                // Add a click listener to the profile picture
                profilePicture.setOnClickListener {
                    showLogoutDialog()
                }
            }
        }
    }

    private fun showLogoutDialog() {
        // Get the current user
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Create an instance of the LogoutDialogFragment
            val logoutDialog = LogoutDialogFragment.newInstance()
            logoutDialog.setAnchorView(profilePicture) // Set the anchor view
            logoutDialog.setUserProfilePictureUrl(currentUser.photoUrl.toString()) // Pass the user profile picture URL
            logoutDialog.setUsername("Hello, ${currentUser.displayName ?: "Unknown User"}!")
            logoutDialog.show(childFragmentManager, "LogoutDialogFragment")
        } else {
            // add else here
        }
    }



}
