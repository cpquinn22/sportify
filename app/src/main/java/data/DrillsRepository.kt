package data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
        val log = hashMapOf(
            "shotsMade" to shotsMade,
            "shootingPercentage" to shootingPercentage,
            "timestamp" to Timestamp.now()
        )
        val drillLogsCollection = drillsCollection.document(sportName).collection("logs_$drillName")

        drillLogsCollection.add(log).addOnSuccessListener {
            Log.d("Firestore", "Log added successfully for $sportName")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error adding log $sportName", e)
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
            val snapshot =
                drillsCollection.document(sportName).collection(collectionName).get().await()
            val logs = snapshot.documents.map { it.data ?: emptyMap() }
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
        val log = mapOf(
            "weight" to weight,
            "setNumber" to setNumber,
            "reps" to reps,
            "timestamp" to FieldValue.serverTimestamp()
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

}