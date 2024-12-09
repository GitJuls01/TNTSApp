package com.example.TNTSMobileApp

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class Home : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var circleButton: ConstraintLayout
    private lateinit var profilePicture: ImageView
    private lateinit var firestore: FirebaseFirestore // Initialize Firestore
    private lateinit var cardContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var skeletonRecyclerView: RecyclerView
    private lateinit var viewModel: HomeViewModel
    private var lastObservedClassList: List<ClassAdapter.ClassInfo> = emptyList()
    private var isFragmentInitialized = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        circleButton = view.findViewById(R.id.circleButton)
        profilePicture = view.findViewById(R.id.profilePicture)
        //cardContainer = view.findViewById(R.id.cardContainer)
        progressBar = view.findViewById(R.id.progressBar)
        val fab: View = view.findViewById(R.id.fab)

        shimmerLayout =view.findViewById(R.id.shimmerLayout)
        skeletonRecyclerView = view.findViewById(R.id.skeletonRecyclerView)
        recyclerView = view.findViewById(R.id.cardContainerRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext()) // For vertical scrolling
        // Use activityViewModels to persist the ViewModel across fragment transactions
        viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        val currentUser = auth.currentUser?.uid ?: return
        // Configure the RecyclerView
        skeletonRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        skeletonRecyclerView.adapter = SkeletonAdapter(10) // Show 10 skeleton items
        // Set an OnClickListener for the FloatingActionButton
        fab.setOnClickListener {
            showBottomSheetDialog()
        }

        // Display user's profile information in the button
        displayUserProfile()
        //fetchData()
        observeViewModel()

        // Fetch data if not already loaded
        if (viewModel.classList.value == null) {
            showSkeletonLoading()
            viewModel.fetchData(currentUser)
        }
    }

    private fun observeViewModel() {
        viewModel.classList.observe(viewLifecycleOwner) { classList ->
            if (classList.isNotEmpty()) {
                // Check if this is the first time the fragment is being loaded
                if (!isFragmentInitialized) {
                    // Initial load: No delay, just update the RecyclerView immediately
                    val adapter = ClassAdapter(classList.toMutableList(), { code ->
                        showBottomSheetMoreDialog(code)
                    }, viewModel) // Pass the HomeViewModel to the adapter
                    recyclerView.adapter = adapter
                    hideSkeletonLoading()
                    isFragmentInitialized = true // Mark the fragment as initialized
                } else {
                    // If the class list has changed, add a delay
                    if (classList != lastObservedClassList) {
                        // If the list has changed, apply a delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            val adapter = ClassAdapter(classList.toMutableList(), { code ->
                                showBottomSheetMoreDialog(code)
                            }, viewModel)

                            recyclerView.adapter = adapter
                            hideSkeletonLoading()
                        }, 1000) // 1 second delay
                    } else {
                        // If the list hasn't changed, just update the adapter without delay
                        val adapter = ClassAdapter(classList.toMutableList(), { code ->
                            showBottomSheetMoreDialog(code)
                        }, viewModel)
                        recyclerView.adapter = adapter
                        hideSkeletonLoading()
                    }
                }

                // Update the last observed class list
                lastObservedClassList = classList
            } else {
                // Handle empty state if needed
                hideSkeletonLoading()
                Toast.makeText(requireContext(), "No classes found.", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun showSkeletonLoading() {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
        recyclerView.visibility = View.GONE
    }

    private fun hideSkeletonLoading() {
        shimmerLayout.visibility = View.GONE
        shimmerLayout.stopShimmer()
        recyclerView.visibility = View.VISIBLE
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE

    }

    // Define a data class for better structure
    data class ClassInfo(
        val subjectName: String,
        val section: String,
        val createdByUserName: String,
        val code: String,
        val joinedDate: Date
    )

    private fun fetchData() {
        showSkeletonLoading()
        val currentUser = auth.currentUser?.uid ?: return

        // Initial query to retrieve documents
        firestore.collection("Classes")
            .get()
            .addOnSuccessListener { documents ->
                val classList = mutableListOf<ClassAdapter.ClassInfo>()
                for (document in documents) {
                    val subjectName = document.getString("subjectName") ?: "N/A"
                    val section = document.getString("section") ?: "N/A"
                    val createdByUserName = document.getString("createdByUserName") ?: "N/A"
                    val code = document.getString("code") ?: "N/A"
                    val members = document.get("members") as? List<Map<String, Any>>
                    if (members != null) {
                        val userEntry = members.find { it["userId"] == currentUser && it["leaveDate"] == "" }
                        if (userEntry != null) {
                            val joinedDate = (userEntry["joinedDate"] as? Timestamp)?.toDate()
                            if (joinedDate != null) {
                                classList.add(ClassAdapter.ClassInfo(subjectName, section, createdByUserName, code, joinedDate))
                            }
                        }
                    } else {
                        Log.d("FirestormDebug", "No members field or wrong format in document ID: ${document.id}")
                    }
                }

                // Sort classes by joinedDate in descending order
                classList.sortByDescending { it.joinedDate }

                // Update RecyclerView with fetched data
                // Set adapter with the classList and handle item click
//                val adapter = ClassAdapter(classList) { code ->
//                    // Call showBottomSheetMoreDialog with the class code
//                    showBottomSheetMoreDialog(code)
//                }
                //recyclerView.adapter = adapter
//                // Clear previous views to prevent duplication
//                cardContainer.removeAllViews()
//
//                // Display the sorted classes
//                for (classInfo in classList) {
//                    val cardView = createCardView(classInfo.subjectName, classInfo.section, classInfo.createdByUserName, classInfo.code)
//                    cardContainer.addView(cardView)
//                }
                // Hide the loading indicator after successful data fetch
                hideSkeletonLoading()
            }
            .addOnFailureListener { e ->
                Log.e("FirestormError", "Error fetching documents: ", e)
                Toast.makeText(requireContext(), "Error fetching classes: ${e.message}", Toast.LENGTH_SHORT).show()

            }
    }

    private fun createCardView(subjectName: String, section: String, createdByUserName: String, code: String): CardView {
        val cardView = CardView(requireContext())
        cardView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            250
        ).apply {
            setMargins(8, 8, 8, 8)
        }
        cardView.radius = 40f
        cardView.cardElevation = 5f
        cardView.setContentPadding(16, 16, 16, 16)
        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        // Horizontal layout to hold text and button
        val horizontalLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Vertical layout for the text views
        val textLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // Take up available space
            )
            setPadding(25, 0, 0, 0)
        }

        //val leftMargin = 25 // Set the left margin in pixels or use a dp conversion

        // Subject Name TextView
        val subjectNameTextView = TextView(requireContext()).apply {
            text = subjectName
            textSize = 15f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }
        // Section TextView
        val sectionTextView = TextView(requireContext()).apply {
            text = section
            textSize = 12f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10 // Set the top margin in pixels (you can convert dp to pixels if needed)
            }
        }

        val teacherTextView = TextView(requireContext()).apply {
            text = getString(R.string.teacher_name_label, createdByUserName)  // Use string resource with placeholder
            textSize = 12f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 30 // Set the top margin in pixels (you can convert dp to pixels if needed)
            }
        }

        val button = Button(requireContext()).apply {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_more_vert_24, 0, 0, 0)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                130, // Set a specific width, e.g., 80 pixels
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        }

        // Add TextViews to the text layout
        textLayout.addView(subjectNameTextView)
        textLayout.addView(sectionTextView)
        textLayout.addView(teacherTextView)

        // Add text layout and button to horizontal layout
        horizontalLayout.addView(textLayout)
        horizontalLayout.addView(button)

        // Add the horizontal layout to the card view
        cardView.addView(horizontalLayout)
        // Add TextViews to layout and layout to CardView

        // Set OnClickListener to display SubjectDetailFragment when clicked
        cardView.setOnClickListener {
            val newFragment = SubjectDetailFragment()
            val bundle = Bundle().apply {
                putString("subjectName", subjectName)
                putString("code", code)
            }
            newFragment.arguments = bundle
            (activity as MainActivity).replaceFragment(newFragment)
        }

        button.setOnClickListener {
            showBottomSheetMoreDialog(code)
        }
        return cardView
    }

    private fun generateRandomCode(length: Int = 5): String {
        val characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" // Alphanumeric characters
        return (1..length)
            .map { characters[Random.nextInt(characters.length)] } // Randomly select characters
            .joinToString("") // Join them into a single string
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

    private fun showBottomSheetMoreDialog(code: String) {
        // Create and show the BottomSheetDialog
        val bottomSheetMoreDialog = BottomSheetDialog(requireContext())
        val bottomSheetMoreView = layoutInflater.inflate(R.layout.class_more_layout, null)

        bottomSheetMoreDialog.setContentView(bottomSheetMoreView)

        // Handle button clicks inside the BottomSheetDialog
        bottomSheetMoreView.findViewById<Button>(R.id.btnCopyClassCode).setOnClickListener { // Handle Create Class button click
                // Copy the class code to the clipboard
                val clipboard =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Class Code", code)
                clipboard.setPrimaryClip(clip)

                // Show a confirmation message
                Toast.makeText(
                    requireContext(),
                    "Class code copied to clipboard",
                    Toast.LENGTH_SHORT
                ).show()

                bottomSheetMoreDialog.dismiss()
            }
        bottomSheetMoreView.findViewById<Button>(R.id.btnLeaveClass).setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: ""

            if (currentUserId.isNotEmpty()) {
                // Create the confirmation dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Leave Class")
                    .setMessage("Are you sure you want to leave the class?")
                    .setPositiveButton("Yes") { _, _ ->
                        // If the user confirms, proceed with leaving the class
                        showLoading()
                        firestore.collection("Classes")
                            .whereEqualTo(
                                "code",
                                code
                            ) // Assuming `code` is the class code you're checking
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    // If a matching class code is found, check the members field
                                    val classDocument =
                                        querySnapshot.documents[0] // Assuming one matching document
                                    val classId = classDocument.id // Get the document ID
                                    val members =
                                        classDocument.get("members") as? MutableList<Map<String, Any>>
                                            ?: mutableListOf()

                                    // Update leaveDate for the member
                                    val updatedMembers = members.map { member ->
                                        if (member["userId"] == currentUserId) {
                                            member.toMutableMap().apply {
                                                put(
                                                    "leaveDate",
                                                    Timestamp.now()
                                                ) // Set leaveDate to the current server timestamp

                                                //fetchData()  // Assuming fetchData() updates the UI or other data
                                            }
                                        } else {
                                            member
                                        }
                                    }

                                    // Update the members field in the Firestorm document
                                    firestore.collection("Classes").document(classId)
                                        .update("members", updatedMembers)
                                        .addOnSuccessListener {
                                            // Remove the class from the RecyclerView
                                            (recyclerView.adapter as? ClassAdapter)?.removeClassByCode(code)
                                            Toast.makeText(requireContext(), "You left the Class", Toast.LENGTH_SHORT).show()
                                            hideLoading()
                                        }

                                        .addOnFailureListener {
                                            e -> Toast.makeText(requireContext(), "Failed to Leave Class: ${e.message}", Toast.LENGTH_SHORT).show() }
                                } else {
                                    // If no matching class code is found
                                    Toast.makeText(requireContext(), "Class Code does not match.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error checking class code: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss() // Dismiss the dialog if the user cancels
                    }
                    .show() // Display the dialog
            } else {
                Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT)
                    .show()
            }
            // Dismiss the dialog
            bottomSheetMoreDialog.dismiss()
        }
        bottomSheetMoreView.findViewById<Button>(R.id.btnSeeMembers).setOnClickListener {
                firestore.collection("Classes")
                    .whereEqualTo("code", code)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            Toast.makeText(context, "No class found for this code.", Toast.LENGTH_SHORT).show()
                        } else {
                            val membersList = mutableListOf<MembersAdapter.Member>()

                            // Extract userId and userName from members
                            for (document in documents) {
                                val members = document.get("members") as? List<Map<String, Any>> ?: emptyList()
                                members.forEach { member ->
                                    val leaveDate = member["leaveDate"] as? String
                                    if (leaveDate == "") { // Filter members with an empty leaveDate
                                        val userName = member["userName"] as? String ?: "Unknown"
                                        val profilePictureUrl = member["profilePictureUrl"] as? String ?: ""
                                        membersList.add(MembersAdapter.Member(userName, profilePictureUrl)
                                        )
                                    }
                                }
                            }
                            if (membersList.isEmpty()) {
                                Toast.makeText(context, "No members found in this class.", Toast.LENGTH_SHORT).show()
                            } else {
                                showMembersDialog(membersList) // Show dialog with userIds.
                            }
                        }

                        bottomSheetMoreDialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to fetch data: ${e.message}", Toast.LENGTH_SHORT).show()
                        bottomSheetMoreDialog.dismiss()
                    }
            }

        bottomSheetMoreDialog.show()
    }

    private fun showMembersDialog(members: List<MembersAdapter.Member>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.see_members, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewMembers)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = MembersAdapter(members)

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun showBottomSheetDialog() {
        // Create and show the BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)

        // Handle button clicks inside the BottomSheetDialog
        bottomSheetView.findViewById<Button>(R.id.btnCreateClass).setOnClickListener {
            // Handle Create Class button click
            bottomSheetDialog.dismiss()

            // Inflate the "Create Class" dialog layout
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_class, null)

            // Create the "Create Class" dialog
            val createClassDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            createClassDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

            // Calculate the width of the dialog
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()

            createClassDialog.setOnShowListener {
                val dialogWindow = createClassDialog.window
                dialogWindow?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

             //Handle the submit button click inside the "Create Class" dialog
            dialogView.findViewById<Button>(R.id.btnSubmitClass).setOnClickListener {
                val subjectName = dialogView.findViewById<EditText>(R.id.etSubjectName).text.toString()
                val section = dialogView.findViewById<EditText>(R.id.etSection).text.toString()
                val createdByUserName = auth.currentUser?.displayName.toString()

                // Validate inputs
                if (subjectName.isBlank() || section.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }else if (section.isBlank()) {
                    Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Generate a random code
                val randomCode = generateRandomCode()

                showLoading()
                val classInfo = hashMapOf(
                    "subjectName" to subjectName,
                    "section" to section,
                    "code" to randomCode, // Save the generated code
                    "createdByUserId" to auth.currentUser?.uid, // Save the userId of the creator
                    "createdByUserName" to createdByUserName,
                    "members" to arrayListOf(
                        hashMapOf(
                            "userId" to (auth.currentUser?.uid ?: ""),
                            "joinedDate" to Timestamp.now(), // Store joined date here
                            "leaveDate" to "",
                            "userName" to (auth.currentUser?.displayName ?: ""),
                            "profilePictureUrl" to auth.currentUser?.photoUrl.toString()
                        )
                    )
                )

                firestore.collection("Classes").add(classInfo)
                    .addOnSuccessListener {

                        // Create a new ClassInfo object
                        val newClassInfo = ClassAdapter.ClassInfo(
                            subjectName = subjectName,
                            section = section,
                            createdByUserName = createdByUserName,
                            code = randomCode,
                            joinedDate = Date() // Set the current date
                        )

                        // Add the new class to the RecyclerView adapter
                        (recyclerView.adapter as? ClassAdapter)?.addClass(newClassInfo)
                        //fetchData()
//                        // After successfully saving, create a new CardView and display it
//                        val newCardView = createCardView(subjectName, section, createdByUserName, randomCode)
//                        cardContainer.addView(newCardView, 0)

                        Toast.makeText(requireContext(), "Class created Successfully", Toast.LENGTH_SHORT).show()
                        hideLoading()
                        // Clear input fields
                        dialogView.findViewById<EditText>(R.id.etSubjectName).text.clear()
                        dialogView.findViewById<EditText>(R.id.etSection).text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "${e.message}", Toast.LENGTH_SHORT).show()
                    }

                createClassDialog.dismiss()
            }

            // Show the "Create Class" dialog
            createClassDialog.show()
        }
        bottomSheetView.findViewById<Button>(R.id.btnJoinClass).setOnClickListener {
            // Handle Join Class button click
            bottomSheetDialog.dismiss()
            // Inflate the "Join Class" dialog layout
            val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_join_class, null)

            // Create the "Join Class" dialog
            val joinClassDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            joinClassDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background
            // Calculate the width of the dialog
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt()

            joinClassDialog.setOnShowListener {
                val dialogWindow = joinClassDialog.window
                dialogWindow?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            // Handle the submit button click inside the "Join Class" dialog
            dialogView.findViewById<Button>(R.id.btnSubmitCode).setOnClickListener {
                val classCode = dialogView.findViewById<EditText>(R.id.etClassCode).text.toString()

                if (classCode.isEmpty()) { Toast.makeText(requireContext(), "Please enter a class code", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val currentUserId = auth.currentUser?.uid ?: ""
                val joinedDate = Timestamp.now()
                val currentUserName = auth.currentUser?.displayName ?: ""
                val leaveDate = ""
                val profilePictureUrl = auth.currentUser?.photoUrl.toString()

                firestore.collection("Classes")
                    .whereEqualTo("code", classCode)
                    .limit(1)  // Limit to one result to prevent unnecessary updates
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            Toast.makeText(requireContext(), "Class not found", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val document = documents.documents[0]  // Get the first document

                            // Retrieve members as a list of maps, each containing userId and joinedDate
                            val members = document.get("members") as? List<Map<String, Any>> ?: emptyList()
                            val classId = document.id // Get the document ID
                            val member = members.find { it["userId"] == currentUserId }

                            if (member != null) {
                                // Check the leaveDate for the member
                                val currentLeaveDate = member["leaveDate"]
                                if (currentLeaveDate == "") {
                                    Toast.makeText(requireContext(), "You are already joined this class", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Update leaveDate to null (delete it) for the current member
                                    val updatedMembers = members.map { m ->
                                        if (m["userId"] == currentUserId) {
                                            m.toMutableMap().apply {
                                                put("leaveDate", "") // Remove the leaveDate field
                                                //fetchData()
                                            }
                                        } else {
                                            m
                                        }
                                    }
                                    firestore.collection("Classes")
                                        .document(classId)
                                        .update("members", updatedMembers)
                                        .addOnSuccessListener {
                                            showLoading()
                                            val subjectName = document.getString("subjectName") ?: "Unknown"
                                            val section = document.getString("section") ?: "Unknown"
                                            val createdByUserName = document.getString("createdByUserName") ?: "N/A"
                                            val code = document.getString("code") ?: "Unknown"

                                            // Update the existing class in RecyclerView
                                            val updatedClass = ClassAdapter.ClassInfo(
                                                subjectName = subjectName,
                                                section = section,
                                                createdByUserName = createdByUserName,
                                                code = code,
                                                joinedDate = joinedDate.toDate()
                                            )
                                            (recyclerView.adapter as? ClassAdapter)?.addClass(updatedClass)
                                            Toast.makeText(requireContext(), "Rejoin the Class Successfully", Toast.LENGTH_SHORT).show()
                                            hideLoading()
                                        }
                                }
                            }else {
                                // If not a member, create a new member entry with userId and joinedDate
                                showLoading()
                                val newMember = mapOf(
                                    "userId" to currentUserId,
                                    "joinedDate" to joinedDate,
                                    "userName" to currentUserName,
                                    "leaveDate" to leaveDate,
                                    "profilePictureUrl" to profilePictureUrl
                                )
                                // Add the new member to the members array in Firestorm
                                document.reference.update("members", FieldValue.arrayUnion(newMember))
                                    .addOnSuccessListener {
                                        // Retrieve class details to display in a CardView
                                        val subjectName = document.getString("subjectName") ?: "Unknown"
                                        val section = document.getString("section") ?: "Unknown"
                                        val createdByUserName = document.getString("createdByUserName") ?: "N/A"
                                        val code = document.getString("code") ?: "Unknown"

                                        //fetchData()
//                                        // Call function to create and display a new CardView
//                                        val newCardView = createCardView(subjectName, section, createdByUserName, code)
//                                        cardContainer.addView(newCardView, 0)
                                        // Add the new class to RecyclerView
                                        val newClass = ClassAdapter.ClassInfo(
                                            subjectName = subjectName,
                                            section = section,
                                            createdByUserName = createdByUserName,
                                            code = code,
                                            joinedDate = joinedDate.toDate()
                                        )
                                        (recyclerView.adapter as? ClassAdapter)?.addClass(newClass)

                                        Toast.makeText(requireContext(), "Joined Class Successfully", Toast.LENGTH_SHORT).show()
                                        hideLoading()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                // Dismiss the Join Class dialog
                joinClassDialog.dismiss()
            }
            // Show the "Join Class" dialog
            joinClassDialog.show()
        }
            bottomSheetView.findViewById<Button>(R.id.btnUploadFile).setOnClickListener {
            // Handle Upload File button click
            bottomSheetDialog.dismiss()
            val dialogView =
                LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_file, null)

            // Create the "Upload File" dialog
            val uploadFileDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            uploadFileDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

            // Calculate the width of the dialog
            val displayMetrics = requireContext(). resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt ()

            uploadFileDialog.setOnShowListener {
                val dialogWindow = uploadFileDialog.window
                dialogWindow?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            // Handle the Choose File button click (currently doing nothing or can show a Toast)
            dialogView.findViewById<Button>(R.id.btnChooseFile).setOnClickListener {
                // Placeholder action, can show a Toast or do nothing
                Toast.makeText(requireContext(), "Choose File button clicked", Toast.LENGTH_SHORT).show()
            }

            // Show the "Upload File" dialog
            uploadFileDialog.show()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }
}