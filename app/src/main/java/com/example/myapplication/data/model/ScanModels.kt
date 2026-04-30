package com.example.myapplication.data.model

/* { "url": "https://..." } */
data class UrlScanRequest(val url: String)

/* { "result": { "isPhishing": true, ... } } */
data class ScanResultContainer(val result: ScanDetails)

data class ScanDetails(
    val isPhishing: Boolean,
    val riskLevel: String,
    val threatDetails: String?
)
