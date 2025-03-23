package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import model.Workout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AdminWorkoutListScreen(
    teamId: String,
    workouts: List<Pair<String, Workout>>,
    navController: NavHostController,
    viewModel: TeamViewModel
) {
    LaunchedEffect(teamId) {
        viewModel.fetchTeamWorkouts(teamId)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Available Workouts:", style = MaterialTheme.typography.headlineSmall)

        workouts.forEach { (id, workout) ->
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                navController.navigate("logWorkout/$teamId/$id")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Log")
                        }

                        OutlinedButton(
                            onClick = {
                                navController.navigate("logViewer/$teamId/$id")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("View Logs")
                        }
                    }
                }
            }
        }
    }
}