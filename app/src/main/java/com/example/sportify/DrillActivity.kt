package com.example.sportify

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun DrillActivity(navController: NavHostController, sportName: String) {
    when (sportName) {
        "Basketball" -> BasketballDrillScreen(navController)
        "Tennis" -> ComingSoonScreen(navController, sportName)
        "Football" -> ComingSoonScreen(navController, sportName)
        else -> DefaultDrillScreen(navController, sportName)
    }
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
        Text("Basketball Drills", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Button(onClick = { navController.navigate("drill/shooting") }, modifier = Modifier.fillMaxWidth()) {
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
        Button(onClick = { navController.navigate("drill/3_point_shooting") }, modifier = Modifier.fillMaxWidth()) {
            Text("3 Point Shooting")
        }
        Button(onClick = { navController.navigate("drill/free_throw") }, modifier = Modifier.fillMaxWidth()) {
            Text("Free Throws")
        }
        Button(onClick = { navController.navigate("drill/off_the_dribble_shots") }, modifier = Modifier.fillMaxWidth()) {
            Text("Off the Dribble Shots")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("details/Basketball") }, modifier = Modifier.fillMaxWidth()) {
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
        Text("3 Point Shooting", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Set yourself up on a spot at the 3 point line.")
        Text("Take 15 shots at this spot and record how many you make.")
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
        Text("Shooting Percentage: $shootingPercentage%", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("drill/shooting") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Shooting Drills")
        }
    }
}

@Composable
fun OffTheDribbleScreen (navController: NavHostController) {
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
        Text("Free Throw Shooting", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
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
        Text("Shooting Percentage: $shootingPercentage%", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("drill/shooting") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Shooting Drills")
        }
    }
}

fun calculatePercentage(shots: String, totalShots: Int): Int {
    return shots.toIntOrNull()?.let {
        ((it.toDouble() / totalShots) * 100).toInt()
    } ?: 0
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
        Text("$sportName features are coming soon!", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
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