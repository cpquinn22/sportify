package data

import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.type.Date
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

data class Drill(
    val name: String = "",
    val description: String,
    val steps: Map<String, String> = emptyMap()
)

class DrillsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    val drillsCollection = firestore.collection("drills")

    // Fetch drills for a given sport
    suspend fun getDrillsBySport(sportName: String): Map<String, Drill> {
        return try {
            val documentSnapshot = drillsCollection.document(sportName).get().await()
            val data = documentSnapshot.data

            data?.mapValues { entry ->
                val drillData = entry.value as Map<*, *>
                Drill(
                    name = drillData["name"] as String,
                    description = drillData["description"] as? String ?: "",
                    steps = drillData.filterKeys {
                        it.toString().startsWith("step_")
                    } as Map<String, String>
                )
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun addLogToFirestore(
        sportName: String,
        drillName: String,
        shotsMade: Int,
        shootingPercentage: Int
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val log = hashMapOf(
            "shotsMade" to shotsMade,
            "shootingPercentage" to shootingPercentage,
            "timestamp" to Timestamp.now(),
            "userId" to user.uid,
            "userName" to (user.displayName ?: user.email ?: "Unknown")
        )
        val drillLogsCollection = drillsCollection.document(sportName).collection("logs_$drillName")

        drillLogsCollection.add(log).addOnSuccessListener {
            Log.d("Firestore", "Log added successfully for $sportName")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error adding log $sportName", e)
        }
    }

    fun addBasketballLogsListener(
        drillKey: String,
        logs: SnapshotStateList<Map<String, Any>>
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("drills")
            .document("Basketball")
            .collection("logs_${drillKey.lowercase()}")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("BasketballLogs", "Error fetching user-specific basketball logs", e)
                    return@addSnapshotListener
                }

                logs.clear()
                snapshot?.documents?.mapNotNull { it.data }?.let { logs.addAll(it) }
            }
    }

    suspend fun getLogsByDrill(sportName: String, drillName: String): List<Map<String, Any>> {
        return try {
            // Determine naming convention based on sportName
            val normalizedDrillName = when (sportName) {
                "Basketball" -> drillName // Retain spaces for Basketball
                "WeightTraining" -> drillName.lowercase() // Convert to lowercase for WeightTraining
                else -> drillName // Default case
            }

            val collectionName = "logs_$normalizedDrillName"

            // Order logs by timestamp (newest first)
            val snapshot = drillsCollection.document(sportName)
                .collection(collectionName)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val logs = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null && data["userId"] == userId) data else null
            }
            Log.d("Firestore", "✅ Logs fetched for $sportName - $drillName: $logs")
            logs
        } catch (e: Exception) {
            Log.e("Firestore", "❌ Error fetching logs for $sportName - $drillName", e)
            emptyList()
        }
    }

    fun formatTimestampToDateString(timestamp: Any?): String {
        return if (timestamp is Timestamp) {
            val date = timestamp.toDate()
            val format = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            format.format(date)
        } else if (timestamp is Long) {
            val date = java.util.Date(timestamp)
            val format = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            format.format(date)
        } else {
            "Unknown Date"
        }

    }

    suspend fun getWeightTrainingDrills(): Map<String, Drill> {
        return try {
            val documentSnapshot = drillsCollection.document("WeightTraining").get().await()
            documentSnapshot.data?.mapValues { entry ->
                val exerciseData = entry.value as Map<*, *>
                Drill(
                    name = entry.key,
                    description = exerciseData["description"] as? String ?: "",
                    steps = (exerciseData["Steps"] as? Map<String, String>) ?: emptyMap()
                )
            } ?: emptyMap()
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching weight training drills", e)
            emptyMap()
        }
    }

    fun addWeightTrainingLog(
        exerciseName: String,
        weight: Int,
        setNumber: Int,
        reps: Int
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val log = mapOf(
            "weight" to weight,
            "setNumber" to setNumber,
            "reps" to reps,
            "timestamp" to FieldValue.serverTimestamp(),
            "userId" to user.uid,
            "userName" to (user.displayName ?: user.email ?: "Unknown")
        )

        val logsCollection = drillsCollection.document("WeightTraining")
            .collection("logs_${exerciseName.lowercase()}")

        logsCollection.add(log)
            .addOnSuccessListener {
                Log.d("Firestore", "Successfully added log for $exerciseName")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding log for $exerciseName", e)
            }
    }

    suspend fun getFitnessDrills(): Map<String, Drill> {
        return try {
            val documentSnapshot = drillsCollection.document("Fitness").get().await()
            documentSnapshot.data?.mapValues { entry ->
                val fitnessData = entry.value as Map<*, *>
                Drill(
                    name = fitnessData["name"] as String,
                    description = fitnessData["description"] as? String ?: "",
                    steps = (fitnessData["steps"] as? Map<String, String>) ?: emptyMap()
                )
            } ?: emptyMap()
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching fitness drills", e)
            emptyMap()
        }
    }

    fun addFitnessLog(drillName: String, totalTime: Float, avgTimePerKm: Float) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val log = mapOf(
            "totalTime" to totalTime,
            "avgTimePerKm" to avgTimePerKm,
            "timestamp" to FieldValue.serverTimestamp(),
            "userId" to user.uid,
            "userName" to (user.displayName ?: user.email ?: "Unknown")
        )

        val logsCollection = drillsCollection.document("Fitness")
            .collection("logs_${drillName.lowercase()}")

        logsCollection.add(log)
            .addOnSuccessListener {
                Log.d("Firestore", "Successfully added log for $drillName")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding log for $drillName", e)
            }
    }

    fun logRounds(drillType: String, rounds: Int) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val log = mapOf(
            "roundsCompleted" to rounds,
            "timestamp" to FieldValue.serverTimestamp(),
            "userId" to user.uid,
            "userName" to (user.displayName ?: user.email ?: "Unknown")
        )

        drillsCollection.document("Fitness").collection("logs_$drillType")
            .add(log)
            .addOnSuccessListener {
                Log.d("Firestore", "Rounds log added successfully for $drillType")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding rounds log for $drillType", e)
            }
    }

    fun addLogsSnapshotListener(
        drillKey: String,
        logs: SnapshotStateList<Map<String, Any>>,
        sport: String = "Fitness" // or "WeightTraining", "Basketball", etc.
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val currentUserId = currentUser.uid

        val logsCollection = FirebaseFirestore.getInstance()
            .collection("drills")
            .document(sport)
            .collection("logs_${drillKey.lowercase()}")

        logsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "❌ Error fetching logs for $drillKey", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    logs.clear()

                    val filteredLogs = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null && data["userId"] == currentUserId) {
                            data
                        } else {
                            null
                        }
                    }

                    logs.addAll(filteredLogs)
                    Log.d("Firestore", "✅ Filtered logs for $drillKey: ${filteredLogs.size}")
                } else {
                    logs.clear()
                    Log.d("Firestore", "ℹ️ No logs found for $drillKey")
                }
            }
    }
}