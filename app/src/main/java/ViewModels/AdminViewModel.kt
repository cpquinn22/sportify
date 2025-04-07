package ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    fun addUserToTeamByEmail(
        email: String,
        teamId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDoc = documents.first()
                    val userId = userDoc.id

                    val teamRef = db.collection("teams").document(teamId)
                    teamRef.update("members", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener {
                            db.collection("users").document(userId)
                                .update("teams", FieldValue.arrayUnion(teamId))
                                .addOnSuccessListener {
                                    onResult(true, "✅ User added to team successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("AdminViewModel", "Failed to update user team list", e)
                                    onResult(false, "⚠️ Added to team but failed to update user's profile")
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("AdminViewModel", "Failed to update team", e)
                            onResult(false, "❌ Failed to add user to team: ${e.message}")
                        }
                } else {
                    onResult(false, "❌ No user found with that email")
                }
            }
            .addOnFailureListener { e ->
                Log.e("AdminViewModel", "Failed to query users", e)
                onResult(false, "❌ Error searching for user: ${e.message}")
            }
    }
}
