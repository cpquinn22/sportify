package ViewModels

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.TeamRepository
import com.example.sportify.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import model.Team
import model.Workout
import com.google.firebase.firestore.DocumentChange
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import com.google.auth.oauth2.GoogleCredentials
import java.io.File
import java.io.FileInputStream
import com.google.firebase.functions.FirebaseFunctions



class TeamViewModel : ViewModel() {

    private val repository = TeamRepository() // No need for factory!

    private val _userTeams = MutableStateFlow<Map<String, String>>(emptyMap())
    val userTeams: StateFlow<Map<String, String>> = _userTeams

    private val _selectedTeam = MutableStateFlow<Team?>(null)
    val selectedTeam: StateFlow<Team?> = _selectedTeam

    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries



    fun createTeam(teamName: String, selectedSport: String, userId: String) {
        viewModelScope.launch {
            repository.createTeam(teamName, selectedSport, userId)
        }
    }

    fun loadTeamDetails(teamId: String) {
        viewModelScope.launch {
            Log.d("TeamDebug", "🔍 Loading team details for ID: $teamId")
            val team = repository.getTeamById(teamId)
            Log.d("TeamDebug", "📦 Loaded team object: $team")
            _selectedTeam.value = team
        }
    }
    fun joinTeam(userId: String, teamCode: String) {
        viewModelScope.launch {
            repository.joinTeam(userId, teamCode)
        }
    }

    suspend fun getTeamIdByName(name: String): String? {
        return repository.getTeamIdByName(name)
    }

    fun fetchUserTeams(firebaseUid: String?) {
        viewModelScope.launch {
            val userDocId = firebaseUid

            if (userDocId != null) {
                val teamIds = repository.getUserTeams(userDocId)
                Log.d("FirestoreDebug", "Fetched team IDs: $teamIds")

                val teamMap = mutableMapOf<String, String>()
                for (teamId in teamIds) {
                    val name = repository.getTeamNameById(teamId)
                    if (name != null) {
                        teamMap[teamId] = name
                    }
                }

                _userTeams.value = teamMap // key = id, value = name
                Log.d("FirestoreDebug", "Final team map: $teamMap")
            }
        }
    }

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun fetchAdminStatus(userId: String) {
        viewModelScope.launch {
            _isAdmin.value = repository.isUserAdmin(userId)
            Log.d("AuthDebug", "Admin status: ${_isAdmin.value}")
        }
    }

    suspend fun getSportsList(): List<String> {
        return repository.getSportsList()
    }

    fun createWorkout(teamId: String, workout: Workout) {
        viewModelScope.launch {
            try {
                repository.saveWorkout(teamId, workout)
                Log.d("WorkoutDebug", "✅ Workout saved successfully for team $teamId")
            } catch (e: Exception) {
                Log.e("WorkoutDebug", "❌ Error saving workout", e)
            }
        }
    }

    fun saveWorkoutToFirestore(teamId: String, workout: Workout) {
        val db = FirebaseFirestore.getInstance()
        val workoutRef = db.collection("teams")
            .document(teamId)
            .collection("workouts")
            .document()

        workoutRef.set(workout)
            .addOnSuccessListener {
                Log.d("WorkoutSave", "Workout saved successfully!")

                Log.d("FCM", "Sending teamId: $teamId")
                Log.d("FCM", "Sending workoutName: ${workout.name}")

                val data = hashMapOf(
                    "teamId" to teamId,
                    "workoutName" to workout.name
                )

                Log.d("FCM", "📦 Sending payload to Cloud Function: $data")

                FirebaseFunctions.getInstance()
                    .getHttpsCallable("sendWorkoutNotification")
                    .call(data)
                    .addOnSuccessListener { result ->
                        Log.d("FCM", "✅ Cloud Function Success")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "❌ Cloud Function Failed", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutSave", "Error saving workout", e)
            }
    }

    fun sendPushNotificationToTeamMembers(teamId: String, workoutName: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("teams").document(teamId).get()
            .addOnSuccessListener { teamDoc ->
                val members = teamDoc.get("members") as? List<*>

                if (members.isNullOrEmpty()) {
                    Log.d("FCM", "No members in team $teamId")
                    return@addOnSuccessListener
                }

                members.forEach { memberId ->
                    if (memberId is String) {
                        db.collection("users").document(memberId).get()
                            .addOnSuccessListener { userDoc ->
                                val token = userDoc.getString("fcmToken")
                                val name = userDoc.getString("name")

                                if (!token.isNullOrEmpty()) {
                                    Log.d("FCM", "📨 Sending notification to $name ($memberId)")
                                    sendNotificationToToken(token, workoutName)
                                } else {
                                    Log.w("FCM", "⚠️ No token for user $memberId")
                                }
                            }
                            .addOnFailureListener {
                                Log.e("FCM", "❌ Failed to fetch user $memberId", it)
                            }
                    }
                }
            }
            .addOnFailureListener {
                Log.e("FCM", "❌ Failed to fetch team $teamId", it)
            }
    }

    fun sendNotificationToToken(token: String, workoutName: String) {
        try {
            val credentials = GoogleCredentials.fromStream(
                FileInputStream(File("app", "sportify-11df1-firebase-adminsdk-u0gjb-80a37a7c71.json"))
            )
            val accessToken = credentials.refreshAccessToken().tokenValue
            val client = OkHttpClient()

            val json = """
        {
            "to": "$token",
            "notification": {
                "title": "New Workout Available!",
                "body": "A new workout \"$workoutName\" was added to your team."
            }
        }
        """.trimIndent()

            Log.d("FCM", "📨 Preparing to send notification. JSON: $json")

            val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .addHeader("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FCM", "❌ Notification failed to send: ${e.message}", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    Log.d("FCM", "✅ Notification sent successfully. Response: $body")
                }
            })

        } catch (e: Exception) {
            Log.e("FCM", "❌ Error sending push notification: ${e.message}", e)
        }
    }


    suspend fun loadWorkout(teamId: String, workoutId: String): Workout? {
        return try {
            val doc = Firebase.firestore
                .collection("teams")
                .document(teamId)
                .collection("workouts")
                .document(workoutId)
                .get()
                .await()

            doc.toObject(Workout::class.java)
        } catch (e: Exception) {
            Log.e("WorkoutLog", "Error loading workout: ", e)
            null
        }
    }

    fun saveWorkoutLog(
        teamId: String,
        workoutId: String,
        logData: Map<String, String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val logRef = Firebase.firestore
            .collection("teams")
            .document(teamId)
            .collection("workouts")
            .document(workoutId)
            .collection("allLogs") // use flat structure
            .document()

        logRef.set(logData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                Log.e("WorkoutLog", "Error saving log", e)
                onError(e)
            }
    }


    private val _teamWorkouts = MutableStateFlow<List<Pair<String, Workout>>>(emptyList())
    val teamWorkouts: StateFlow<List<Pair<String, Workout>>> = _teamWorkouts

    fun fetchTeamWorkouts(teamId: String) {
        Firebase.firestore.collection("teams")
            .document(teamId)
            .collection("workouts")
            .get()
            .addOnSuccessListener { snapshot ->
                val workouts = snapshot.documents.mapNotNull { doc ->
                    val workout = doc.toObject(Workout::class.java)
                    if (workout != null) {
                        workout.id = doc.id  // Inject ID into the object
                        doc.id to workout     // Return Pair<String, Workout>
                    } else null
                }
                _teamWorkouts.value = workouts // List<Pair<String, Workout>>
            }
            .addOnFailureListener { e ->
                Log.e("TeamScreen", "Error fetching workouts", e)
            }
    }

    suspend fun loadWorkoutLogs(teamId: String, workoutId: String): List<Map<String, String>> {
        return try {
            val userId = Firebase.auth.currentUser?.uid ?: return emptyList()

            val snapshot = Firebase.firestore
                .collection("teams")
                .document(teamId)
                .collection("workouts")
                .document(workoutId)
                .collection("allLogs")
                .whereEqualTo("userId", FirebaseAuth.getInstance().currentUser?.uid)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.data as? Map<String, String> }
        } catch (e: Exception) {
            Log.e("WorkoutLogViewer", "Error loading workout logs", e)
            emptyList()
        }
    }

    suspend fun loadAllLogsForWorkout(teamId: String, workoutId: String): List<Map<String, String>> {
        return try {
            val snapshot = Firebase.firestore
                .collection("teams")
                .document(teamId)
                .collection("workouts")
                .document(workoutId)
                .collection("allLogs")  // <- correct collection name
                .get()
                .await()

            Log.d("AdminLogDetail", "Fetched ${snapshot.size()} logs from Firestore")

            snapshot.documents.mapNotNull { it.data as? Map<String, String> }
        } catch (e: Exception) {
            Log.e("AdminLogDetail", "Error fetching logs", e)
            emptyList()
        }
    }

    suspend fun loadAllLogsForTeam(teamId: String): List<Map<String, String>> {
        val db = Firebase.firestore
        val allLogs = mutableListOf<Map<String, String>>()

        try {
            val workoutsSnapshot = db.collection("teams")
                .document(teamId)
                .collection("workouts")
                .get()
                .await()

            for (workoutDoc in workoutsSnapshot.documents) {
                val workoutId = workoutDoc.id
                val logsSnapshot = db.collection("teams")
                    .document(teamId)
                    .collection("workouts")
                    .document(workoutId)
                    .collection("allLogs")
                    .get()
                    .await()

                val logs = logsSnapshot.documents.mapNotNull { it.data as? Map<String, String> }
                allLogs.addAll(logs)
            }

        } catch (e: Exception) {
            Log.e("Leaderboard", "Failed to load logs for team $teamId", e)
        }

        return allLogs
    }

    fun loadLeaderboard(teamId: String, filter: String) {
        viewModelScope.launch {
            val logs = loadAllLogsAcrossUsers(teamId)  // You should already have this function
            val processed = processLogsForLeaderboard(logs, filter)
            _leaderboardEntries.value = processed
        }
    }

    suspend fun loadAllLogsAcrossUsers(teamId: String): List<Map<String, String>> {
        val firestore = Firebase.firestore
        val logs = mutableListOf<Map<String, String>>()

        val workoutsSnapshot = firestore
            .collection("teams")
            .document(teamId)
            .collection("workouts")
            .get()
            .await()

        for (workout in workoutsSnapshot.documents) {
            val workoutId = workout.id

            val logsSnapshot = firestore
                .collection("teams")
                .document(teamId)
                .collection("workouts")
                .document(workoutId)
                .collection("allLogs")
                .get()
                .await()

            logs += logsSnapshot.documents.mapNotNull { it.data as? Map<String, String> }
        }

        return logs
    }
    fun processLogsForLeaderboard(
        logs: List<Map<String, String>>,
        metric: String
    ): List<LeaderboardEntry> {
        val userStats = mutableMapOf<String, Pair<String, Float>>() // userId to (userName, score)

        for (log in logs) {
            val userId = log["userId"] ?: continue
            val userName = log["userName"] ?: "Unknown"

            when (metric) {
                "Most Active" -> {
                    val current = userStats[userId]?.second ?: 0f
                    userStats[userId] = userName to (current + 1)
                }

                "Top Runner" -> {
                    // Sum distance from keys like "stepX-Distance (km)"
                    val totalDistance = log.entries
                        .filter { it.key.contains("Distance (km)") }
                        .sumOf { it.value.toDoubleOrNull() ?: 0.0 }

                    val current = userStats[userId]?.second ?: 0f
                    userStats[userId] = userName to (current + totalDistance).toFloat()

                }

                "Top Shooter" -> {
                    val made = log.entries
                        .filter { it.key.contains("Shots Made") }
                        .sumOf { it.value.toIntOrNull() ?: 0 }

                    val current = userStats[userId]?.second ?: 0f
                    userStats[userId] = userName to (current + made)
                }

                "Top Lifter" -> {
                    // Sum all weights from keys like "stepX-Weight (kg)"
                    val totalWeight = log.entries
                        .filter { it.key.contains("Weight (kg)") }
                        .sumOf { it.value.toIntOrNull() ?: 0 }

                    val current = userStats[userId]?.second ?: 0f
                    userStats[userId] = userName to (current + totalWeight)
                }
            }
        }

        return userStats.map { (userId, pair) ->
            LeaderboardEntry(
                userId = userId,
                userName = pair.first,
                statValue = pair.second,
                score = pair.second
            )
        }.sortedByDescending { it.score }
    }

    data class LeaderboardEntry(
        val userId: String,
        val statValue: Float,
        val userName: String,
        val score: Float,

    )



}
