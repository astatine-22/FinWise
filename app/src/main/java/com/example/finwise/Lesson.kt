package com.example.finwise

data class Lesson(
    val id: Int = 0,
    val title: String,
    val subtitle: String,
    val xp: String,
    val videoUrl: String,
    val youtubeId: String = "" // Default for backward compatibility
)
