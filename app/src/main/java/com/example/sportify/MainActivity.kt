package com.example.sportify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sportify.ui.theme.SportifyTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check authentication state
        val user = Firebase.auth.currentUser
        if (user == null) {
            // Navigate to AuthActivity if not signed in
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        } else {
            // Set content if user is signed in
            setContent {
                SportifyTheme {
                     Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MyApp(user.displayName ?: "User")
                    }
                }
            }
        }
    }

    @Composable
    fun MyApp(userName: String) {
        val navController = rememberNavController();
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { HomeScreen(navController, userName) }
        }
    }


    @Composable
    fun HomeScreen(navController: NavHostController, userName: String) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Welcome, $userName!")
            Button(onClick = { navController.navigate("home") }) {
                Text("Main Page")

            }
            LogoutScreen()
        }
    }


    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        SportifyTheme {
            HomeScreen(rememberNavController(), "Spotify")
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


}
