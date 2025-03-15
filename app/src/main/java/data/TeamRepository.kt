package com.example.myapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class TeamRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val teamsCollection = firestore.collection("teams")
    private val usersCollection = firestore.collection("users")
    private val sportsCollection = firestore.collection("sports")
    // Create a new team
    suspend fun createTeam(teamName: String, sport: String, userId: String): String? {
        val teamId = teamsCollection.document().id
        val teamCode = generateUniqueTeamCode()

        val teamData = hashMapOf(
            "name" to teamName,
            "sport" to sport,
            "teamCode" to teamCode,
            "adminId" to userId,
            "members" to listOf(userId)
        )

        return try {
            teamsCollection.document(teamId).set(teamData).await()
            updateUserWithNewTeam(userId, teamId, isAdmin = true)
            teamId
        } catch (e: Exception) {
            null
        }
    }

    // Join an existing team by its unique code
    suspend fun joinTeam(userId: String, teamCode: String): Boolean {
        return try {
            val querySnapshot = teamsCollection
                .whereEqualTo("teamCode", teamCode)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val teamDoc = querySnapshot.documents.first()
                val teamId = teamDoc.id

                teamsCollection.document(teamId)
                    .update("members", FieldValue.arrayUnion(userId))
                    .await()

                updateUserWithNewTeam(userId, teamId, isAdmin = false)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSportsList(): List<String> {
        return try {
            val snapshot = sportsCollection.get().await()
            snapshot.documents.mapNotNull { it.getString("name") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get a list of teams the user is part of
    suspend fun getUserTeams(userId: String): List<String> {
        val userDoc = usersCollection.document(userId).get().await()
        return userDoc.get("teams") as? List<String> ?: emptyList()
    }

    // Check if a user is an admin of a team
    suspend fun isUserAdmin(userId: String, teamId: String): Boolean {
        val userDoc = usersCollection.document(userId).get().await()
        val adminTeams = userDoc.get("adminTeams") as? List<String> ?: emptyList()
        return adminTeams.contains(teamId)
    }

    // Private helper function to update user's teams
    private suspend fun updateUserWithNewTeam(userId: String, teamId: String, isAdmin: Boolean) {
        val userRef = usersCollection.document(userId)
        val userDoc = userRef.get().await()

        if (userDoc.exists()) {
            val userTeams = userDoc.get("teams") as? List<String> ?: emptyList()
            val adminTeams = userDoc.get("adminTeams") as? List<String> ?: emptyList()

            val updatedTeams = userTeams + teamId
            val updatedAdminTeams = if (isAdmin) adminTeams + teamId else adminTeams

            userRef.update(
                "teams", updatedTeams,
                "adminTeams", updatedAdminTeams
            ).await()
        }
    }

    // Generate a random unique team code
    private fun generateUniqueTeamCode(): String {
        return (100000..999999).random().toString() // Example: 6-digit random number
    }
}
