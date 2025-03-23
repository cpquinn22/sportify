package model

data class Workout(
    var id: String = "",
    val name: String = "",
    val info: String = "",
    val steps: Map<String, String> = emptyMap(),
    val logTypes: Map<String, String> = emptyMap(),
    val logFields: Map<String, String> = emptyMap()
)

data class LogField(
    val label: String = "", // e.g., "Minutes to complete 5K"
    val type: String = "text" // Could be "number", "text", etc. for flexibility
)
