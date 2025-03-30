package com.example.sportify

import ViewModels.TeamViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LeaderboardScreen(
    teamId: String,
    viewModel: TeamViewModel,
    onBack: () -> Unit
) {
    val leaderboardEntries by viewModel.leaderboardEntries.collectAsState()

    val filterOptions = listOf("Most Active", "Top Runner", "Top Shooter", "Top Lifter")
    var currentFilter by remember { mutableStateOf(filterOptions.first()) }
    LaunchedEffect(currentFilter) {
        viewModel.loadLeaderboard(teamId, currentFilter)
    }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter dropdown
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = currentFilter)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filterOptions.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter) },
                        onClick = {
                            expanded = false
                            currentFilter = filter
                            viewModel.loadLeaderboard(teamId, filter)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Leaderboard Entries
        if (leaderboardEntries.isEmpty()) {
            Text("No data available.", style = MaterialTheme.typography.bodyLarge)
        } else {
            leaderboardEntries.forEachIndexed { index, entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "#${index + 1}: ${entry.userName}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val scoreLabel = when (currentFilter) {
                            "Most Active" -> "${entry.statValue.toInt()} logs"
                            "Top Runner" -> "${"%.2f".format(entry.statValue)} km"
                            "Top Shooter" -> "${entry.statValue.toInt()} shots made"
                            "Top Lifter" -> "${entry.statValue.toInt()} kg"
                            else -> "${entry.statValue}"
                        }

                        Text(text = "Score: $scoreLabel")
                    }
                }
            }
        }
    }
}

