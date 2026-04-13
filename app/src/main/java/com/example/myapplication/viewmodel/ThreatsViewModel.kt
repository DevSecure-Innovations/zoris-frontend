package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.model.ThreatEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ThreatsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.threatDao()
    val allThreats: Flow<List<ThreatEntity>> = dao.getAllThreats()

    fun clearAllThreats() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteAllThreats()
        }
    }
}