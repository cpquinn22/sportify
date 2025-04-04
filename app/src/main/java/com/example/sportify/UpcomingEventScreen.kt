package com.example.sportify

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.FirebaseFirestore
import data.TeamEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpcomingEventsScreen(teamId: String) {
    val db = FirebaseFirestore.getInstance()
    var events by remember { mutableStateOf<List<TeamEvent>>(emptyList()) }

    LaunchedEffect(teamId) {
        db.collection("teams")
            .document(teamId)
            .collection("events")
            .get()
            .addOnSuccessListener { result ->
                events = result.documents.mapNotNull { it.toObject(TeamEvent::class.java) }
            }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Upcoming Events", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(events) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = event.name, style = MaterialTheme.typography.titleMedium)
                        Text(text = "📍 ${event.location}")
                        Text(text = " ${event.date} at ${event.time}")
                        Text(text = event.info)
                    }
                }
            }
        }
    }
}
