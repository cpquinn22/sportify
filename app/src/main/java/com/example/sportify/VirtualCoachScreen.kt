package com.example.sportify

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import data.DrillsRepository
import kotlinx.coroutines.tasks.await

suspend fun generateCoachFeedbackWithInsights(
    selectedDrill: String,
    userId: String
): String {
    val firestore = FirebaseFirestore.getInstance()

    // Map user-friendly drill names to their Firestore collection keys
    val drillKeys = mapOf(
        "3 Point Shooting" to "3 Point Shooting",
        "Free Throw" to "Free Throw",
        "Mid Range Shooting" to "Mid Range Shooting",
        "Deadlift" to "deadlift",
        "Squat" to "squat",
        "Bench Press" to "benchpress",
        "5K Run" to "5k_run",
        "Interval Sprints" to "interval_sprints",
        "Skipping" to "skipping"
    )

    // Determine the sport category
    val drillCollection = when (selectedDrill) {
        in listOf("3 Point Shooting", "Free Throw", "Mid Range Shooting") -> "Basketball"
        in listOf("Deadlift", "Squat", "Bench Press") -> "WeightTraining"
        else -> "Fitness"
    }

    // Get the Firestore collection key
    val drillKey = drillKeys[selectedDrill] ?: return "Unknown drill selected."

    val logsRef = firestore
        .collection("drills")
        .document(drillCollection)
        .collection("logs_$drillKey")

    return try {
        val snapshot = logsRef
            .whereEqualTo("userId", userId)
            .get()
            .await()

        val logs = snapshot.documents.mapNotNull { it.data }

        if (logs.isEmpty()) {
            return "No performance data found for $selectedDrill yet."
        }

        // Basic performance insights by drill
        when (selectedDrill) {
            "3 Point Shooting", "Mid Range Shooting" -> {
                val percentages = logs.mapNotNull { it["shootingPercentage"] as? Long }
                val avg = percentages.average()
                val best = percentages.maxOrNull()
                val worst = percentages.minOrNull()

                buildString {
                    append("📊 Your Shooting Stats\n")
                    append("• Average: ${"%.1f".format(avg)}%\n")
                    append("• Best: $best%, Worst: $worst%\n")
                    append(
                        when {
                            avg >= 70 -> "🔥 You're lights out from range! Keep it up!"
                            avg >= 50 -> "🎯 Solid performance. Focus on improving consistency."
                            else -> "⛹️‍♂️ Focus on form, follow-through, and repetition."
                        }
                    )
                }
            }

            "Free Throw" -> {
                val percentages = logs.mapNotNull { it["shootingPercentage"] as? Long }
                val avg = percentages.average()
                buildString {
                    append("🏀 Free Throw Accuracy: ${"%.1f".format(avg)}%\n")
                    append(
                        when {
                            avg >= 85 -> "🎯 Automatic! You've got a great routine."
                            avg >= 65 -> "💡 Good. Try building consistency with a routine."
                            else -> "📌 Focus on fundamentals: stance, breath, follow-through."
                        }
                    )
                }
            }

            "Deadlift", "Squat", "Bench Press" -> {
                val weights = logs.mapNotNull { it["weight"] as? Long }
                val sets = logs.mapNotNull { it["setNumber"] as? Long }
                val totalLifted = weights.zip(sets).sumOf { it.first * it.second }
                val avgWeight = if (weights.isNotEmpty()) weights.average() else 0.0
                val max = weights.maxOrNull()

                buildString {
                    append("🏋️ Total Volume: $totalLifted kg\n")
                    append("• Average Weight: ${"%.1f".format(avgWeight)} kg\n")
                    append("• Max Lift: $max kg\n")
                    append(
                        when {
                            avgWeight >= 100 -> "💪 Strong work! Focus on progressive overload and recovery."
                            avgWeight >= 60 -> "📈 You're on the right track. Work on form and technique."
                            else -> "🏗️ Build up gradually and master the movement pattern first."
                        }
                    )
                }
            }

            "5K Run" -> {
                val times = logs.mapNotNull { it["totalTime"] as? Double }
                val avg = times.average()
                val best = times.minOrNull()

                buildString {
                    append("🏃 Avg 5K Time: ${"%.2f".format(avg)} min\n")
                    append("🏁 Best Time: ${"%.2f".format(best)} min\n")
                    append(
                        when {
                            avg <= 25 -> "🔥 You're in great shape! Keep training smart."
                            avg <= 30 -> "✅ Nice pace! Try incorporating tempo runs to get faster."
                            else -> "💡 Focus on building aerobic base and endurance with intervals."
                        }
                    )
                }
            }

            "Interval Sprints", "Skipping" -> {
                val rounds = logs.mapNotNull { it["roundsCompleted"] as? Long }
                val avg = rounds.average()
                val max = rounds.maxOrNull()

                buildString {
                    append("⏱️ Avg Rounds: ${"%.1f".format(avg)}\n")
                    append("🏅 Max Rounds: $max\n")
                    append(
                        when {
                            avg >= 10 -> "💨 Excellent endurance! Consider increasing intensity."
                            avg >= 5 -> "👍 You're doing great. Keep progressing steadily."
                            else -> "👟 Try shorter intervals more frequently to build stamina."
                        }
                    )
                }
            }

            else -> "No custom feedback available for this drill yet."
        }
    } catch (e: Exception) {
        Log.e("VirtualCoach", "Error fetching logs", e)
        "⚠️ Error generating feedback. Please try again."
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualCoachScreen() {
    val firestore = FirebaseFirestore.getInstance()
    val userName = remember { mutableStateOf("") }
    val userId = Firebase.auth.currentUser?.uid ?: ""
    val selectedDrill = remember { mutableStateOf("") }
    val expanded = remember { mutableStateOf(false) }
    val performanceFeedback = remember { mutableStateOf("") }

    // Drill categories
    val categorizedDrills = mapOf(
        "🏀 Shooting" to listOf("3 Point Shooting", "Free Throw", "Mid Range Shooting"),
        "🏋️ Weight Training" to listOf("Deadlift", "Squat", "Bench Press"),
        "💪 Fitness" to listOf("5K Run", "Interval Sprints", "Skipping")
    )

    // Curated static tips
    val drillTips = mapOf(
        "3 Point Shooting" to listOf(
            "🏀 Focus on quick release and consistent arc.",
            "📍 Practice from different spots around the arc.",
            "⛹️‍♂️ Shoot off the catch and off the dribble."
        ),
        "Free Throw" to listOf(
            "🎯 Use the same pre-shot routine each time.",
            "🙌 Focus on your follow-through.",
            "🦶 Keep feet shoulder-width apart and square to the rim."
        ),
        "Mid Range Shooting" to listOf(
            "💨 Practice quick stops off the dribble.",
            "🌀 Combine footwork with shot mechanics.",
            "🧱 Work on elevation with control."
        ),
        "Deadlift" to listOf(
            "🔒 Keep your back straight, hinge at the hips.",
            "🧠 Brace your core and engage your lats.",
            "🧱 Lift with control and avoid jerking the bar."
        ),
        "Squat" to listOf(
            "🦵 Push through heels, chest up.",
            "🏋️ Experiment with stance for balance and depth.",
            "📏 Brace your core before each rep."
        ),
        "Bench Press" to listOf(
            "📐 Slight arch in your lower back for stability.",
            "🦶 Strong leg drive helps full-body tension.",
            "🔥 Warm up triceps and shoulders properly."
        ),
        "5K Run" to listOf(
            "🏃 Use tempo and interval training to improve pace.",
            "📏 Track average pace per KM.",
            "🕒 Focus on even pacing instead of fast starts."
        ),
        "Skipping" to listOf(
            "🔁 Keep elbows in and wrists relaxed.",
            "⛹️‍♀️ Jump just enough to clear the rope.",
            "⏱️ Alternate high and low intensity intervals."
        ),
        "Interval Sprints" to listOf(
            "⚡ Sprint at 80–90% effort, rest 2x your sprint time.",
            "🚦 Use cones or distance markers for consistency.",
            "🌀 Improve explosiveness with plyometrics."
        )
    )

    // Fetch user name on load
    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    userName.value = doc.getString("name") ?: ""
                }
        }
    }

    // Fetch performance feedback when drill is selected
    LaunchedEffect(selectedDrill.value) {
        if (selectedDrill.value.isNotEmpty() && userId.isNotEmpty()) {
            val feedback = generateCoachFeedbackWithInsights(selectedDrill.value, userId)
            performanceFeedback.value = feedback
        }
    }

    val tips = drillTips[selectedDrill.value] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (userName.value.isNotEmpty()) "Virtual Coach for ${userName.value}" else "Virtual Coach",
            style = MaterialTheme.typography.headlineSmall
        )

        // Drill Selector
        ExposedDropdownMenuBox(
            expanded = expanded.value,
            onExpandedChange = { expanded.value = !expanded.value }
        ) {
            OutlinedTextField(
                value = selectedDrill.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Drill") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                categorizedDrills.forEach { (category, drills) ->
                    DropdownMenuItem(
                        text = { Text(category, style = MaterialTheme.typography.labelLarge, color = Gray) },
                        onClick = {},
                        enabled = false
                    )
                    drills.forEach { drill ->
                        DropdownMenuItem(
                            text = { Text(drill) },
                            onClick = {
                                selectedDrill.value = drill
                                expanded.value = false
                            }
                        )
                    }
                }
            }
        }

        // Feedback Card
        if (selectedDrill.value.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tips for ${selectedDrill.value}", style = MaterialTheme.typography.titleMedium)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (performanceFeedback.value.isNotEmpty()) {
                        Text(
                            text = performanceFeedback.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2E7D32) // Green tone for feedback
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    tips.forEach { tip ->
                        Text("• $tip", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}


