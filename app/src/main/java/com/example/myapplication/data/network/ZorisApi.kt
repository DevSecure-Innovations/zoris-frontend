package com.example.myapplication.data.network

import com.example.myapplication.data.model.ScanResultContainer
import com.example.myapplication.data.model.UrlScanRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST



interface ZorisApi {
    @POST("/security/url/check")
    suspend fun checkUrl(@Body request: UrlScanRequest): Response<ScanResultContainer>

}