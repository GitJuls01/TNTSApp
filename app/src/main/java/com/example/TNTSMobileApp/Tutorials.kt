package com.example.TNTSMobileApp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide

class Tutorials : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var circleButton: ConstraintLayout
    private lateinit var profilePicture: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tutorials, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        circleButton = view.findViewById(R.id.circleButton)
        profilePicture = view.findViewById(R.id.profilePicture)

        val circleButton: View = view.findViewById(R.id.circleButton)

        circleButton.setOnClickListener {
            // Create and show the dialog fragment
            val dialogFragment = LogoutDialogFragment.newInstance()
            dialogFragment.setAnchorView(circleButton) // Set the anchor view
            dialogFragment.show(childFragmentManager, "StudentDialog")
        }

        val firstCardView: View = view.findViewById(R.id.firstCardView)

        firstCardView.setOnClickListener {
            // Use the replaceFragment method from MainActivity to navigate
            val newFragment = TIntroductionHardwareFragment() // Replace with your target fragment
            (activity as MainActivity).replaceFragment(newFragment) // Call replaceFragment from MainActivity
        }

        val secondCardView: View = view.findViewById(R.id.secondCardView)

        secondCardView.setOnClickListener {
            // Use the replaceFragment method from MainActivity to navigate
            val newFragment = TBasicOperatingSystemGuidesFragment() // Replace with your target fragment
            (activity as MainActivity).replaceFragment(newFragment) // Call replaceFragment from MainActivity
        }

        val thirdCardView: View = view.findViewById(R.id.thirdCardView)

        thirdCardView.setOnClickListener {
            // Use the replaceFragment method from MainActivity to navigate
            val newFragment = TIntroductionInHtmlFragment() // Replace with your target fragment
            (activity as MainActivity).replaceFragment(newFragment) // Call replaceFragment from MainActivity
        }

        val fourthCardView: View = view.findViewById(R.id.fourthCardView)

        fourthCardView.setOnClickListener {
            // Use the replaceFragment method from MainActivity to navigate
            val newFragment = TAdobePhotoshopFragment() // Replace with your target fragment
            (activity as MainActivity).replaceFragment(newFragment) // Call replaceFragment from MainActivity
        }


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
