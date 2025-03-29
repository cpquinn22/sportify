package com.example.sportify

import ViewModels.TeamViewModel
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import model.Workout
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AdminLogDetailScreen(
    teamId: String,
    workoutId: String,
    viewModel: TeamViewModel,
    onBack: () -> Unit = {}
) {
    var workout by remember { mutableStateOf<Workout?>(null) }
    var logs by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    LaunchedEffect(workoutId) {
        Log.d("AdminLogDetail", "Loading logs for team: $teamId, workout: $workoutId")
        workout = viewModel.loadWorkout(teamId, workoutId)
        logs = viewModel.loadAllLogsForWorkout(teamId, workoutId)
        Log.d("AdminLogDetail", "Logs loaded: ${logs.size}")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (workout == null) {
            Text("Loading workout...", style = MaterialTheme.typography.titleMedium)
        } else {
            Text("Logs for: ${workout!!.name}", style = MaterialTheme.typography.headlineSmall)

            if (logs.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("No logs found.")
            } else {
                // 🔁 Group logs by userId
                val logsByUser = logs.groupBy { it["userId"] ?: "unknownUser" }

                logsByUser.forEach { (userId, userLogs) ->
                    val userName = userLogs.firstOrNull()?.get("userName") ?: "Unknown User"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            userLogs
                                .sortedByDescending { it["timestamp"] }
                                .forEachIndexed { index, log ->
                                    val date = log["date"] ?: "Unknown Date"
                                    Text("Log ${index + 1} — $date", fontWeight = FontWeight.Bold)

                                    workout!!.steps.forEach { (stepKey, stepLabel) ->
                                        val logType = workout!!.logTypes[stepKey] ?: "None"
                                        val entry = formatLogEntry(stepKey, stepLabel, logType, log)
                                        Text(entry, style = MaterialTheme.typography.bodyMedium)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                        }
                    }
                }
            }
        }
    }
}