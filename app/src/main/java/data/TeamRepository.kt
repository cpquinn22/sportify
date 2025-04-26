package com.example.myapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import model.Team
import model.Workout

class TeamRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val teamsCollection = firestore.collection("teams")
    private val usersCollection = firestore.collection("users")
    private val sportsCollection = firestore.collection("sports")


    // Create a new team and assign the creating user as admin
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
            // save team in firestore
            teamsCollection.document(teamId).set(teamData).await()
            // update users document to include new team
            updateUserWithNewTeam(userId, teamId, isAdmin = true)
            teamId
        } catch (e: Exception) {
            null
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
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            userDoc.get("teams") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "❌ Error fetching user teams", e)
            emptyList()
        }
    }

    suspend fun getTeamNameById(teamId: String): String? {
        return try {
            val snapshot = teamsCollection.document(teamId).get().await()
            snapshot.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    // Check if a user is an admin of a team
    suspend fun isUserAdmin(userId: String): Boolean {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            snapshot.getBoolean("isAdmin") ?: false
        } catch (e: Exception) {
            false
        }
    }

    // Private helper function to update user's teams
    private suspend fun updateUserWithNewTeam(userId: String, teamId: String, isAdmin: Boolean) {
        val userRef = usersCollection.document(userId)

        try {
            Log.d("FirestoreDebug", "🔍 Starting updateUserWithNewTeam() for userId: $userId, teamId: $teamId, isAdmin: $isAdmin")
            val userDoc = userRef.get().await()
            Log.d("FirestoreDebug", "📄 User document fetched: ${userDoc.data}")

            // fetch current teams and adminTeams
            val userTeams = (userDoc.get("teams") as? List<String>)?.toMutableList() ?: mutableListOf()
            val adminTeams = (userDoc.get("adminTeams") as? List<String>)?.toMutableList() ?: mutableListOf()

            if (!userTeams.contains(teamId)) {
                userTeams.add(teamId)
            }

            if (isAdmin && !adminTeams.contains(teamId)) {
                adminTeams.add(teamId)
            }

            val updates = mutableMapOf<String, Any>(
                "teams" to userTeams
            )

            if (isAdmin) {
                updates["adminTeams"] = adminTeams
            }
            Log.d("FirestoreDebug", "📦 Prepared update payload: $updates")

            // commit changes to Firestore
            userRef.update(updates).await()
            Log.d("FirestoreDebug", "✅ User document updated successfully!")
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "❌ Error updating user document", e)
            e.printStackTrace()
        }
    }

    suspend fun getTeamById(teamId: String): Team? {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("teams")
                .document(teamId)
                .get()
                .await()

            Log.d("TeamDebug", "📄 Document snapshot: ${snapshot.data}")

            snapshot.toObject(Team::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e("TeamDebug", "❌ Error fetching team: $e")
            null
        }
    }

    // Generate a random unique team code
    private fun generateUniqueTeamCode(): String {
        return (100000..999999).random().toString()
    }
}
