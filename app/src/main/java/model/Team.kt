package model

data class Team(
    val name: String = "",
    val sport: String = "",
    val adminId: String = "",
    val teamCode: String = "",
    val members: List<String> = emptyList(),
    val id: String = ""
)
