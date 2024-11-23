package com.example.TNTSMobileApp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth

class C4Pics1WordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var circleButton: ConstraintLayout
    private lateinit var profilePicture: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_c4_pics1_word, container, false)
        val btnBack: View = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            val newFragment = Challenges() // Replace with your target fragment
            (activity as MainActivity).replaceFragment(newFragment) // Call replaceFragment from MainActivity
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        circleButton = view.findViewById(R.id.circleButton)
        profilePicture = view.findViewById(R.id.profilePicture)

        // Display user's profile information in the button
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
