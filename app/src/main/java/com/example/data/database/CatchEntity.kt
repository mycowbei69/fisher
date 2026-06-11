package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catches")
data class CatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fishName: String,
    val weight: Double, // in kg
    val length: Double, // in cm
    val locationName: String,
    val tide: String, // e.g., 漲潮, 退潮, 乾潮, 滿潮
    val windSpeed: Double, // km/h
    val waveHeight: Double, // m
    val temperature: Double, // °C
    val rating: Int, // 1 to 5 stars
    val notes: String = "",
    val photoUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false // Track sync to AWS/Cloud
)
