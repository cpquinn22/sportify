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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment


@Composable
fun CreateWorkoutScreen(
    teamId: String,
    viewModel: TeamViewModel,
    onWorkoutCreated: () -> Unit
)
    {
    var name by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
        val steps = remember { mutableStateListOf("") }

    Column(modifier = Modifier
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
            OutlinedTextField(
                value = step,
                onValueChange = { newValue ->
                    steps[index] = newValue
                },
                label = { Text("Step ${index + 1}") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = {
                steps.add("")
            },
            Modifier.align(Alignment.End)
        ) {
            Text("Add Step")
        }

        Button(
            onClick = {
                if (name.isNotBlank() && info.isNotBlank() && steps.all { it.isNotBlank() }) {
                    val workout = Workout(
                        name = name,
                        info = info,
                        steps = steps.mapIndexed { index, step -> "step_${index + 1}" to step }.toMap()
                    )
                    viewModel.saveWorkoutToFirestore(teamId, workout)
                    onWorkoutCreated() // Navigate back or show confirmation
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Workout")
        }
    }
}