package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController


@Composable
fun TeamScreen(
    teamId: String,
    userId: String?,
    navController: NavHostController,
    viewModel: TeamViewModel
) {
    val teamDetails by viewModel.selectedTeam.collectAsState()
    val workouts by viewModel.teamWorkouts.collectAsState(emptyList())

    LaunchedEffect(teamId) {
        viewModel.loadTeamDetails(teamId)
        viewModel.fetchTeamWorkouts(teamId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        teamDetails?.let { team ->
            Text("Team: ${team.name}", style = MaterialTheme.typography.headlineMedium)
            Text("Sport: ${team.sport}")
            Spacer(modifier = Modifier.height(16.dp))

            if (team.adminId == userId) {
                Button(onClick = {
                    navController.navigate("createWorkout/$teamId")
                }) {
                    Text("Create Workout")
                }
                Button(onClick = { /* TODO: Navigate to CreateMatch */ }) {
                    Text("Create Match")
                }
                Button(onClick = { /* TODO: Navigate to CreateEvent */ }) {
                    Text("Create Event")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    navController.navigate("workoutList/$teamId")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Available Workouts")
            }
                }
            }
        }
