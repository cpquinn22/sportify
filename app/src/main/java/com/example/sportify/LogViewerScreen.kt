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
import androidx.compose.foundation.layout.height


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
    viewModel: TeamViewModel
) {
    var workout by remember { mutableStateOf<Workout?>(null) }
    var logs by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    LaunchedEffect(workoutId) {
        workout = viewModel.loadWorkout(teamId, workoutId)
        logs = viewModel.loadWorkoutLogs(teamId, workoutId)
    }

    Column(modifier = Modifier
        .fillMaxSize()
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
            logs
                .sortedByDescending { it["timestamp"] }
                .groupBy { it["date"] }
                .forEach { (date, logsForDate) ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = date ?: "Unknown Date",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    logsForDate.forEachIndexed { index, log ->
                        Text("Log ${index + 1}", style = MaterialTheme.typography.titleSmall)

                        workout!!.steps.forEach { (stepKey, stepLabel) ->
                            val logType = workout!!.logTypes[stepKey] ?: "None"
                            val line = formatLogEntry(stepKey, stepLabel, logType, log)
                            Text(text = line, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
        }
    }

}