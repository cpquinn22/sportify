package ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.TeamRepository
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

class TeamViewModel : ViewModel() {

    private val repository = TeamRepository() // No need for factory!

    private val _userTeams = MutableStateFlow<Map<String, String>>(emptyMap())
    val userTeams: StateFlow<Map<String, String>> = _userTeams

    private val _selectedTeam = MutableStateFlow<Team?>(null)
    val selectedTeam: StateFlow<Team?> = _selectedTeam

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
            }
            .addOnFailureListener { e ->
                Log.e("WorkoutSave", "Error saving workout", e)
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

}
