package data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.type.Date
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

data class Drill(
    val name: String = "",
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
            val snapshot =
                drillsCollection.document(sportName).collection("logs_$drillName").get().await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
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
}