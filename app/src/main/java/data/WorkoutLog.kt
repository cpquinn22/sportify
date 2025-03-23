package data

data class WorkoutLog(
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val stepResponses: Map<String, String> = emptyMap()
)
