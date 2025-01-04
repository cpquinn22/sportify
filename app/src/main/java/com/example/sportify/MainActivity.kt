package com.example.sportify

import android.content.Intent
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
import com.google.firebase.FirebaseApp
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
            composable("home") { HomeScreen(navController, userName = "User") }
            composable("sports") { SportsScreen(navController) }
            composable("details/{sportName}")
            { backStackEntry ->
                val sportName =
                    backStackEntry.arguments?.getString("sportName")
                        ?: "Unknown Sport"
                DrillActivity(navController, sportName, DrillsRepository())
            }
            composable("drillDetails/{drillKey}") { backStackEntry ->
                val sportName = backStackEntry.arguments?.getString("sportName") ?: "Unknown Sport"
                val drillKey = backStackEntry.arguments?.getString("drillKey")
                val drillsRepository = DrillsRepository()
                val drillState = remember { mutableStateOf<Drill?>(null) }

                LaunchedEffect(drillKey){
                    if(drillKey != null) {
                        try{
                            val drills = drillsRepository.getDrillsBySport("Basketball")
                            drillState.value = drills[drillKey]
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.e("DrillDetails", "Error fetching drill", e)
                        }
                    } else {
                        Log.e("DrillDetails", "drillKey is null")
                    }
                }

                if (drillState.value != null) {
                    DrillDetailsScreen(navController = navController,
                        drill = drillState.value!!,
                        sportName = sportName)
                } else {
                    Log.d("DrillDetails", "Drill is still loading or null")
                    Text("Loading drill details...")
                }
            }
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
                    navController.navigate("details/${sport.name}")
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
    fun SportDetailsScreen(sportName: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = sportName, style = MaterialTheme.typography.titleLarge)
            ////////////// Add more details here /////////////////////////
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

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        SportifyTheme {
            HomeScreen(rememberNavController(), "Sportify")
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
