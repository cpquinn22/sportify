package data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class Drill(
    val name: String = "",
    val steps: Map<String, String> = emptyMap()
)

class DrillsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val drillsCollection = firestore.collection("drills")

    // Fetch drills for a given sport
    suspend fun getDrillsBySport(sportName: String): Map<String, Drill> {
        return try {
            val documentSnapshot = drillsCollection.document(sportName).get().await()
            val data = documentSnapshot.data

            data?.mapValues { entry ->
                val drillData = entry.value as Map<*, *>
                Drill(
                    name = drillData["name"] as String,
                    steps = drillData.filterKeys { it.toString().startsWith("step_") } as Map<String, String>
                )
            } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}