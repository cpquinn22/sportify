package com.example.sportify

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.ktx.auth


@Composable
fun DrillActivity(
    navController: NavHostController,
    sportName: String,
    drillsRepository: DrillsRepository = DrillsRepository()
) {
    // holds drills loaded from Firestore
    val drills = remember { mutableStateOf<Map<String, Drill>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // Fetch drills for selected sport
    LaunchedEffect(sportName) {
        scope.launch {
            drills.value = drillsRepository.getDrillsBySport(sportName)
        }
    }

    // route user to correct screen based on sport name
    when {
        sportName == "Basketball" -> {
            BasketballDrillScreen(navController)
        }
        sportName == "Tennis" -> {
            TennisDrillScreen(navController)
        }
        sportName == "Football" -> {
            FootballDrillScreen(navController)
        }
        drills.value.isEmpty() -> {
            ComingSoonScreen(navController, sportName)
        }
        else -> {
            DrillsListScreen(drills = drills.value, navController = navController)
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
    // stat for inout and feedback
    val repository = DrillsRepository()
    var shotsMade by remember { mutableStateOf("") }
    val totalShots = 15
    val percentage = calculatePercentage(shotsMade, totalShots)
    val logs = remember { mutableStateListOf<Map<String, Any>>() }
    val drillName = drill.name
    val scrollState = rememberScrollState()
    var quickStats by remember { mutableStateOf("") }



    // fetch and filter logs for the current user
    suspend fun fetchLogs() {
        val userId = Firebase.auth.currentUser?.uid
        val fetchedLogs = repository.getLogsByDrill(sportName, drillName)
        val userLogs = if (userId != null) {
            fetchedLogs.filter { it["userId"] == userId }
        } else {
            emptyList()
        }
        logs.clear()
        logs.addAll(userLogs)
        Log.d("Firestore", "Filtered logs for user $userId: $userLogs")
    }

    // fetch logs when the drill screen loads
    LaunchedEffect(drillName) {
        fetchLogs()
    }

    // Group logs by date
    val groupedLogs = logs.groupBy { log ->
        val timestamp = log["timestamp"]
        repository.formatTimestampToDateString(timestamp).split(" ")[0] // Extract date only
    }

    // Sort logs by date (most recent first)
    val sortedGroupedLogs = groupedLogs.toSortedMap(compareByDescending { dateString ->
        try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateString)
        } catch (e: ParseException) {
            Log.e("DateParsing", "❌ Failed to parse date: $dateString", e)
            Date(0) // Return a default date (01-01-1970) so invalid dates go to the bottom
        }
    })

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

        // display drill steps
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))


            Button(
                onClick = {
                    if (shotsMade.isNotEmpty()) {
                        val shots = shotsMade.toIntOrNull() ?: 0
                        drillsRepository.addLogToFirestore(sportName, drillName, shots, percentage)

                        scope.launch {
                            fetchLogs() // refresh logs after adding new data
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Log")
            }

           Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    val shootingPercentages = logs.mapNotNull {
                        (it["shootingPercentage"] as? Long)?.toFloat()
                    }

                    quickStats = if (shootingPercentages.isNotEmpty()) {
                        val avg = shootingPercentages.average()
                        val max = shootingPercentages.maxOrNull()
                        val min = shootingPercentages.minOrNull()

                        buildString {
                            append("📊 Average Shooting %: ${"%.1f".format(avg)}%\n")
                            append("🏅 Best: ${max?.toInt()}%, 🧱 Worst: ${min?.toInt()}%")
                        }
                    } else {
                        "Not enough data to show quick stats yet."
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Quick Stats")
            }
        }

        // show quick stats
        if (quickStats.isNotEmpty()) {
            Text(
                text = quickStats,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF2E7D32),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // display logs with grouped dates
        Text("Logs", style = MaterialTheme.typography.titleMedium)

        if (logs.isEmpty()) {
            Text("No logs available", style = MaterialTheme.typography.bodyLarge)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                sortedGroupedLogs.forEach { (date, logsForDate) ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF03A9F4),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    logsForDate.forEachIndexed { index, log ->
                        val shots = log["shotsMade"] as? Long ?: 0
                        val percentage = log["shootingPercentage"] as? Long ?: 0
                        val timestamp = log["timestamp"]
                        val formattedDate = drillsRepository.formatTimestampToDateString(timestamp)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Log ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("Shots Made: $shots")
                                Text("Shooting %: $percentage%")
                                Text("Date: $formattedDate")
                            }
                        }
                    }
                }
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
    val analytics = Firebase.analytics // Get Firebase Analytics instance

    // Log screen visit event
    LaunchedEffect(Unit) {
        analytics.logEvent("sport_selected") {
            param("sport_name", "Basketball")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Basketball",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
        )
        Button(
            onClick = { navController.navigate("drillsList/Basketball") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Basketball Drills")
        }
        Button(onClick = { navController.navigate("weightTraining") }
            , modifier = Modifier.fillMaxWidth()) {
            Text("Weight Training")
        }
        Button(onClick = { navController.navigate("fitness") }, modifier = Modifier.fillMaxWidth()) {
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
fun TennisDrillScreen(navController: NavHostController) {
    val analytics = Firebase.analytics // Get Firebase Analytics instance

    // Log screen visit event
    LaunchedEffect(Unit) {
        analytics.logEvent("sport_selected") {
            param("sport_name", "Tennis")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tennis", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { navController.navigate("drillsList/Tennis") }, modifier = Modifier.fillMaxWidth()) {
            Text("Tennis Drills")
        }
        Button(onClick = { navController.navigate("weightTraining") }, modifier = Modifier.fillMaxWidth()) {
            Text("Weight Training")
        }
        Button(onClick = { navController.navigate("fitness") }, modifier = Modifier.fillMaxWidth()) {
            Text("Fitness")
        }
        Button(onClick = { navController.navigate("sports") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sports")
        }
    }
}

@Composable
fun FootballDrillScreen(navController: NavHostController) {
    val analytics = Firebase.analytics // Get Firebase Analytics instance

    // Log screen visit event
    LaunchedEffect(Unit) {
        analytics.logEvent("sport_selected") {
            param("sport_name", "Football")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Football", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { navController.navigate("drillsList/Football") }, modifier = Modifier.fillMaxWidth()) {
            Text("Football Drills")
        }
        Button(onClick = { navController.navigate("weightTraining") }, modifier = Modifier.fillMaxWidth()) {
            Text("Weight Training")
        }
        Button(onClick = { navController.navigate("fitness") }, modifier = Modifier.fillMaxWidth()) {
            Text("Fitness")
        }
        Button(onClick = { navController.navigate("sports") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Sports")
        }
    }
}


@Composable
fun WeightTrainingScreen(
    navController: NavHostController,
    drillsRepository: DrillsRepository
) {
    val exercises = remember { mutableStateOf<Map<String, Drill>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // Fetch Weight Training drills
    LaunchedEffect(Unit) {
        scope.launch {
            exercises.value = drillsRepository.getWeightTrainingDrills()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Weight Training",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (exercises.value.isEmpty()) {
            Text("No weight training drills available.")
        } else {
            exercises.value.forEach { (key, drill) ->
                Button(
                    onClick = { navController.navigate("weightTrainingDetails/$key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // Adjust height for consistent button size
                        .padding(horizontal = 0.dp) // Remove extra padding
                ) {
                    Text(
                        text = drill.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun WeightTrainingDetailsScreen(
    navController: NavHostController,
    exerciseKey: String,
    drillsRepository: DrillsRepository
) {
    var exerciseDetails by remember { mutableStateOf<Drill?>(null) }
    var weight by remember { mutableStateOf("") }
    var setNumber by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var logs = remember { mutableStateListOf<Map<String, Any>>() }

    var quickStats by remember { mutableStateOf("") }
    var showQuickStats by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()


    // Fetch drill details and start listening for logs when the screen loads
    LaunchedEffect(exerciseKey) {
        scope.launch {
            val allDrills = drillsRepository.getWeightTrainingDrills()
            exerciseDetails = allDrills[exerciseKey]

            drillsRepository.addLogsSnapshotListener(
                drillKey = exerciseKey,
                logs = logs,
                sport = "WeightTraining"
            )
        }
    }

    // Group logs by date
    val groupedLogs = logs.groupBy { log ->
        val timestamp = log["timestamp"]

        // Check for null or invalid timestamp before formatting
        if (timestamp == null || timestamp == "Unknown") {
            Log.e("DateParsing", "⚠️ Skipping log with invalid timestamp: $log")
            "01-01-1970" // Use a fallback default date
        } else {
            drillsRepository.formatTimestampToDateString(timestamp).split(" ")[0]
        }
    }

    // Sort logs by date (most recent first)
    val sortedGroupedLogs = groupedLogs.toSortedMap(compareByDescending { dateString ->
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateString)
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${exerciseDetails?.name ?: exerciseKey}",
            style = MaterialTheme.typography.titleLarge
        )

        // Display exercise description
        exerciseDetails?.description?.let { description ->
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }

        // Display steps
        exerciseDetails?.steps
            ?.toSortedMap(compareBy { it })
            ?.forEach { (key, step) ->
                Text(
                    text = "$key: $step",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(), // takes full width
                    textAlign = TextAlign.Center // centers the text itself
                )
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight Field
        TextField(
            value = weight,
            onValueChange = { weight = it.filter { it.isDigit() } },
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Set Number Field
        TextField(
            value = setNumber,
            onValueChange = { setNumber = it.filter { it.isDigit() } },
            label = { Text("Sets Complete") },
            modifier = Modifier.fillMaxWidth()
        )

        // Reps Field
        TextField(
            value = reps,
            onValueChange = { reps = it.filter { it.isDigit() } },
            label = { Text("Reps") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (weight.isNotEmpty() && setNumber.isNotEmpty() && reps.isNotEmpty()) {
                        drillsRepository.addWeightTrainingLog(
                            exerciseName = exerciseKey,
                            weight = weight.toInt(),
                            setNumber = setNumber.toInt(),
                            reps = reps.toInt()
                        )
                        weight = ""
                        setNumber = ""
                        reps = ""
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Log Exercise")
            }

            OutlinedButton(
                onClick = {
                    val weights = logs.mapNotNull { it["weight"] as? Long }
                    val repsList = logs.mapNotNull { it["reps"] as? Long }
                    val setsList = logs.mapNotNull { it["setNumber"] as? Long }

                    if (weights.isNotEmpty() && setsList.isNotEmpty()) {
                        val avgWeight = weights.average()
                        val maxWeight = weights.maxOrNull() ?: 0

                        val totalVolume = logs.sumOf {
                            val w = (it["weight"] as? Long) ?: 0L
                            val s = (it["setNumber"] as? Long) ?: 0L
                            w * s
                        }
                        val avgVolume = if (logs.isNotEmpty()) totalVolume.toFloat() / logs.size else 0f

                        quickStats = buildString {
                            append("🏋 Average Weight: ${"%.1f".format(avgWeight)} kg\n")
                            append("📦 Average Volume (Weight × Sets): ${"%.1f".format(avgVolume)} kg\n")
                            append("🏆 Best Lift: $maxWeight kg\n")
                        }
                    } else {
                        quickStats = "Not enough logs to show quick stats yet."
                    }
                    showQuickStats = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Quick Stats")
            }
        }

        if (showQuickStats && quickStats.isNotEmpty()) {
            Text(
                text = quickStats,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF2E7D32),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Logs", style = MaterialTheme.typography.titleMedium)


        if (logs.isEmpty()) {
            Text("No logs available", style = MaterialTheme.typography.bodyLarge)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                sortedGroupedLogs.forEach { (date, logsForDate) ->
                    // Light Blue Date Header
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF03A9F4),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    logsForDate.forEachIndexed { index, log ->
                        val logWeight = log["weight"] as? Long ?: 0
                        val logSetNumber = log["setNumber"] as? Long ?: 0
                        val logReps = log["reps"] as? Long ?: 0
                        val timestamp = log["timestamp"]
                        val formattedDate = drillsRepository.formatTimestampToDateString(timestamp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Log ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text("Weight: $logWeight kg")
                                Text("Sets Complete: $logSetNumber")
                                Text("Reps: $logReps")
                                Text("Date: $formattedDate")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Space between groups
                }
            }
        }
    }
}

@Composable
fun FitnessScreen(navController: NavHostController, drillsRepository: DrillsRepository) {
    val drills = remember { mutableStateOf<Map<String, Drill>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    // Fetch fitness drills when screen loads
    LaunchedEffect(Unit) {
        scope.launch {
            drills.value = drillsRepository.getFitnessDrills()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Fitness Drills", style = MaterialTheme.typography.titleLarge)

        if (drills.value.isEmpty()) {
            Text("No fitness drills available.")
        } else {
            drills.value.forEach { (key, drill) ->
                Button(
                    onClick = { navController.navigate("fitnessDetails/$key") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(drill.name)
                }
            }
        }
    }
}


@Composable
fun FitnessDetailsScreen(
    navController: NavHostController,
    drillKey: String,
    drillsRepository: DrillsRepository
) {
    var drillDetails by remember { mutableStateOf<Drill?>(null) }
    var minutesCompleted by remember { mutableStateOf("") }
    var roundsCompleted by remember { mutableStateOf("") }
    var logs = remember { mutableStateListOf<Map<String, Any>>() }
    val averageTimePerKm = remember(minutesCompleted) {
        calculateAverageTimePerKm(minutesCompleted)
    }
    val scope = rememberCoroutineScope()

    var quickStats by remember { mutableStateOf("") }
    var showQuickStats by remember { mutableStateOf(false) }


    // Fetch drill details and logs
    LaunchedEffect(drillKey) {
        scope.launch {
            val allDrills = drillsRepository.getFitnessDrills()
            drillDetails = allDrills[drillKey]
            drillsRepository.addLogsSnapshotListener(drillKey, logs, sport = "Fitness")
        }
    }

    // Group logs by date
    val groupedLogs = logs.groupBy { log ->
        val timestamp = log["timestamp"]

        if (timestamp == null || timestamp == "Unknown") {
            Log.e("DateParsing", "⚠️ Skipping log with invalid timestamp: $log")
            "01-01-1970" // Use fallback date for invalid entries
        } else {
            drillsRepository.formatTimestampToDateString(timestamp).split(" ")[0]
        }
    }

    // Sort logs by date (most recent first)
    val sortedGroupedLogs = groupedLogs.toSortedMap(compareByDescending { dateString ->
        try {
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(dateString)
        } catch (e: ParseException) {
            Log.e("DateParsing", "❌ Failed to parse date: $dateString", e)
            Date(0) // Push invalid dates to the bottom
        }
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable vertical scrolling for the entire screen
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drill Title
        Text(drillDetails?.name ?: drillKey, style = MaterialTheme.typography.titleLarge)

        // Drill Description
        drillDetails?.description?.let { description ->
            Text(description, style = MaterialTheme.typography.bodyLarge)
        }

        // Drill Steps
        drillDetails?.steps?.toSortedMap(compareBy { it })?.forEach { (key, step) ->
            Text("$key: $step", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input and logic for 5K or rounds
        if (drillKey == "5k_run") {
            // 5K specific input
            TextField(
                value = minutesCompleted,
                onValueChange = {
                    minutesCompleted = it.filter { char -> char.isDigit() || char == '.' }
                },
                label = { Text("Minutes to complete 5K") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Average Time per KM: $averageTimePerKm min/km",
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (minutesCompleted.isNotEmpty()) {
                            drillsRepository.addFitnessLog(
                                drillName = drillKey,
                                totalTime = minutesCompleted.toFloat(),
                                avgTimePerKm = averageTimePerKm.toFloatOrNull() ?: 0f
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Log")
                }

                OutlinedButton(
                    onClick = {
                        val totalTimes = logs.mapNotNull { it["totalTime"] as? Double }
                        quickStats = if (totalTimes.isNotEmpty()) {
                            val avg = totalTimes.average()
                            val best = totalTimes.minOrNull()
                            val worst = totalTimes.maxOrNull()
                            buildString {
                                append("🏃‍♂️ 5K Performance:\n")
                                append("Average Time: ${"%.2f".format(avg)} min\n")
                                append("Best: ${"%.2f".format(best)} min, Worst: ${"%.2f".format(worst)} min")
                            }
                        } else {
                            "Not enough data to show quick stats yet."
                        }

                        showQuickStats = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Quick Stats")
                }
            }
        } else {
            // General rounds drill input
            TextField(
                value = roundsCompleted,
                onValueChange = { roundsCompleted = it.filter { it.isDigit() } },
                label = { Text("Enter rounds completed") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (roundsCompleted.isNotEmpty()) {
                            drillsRepository.logRounds(drillKey, roundsCompleted.toInt())
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Log Rounds")
                }

                OutlinedButton(
                    onClick = {
                        val rounds = logs.mapNotNull { it["roundsCompleted"] as? Long }
                        quickStats = if (rounds.isNotEmpty()) {
                            val avg = rounds.average()
                            val max = rounds.maxOrNull()
                            val min = rounds.minOrNull()
                            buildString {
                                append("🌀 Rounds Completed:\n")
                                append("Average: ${"%.1f".format(avg)}, Best: $max, Lowest: $min")
                            }
                        } else {
                            "Not enough data to show quick stats yet."
                        }

                        showQuickStats = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Quick Stats")
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))
        if (showQuickStats && quickStats.isNotEmpty()) {
            Text(
                text = quickStats,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF2E7D32),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // Logs Section
        Text("Logs", style = MaterialTheme.typography.titleMedium)

        if (logs.isEmpty()) {
            Text("No logs available", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // Restrict height for LazyColumn to allow smooth scrolling
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedGroupedLogs.forEach { (date, logsForDate) ->
                    // Light Blue Date Header
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF03A9F4),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(logsForDate) { log ->
                        val rounds = log["roundsCompleted"] as? Long ?: 0
                        val totalTime = log["totalTime"] as? Double ?: 0.0
                        val avgTimePerKm = log["avgTimePerKm"] as? Double ?: 0.0
                        val timestamp = log["timestamp"]
                        val formattedDate = drillsRepository.formatTimestampToDateString(timestamp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Date: $formattedDate", style = MaterialTheme.typography.titleSmall)

                                if (drillKey == "5k_run") {
                                    Text("Total Time: ${String.format("%.2f", totalTime)} min")
                                    Text("Average Time per KM: ${String.format("%.2f", avgTimePerKm)} min/km")
                                } else {
                                    Text("Rounds Completed: $rounds")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}


// Function to calculate average time per kilometer
fun calculateAverageTimePerKm(totalMinutes: String): String {
    val minutes = totalMinutes.toFloatOrNull() ?: return "0.0"
    val avgTime = minutes / 5 // Divide by 5K
    return String.format("%.2f", avgTime)
}


