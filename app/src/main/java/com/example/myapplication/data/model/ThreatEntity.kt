package com.example.myapplication.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "threat_logs")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val snippet: String,
    val time: String
)