package com.example.myapplication.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
// CRITICAL: You must import the entity here for KSP to see it!
import com.example.myapplication.data.model.ThreatEntity

@Dao
interface ThreatDao {
    @Insert
    suspend fun insertThreat(threat: ThreatEntity)

    @Query("SELECT * FROM threat_logs ORDER BY id DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>

    @Query("DELETE FROM threat_logs")
    suspend fun deleteAllThreats()
}