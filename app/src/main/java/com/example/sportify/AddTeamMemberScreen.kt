package com.example.sportify

import ViewModels.AdminViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier

@Composable
fun AddTeamMemberScreen(teamId: String, navController: NavHostController) {
    val context = LocalContext.current
    val adminViewModel: AdminViewModel = viewModel()
    var emailInput by remember { mutableStateOf("") }   // holds email entered by user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Add Team Member", style = MaterialTheme.typography.headlineLarge)

        TextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Enter user email") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                // call ViewModel function to add user by email
                adminViewModel.addUserToTeamByEmail(emailInput, teamId) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    if (success) {
                        emailInput = ""
                        navController.popBackStack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Member")
        }
    }
}