package com.example.TNTSMobileApp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SubjectDetailViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _activityList = MutableLiveData<List<ActivityAdapter.ActivityInfo>>()
    val activityList: LiveData<List<ActivityAdapter.ActivityInfo>> get() = _activityList

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var currentClassCode: String? = null // Track the current class code

    fun fetchData(classCode: String, currentUser: String) {

        // If the class code hasn't changed and the activity list is already empty, skip
        if (currentClassCode == classCode && _activityList.value.isNullOrEmpty()) {
            return
        }
        if (!(currentClassCode != classCode || !((_activityList.value?.isNotEmpty() ?: "") as Boolean))) {
            return
        }

        if (currentClassCode != classCode) {
            clearViewModel() // Clear existing data if classCode changes
        }

        // Update the current class code
        currentClassCode = classCode

        _isLoading.postValue(true) // Start loading
        firestore.collection("Classes")
            .whereEqualTo("code", classCode)
            .get()
            .addOnSuccessListener { classDocuments ->
                if (classDocuments.isEmpty) {
                    _activityList.postValue(emptyList())
                    _status.postValue("Class not found")
                    _isLoading.postValue(false) // Stop loading
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
                            // Check if the message has already been shown for this class code
                            if (_activityList.value.isNullOrEmpty()) {
                                val message = if (currentUser == classCreator) {
                                    "No activities found. Add Activities here"
                                } else {
                                    "No activities found. Ask your teacher"
                                }
                                _status.postValue(message)
                            }
                            _activityList.postValue(emptyList())
                            _isLoading.postValue(false) // Stop loading
                            return@addOnSuccessListener
                        }

                        val fetchActivityList = mutableListOf<ActivityAdapter.ActivityInfo>()
                        for (activityDocument in activityDocuments) {
                            val code = activityDocument.getString("code") ?: "N/A"
                            val activityName = activityDocument.getString("activityName") ?: "N/A"
                            val activityDesc = activityDocument.getString("activityDesc") ?: "N/A"
                            val activityId = activityDocument.getString("activityId") ?: "N/A"
                            val subjectName = classDocument.getString("subjectName") ?: "N/A"
                            val createdDate = activityDocument.getTimestamp("createdDate")
                            val formattedCreatedDate = formatCreatedDate(createdDate)
                            val dueDate = activityDocument.getString("dueDate")
                            val formattedDueDate = formatDueDate(dueDate)

                            fetchActivityList.add(
                                ActivityAdapter.ActivityInfo(
                                    activityName,
                                    formattedCreatedDate,
                                    formattedDueDate,
                                    activityDesc,
                                    code,
                                    activityId,
                                    subjectName,
                                    classCreator
                                )
                            )
                        }
                        _activityList.postValue(fetchActivityList)
                        _isLoading.postValue(false) // Stop loading
                    }
                    .addOnFailureListener {
                        _status.postValue("Failed to load activities.")
                        _isLoading.postValue(false) // Stop loading
                    }
            }
            .addOnFailureListener {
                _status.postValue("Failed to load class.")
                _isLoading.postValue(false) // Stop loading
            }
    }


    // New method to add, join and rejoin a class
    fun addClass(activityInfo: ActivityAdapter.ActivityInfo) {
        val updatedList = _activityList.value?.toMutableList() ?: mutableListOf()
        // Add the new class to the top of the list
        updatedList.add(0, activityInfo)
        //_activityList.value = updatedList
    }

    private fun clearViewModel() {
        _activityList.value = emptyList() // Clear activity list
        _status.value = "" // Clear status
    }

    private fun formatDueDate(dueDate: String?): String {
        return try {
            val dateFormat = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault()) // Adjust format as needed
            if (dueDate == "Set Due Date") {
                "No Due Date"
            }
            else {
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
            }
        }catch (e: Exception) {
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
}
