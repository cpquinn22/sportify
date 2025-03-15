package ViewModels

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

    fun fetchUserTeams(userId: String) {
        viewModelScope.launch {
            val teams = repository.getUserTeams(userId)
            _userTeams.value = teams
        }
    }

    suspend fun getSportsList(): List<String> {
        return repository.getSportsList()
    }
}
