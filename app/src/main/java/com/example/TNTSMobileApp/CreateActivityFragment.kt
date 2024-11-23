package com.example.TNTSMobileApp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast

class CreateActivityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.dialog_create_activity, container, false)

//        val circleButton: View = view.findViewById(R.id.circleButton)

//        circleButton.setOnClickListener {
//            // Create and show the dialog fragment
//            val dialogFragment = LogoutDialogFragment.newInstance()
//            dialogFragment.setAnchorView(circleButton) // Set the anchor view
//            dialogFragment.show(childFragmentManager, "StudentDialog")
//        }

        //val btnBack: View = view.findViewById(R.id.btnBack)
//        btnBack.setOnClickListener {
//            val code = arguments?.getString("code") ?: "N/A"
//            val subjectName = arguments?.getString("subjectName") ?: "N/A"
//            val newFragment = SubjectDetailFragment()
//            val bundle = Bundle().apply {
//                putString("code", code)
//                putString("subjectName", subjectName)
//            }
//            newFragment.arguments = bundle
//            (activity as MainActivity).replaceFragment(newFragment)
//        }

        val btnSubmitCreatedActivity: View = view.findViewById(R.id.btnSubmitCreatedActivity)

        btnSubmitCreatedActivity.setOnClickListener {
            val activityName = view.findViewById<EditText>(R.id.etActivityName).text.toString()
            val activityDesc = view.findViewById<EditText>(R.id.etDescription).text.toString()
            val code = arguments?.getString("code") ?: "N/A"


            // Validate inputs
            if (activityName.isEmpty() || activityDesc.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields: $code", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
//            val newFragment = SubjectDetailFragment() // Replace with your target fragment
//            (activity as MainActivity).replaceFragment(newFragment) // Call replaceFragment from MainActivity
        }

        return view
    }
}
