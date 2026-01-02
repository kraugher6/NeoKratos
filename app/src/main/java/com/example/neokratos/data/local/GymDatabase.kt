package com.example.neokratos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.neokratos.data.local.dao.ExerciseDao
import com.example.neokratos.data.local.dao.WorkoutSessionDao
import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity

@Database(
    entities = [
        WorkoutTemplateEntity::class,
        WorkoutSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {

    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        @Volatile private var INSTANCE: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "neokratos_db"
                ).build().also { INSTANCE = it }
            }
    }
}

