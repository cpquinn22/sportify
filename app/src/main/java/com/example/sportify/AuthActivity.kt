package com.example.sportify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class AuthActivity : ComponentActivity() {

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Handle successful sign-in
            val user = Firebase.auth.currentUser
            Log.d("AuthActivity", "Sign-in successful: ${user?.email}")
            navigateToMain()
        } else {
            Log.e("AuthActivity", "Sign-in failed")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startFirebaseUI()
    }

    private fun startFirebaseUI() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )

        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false) // Disable Smart Lock for testing
            .build()

        signInLauncher.launch(intent)
    }

    private fun navigateToMain() {
        saveUserToFirestore() // save user data to firestore
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // Generate a unique 8-digit UID
    private suspend fun generateUniqueUid(): String {
        val db = FirebaseFirestore.getInstance()
        var uniqueUid: String
        var isUnique: Boolean

        do {
            // Generate a random 8-digit number
            uniqueUid = Random.nextInt(10000000, 99999999).toString()

            // Check if the UID already exists in Firestore
            val documentSnapshot = db.collection("users").document(uniqueUid).get().await()
            isUnique = !documentSnapshot.exists() // UID is unique if no document exists
        } while (!isUnique)

        return uniqueUid
    }

    private suspend fun generateUniqueCustomUid(displayName: String?, email: String?): String {
        val db = FirebaseFirestore.getInstance()
        var customUid: String
        var isUnique: Boolean

        // Extract initials
        val initials = displayName?.split(" ")?.joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
            ?: email?.split("@")?.firstOrNull()?.take(2)?.uppercase() // Default to first 2 letters of email if no displayName

        do {
            // Generate a random 8-digit number
            val randomNumber = Random.nextInt(10000000, 99999999)
            customUid = "$initials$randomNumber" // Combine initials and random number

            // Check if the custom UID already exists in Firestore
            val documentSnapshot = db.collection("users").document(customUid).get().await()
            isUnique = !documentSnapshot.exists() // UID is unique if no document exists
        } while (!isUnique)

        return customUid
    }

    fun saveUserToFirestore() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val db = FirebaseFirestore.getInstance()
            val usersCollection = db.collection("users")

            // check if the email already exists in the database
            usersCollection.whereEqualTo("email", firebaseUser.email).get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty) {
                        // Email does not exist: create a new user
                        kotlinx.coroutines.GlobalScope.launch {
                            val customUid = generateUniqueCustomUid(firebaseUser.displayName, firebaseUser.email)
                            val userData = mapOf(
                                "uid" to customUid,
                                "name" to (firebaseUser.displayName ?: "Anonymous"),
                                "email" to (firebaseUser.email ?: ""),
                                "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                                "isAdmin" to false // default to false
                            )

                db.collection("users").document(customUid)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d("Firestore", "User data successfully written!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error writing user data", e)
                    }
            }

                    } else {
                        // Email exists: update the existing user's data
                        val existingUserDocument = querySnapshot.documents[0]
                        val existingUid = existingUserDocument.id

                        val updatedData = mapOf(
                            "name" to (firebaseUser.displayName ?: existingUserDocument.getString("name")),
                            "photoUrl" to (firebaseUser.photoUrl?.toString()
                                ?: existingUserDocument.getString("photoUrl"))
                        )

                        db.collection("users").document(existingUid)
                            .update(updatedData)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Existing user data successfully updated!")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Error updating existing user data", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error checking if email exists in Firestore", e)
                }
        } else {
            Log.e("Firestore", "No authenticated user found")
        }
    }
}


    @Composable
    fun AuthScreen() {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Authenticating...")
            }
        }
    }

