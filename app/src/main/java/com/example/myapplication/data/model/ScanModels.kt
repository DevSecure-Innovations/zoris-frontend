package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/* { "url": "https://..." } */
data class UrlScanRequest(val url: String)

/* { "result": { "isPhishing": true, ... } } */
data class ScanResultContainer(
    @SerializedName("result")
    val result: ScanDetails
)

data class ScanDetails(
    @SerializedName("safe")
    val isSafeFromServer: Boolean,

    @SerializedName("threats")
    val threatList: List<String>?
)
