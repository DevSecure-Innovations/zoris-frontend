package com.example.myapplication.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ScanBridge
import com.example.myapplication.data.model.ScanResult
import com.example.myapplication.data.model.ScanType
import com.example.myapplication.data.model.ThreatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("phishguard_prefs", Context.MODE_PRIVATE)

    init {
        // 1. Load initial state from storage
        val savedStatus = sharedPrefs.getBoolean("PROTECTION_ACTIVE", false)
        val savedTotal = sharedPrefs.getInt("TOTAL_SCANS_COUNT", 0)

        _uiState.update { it.copy(
            isProtectionActive = savedStatus,
            totalScanned = savedTotal
        ) }

        // 2. Listen to Live Bridge (Background SMS activity)
        viewModelScope.launch {
            ScanBridge.incomingScans.collect {
                incrementTotalScanned()
            }
        }

        // 3. Listen to Room DB (Background SMS Threats)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                db.threatDao().getAllThreats().collect { savedThreats ->
                    // Map ThreatEntity to ScanResult manually to avoid "unresolved reference"
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

            // Save to SharedPreferences inside the update to stay in sync
            sharedPrefs.edit { putInt("TOTAL_SCANS_COUNT", newTotal) }

            currentState.copy(totalScanned = newTotal)
        }
    }
    fun toggleProtection(isActive: Boolean) {
        sharedPrefs.edit { putBoolean("PROTECTION_ACTIVE", isActive) }
        _uiState.update { it.copy(isProtectionActive = isActive) }
    }

    fun scanManualUrl() {
        val url = _uiState.value.manualUrlInput.trim()
        if (url.isBlank()) return

        _uiState.update { it.copy(isScanningUrl = true, urlScanResult = null) }

        viewModelScope.launch {
            try {
                delay(1500)
                val isSafe = !(url.contains("scam", true) || url.contains("free", true))

                incrementTotalScanned()

                val newResult = ScanResult(
                    id = UUID.randomUUID().toString(),
                    type = ScanType.URL,
                    isSafe = isSafe,
                    confidence = if (isSafe) 100 else 95,
                    analysisDetails = url
                )

                _uiState.update { currentState ->
                    val updatedManualList = (listOf(newResult) + currentState.manualScans).take(50)
                    val totalBlocked = currentState.realtimeThreats.size + updatedManualList.count { !it.isSafe }

                    currentState.copy(
                        isScanningUrl = false,
                        isUrlSafe = isSafe,
                        urlScanResult = if (isSafe) "Link is Safe" else "Malicious Link Detected!",
                        manualScans = updatedManualList,
                        threatsBlocked = totalBlocked
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isScanningUrl = false, urlScanResult = "Error scanning link.") }
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


                sharedPrefs.edit(commit = true) {
                    putInt("TOTAL_SCANS_COUNT", 0)
                }

                Log.d("PhishGuard_VM", "All data wiped successfully.")
            } catch (e: Exception) {
                Log.e("PhishGuard_VM", "Error during wipe: ${e.message}")
            }
        }
    }

    fun updateUrlInput(input: String) = _uiState.update { it.copy(manualUrlInput = input, urlScanResult = null) }
}