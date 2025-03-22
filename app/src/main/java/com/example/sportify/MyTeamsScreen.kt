package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


@Composable
fun MyTeamsScreen(viewModel: TeamViewModel, userId: String, navController: NavHostController) {
    val teams by viewModel.userTeams.collectAsState()
    val userId = Firebase.auth.currentUser?.uid


    // Load user's teams when screen loads
    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.fetchUserTeams(userId)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("My Teams", style = MaterialTheme.typography.titleLarge)

        if (teams.isEmpty()) {
            Text("You are not part of any teams.")
        } else {
            teams.forEach { teamId ->
                Button(
                    onClick = {
                        // Navigate to team details screen
                        navController.navigate("teamDetails/$teamId")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Team ID: $teamId") // You can fetch full team names in future
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { navController.navigate("home") }) {
            Text("Back to Home")
        }
    }
}