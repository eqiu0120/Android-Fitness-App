package com.example.accountabuddy.pages.GroupInfo.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.ZoneId

class GroupInfoViewModel : ViewModel() {
    var members by mutableStateOf(listOf<String>())
    var activities by mutableStateOf<List<Map<String, Any?>>>(emptyList())
    var streak by mutableStateOf<Int?>(null)
    private val db = Firebase.firestore

    // get the group members for the current group
    fun loadGroupMembers(groupName: String) {
        val db = Firebase.firestore

        db.collection("groups")
            .whereEqualTo("name", groupName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val membersList = doc.get("members") as? List<String> ?: emptyList()
                    members = membersList
                } else {
                    members = emptyList()
                }
            }
            .addOnFailureListener {
                members = emptyList()
            }
    }

    fun removeMember(groupName: String, memberEmail: String) {
        val db = Firebase.firestore
        db.collection("groups")
            .whereEqualTo("name", groupName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val removeFrom = querySnapshot.documents.first().reference
                    removeFrom.update("members", FieldValue.arrayRemove(memberEmail))
                        .addOnSuccessListener {
                            members = members.filterNot { it == memberEmail }
                        }
                }
            }
    }

    fun loadGroupActivities(groupName: String) {
        val db = Firebase.firestore

        // get the group collection from firestore
        db.collection("groups")
            .whereEqualTo("name", groupName)
            .get()
            .addOnSuccessListener { groupSnapshot ->
                if (!groupSnapshot.isEmpty) {
                    val doc = groupSnapshot.documents.first()
                    val membersList = doc.get("members") as? List<String> ?: emptyList()

                    // return a empty list
                    if (membersList.isEmpty()) {
                        activities = emptyList()
                        return@addOnSuccessListener
                    }

                    // get the activities
                    db.collection("activities")
                        .get()
                        .addOnSuccessListener { activitiesSnapshot ->
                            val membersLowercase = membersList.map { it.lowercase().trim() }

                            val filteredActivities = activitiesSnapshot.documents.mapNotNull { document ->
                                val data = document.data ?: return@mapNotNull null
                                val userEmail = (data["userEmail"] as? String)?.lowercase()?.trim()

                                // include the activities for the group that the user is in
                                if (userEmail != null && membersLowercase.contains(userEmail)) {
                                    data
                                } else {
                                    null
                                }
                            }

                            // sort by time
                            val sortedActivities = filteredActivities.sortedByDescending { activityData ->
                                (activityData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()
                            }

                            activities = sortedActivities
                        }
                        .addOnFailureListener {
                            activities = emptyList()
                        }
                } else {
                    activities = emptyList()
                }
            }
            .addOnFailureListener {
                activities = emptyList()
            }
    }

    fun loadGroupStreak(groupName: String) {
        //get the groups that the user is in
        db.collection("groups")
            .whereEqualTo("name", groupName)
            .get()
            .addOnSuccessListener { groupSnap ->
                if (groupSnap.isEmpty) {
                    streak = 0; return@addOnSuccessListener
                }
                val membersList = groupSnap.documents.first().get("members") as? List<String> ?: emptyList()
                if (membersList.isEmpty()) {
                    streak = 0; return@addOnSuccessListener
                }

                // get the activtiess
                db.collection("activities")
                    .whereIn("userEmail", membersList)
                    .get()
                    .addOnSuccessListener { snap ->
                        // store the dates of the activities
                        val dateToMembers = mutableMapOf<LocalDate, MutableSet<String>>()
                        snap.documents.forEach { doc ->
                            val time = doc.getTimestamp("timestamp")?.toDate() ?: return@forEach
                            val email = doc.getString("userEmail") ?: return@forEach
                            val date = time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                            dateToMembers.getOrPut(date) { mutableSetOf() }.add(email)
                        }

                        var count = 0
                        // current time to count the streak
                        var day = LocalDate.now()
                        while (dateToMembers[day]?.containsAll(membersList) == true) {
                            count++
                            day = day.minusDays(1)
                        }
                        streak = count
                    }
                    .addOnFailureListener { streak = 0 }
            }
            .addOnFailureListener { streak = 0 }
    }
}
