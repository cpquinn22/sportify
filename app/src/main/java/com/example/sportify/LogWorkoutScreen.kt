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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@Composable
fun LogWorkoutScreen(
    teamId: String,
    workoutId: String,
    viewModel: TeamViewModel,
    onLogSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    // holds loaded workout
    var workout by remember { mutableStateOf<Workout?>(null) }
    // map that holds the users responses
    val responses = remember { mutableStateMapOf<String, String>() }

    // load workout and prepare response fields based on log type
    LaunchedEffect(workoutId) {
        val loadedWorkout = viewModel.loadWorkout(teamId, workoutId)
        workout = loadedWorkout
        // initialize expected log fields based on log type
        loadedWorkout?.stepOrder?.forEach { stepKey ->
            when (loadedWorkout.logTypes[stepKey]) {
                "Weight Training" -> {
                    listOf("Weight (kg)", "Reps", "Sets Complete").forEach { field ->
                        responses["$stepKey-$field"] = ""
                    }
                }
                "Running" -> {
                    responses["$stepKey-Distance (km)"] = ""
                    responses["$stepKey-Time (minutes)"] = ""
                }
                "Shooting" -> {
                    responses["$stepKey-Shots Made"] = ""
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (workout == null) {
            Text("Loading workout...", style = MaterialTheme.typography.titleMedium)
        } else {
            Text(text = workout!!.name, style = MaterialTheme.typography.titleLarge)
            Text(text = workout!!.info, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(12.dp))

            // loop through each step and show appropriate input fields
            workout!!.stepOrder.forEach { stepKey ->
                val stepLabel = workout!!.steps[stepKey] ?: return@forEach
                val logType = workout!!.logTypes[stepKey] ?: "None"

                when (logType) {
                    "Weight Training" -> {
                        listOf("Weight (kg)", "Reps", "Sets Complete").forEach { field ->
                            val key = "$stepKey-$field"
                            OutlinedTextField(
                                value = responses[key] ?: "",
                                onValueChange = { input ->
                                    responses[key] = input.filter { it.isDigit() } // only allow numbers
                                },
                                label = { Text("$stepLabel - $field") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    "Running" -> {
                        val distKey = "$stepKey-Distance (km)"
                        val timeKey = "$stepKey-Time (minutes)"

                        OutlinedTextField(
                            value = responses[distKey] ?: "",
                            onValueChange = { input ->
                                responses[distKey] = input.filter { it.isDigit() || it == '.' } // allow decimals
                            },
                            label = { Text("Distance (km)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = responses[timeKey] ?: "",
                            onValueChange = { input ->
                                responses[timeKey] = input.filter { it.isDigit() || it == '.' }
                            },
                            label = { Text("Time (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )

                        // automatically calculate and show pace
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
                            onValueChange = { input ->
                                responses[shotsKey] = input.filter { it.isDigit() }
                            },
                            label = { Text("$stepLabel - Shots Made") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                        )

                        val made = responses[shotsKey]?.toIntOrNull()
                        val total = 15
                        if (made != null && made in 0..total) {
                            val pct = (made.toFloat() / total) * 100
                            Text("Shooting %: ${"%.0f".format(pct)}%")
                        }
                    }

                    else -> {
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // submit button to log workout
            Button(
                onClick = {
                    // validate all fields are filled
                    val hasEmptyField = responses.values.any { it.isBlank() }
                    if (hasEmptyField) {
                        Toast.makeText(context, "Please fill in all fields before submitting.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // validate all values are numeric
                    val nonNumeric = responses.any { (_, value) -> value.toFloatOrNull() == null }
                    if (nonNumeric) {
                        Toast.makeText(context, "Please enter numbers only in all fields.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // create timestamp and user info
                    val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    val currentDate = formatter.format(Date())

                    val user = Firebase.auth.currentUser
                    val userId = user?.uid ?: "unknown"
                    val userName = user?.displayName ?: "unknown"

                    // add meta data to the log
                    val logDataWithDate = responses.toMutableMap().apply {
                        put("date", currentDate)
                        put("timestamp", System.currentTimeMillis().toString())
                        put("userId", userId)
                        put("userName", userName)
                    }

                    // save log to firestore using the ViewModel
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