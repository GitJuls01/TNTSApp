package com.example.TNTSMobileApp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SubjectDetailFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var circleButton: ConstraintLayout
    private lateinit var profilePicture: ImageView
    private lateinit var subjectNameTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var cardContainer: LinearLayout
    private lateinit var fabActivity: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_subject_detail, container, false)

        fabActivity = view.findViewById(R.id.fabActivity)
        fabActivity.setOnClickListener {
            createActivity()
        }

        val btnBack: View = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            val newFragment = Home()
            (activity as MainActivity).replaceFragment(newFragment)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        circleButton = view.findViewById(R.id.circleButton)
        profilePicture = view.findViewById(R.id.profilePicture)
        subjectNameTextView = view.findViewById(R.id.classSubjectNameText)
        firestore = FirebaseFirestore.getInstance()
        cardContainer = view.findViewById(R.id.cardContainer)

        // Retrieve data from arguments and set to TextView
        subjectNameTextView.text = arguments?.getString("subjectName") ?: "N/A"

        displayUserProfile()
        checkIfUserIsCreator()
        fetchData()
    }

    private fun fetchData() {
        val classCode = arguments?.getString("code") ?: "N/A"
        val currentUser = auth.currentUser?.uid ?: return

        firestore.collection("Classes")
            .whereEqualTo("code", classCode)
            .get()
            .addOnSuccessListener { classDocuments ->
                if (classDocuments.isEmpty) { Toast.makeText(requireContext(), "Class not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val classDocument = classDocuments.first()
                val classCreator = classDocument.getString("createdByUserId") ?: ""

                firestore.collection("Activities")
                    .whereEqualTo("code", classCode)
                    .orderBy("createdDate", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { activityDocuments ->
                        if (activityDocuments.isEmpty) {
                            val message = if (currentUser == classCreator) {
                                "No activities found. Add Activities here"
                            } else {
                                "No activities found. Ask your teacher"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        for (activityDocument in activityDocuments) {
                            val activityName = activityDocument.getString("activityName") ?: "N/A"
                            val teacherName = activityDocument.getString("createdByUserName") ?: "N/A"
                            val activityDesc = activityDocument.getString("activityDesc") ?: "N/A"
                            val cardView = createCardView(activityName, teacherName, activityDesc)
                            cardContainer.addView(cardView)
                        }
                    }
                    .addOnFailureListener { exception -> Toast.makeText(requireContext(), "Failed to load activities: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { exception -> Toast.makeText(requireContext(), "Failed to load class data: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun createCardView(activityName: String, teacherName: String, activityDesc: String): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                250
            ).apply {
                setMargins(8, 8, 8, 8)
            }
            radius = 40f
            cardElevation = 5f
            setContentPadding(16, 16, 16, 16)
            setCardBackgroundColor(Color.parseColor("#FFEBEE"))
        }

        // Create a layout inside the CardView for the TextViews
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT // Fill parent height to allow vertical centering
            )
        }

        // Set padding as needed
        val leftMargin = 25

        // Activity Name TextView
        val activityNameTextView = TextView(requireContext()).apply {
            text = activityName
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(leftMargin, 0, 0, 0)
        }

        // Teacher Name TextView
        val teacherNameTextView = TextView(requireContext()).apply {
            text = getString(R.string.teacher_name_label, teacherName)  // Use string resource with placeholder teacherName
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(leftMargin, 0, 0, 0)
        }

        val activityDescTextView = TextView(requireContext()).apply {
            text = activityDesc
            textSize = 12f
            setTextColor(Color.BLACK)
        }

        layout.addView(activityNameTextView)
        layout.addView(teacherNameTextView)
        cardView.addView(layout)

        //Set OnClickListener to display SubjectDetailFragment when clicked
        cardView.setOnClickListener {
            val code = arguments?.getString("code") ?: "N/A"
            val subjectName = arguments?.getString("subjectName") ?: "N/A"
            val newFragment = ActivityDetailFragment()
            val bundle = Bundle().apply {
                putString("activityName", activityName)
                putString("teacherName", teacherName)
                putString("activityDesc", activityDesc)
                putString("subjectName", subjectName)
                putString("code", code)
            }
            newFragment.arguments = bundle
            (activity as MainActivity).replaceFragment(newFragment)
        }

        return cardView
    }

    private fun checkIfUserIsCreator() {
        val currentUser = auth.currentUser?.uid ?: return

        firestore.collection("Classes")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val code = document.getString("code") ?: ""
                    val classCode = arguments?.getString("code") ?: "N/A"
                    if (code== classCode) {
                        val createdBy = document.getString("createdByUserId") ?: ""
                        // Show FAB if current user is the creator
                        fabActivity.visibility = if (createdBy == currentUser) View.VISIBLE else View.GONE
                    }
                }
            }
            .addOnFailureListener {
                fabActivity.visibility = View.GONE // Hide FAB on error
            }
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

    private fun createActivity() {
        // Inflate the Create Activity dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_activity, null)
        val btnUploadFile: Button = dialogView.findViewById(R.id.btnUploadFile)

        // Create the "Create Activity" dialog
        val createClassDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        createClassDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        btnUploadFile.setOnClickListener {
            // Handle file upload logic here
            Toast.makeText(requireContext(), "File upload not implemented", Toast.LENGTH_SHORT).show()
        }

        //Handle the submit button click inside the "Create Class" dialog
        dialogView.findViewById<Button>(R.id.btnSubmitCreatedActivity).setOnClickListener {
            val activityName = dialogView.findViewById<EditText>(R.id.etActivityName).text.toString()
            val activityDesc = dialogView.findViewById<EditText>(R.id.etDescription).text.toString()
            val code = arguments?.getString("code") ?: "N/A"

            // Validate inputs
            if (activityName.isEmpty() || activityDesc.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Fetch the createdByUserName based on the class code
            firestore.collection("Classes")
                .whereEqualTo("code", code)
                .get()
                .addOnSuccessListener { classDocuments ->
                    if (classDocuments.isEmpty) {
                        Toast.makeText(requireContext(), "Class not found for the provided code", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Retrieve createdByUserName from the class document
                    val classDocument = classDocuments.first()
                    val createdByUserName = classDocument.getString("createdByUserName") ?: "Unknown"

                    // Prepare activity information
                    val activityInfo = hashMapOf(
                        "activityName" to activityName,
                        "activityDesc" to activityDesc,
                        "createdByUserId" to auth.currentUser?.uid,
                        "createdByUserName" to auth.currentUser?.displayName,
                        "code" to code,
                        "createdDate"  to Timestamp.now()
                    )

                    // Add activity to Firestore
                    firestore.collection("Activities").add(activityInfo)
                        .addOnSuccessListener {
                            // After saving, create a new CardView with activityName and createdByUserName
                            val newCardView = createCardView(activityName, createdByUserName, activityDesc)
                            cardContainer.addView(newCardView, 0)

                            Toast.makeText(requireContext(), "Activity Created Successfully", Toast.LENGTH_SHORT).show()

                            // Clear input fields
                            dialogView.findViewById<EditText>(R.id.etActivityName).text.clear()
                            dialogView.findViewById<EditText>(R.id.etDescription).text.clear()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to save activity: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to load class data: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            createClassDialog.dismiss()
        }
        // Show the "Create Activity" dialog
        createClassDialog.show()
    }

}