package com.example.sportify

import ViewModels.TeamViewModel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun TeamScreen(navController: NavController, viewModel: TeamViewModel, userId: String) {
    var teamName by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf("") }
    var sportsList by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Fetch sports from Firestore
    LaunchedEffect(Unit) {
        sportsList = viewModel.getSportsList()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Create a New Team", style = MaterialTheme.typography.headlineLarge)

        // Team Name Input
        OutlinedTextField(
            value = teamName,
            onValueChange = { teamName = it },
            label = { Text("Enter Team Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sport Dropdown Menu
        if (sportsList.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }

            Box {
                OutlinedTextField(
                    value = selectedSport,
                    onValueChange = {},
                    label = { Text("Select Sport") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    sportsList.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport) },
                            onClick = {
                                selectedSport = sport
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create Team Button
        Button(
            onClick = {
                if (teamName.isNotBlank() && selectedSport.isNotBlank()) {
                    coroutineScope.launch {
                        val teamId = viewModel.createTeam(teamName, selectedSport, userId)
                        if (teamId != null) {
                            navController.navigate("home") // Navigate back to HomeScreen
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = teamName.isNotBlank() && selectedSport.isNotBlank()
        ) {
            Text("Create Team")
        }
    }
}
