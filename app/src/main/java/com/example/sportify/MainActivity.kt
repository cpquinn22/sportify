package com.example.sportify

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.sportify.ui.theme.SportifyTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import data.Drill
import data.DrillsRepository



// Data Model
data class Sport(
    val name: String,
    val imageResId: Int // For now, use local drawable resource IDs
)

// ViewModel
class SportsViewModel : ViewModel() {

    private val _sports = MutableStateFlow<List<Sport>>(listOf())
    val sports: StateFlow<List<Sport>> get() = _sports

    init {
        loadSports()
    }

    private fun loadSports() {
        _sports.value = listOf(
            Sport("Basketball", R.drawable.basketball),
            Sport("Tennis", R.drawable.tennis),
            Sport("Football", R.drawable.football)
        )
    }

}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        // Request notification permission (For Android 13+)
        requestNotificationPermission()

        // Subscribe to FCM topic for daily workout reminders
        subscribeToWorkoutReminders()

        // Get and log the FCM token
        retrieveFcmToken()

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

    /**
     * Request notification permission (Required for Android 13+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) { // API 33+ (Android 13+)
            val permission = "android.permission.POST_NOTIFICATIONS"
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
            }
        }
    }

    /**
     * Subscribe to the "workout_reminders" topic
     */
    private fun subscribeToWorkoutReminders() {
        FirebaseMessaging.getInstance().subscribeToTopic("workout_reminders")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "✅ Successfully subscribed to 'workout_reminders' topic")
                } else {
                    Log.e("FCM", "❌ Failed to subscribe to topic", task.exception)
                }
            }
    }

    /**
     * Retrieve and log the Firebase Cloud Messaging (FCM) token
     */
    private fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FCM", "❌ Failed to get FCM token", task.exception)
                    return@addOnCompleteListener
                }
                // Get the new token
                val token = task.result
                Log.d("FCM", "🔑 FCM Token: $token")

                // Here, you could send the token to your backend if needed
            }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        SportifyTheme {
            HomeScreen(rememberNavController(), "Sportify")
        }
    }

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
            Spacer(modifier = Modifier.height(16.dp))
            LogoutScreen()
        }
    }

    @Composable
    fun SportsScreen(navController: NavHostController, viewModel: SportsViewModel = viewModel()) {
        val sports by viewModel.sports.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            sports.forEach { sport ->
                SportCard(sport) {
                    navController.navigate("details/${sport.name}") {
                        Log.d("Navigation", "Navigating to details/${sport.name}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("home") }) {
                Text("Back to Home Screen")
            }
        }
    }

    @Composable
    fun SportCard(sport: Sport, onClick: () -> Unit) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .clickable { onClick() },
            elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = sport.imageResId),
                    contentDescription = sport.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = sport.name, style = MaterialTheme.typography.titleLarge)
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

    @Composable
    fun MyApp(userName: String) {
        val navController = rememberNavController();
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { HomeScreen(navController, userName) }
            composable("sports") { SportsScreen(navController) }
            composable("details/{sportName}") { backStackEntry ->
                val sportName = backStackEntry.arguments?.getString("sportName") ?: "Unknown"
                DrillActivity(navController, sportName, DrillsRepository())
            }
            composable("basketballDrills") { BasketballDrillScreen(navController) }
            composable("drillsList/{sportName}") { backStackEntry ->
                val sportName = backStackEntry.arguments?.getString("sportName") ?: ""
                val drillsRepository = DrillsRepository()
                val drills = remember { mutableStateOf<Map<String, Drill>>(emptyMap()) }

                // Fetch drills dynamically for the given sport
                LaunchedEffect(sportName) {
                    drills.value = drillsRepository.getDrillsBySport(sportName)
                }

                // Pass the fetched drills to DrillsListScreen
                DrillsListScreen(
                    drills = drills.value,
                    navController = navController
                )
            }

            composable("shootingDrills") { ShootingDrillScreen(navController) }
            composable("drillDetails/{drillKey}")
            { backStackEntry ->
                val drillKey = backStackEntry.arguments?.getString("drillKey") ?: "Unknown"
                val drillsRepository = DrillsRepository()
                val drillState = remember { mutableStateOf<Drill?>(null) }

                LaunchedEffect(drillKey) {
                    if (drillKey != "Unknown") {
                        val drills = drillsRepository.getDrillsBySport("Basketball")
                        drillState.value = drills[drillKey]
                    }
                }
                drillState.value?.let { drill ->
                    DrillDetailsScreen(
                        navController = navController,
                        drillKey = drillKey,
                        drill = drill,
                        drillsRepository = drillsRepository,
                        sportName = "Basketball"
                    )
                }
            }

            composable("tennisDrills") { TennisDrillScreen(navController) }

            composable("footballDrills") { FootballDrillScreen(navController) }

            composable("weightTraining") {
                WeightTrainingScreen(
                    navController = navController,
                    drillsRepository = DrillsRepository()
                )
            }
            composable("weightTrainingDetails/{exerciseKey}") { backStackEntry ->
                val exerciseKey = backStackEntry.arguments?.getString("exerciseKey") ?: ""
                WeightTrainingDetailsScreen(
                    navController = navController,
                    exerciseKey = exerciseKey,
                    drillsRepository = DrillsRepository()
                )
            }

            composable("fitness") {
                FitnessScreen(navController = navController, drillsRepository = DrillsRepository())
            }
            composable("fitnessDetails/{drillKey}") { backStackEntry ->
                val drillKey = backStackEntry.arguments?.getString("drillKey") ?: ""
                FitnessDetailsScreen(
                    navController = navController,
                    drillKey = drillKey,
                    drillsRepository = DrillsRepository()
                )
            }
        }
    }
}