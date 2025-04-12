package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import model.Workout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment


@Composable
fun CreateWorkoutScreen(
    teamId: String,
    viewModel: TeamViewModel,
    onWorkoutCreated: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf("") }
    val stepLogTypes = remember { mutableStateListOf("None") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create New Workout", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Workout Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = info,
            onValueChange = { info = it },
            label = { Text("Info") },
            modifier = Modifier.fillMaxWidth()
        )

        steps.forEachIndexed { index, step ->
            Column {
                OutlinedTextField(
                    value = step,
                    onValueChange = { newValue -> steps[index] = newValue },
                    label = { Text("Step ${index + 1}") },
                    modifier = Modifier.fillMaxWidth()
                )

                var expanded by remember { mutableStateOf(false) }
                val options = listOf("None", "Weight Training", "Running", "Shooting")

                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("Log Type: ${stepLogTypes[index]}")
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        options.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    stepLogTypes[index] = selection
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Button(onClick = {
            steps.add("")
            stepLogTypes.add("None")
        }, modifier = Modifier.align(Alignment.End)) {
            Text("Add Step")
        }

        Button(
            onClick = {
                if (name.isNotBlank() && info.isNotBlank() && steps.all { it.isNotBlank() }) {
                    val stepIds = steps.mapIndexed { i, _ -> "step_${i + 1}" }
                    val workout = Workout(
                        name = name,
                        info = info,
                        steps = steps.mapIndexed { i, step -> "step_${i + 1}" to step }.toMap(),
                        logTypes = steps.mapIndexed { i, _ -> "step_${i + 1}" to stepLogTypes[i] }.toMap(),
                                stepOrder = stepIds
                    )
                    viewModel.saveWorkoutToFirestore(teamId, workout)
                    onWorkoutCreated()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Workout")
        }
    }
}