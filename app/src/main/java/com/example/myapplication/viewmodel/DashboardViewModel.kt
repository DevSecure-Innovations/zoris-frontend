package com.example.myapplication.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ScanBridge
import com.example.myapplication.data.model.ScanResult
import com.example.myapplication.data.model.ScanType
import com.example.myapplication.data.repository.ZorisRepository
import com.example.myapplication.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DashboardState(
    val isProtectionActive: Boolean = false,
    val manualScans: List<ScanResult> = emptyList(),
    val realtimeThreats: List<ScanResult> = emptyList(),
    val totalScanned: Int = 0,
    val threatsBlocked: Int = 0,
    val manualUrlInput: String = "",
    val isScanningUrl: Boolean = false,
    val urlScanResult: String? = null,
    val isUrlSafe: Boolean = true
)

class DashboardViewModel(
    application: Application,
    private val repository: ZorisRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("phishguard_prefs", Context.MODE_PRIVATE)

    init {
        val savedStatus = sharedPrefs.getBoolean("PROTECTION_ACTIVE", false)
        val savedTotal = sharedPrefs.getInt("TOTAL_SCANS_COUNT", 0)

        _uiState.update { it.copy(
            isProtectionActive = savedStatus,
            totalScanned = savedTotal
        ) }

        viewModelScope.launch {
            ScanBridge.incomingScans.collect {
                incrementTotalScanned()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                db.threatDao().getAllThreats().collect { savedThreats ->
                    val historicalScans = savedThreats.map { threat ->
                        ScanResult(
                            id = threat.id.toString(),
                            type = ScanType.SMS,
                            isSafe = false,
                            confidence = 99,
                            analysisDetails = "From ${threat.sender}: ${threat.snippet}"
                        )
                    }

                    _uiState.update { currentState ->
                        val manualThreatCount = currentState.manualScans.count { !it.isSafe }
                        currentState.copy(
                            realtimeThreats = historicalScans,
                            threatsBlocked = historicalScans.size + manualThreatCount
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PhishGuard_VM", "DB Error: ${e.message}")
            }
        }
    }

    private fun incrementTotalScanned() {
        _uiState.update { currentState ->
            val newTotal = currentState.totalScanned + 1
            sharedPrefs.edit { putInt("TOTAL_SCANS_COUNT", newTotal) }
            currentState.copy(totalScanned = newTotal)
        }
    }

    fun toggleProtection(isActive: Boolean) {
        sharedPrefs.edit { putBoolean("PROTECTION_ACTIVE", isActive) }
        _uiState.update { it.copy(isProtectionActive = isActive) }
    }

    fun updateUrlInput(input: String) = _uiState.update { it.copy(manualUrlInput = input, urlScanResult = null) }

    fun scanManualUrl() {
        val url = _uiState.value.manualUrlInput.trim()
        if (url.isBlank()) return

        _uiState.update { it.copy(isScanningUrl = true, urlScanResult = null) }

        viewModelScope.launch {
            when (val result = repository.checkUrl(url)) {
                is Resource.Success -> {
                    val scanDetails = result.data?.result

                    val isSafe = scanDetails?.isSafeFromServer ?: true

                    incrementTotalScanned()

                    val threatString = scanDetails?.threatList?.joinToString(", ") ?: "Unknown Malware"

                    val newResult = ScanResult(
                        id = UUID.randomUUID().toString(),
                        type = ScanType.URL,
                        isSafe = isSafe,
                        confidence = if (isSafe) 100 else 98,
                        // Combine the Threat and URL for the Dialog
                        analysisDetails = if (isSafe) url else "Threat: $threatString | URL: $url"
                    )

                    _uiState.update { currentState ->
                        val updatedManualList = (listOf(newResult) + currentState.manualScans).take(50)
                        val totalBlocked = currentState.realtimeThreats.size + updatedManualList.count { !it.isSafe }

                        currentState.copy(
                            isScanningUrl = false,
                            isUrlSafe = isSafe,
                            urlScanResult = if (isSafe) "Link is Safe" else " Detected Threat",
                            manualScans = updatedManualList,
                            threatsBlocked = totalBlocked
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(
                        isScanningUrl = false,
                        isUrlSafe = false,
                        urlScanResult = result.message ?: "Failed to reach server."
                    ) }
                }
                is Resource.Loading -> { /* Handled above */ }
            }
        }
    }
    fun clearAllHistory() {
        _uiState.update { it.copy(
            manualScans = emptyList(),
            realtimeThreats = emptyList(),
            totalScanned = 0,
            threatsBlocked = 0,
            urlScanResult = null,
            manualUrlInput = ""
        ) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                db.threatDao().deleteAllThreats()
                sharedPrefs.edit(commit = true) { putInt("TOTAL_SCANS_COUNT", 0) }
            } catch (e: Exception) {
                Log.e("PhishGuard_VM", "Error during wipe: ${e.message}")
            }
        }
    }
}

class DashboardViewModelFactory(
    private val application: Application,
    private val repository: ZorisRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}