package com.example.sportify

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreen(navController: NavHostController, userName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to Sportify, $userName!")
        Button(onClick = { navController.navigate("sports") }) {
            Text("Go to Sports")
        }
        Button(onClick = { navController.navigate("createTeam") }) {
            Text("Create a team")
        }
        Spacer(modifier = Modifier.height(16.dp))
        LogoutScreen()
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
