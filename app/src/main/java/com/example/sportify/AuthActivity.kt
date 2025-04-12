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
            val user = Firebase.auth.currentUser
            Log.d("AuthActivity", "✅ Sign-in successful: ${user?.email}")

            saveUserToFirestore()
            navigateToMain()
        } else {
            Log.e("AuthActivity", "❌ Sign-in failed")
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
            .setIsSmartLockEnabled(false)
            .build()

        signInLauncher.launch(intent)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }


    fun saveUserToFirestore() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            Log.d("FirestoreDebug", "🛠️ saveUserToFirestore() called for ${firebaseUser.email}")

            val db = FirebaseFirestore.getInstance()
            val usersCollection = db.collection("users")

            val firebaseUid = firebaseUser.uid
            val email = firebaseUser.email ?: ""
            val name = firebaseUser.displayName ?: "Anonymous"

            usersCollection.document(firebaseUid).get()
                .addOnSuccessListener { documentSnapshot ->
                    if (!documentSnapshot.exists()) {
                        val userData = mapOf(
                            "authUid" to firebaseUid,
                            "name" to name,
                            "email" to email,
                            "isAdmin" to false,
                            "teams" to listOf<String>(),
                            "adminTeams" to listOf<String>()
                        )

                        usersCollection.document(firebaseUid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("Firestore", "✅ New user created with UID: $firebaseUid")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "❌ Failed to create user", e)
                            }
                    } else {
                        // Optionally update their name if needed
                        val updatedData = mapOf("name" to name)

                        usersCollection.document(firebaseUid)
                            .update(updatedData)
                            .addOnSuccessListener {
                                Log.d("Firestore", "✅ Existing user updated: $firebaseUid")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "❌ Failed to update user", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "❌ Failed to fetch user doc", e)
                }
        } else {
            Log.e("Firestore", "❌ No authenticated Firebase user found")
        }
    }

}

