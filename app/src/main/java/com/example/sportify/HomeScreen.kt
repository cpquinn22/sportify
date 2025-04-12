package com.example.sportify

import ViewModels.TeamViewModel
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreen(navController: NavHostController,
               userName: String,
               viewModel: TeamViewModel
) {
    val userId = Firebase.auth.currentUser?.uid
    val isAdmin by viewModel.isAdmin.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(userId) {
        if (userId != null) {
            viewModel.fetchAdminStatus(userId)
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Log out button at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                Firebase.auth.signOut()
                context.startActivity(Intent(context, MainActivity::class.java))
                (context as ComponentActivity).finish()
            }) {
                Text("Log Out")
            }
        }

        // Centered content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Sportify!",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("sports") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Sports")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isAdmin) {
                Button(
                    onClick = { navController.navigate("createTeam") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create a Team")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = { navController.navigate("myTeams") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("My Teams")
            }
        }
    }
}


