package com.example.sportify

import ViewModels.TeamViewModel
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import androidx.activity.viewModels
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapp.data.TeamRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar;



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

private fun scheduleDailyWorkoutReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WorkoutReminderReceiver::class.java) // Ensure this matches your receiver class name
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12) // Set time for 10:00 AM
        set(Calendar.MINUTE, 11)
        set(Calendar.SECOND, 1)
    }

    // If the time has already passed today, schedule it for tomorrow
    if (Calendar.getInstance().after(calendar)) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}
class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val teamViewModel: TeamViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics

        FirebaseApp.initializeApp(this)

        // Request notification permission (For Android 13+)
        requestNotificationPermission()

        // Schedule the daily alarm
        scheduleDailyWorkoutReminder(this)

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
            // ✅ Update FCM token for existing user
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener {
                                Log.d("FCM", "✅ FCM token updated after login")
                            }
                            .addOnFailureListener { e ->
                                Log.e("FCM", "❌ Failed to update FCM token", e)
                            }
                    } else {
                        Log.e("FCM", "❌ Could not fetch token: ${task.exception}")
                    }
                }
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
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
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

            }
    }

//    @Preview(showBackground = true)
  //  @Composable
    //fun GreetingPreview() {
      //  SportifyTheme {
        //    val dummyViewModel = TeamViewModel(TeamRepository()) // or use a fake if needed
          //  HomeScreen(
            //    navController = rememberNavController(),
              //  userName = "Sportify",
                //viewModel = dummyViewModel
            //)
        //}
    //}


  //****************************
    //****************************  Keep this out?
    //****************************
    //****************************


    @Composable
    fun MyApp(userName: String) {
        val navController = rememberNavController();
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                val viewModel: TeamViewModel = viewModel()
                HomeScreen(navController, userName, viewModel)
            }
            composable("sports") { SportsScreen(navController) }
            Firebase.auth.currentUser?.let { it1 ->  composable("createTeam") { CreateTeamScreen(navController, teamViewModel, it1.uid) } }
            composable("myTeams") {
                MyTeamsScreen(viewModel = teamViewModel, userId = Firebase.auth.currentUser?.uid ?: "", navController = navController)
            }
            Log.d("UserCheck", "Current user UID: ${Firebase.auth.currentUser?.uid}")
            composable("details/{sportName}") { backStackEntry ->
                val sportName = backStackEntry.arguments?.getString("sportName") ?: "Unknown"
                DrillActivity(navController, sportName, DrillsRepository())
            }
            composable("team/{teamId}") { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val viewModel: TeamViewModel = viewModel()

                TeamScreen(
                    teamId = teamId,
                    userId = userId,
                    navController = navController,
                    viewModel = viewModel
                )
            }
            composable(
                route = "createWorkout/{teamId}",
                arguments = listOf(navArgument("teamId") { type = NavType.StringType })
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                CreateWorkoutScreen(
                    teamId = teamId,
                    viewModel = viewModel(),
                    onWorkoutCreated = { navController.popBackStack() }
                )
            }
            composable(
                route = "logWorkout/{teamId}/{workoutId}",
                arguments = listOf(
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("workoutId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                val viewModel: TeamViewModel = viewModel()

                LogWorkoutScreen(
                    teamId = teamId,
                    workoutId = workoutId,
                    viewModel = viewModel,
                    onLogSaved = {
                        // Navigate back or show a success message
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "logViewer/{teamId}/{workoutId}",
                arguments = listOf(
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("workoutId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
                LogViewerScreen(
                    teamId = teamId,
                    workoutId = workoutId,
                    viewModel = viewModel() // or hiltViewModel() if using Hilt
                )
            }
            composable("workoutList/{teamId}") { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val viewModel: TeamViewModel = viewModel()
                val workouts = viewModel.teamWorkouts.collectAsState().value
                AdminWorkoutListScreen(teamId, workouts, navController, viewModel)
            }

            composable("adminLogView/{teamId}") { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
                val viewModel: TeamViewModel = viewModel()

                AdminLogViewScreen(
                    teamId = teamId,
                    viewModel = viewModel,
                    navController = navController
                )
            }

            composable(
                route = "adminLogDetail/{teamId}/{workoutId}",
                arguments = listOf(
                    navArgument("teamId") { type = NavType.StringType },
                    navArgument("workoutId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
                val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
                val viewModel: TeamViewModel = viewModel()

                AdminLogDetailScreen(
                    teamId = teamId,
                    workoutId = workoutId,
                    viewModel = viewModel
                )
            }
            composable("adminWorkoutList/{teamId}") { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: return@composable
                val viewModel: TeamViewModel = viewModel()
                val workouts by viewModel.teamWorkouts.collectAsState(emptyList())

                LaunchedEffect(teamId) {
                    viewModel.fetchTeamWorkouts(teamId)
                }

                AdminWorkoutListScreen(
                    teamId = teamId,
                    workouts = workouts,
                    navController = navController,
                    viewModel = viewModel
                )
            }
            composable("leaderboard/{teamId}") { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString("teamId") ?: ""
                val viewModel: TeamViewModel = viewModel()
                LeaderboardScreen(
                    teamId = teamId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("virtualCoach") {
                VirtualCoachScreen()
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