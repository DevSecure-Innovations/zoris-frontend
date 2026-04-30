package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.model.UrlScanRequest
import com.example.myapplication.data.network.ZorisApi
import com.example.myapplication.utils.Resource
import com.example.myapplication.data.model.ScanResultContainer


class ZorisRepository(private val api: ZorisApi) {
    suspend fun checkUrl(url: String): Resource<ScanResultContainer> {
        return try {
            val response = api.checkUrl(UrlScanRequest(url))

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Scan failed. Error code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("NetworkDebug", "The real error is: ", e)
            Resource.Error("internal server error!! server can't be reached")
        }
    }

}

