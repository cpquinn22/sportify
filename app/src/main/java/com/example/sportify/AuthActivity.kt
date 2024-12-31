package com.example.sportify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

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
            .build()

        signInLauncher.launch(intent)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}