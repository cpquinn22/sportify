package com.example.sportify

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import com.google.firebase.functions.FirebaseFunctions

@Composable
fun CreateEventScreen(
    teamId: String,
    onEventCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    var eventName by remember { mutableStateOf("") }
    var eventInfo by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }

    val context = LocalContext.current


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Create Event", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = eventName,
            onValueChange = { eventName = it },
            label = { Text("Event Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = eventInfo,
            onValueChange = { eventInfo = it },
            label = { Text("Event Info") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = eventLocation,
            onValueChange = { eventLocation = it },
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = eventDate,
            onValueChange = { eventDate = it },
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = eventTime,
            onValueChange = { eventTime = it },
            label = { Text("Time") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (eventName.isBlank() || eventInfo.isBlank() || eventLocation.isBlank() || eventDate.isBlank() || eventTime.isBlank()) {
                    Toast.makeText(context, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val event = mapOf(
                    "name" to eventName,
                    "info" to eventInfo,
                    "location" to eventLocation,
                    "date" to eventDate,
                    "time" to eventTime,
                    "timestamp" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("teams")
                    .document(teamId)
                    .collection("events")
                    .add(event)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Event created", Toast.LENGTH_SHORT).show()
                        onEventCreated()
                    }
                    .addOnFailureListener {exception ->
                        Log.e("CreateEvent", "❌ Failed to create event: ${exception.message}", exception)
                        Toast.makeText(context, "Failed to create event", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Event")
        }
    }
}

