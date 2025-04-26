package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import model.Workout
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


fun formatLogEntry(
    stepKey: String,
    stepLabel: String,
    logType: String,
    responses: Map<String, String>
): String {
    return when (logType) {
        "Weight Training" -> {
            val weight = responses["$stepKey-Weight (kg)"] ?: "-"
            val reps = responses["$stepKey-Reps"] ?: "-"
            val sets = responses["$stepKey-Sets Complete"] ?: "-"
            "$stepLabel: $weight kg, $reps reps x $sets sets"
        }

        "Running" -> {
            val distance = responses["$stepKey-Distance (km)"]?.toFloatOrNull()
            val time = responses["$stepKey-Time (minutes)"]?.toFloatOrNull()
            if (distance != null && time != null && distance > 0) {
                val pace = time / distance
                "$distance km in $time min (Avg: %.2f min/km)".format(pace)
            } else {
                "$stepLabel: Distance or Time missing"
            }
        }

        "Shooting" -> {
            val made = responses["$stepKey-Shots Made"]?.toIntOrNull()
            val total = 15
            if (made != null) {
                val pct = (made.toFloat() / total) * 100
                "$stepLabel: $made made (${pct.toInt()}%)"
            } else {
                "$stepLabel: No data"
            }
        }

        else -> "$stepLabel: No log"
    }
}

@Composable
fun LogViewerScreen(
    teamId: String,
    workoutId: String,
    viewModel: TeamViewModel // ViewModel used to fetch logs and workout data
) {
    // holds workout metadata
    var workout by remember { mutableStateOf<Workout?>(null) }

    // list of logs submitted by the current user
    var logs by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    val currentUserId = Firebase.auth.currentUser?.uid

    // load workout details and filter logs to only those created by current user
    LaunchedEffect(workoutId) {
        workout = viewModel.loadWorkout(teamId, workoutId)
        val allLogs = viewModel.loadWorkoutLogs(teamId, workoutId)
        logs = allLogs.filter { it["userId"] == currentUserId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (workout == null) {
            Text("Loading...", style = MaterialTheme.typography.titleMedium)
            return
        }

        Text("Logs for: ${workout!!.name}", style = MaterialTheme.typography.headlineSmall)

        if (logs.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("No logs found.")
        } else {
            // group logs by date and display each entry inside a styled card
            logs
                // most recent log first
                .sortedByDescending { it["timestamp"] }
                .groupBy { it["date"] ?: "Unknown Date" }
                .forEach { (date, logsForDate) ->
                    logsForDate.forEachIndexed { index, log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Log ${index + 1} — $date",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                // loop through step and display results for each
                                workout!!.steps.forEach { (stepKey, stepLabel) ->
                                    val logType = workout!!.logTypes[stepKey] ?: "None"
                                    val line = formatLogEntry(stepKey, stepLabel, logType, log)
                                    Text(text = line, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
        }
    }
}

