package com.example.neokratos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.neokratos.data.local.dao.WorkoutDao
import com.example.neokratos.data.local.entity.SetEntity
import com.example.neokratos.data.local.entity.WorkoutEntity

@Database(
    entities = [WorkoutEntity::class, SetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "neokratos_db"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
