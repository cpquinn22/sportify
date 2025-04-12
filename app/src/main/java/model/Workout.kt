package model

data class Workout(
    var id: String = "",
    val name: String = "",
    val info: String = "",
    val steps: Map<String, String> = emptyMap(),
    val logTypes: Map<String, String> = emptyMap(),
    val logFields: Map<String, String> = emptyMap(),
    val stepOrder: List<String> = emptyList()
)

