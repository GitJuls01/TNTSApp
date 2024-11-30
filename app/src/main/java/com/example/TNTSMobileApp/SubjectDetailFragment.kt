package com.example.TNTSMobileApp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SubjectDetailFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var circleButton: ConstraintLayout
    private lateinit var profilePicture: ImageView
    private lateinit var subjectNameTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var cardContainer: LinearLayout
    private lateinit var fabActivity: FloatingActionButton
    private var fileUri: Uri? = null
    private lateinit var fileNameTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSetDueDate: Button
    private var dueDate: String ? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_subject_detail, container, false)
        progressBar = view.findViewById(R.id.progressBar)

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
        showLoading()
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun fetchData() {

        val classCode = arguments?.getString("code") ?: "N/A"
        val currentUser = auth.currentUser?.uid ?: return

        // Clear the container before fetching new data
        cardContainer.removeAllViews()
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
                            hideLoading() // Hide loading after handling empty activities
                            return@addOnSuccessListener
                        }

                        for (activityDocument in activityDocuments) {
                            val activityName = activityDocument.getString("activityName") ?: "N/A"
                            val activityDesc = activityDocument.getString("activityDesc") ?: "N/A"
                            val activityId = activityDocument.getString("activityId") ?: "N/A"
                            val createdDate = activityDocument.getTimestamp("createdDate")
                            val formattedCreatedDate = formatCreatedDate(createdDate)
                            val dueDate = activityDocument.getString("dueDate")
                            val formattedDueDate = formatDueDate(dueDate)

                            val cardView = createCardView(activityName, formattedCreatedDate, formattedDueDate, activityDesc, activityId)
                            cardContainer.addView(cardView)
                        }
                        hideLoading() // Hide loading after successfully adding activities
                    }
                    .addOnFailureListener { exception -> Toast.makeText(requireContext(), "Failed to load activities: ${exception.message}", Toast.LENGTH_SHORT).show() }
                hideLoading()
            }
            .addOnFailureListener { exception -> Toast.makeText(requireContext(), "Failed to load class data: ${exception.message}", Toast.LENGTH_SHORT).show() }
        hideLoading()
    }

    private fun formatDueDate(dueDate: String?): String {
        return try {
            val dateFormat = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault()) // Adjust format as needed
            val date = dateFormat.parse(dueDate ?: return "No Due Date")
            val now = Calendar.getInstance()
            val dueDateCalendar = Calendar.getInstance().apply {
                if (date != null) {
                    time = date
                }
            }

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // 12-hour format with AM/PM

            when {
                // Check if the due date is today
                now.get(Calendar.YEAR) == dueDateCalendar.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == dueDateCalendar.get(Calendar.DAY_OF_YEAR) -> {
                    "Today, ${date?.let { timeFormat.format(it) }}"
                }
                // Check if the due date is tomorrow
                now.get(Calendar.YEAR) == dueDateCalendar.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) + 1 == dueDateCalendar.get(Calendar.DAY_OF_YEAR) -> {
                    "Tomorrow, ${date?.let { timeFormat.format(it) }}"
                }
                // Show only the date for other days
                else -> {
                    val formattedDate = date?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(
                            it
                        )
                    }
                    val formattedTime = date?.let { timeFormat.format(it) } // Format the time as per device settings
                    "$formattedDate $formattedTime"
                }

            }
        } catch (e: Exception) {
            "Fallback if parsing fails" // Fallback if parsing fails
        }
    }

    private fun formatCreatedDate(timestamp: Timestamp?): String {
        return timestamp?.toDate()?.let { date ->
            val now = Calendar.getInstance()
            val postedTime = Calendar.getInstance().apply { time = date }

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // Use 12-hour format with AM/PM

            when {
                // Check if posted today
                now.get(Calendar.YEAR) == postedTime.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) == postedTime.get(Calendar.DAY_OF_YEAR) -> {
                    timeFormat.format(date) // Show time only
                }
                // Check if posted yesterday
                now.get(Calendar.YEAR) == postedTime.get(Calendar.YEAR) &&
                        now.get(Calendar.DAY_OF_YEAR) - postedTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
                    "Yesterday, ${timeFormat.format(date)}" // Show "Yesterday, time"
                }
                // Show full date for older posts
                else -> {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date) // Format as "MMM dd, yyyy"
                }
            }
        } ?: "N/A"
    }

    private fun createCardView(activityName: String, createdDate: String, dueDate: String, activityDesc: String, activityId: String): CardView {
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

        // Set padding as needed
        //val leftMargin = 25

        // Activity Name TextView
        val activityNameTextView = TextView(requireContext()).apply {
            text = getString(R.string.activity_name_label, activityName)
            textSize = 14f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        // Teacher Name TextView
        val createdDateTextView = TextView(requireContext()).apply {
            text = getString(R.string.created_date_label, createdDate)  // Use string resource with placeholder teacherName
            textSize = 12f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10 // Set the top margin in pixels (you can convert dp to pixels if needed)
            }
        }

        val dueDateTextView = TextView(requireContext()).apply {
            text = getString(R.string.due_date_label, dueDate)  // Use string resource with placeholder teacherName
            textSize = 12f
            setTextColor(Color.DKGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10 // Set the top margin in pixels (you can convert dp to pixels if needed)
            }
        }


        val button = Button(requireContext()).apply {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.baseline_more_vert_24, 0, 0, 0)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                130, // Set a specific width, e.g., 80 pixels
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE // Initially hidden
        }

        // Fetch the creator information to decide button visibility
        val checkCode = arguments?.getString("code") ?: "N/A"
        val currentUser = auth.currentUser?.uid
        firestore.collection("Classes")
            .whereEqualTo("code", checkCode)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val createdBy = document.getString("createdByUserId") ?: ""
                    button.visibility = if (createdBy == currentUser) View.VISIBLE else View.GONE
                }
            }
            .addOnFailureListener {
                button.visibility = View.GONE // Hide button on error
            }

        textLayout.addView(activityNameTextView)
        textLayout.addView(createdDateTextView)
        textLayout.addView(dueDateTextView)

        // Add text layout and button to horizontal layout
        horizontalLayout.addView(textLayout)
        horizontalLayout.addView(button)

        // Add the horizontal layout to the card view
        cardView.addView(horizontalLayout)

        //Set OnClickListener to display SubjectDetailFragment when clicked
        cardView.setOnClickListener {
            val code = arguments?.getString("code") ?: "N/A"
            val subjectName = arguments?.getString("subjectName") ?: "N/A"
            val newFragment = ActivityDetailFragment()
            val bundle = Bundle().apply {
                putString("activityName", activityName)
                putString("activityDesc", activityDesc)
                putString("subjectName", subjectName)
                putString("code", code)
            }
            newFragment.arguments = bundle
            (activity as MainActivity).replaceFragment(newFragment)
        }
        button.setOnClickListener {
            button.setOnClickListener {
                val code = arguments?.getString("code") ?: "N/A"
                showBottomSheetMoreDialog(code, activityId) // Pass the activityId instead of code
            }
        }

        return cardView
    }

    private fun showBottomSheetMoreDialog(code: String, activityId: String) {
        // Create and show the BottomSheetDialog
        val bottomSheetMoreDialog = BottomSheetDialog(requireContext())
        val bottomSheetMoreView = layoutInflater.inflate(R.layout.activity_more_layout, null)

        bottomSheetMoreDialog.setContentView(bottomSheetMoreView)

        bottomSheetMoreView.findViewById<Button>(R.id.btnEditAct).setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_activity, null)
            val btnUploadFile: Button = dialogView.findViewById(R.id.btnUploadFile)
            fileNameTextView = dialogView.findViewById(R.id.tvFileName)
            btnSetDueDate = dialogView.findViewById(R.id.btnSetDueDate)

            btnUploadFile.setOnClickListener {
                filePickerLauncher.launch("*/*") // Show all files initially, filter during selection
            }
            btnSetDueDate.setOnClickListener {
                showDatePicker()
            }

            // Create the "Create Activity" dialog
            val editActivityDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            editActivityDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

            val tvEditClassTitle = dialogView.findViewById<TextView>(R.id.tvCreateClassTitle)
            val submitButton = dialogView.findViewById<Button>(R.id.btnSubmitCreatedActivity)
            tvEditClassTitle.text = "Edit Activity"
            submitButton.text = "Save"

            val editActivitiesCollection = firestore.collection("Activities")
            // Query for the document with the matching code and activityId
            editActivitiesCollection
                .whereEqualTo("code", code)
                .whereEqualTo("activityId", activityId) // Compound query for better performance
                .limit(1) // Fetch only one matching document
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.first()
                        val documentId = document.id // Save the document ID for updating

                        val activityName = document.getString("activityName") ?: ""
                        val activityDesc = document.getString("activityDesc") ?: ""
                        val fileName = document.getString("fileName") ?: "No File Selected"
                        // Retrieve fileUri as a string and convert it to Uri
                        val fileUriString = document.getString("fileUri") ?: ""
                        fileUri = if (fileUriString.isNotEmpty()) Uri.parse(fileUriString) else null
                        val dueDate = document.getString("dueDate") ?: "Set Due Date"
                        val points = document.getString("points")

                        // Set the values in the EditTexts
                        val etActivityName = dialogView.findViewById<EditText>(R.id.etActivityName)
                        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
                        fileNameTextView = dialogView.findViewById(R.id.tvFileName)
                        btnSetDueDate = dialogView.findViewById(R.id.btnSetDueDate)
                        val etPoints = dialogView.findViewById<EditText>(R.id.etSetPoints)


                        etActivityName.setText(activityName)
                        etDescription.setText(activityDesc)
                        fileNameTextView.text = fileName
                        btnSetDueDate.text = dueDate
                        etPoints.setText(points)


                        // Ensure that mimeType is retrieved based on fileUri
                        var mimeType: String? = null
                        if (fileUri != null) {
                            mimeType = requireContext().contentResolver.getType(fileUri!!) // Get the mimeType from the fileUri
                        }

                        // Set onClickListener for the TextView to open the file
                        fileNameTextView.setOnClickListener {
                            if (fileUri != null && mimeType != null) {
                                openFile(fileUri!!, mimeType) // Open the file if valid
                            } else {
                                fileNameTextView.isClickable = false // Disable click if no valid fileUri or mimeType
                            }
                        }

                        submitButton.setOnClickListener {
                            showLoading()
                            // Retrieve updated values
                            val updatedActivityName = etActivityName.text.toString()
                            val updatedActivityDesc = etDescription.text.toString()
                            val updatedFileName = fileNameTextView.text.toString()
                            val updatedDueDate = btnSetDueDate.text.toString()
                            val updatedPoints = etPoints.text.toString()

                            // Update Firestorm document
                            editActivitiesCollection.document(documentId)
                                .update(
                                    mapOf(
                                        "activityName" to updatedActivityName,
                                        "activityDesc" to updatedActivityDesc,
                                        "fileName" to updatedFileName,
                                        "fileUri" to fileUri,
                                        "dueDate" to updatedDueDate,
                                        "points" to updatedPoints
                                    )
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(requireContext(), "Activity Updated Successfully", Toast.LENGTH_SHORT).show()
                                    fetchData()

                                    editActivityDialog.dismiss() // Close the dialog
                                }
                        }

                    } else {
                        Toast.makeText(requireContext(), "Activity not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                bottomSheetMoreDialog.dismiss()
            editActivityDialog.show()

        }

        bottomSheetMoreView.findViewById<Button>(R.id.btnDeleteAct).setOnClickListener {
            // Show confirmation dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Activity")
                .setMessage("Are you sure you want to delete this activity?")
                .setPositiveButton("Yes") { dialog, _ ->
                    showLoading()
                    val activitiesCollection = firestore.collection("Activities")

                    // Query for the document with the matching code and activityId
                    activitiesCollection.whereEqualTo("code", code).get()
                        .addOnSuccessListener { documents ->
                            var activityDeleted = false
                            for (document in documents) {
                                val fetchedActivityId = document.getString("activityId") ?: ""
                                if (fetchedActivityId == activityId) {
                                    // Delete the document
                                    activitiesCollection.document(document.id).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(requireContext(), "Activity Deleted Successfully", Toast.LENGTH_SHORT).show()
                                            fetchData()
                                            bottomSheetMoreDialog.dismiss() // Dismiss the bottom sheet dialog after successful deletion
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(requireContext(), "Failed to delete activity: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    activityDeleted = true
                                    break
                                }
                            }
                            if (!activityDeleted) { Toast.makeText(requireContext(), "No matching activity found with the given code and ID.", Toast.LENGTH_SHORT).show() }
                        }
                        .addOnFailureListener { e -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show() }

                    dialog.dismiss() // Close the confirmation dialog
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Close the confirmation dialog without deleting
                }
                .create()
                .show()

            bottomSheetMoreDialog.dismiss()
        }

        bottomSheetMoreDialog.show()

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
        fileNameTextView = dialogView.findViewById(R.id.tvFileName)
        btnSetDueDate = dialogView.findViewById(R.id.btnSetDueDate)

        // Create the "Create Activity" dialog
        val createClassDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        createClassDialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        // Launch the file picker when the button is clicked
        btnUploadFile.setOnClickListener {
            filePickerLauncher.launch("*/*") // Show all files initially, filter during selection
        }
        btnSetDueDate.setOnClickListener {
            showDatePicker()
        }
        //Handle the submit button click inside the "Create Class" dialog
        dialogView.findViewById<Button>(R.id.btnSubmitCreatedActivity).setOnClickListener {
            val activityName = dialogView.findViewById<EditText>(R.id.etActivityName).text.toString()
            val activityDesc = dialogView.findViewById<EditText>(R.id.etDescription).text.toString()
            val points = dialogView.findViewById<EditText>(R.id.etSetPoints).text.toString()
            val createdDate = Timestamp.now()
            val date = createdDate.toDate()    // Convert to Date
            // Format the Date to 12-hour time format with AM/PM
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(date)

            val dueDate = dueDate
            val formattedDueDate = formatDueDate(dueDate)
            val code = arguments?.getString("code") ?: "N/A"

            // Validate inputs
            if (activityName.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else if (activityDesc.isBlank()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Fetch the max activityId for the given code
            firestore.collection("Activities")
                .whereEqualTo("code", code)
                .orderBy("activityId", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { activityDocuments ->
                    // Determine the next activityId
                    val nextActivityId = if (activityDocuments.isEmpty) 1 else {
                        val maxActivityId = activityDocuments.documents.first().getString("activityId")?.toIntOrNull() ?: 0
                        maxActivityId + 1
                    }
                    showLoading()
                    firestore.collection("Classes")
                        .whereEqualTo("code", code)
                        .get()
                        .addOnSuccessListener { classDocuments ->
                            if (classDocuments.isEmpty) {
                                Toast.makeText(requireContext(), "Class not found for the provided code", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            // Prepare activity information
                            val activityInfo = hashMapOf(
                                "activityId" to nextActivityId.toString(),
                                "activityName" to activityName,
                                "activityDesc" to activityDesc,
                                "createdByUserId" to auth.currentUser?.uid,
                                "createdByUserName" to auth.currentUser?.displayName,
                                "code" to code,
                                "createdDate" to createdDate,
                                "fileUri" to fileUri.toString(),
                                "fileName" to fileUri?.let { it1 -> getFileNameFromUri(it1) },
                                "dueDate" to dueDate,
                                "points" to points
                            )

                            // Add activity to Firestore
                            firestore.collection("Activities").add(activityInfo)
                                .addOnSuccessListener {
                                    // After saving, create a new CardView with activityName and createdByUserName
                                    val newCardView = createCardView(activityName, formattedTime, formattedDueDate, activityDesc, nextActivityId.toString())
                                    cardContainer.addView(newCardView, 0)

                                    Toast.makeText(requireContext(), "Activity Created Successfully", Toast.LENGTH_SHORT).show()
                                    hideLoading()
                                    // Clear input fields
                                    dialogView.findViewById<EditText>(R.id.etActivityName).text.clear()
                                    dialogView.findViewById<EditText>(R.id.etDescription).text.clear()
                                    fileUri = null
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed to save activity: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to load class data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to fetch max activity ID: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            createClassDialog.dismiss()
        }

// Show the "Create Activity" dialog
        createClassDialog.show()

    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Correct the day and month usage
                showTimePicker(selectedYear, selectedMonth, selectedDay)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker(year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                // Format the time in 12-hour format with AM/PM
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)

                // Get the formatted time string in 12-hour format
                val formattedTime = timeFormat.format(calendar.time)
                // After selecting the time, display the selected date & time in the TextView
                dueDate = "${month + 1}/${day}/$year $formattedTime"
                btnSetDueDate.text = dueDate // Update TextView with selected date & time
            },
            hour, minute, false // true for 24-hour format, false for 12-hour format
        )
        timePickerDialog.show()
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                fileUri = it
                val mimeType = requireContext().contentResolver.getType(it) // Get the file's MIME type

                if (isAcceptedFileType(mimeType)) {
                    val fileName = getFileNameFromUri(it)
                    fileNameTextView.text = fileName

                    // Enable the TextView to open the file
                    fileNameTextView.setOnClickListener {
                        openFile(fileUri!!, mimeType) // Pass the fileUri explicitly
                    }
                    Toast.makeText(requireContext(), fileName, Toast.LENGTH_SHORT).show()
                } else {
                    fileUri = null
                    fileNameTextView.isClickable = false
                    fileNameTextView.text = "No file selected"
                    Toast.makeText(requireContext(), "PDF or DOCX type only!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun isAcceptedFileType(mimeType: String?): Boolean {
        // Check if the MIME type matches PDF or Word document formats
        return mimeType == "application/pdf" ||
                mimeType == "application/msword" ||
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "Unknown"
        requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                cursor.moveToFirst()
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun openFile(uri: Uri, mimeType: String?) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION // Grant temporary access to the file
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

}