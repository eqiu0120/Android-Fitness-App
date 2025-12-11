package com.example.accountabuddy.pages.AddGroup.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddGroupViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // creates a group, for the group members, multiple emails have to be seprerated by commas
    fun createGroup(groupName: String, membersInput: String, onResult: (Boolean, String) -> Unit) {
        //get the members from the comma seperated string, turn into a list
        val membersList = membersInput
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()

        // add current user to the members list for the group
        auth.currentUser?.email?.let { currentUserEmail ->
            if (!membersList.contains(currentUserEmail)) {
                membersList.add(currentUserEmail)
            }
        }

        // actual data to put into firestore
        val groupData = hashMapOf(
            "name" to groupName,
            "members" to membersList
        )

        //store data
        firestore.collection("groups")
            .add(groupData)
            .addOnSuccessListener {
                onResult(true, "Group created successfully!")
            }
            .addOnFailureListener { exception ->
                onResult(false, exception.localizedMessage ?: "Error creating group.")
            }
    }
}
