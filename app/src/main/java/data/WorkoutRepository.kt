package data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import model.Workout

class WorkoutRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun createWorkout(teamId: String, workout: Workout): Boolean {
        return try {
            db.collection("teams")
                .document(teamId)
                .collection("workouts")
                .add(workout)
                .await()
            Log.d("WorkoutRepo", "✅ Workout created: ${workout.name}")
            true
        } catch (e: Exception) {
            Log.e("WorkoutRepo", "❌ Failed to create workout", e)
            false
        }
    }
}