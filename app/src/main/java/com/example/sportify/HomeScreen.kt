package com.example.sportify

import ViewModels.TeamViewModel
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreen(
    navController: NavController,
    userName: String,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Logout Button
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            Button(onClick = onLogout) {
                Text("Log Out")
            }
        }

        // Centered main content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to Sportify!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(onClick = { navController.navigate("sports") }) {
                Text("Go to Sports")
            }

            Button(onClick = { navController.navigate("createTeam") }) {
                Text("Create a team")
            }

            Button(onClick = { navController.navigate("myTeams") }) {
                Text("My Teams")
            }
        }
    }
}


@Composable
fun LogoutScreen() {
    // Retrieve the context in a valid composable scope
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(text = "You are signed in!")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            Firebase.auth.signOut()
            context.startActivity(Intent(context, MainActivity::class.java))
            (context as ComponentActivity).finish()
        }) {
            Text("Log Out")
        }
    }
}
