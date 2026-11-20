package com.altea.salud

data class SensorReading(
    val bpm: Int = 0,
    val movement: Float = 0f,
    val tilt: Float = 0f,
    val fall: Boolean = false
)
