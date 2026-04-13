package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myapplication.data.model.ScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DashboardState(
    val isProtectionActive: Boolean = false,
    val recentScans: List<ScanResult> = emptyList()
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    fun toggleProtection(isActive: Boolean) {
        // Here, we don't name the parameter, so 'it' works perfectly
        _uiState.update { it.copy(isProtectionActive = isActive) }

        if (isActive) {
            println("Background SMS Listener Service would start here...")
        } else {
            println("Background Service would stop here...")
        }
    }

    fun addIncomingScan(result: ScanResult) {
        // Here we explicitly named it 'currentState', so we must use 'currentState' instead of 'it'
        _uiState.update { currentState ->
            val updatedList = listOf(result) + currentState.recentScans

            // FIX: Changed 'it.copy' to 'currentState.copy'
            currentState.copy(recentScans = updatedList.take(50))
        }
    }
}