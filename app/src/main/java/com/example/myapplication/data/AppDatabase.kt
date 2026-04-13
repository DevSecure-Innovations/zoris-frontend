package com.example.myapplication.data

// --- 1. ROOM IMPORTS (Fixes 'Database', 'Room', 'RoomDatabase') ---
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- 2. LOCAL IMPORTS (Fixes 'ThreatEntity' and 'ThreatDao') ---
// Make sure these package names match where your files actually are!
import com.example.myapplication.data.model.ThreatEntity
import com.example.myapplication.data.model.ThreatDao

@Database(entities = [ThreatEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun threatDao(): ThreatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "phishguard_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}