package com.example.sportify

import ViewModels.TeamViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import model.Workout
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogWorkoutScreen(
    teamId: String,
    workoutId: String,
    viewModel: TeamViewModel,
    onLogSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    var workout by remember { mutableStateOf<Workout?>(null) }
    val responses = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(workoutId) {
        val loadedWorkout = viewModel.loadWorkout(teamId, workoutId)
        workout = loadedWorkout
        loadedWorkout?.logFields?.keys?.forEach { fieldKey ->
            responses[fieldKey] = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (workout == null) {
            Text("Loading workout...", style = MaterialTheme.typography.titleMedium)
        } else {
            Text(text = workout!!.name, style = MaterialTheme.typography.titleLarge)
            Text(text = workout!!.info, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))

            workout!!.steps.forEach { (stepKey, stepLabel) ->
                val logType = workout!!.logTypes[stepKey] ?: "None"

                when (logType) {
                    "Weight Training" -> {
                        listOf("Weight (kg)", "Reps", "Sets Complete").forEach { field ->
                            val key = "$stepKey-$field"
                            OutlinedTextField(
                                value = responses[key] ?: "",
                                onValueChange = { responses[key] = it },
                                label = { Text("$stepLabel - $field") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    "Running" -> {
                        val distKey = "$stepKey-Distance (km)"
                        val timeKey = "$stepKey-Time (minutes)"

                        OutlinedTextField(
                            value = responses[distKey] ?: "",
                            onValueChange = { responses[distKey] = it },
                            label = { Text("Distance (km)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = responses[timeKey] ?: "",
                            onValueChange = { responses[timeKey] = it },
                            label = { Text("Time (minutes)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val distance = responses[distKey]?.toFloatOrNull()
                        val time = responses[timeKey]?.toFloatOrNull()
                        if (distance != null && time != null && distance > 0) {
                            val pace = time / distance
                            Text("Pace: %.2f min/km".format(pace))
                        }
                    }

                    "Shooting" -> {
                        val shotsKey = "$stepKey-Shots Made"
                        OutlinedTextField(
                            value = responses[shotsKey] ?: "",
                            onValueChange = { responses[shotsKey] = it },
                            label = { Text("$stepLabel - Shots Made") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val made = responses[shotsKey]?.toIntOrNull()
                        val total = 15
                        if (made != null && made in 0..total) {
                            val pct = (made.toFloat() / total) * 100
                            Text("Shooting %: ${"%.0f".format(pct)}%")
                        }
                    }

                    else -> {
                        // No log fields for this step
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val currentDate = formatter.format(Date())

                    val user = Firebase.auth.currentUser
                    val userId = user?.uid ?: "unknown"
                    val userName = user?.displayName ?: "unknown"

                    val logDataWithDate = responses.toMutableMap().apply {
                        put("date", currentDate)
                        put("timestamp", System.currentTimeMillis().toString())
                        put("userId", userId)
                        put("userName", userName)
                    }

                    viewModel.saveWorkoutLog(
                        teamId,
                        workoutId,
                        logDataWithDate,
                        onSuccess = {
                            Toast.makeText(context, "Workout logged!", Toast.LENGTH_SHORT).show()
                            onLogSaved()
                        },
                        onError = {
                            Toast.makeText(context, "Error saving log.", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Workout Log")
            }
        }
    }
}