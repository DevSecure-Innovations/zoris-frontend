package com.example.myapplication.data.model

import java.util.UUID

enum class ScanType { SMS, URL }


data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val type: ScanType,
    val isSafe: Boolean,
    val confidence: Int,
    val analysisDetails: String
)