package com.example.myapplication.data.model

// This represents the final verdict after your backend/Gemini analyzes a message
data class ScanResult(
    val isSafe: Boolean,
    val confidence: Float,
    val analysisDetails: String
)