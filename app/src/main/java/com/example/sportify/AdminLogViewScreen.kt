package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import model.Workout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun AdminLogViewScreen(
    teamId: String,
    viewModel: TeamViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    // collect list of workouts for team from ViewModel
    val workouts by viewModel.teamWorkouts.collectAsState(emptyList())

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
        Text("Select a Workout to View Logs", style = MaterialTheme.typography.headlineSmall)

        if (workouts.isEmpty()) {
            Text("No workouts found.")
        } else {
            workouts.forEach { (id, workout) ->
                Button(
                    onClick = {
                        // navigate to AdminLogDetail screen when a workout is selected
                        navController.navigate("adminLogDetail/$teamId/$id")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(workout.name)
                }
            }
        }
    }
}