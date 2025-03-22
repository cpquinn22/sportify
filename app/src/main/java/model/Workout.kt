package model

data class Workout(
    val name: String = "",
    val info: String = "",
    val steps: Map<String, String> = emptyMap()
)
