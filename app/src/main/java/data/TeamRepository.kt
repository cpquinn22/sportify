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

            userRef.update(updates).await()
            Log.d("FirestoreDebug", "✅ User document updated successfully!")
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "❌ Error updating user document", e)
            e.printStackTrace()
        }
    }

    suspend fun getUserDocIdByUid(uid: String?): String? {
        if (uid.isNullOrBlank()) return null

        return try {
            val snapshot = usersCollection.whereEqualTo("uid", uid).get().await()
            snapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("FirestoreDebug", "❌ Failed to query user by UID: $uid", e)
            null
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

    suspend fun getTeamIdByName(name: String): String? {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("teams")
                .whereEqualTo("name", name)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveWorkout(teamId: String, workout: Workout) {
        val db = FirebaseFirestore.getInstance()
        val workoutRef = db.collection("teams")
            .document(teamId)
            .collection("workouts")
            .document()

        val workoutMap = mapOf(
            "name" to workout.name,
            "info" to workout.info,
            "steps" to workout.steps
        )

        workoutRef.set(workoutMap).await()
    }

    // Generate a random unique team code
    private fun generateUniqueTeamCode(): String {
        return (100000..999999).random().toString()
    }
}
