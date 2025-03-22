package ViewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp.data.TeamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeamViewModel : ViewModel() {

    private val repository = TeamRepository() // No need for factory!

    private val _userTeams = MutableStateFlow<List<String>>(emptyList())
    val userTeams: StateFlow<List<String>> = _userTeams

    fun createTeam(teamName: String, selectedSport: String, userId: String) {
        viewModelScope.launch {
            repository.createTeam(teamName, selectedSport, userId)
        }
    }

    fun joinTeam(userId: String, teamCode: String) {
        viewModelScope.launch {
            repository.joinTeam(userId, teamCode)
        }
    }

    fun fetchUserTeams(firebaseUid: String?) {
        viewModelScope.launch {
            val userDocId = firebaseUid // Because now Firebase UID is your document ID

            if (userDocId != null) {
                val teamIds = repository.getUserTeams(userDocId)
                Log.d("FirestoreDebug", "Fetched team IDs: $teamIds")

                val teamNames = mutableListOf<String>()
                for (teamId in teamIds) {
                    val name = repository.getTeamNameById(teamId)
                    if (name != null) teamNames.add(name)
                }

                _userTeams.value = teamNames
                Log.d("FirestoreDebug", "Final teamNames list: $teamNames")
            }
        }
    }

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    fun fetchAdminStatus(userId: String) {
        viewModelScope.launch {
            _isAdmin.value = repository.isUserAdmin(userId)
            Log.d("AuthDebug", "Admin status: ${_isAdmin.value}")
        }
    }

    suspend fun getSportsList(): List<String> {
        return repository.getSportsList()
    }
}
