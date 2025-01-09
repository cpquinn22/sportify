package com.example.sportify

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import data.Drill
import data.DrillsRepository
import kotlinx.coroutines.launch
import java.security.Timestamp


@Composable
fun DrillActivity(
    navController: NavHostController,
    sportName: String,
    drillsRepository: DrillsRepository = DrillsRepository()
) {
    val drills = remember { mutableStateOf<Map<String, Drill>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // Fetch drills when activity loads
    LaunchedEffect(sportName) {
        scope.launch {
            drills.value = drillsRepository.getDrillsBySport(sportName)
        }
    }

    if (drills.value.isEmpty()) {
        ComingSoonScreen(navController, sportName)
    } else {
        navController.navigate("basketballDrills")
    }
}

@Composable
fun DrillsListScreen(drills: Map<String, Drill>, navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (drills.isEmpty()) {
            Text("No drills available.")
        } else {
            drills.forEach { (key, drill) ->
                Button(
                    onClick = {
                        navController.navigate("drillDetails/$key")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(drill.name)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("sports") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sports")
        }
    }
}

@Composable
fun DrillDetailsScreen(
    navController: NavHostController,
    drillKey: String,
    drill: Drill,
    sportName: String,
    drillsRepository: DrillsRepository
) {
    val repository = DrillsRepository()
    var shotsMade by remember { mutableStateOf("") }
    val totalShots = 15
    val percentage = calculatePercentage(shotsMade, totalShots)
    val logs = remember { mutableStateListOf<Map<String, Any>>() }
    val drillName = drill.name
    val scrollState = rememberScrollState()

    // Fetch logs from Firestores
    suspend fun fetchLogs() {
        val fetchedLogs = repository.getLogsByDrill(sportName, drillName)
        logs.clear()
        logs.addAll(
            fetchedLogs.sortedByDescending {
                (it["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
            }
        )
    }

    LaunchedEffect(drillName) {
        fetchLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(drill.name, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        drill.steps.forEach { (_, stepValue) ->
            Text(stepValue, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Input field
        TextField(
            value = shotsMade,
            onValueChange = { input -> shotsMade = input.filter { it.isDigit() } },
            label = { Text("Enter shots made") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Shooting Percentage: $percentage%", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        val scope = rememberCoroutineScope()

        Button(
            onClick = {
                if (shotsMade.isNotEmpty()) {
                    val shots = shotsMade.toIntOrNull() ?: 0
                    drillsRepository.addLogToFirestore(sportName, drillName, shots, percentage)

                    scope.launch {
                        fetchLogs()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(text = "Log")
        }


        Spacer(modifier = Modifier.height(24.dp))

        Text("Logs", style = MaterialTheme.typography.titleMedium)

        if (logs.isEmpty()) {
            Text("No logs available", style = MaterialTheme.typography.bodyLarge)
        } else {
            logs.forEachIndexed { index, log ->
                val shots = log["shotsMade"] as? Long ?: 0
                val percentage = log["shootingPercentage"] as? Long ?: 0
                val timestamp = log["timestamp"]
                val formattedDate = drillsRepository.formatTimestampToDateString(timestamp)
                Text(
                    text = "Log ${index + 1}: Shots Made - $shots, Percentage - $percentage%, Date - $formattedDate",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

fun calculatePercentage(shots: String, totalShots: Int): Int {
    return shots.toIntOrNull()?.let { nonNullShots ->
        ((nonNullShots.toDouble() / totalShots) * 100).toInt()
    } ?: 0
}

@Composable
fun BasketballDrillScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Basketball Drills",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Button(
            onClick = { navController.navigate("drillsList/Basketball") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Shooting")
        }
        Button(onClick = { /* Handle Weight Training */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Weight Training")
        }
        Button(onClick = { /* Handle Fitness */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Fitness")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("sports") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sports")
        }
    }
}


@Composable
fun ShootingDrillScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate("drill/3_point_shooting") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("3 Point Shooting")
        }
        Button(
            onClick = { navController.navigate("drill/free_throw") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Free Throws")
        }
        Button(
            onClick = { navController.navigate("drill/off_the_dribble_shots") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Off the Dribble Shots")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("details/Basketball") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Basketball Drills")
        }
    }
}


@Composable
fun ThreePointShootingScreen(navController: NavHostController) {
    val shotsMade = remember { mutableStateOf("") }

    val shootingPercentage = remember(shotsMade.value) {
        calculatePercentage(shotsMade.value, 15)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "3 Point Shooting",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Set yourself up on a spot at the 3 point line.")
        Text("Take 15 shots at this spot and record how many you make.")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var shotsMade by remember { mutableStateOf("") }
            TextField(
                value = shotsMade,
                onValueChange = { shotsMade = it },
                label = { Text("Enter shots made") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("/15", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        }
        Text(
            "Shooting Percentage: $shootingPercentage%",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("drill/shooting") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Shooting Drills")
        }
    }
}

@Composable
fun OffTheDribbleScreen(navController: NavHostController) {
    val offDribbleShots = remember {
        mutableStateOf("")
    }
    val offDribblePercentage = remember(offDribbleShots.value) {
        calculatePercentage(offDribbleShots.value, 15)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Off the Dribble",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Dribble from the middle of the court and take a shot along the 3 point line.")
        Text("Take 15 shots from the same spot and record how many you make.")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = offDribbleShots.value,
                onValueChange = { offDribbleShots.value = it },
                label = { Text("Enter off-the-dribble shots made") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("/15", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        }
        Text(
            "Off the Dribble Percentage: $offDribblePercentage%",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FreeThrowScreen(navController: NavHostController) {
    val shotsMade = remember { mutableStateOf("") }

    val shootingPercentage = remember(shotsMade.value) {
        calculatePercentage(shotsMade.value, 15)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Free Throw Shooting",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Stand at the free throw line and take 15 shots. Record how many you make.")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = shotsMade.value,
                onValueChange = { shotsMade.value = it },
                label = { Text("Enter shots made") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("/15", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        }
        Text(
            "Shooting Percentage: $shootingPercentage%",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("drill/shooting") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Shooting Drills")
        }
    }
}


@Composable
fun ComingSoonScreen(navController: NavHostController, sportName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "$sportName features are coming soon!",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("sports") }) {
            Text("Back to Sports")
        }
    }
}

@Composable
fun DefaultDrillScreen(navController: NavHostController, sportName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No drills available for $sportName")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("sports") }) {
            Text("Back to Sports")


            @Composable
            fun TennisDrillScreen(navController: NavHostController) {
                // Implement Tennis drill screen layout
            }

            @Composable
            fun FootballDrillScreen(navController: NavHostController) {
                println("Football coming soon")
                // Implement Football drill screen layout
            }

            @Composable
            fun ComingSoonScreen(navController: NavHostController, sportName: String) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$sportName: Coming Soon",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("sports") }) {
                        Text("Back to Sports")
                    }
                }
            }

            @Composable
            fun DefaultDrillScreen(navController: NavHostController) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Drills not available",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("sports") }) {
                        Text("Back to Sports")
                    }
                }
            }
        }
    }
}