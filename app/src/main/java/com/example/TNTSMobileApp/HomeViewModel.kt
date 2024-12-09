package com.example.TNTSMobileApp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.TNTSMobileApp.ClassAdapter.ClassInfo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _classList = MutableLiveData<List<ClassAdapter.ClassInfo>>()
    val classList: LiveData<List<ClassAdapter.ClassInfo>> get() = _classList

    fun fetchData(currentUser: String) {
        firestore.collection("Classes")
            .get()
            .addOnSuccessListener { documents ->
                val fetchedClassList = mutableListOf<ClassAdapter.ClassInfo>()
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
                                fetchedClassList.add(ClassAdapter.ClassInfo(subjectName, section, createdByUserName, code, joinedDate))
                            }
                        }
                    } else {
                        Log.d("FirestormDebug", "No members field or wrong format in document ID: ${document.id}")
                    }
                }

                fetchedClassList.sortByDescending { it.joinedDate }
                _classList.value = fetchedClassList
            }
            .addOnFailureListener { e ->
                Log.e("FirestormError", "Error fetching documents: ", e)
            }
    }

    // New method to add, join and rejoin a class
    fun addClass(classInfo: ClassInfo) {
        // Create a mutable list from the current classList
        val updatedList = _classList.value?.toMutableList() ?: mutableListOf()
        // Add the new class to the top of the list
        updatedList.add(0, classInfo)
        // Update the LiveData with the new list, triggering observers (e.g., RecyclerView)
        _classList.value = updatedList
    }

    fun removeClassByCode(code: String) {
        val updatedList = _classList.value?.toMutableList() ?: return
        val index = updatedList.indexOfFirst { it.code == code }
        if (index != -1) {
            updatedList.removeAt(index)
            _classList.value = updatedList // This updates the LiveData and triggers the observer

        }
    }

}
