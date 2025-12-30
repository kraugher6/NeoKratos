package com.example.neokratos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.neokratos.data.local.dao.ExerciseDao
import com.example.neokratos.data.local.dao.WorkoutDao
import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.local.entity.WorkoutEntity
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity

@Database(
    entities = [
        WorkoutEntity::class,
        WorkoutTemplateEntity::class,
        ExerciseEntity::class
    ],
    version = 3,
    exportSchema = false
)

abstract class GymDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun exerciseDao(): ExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "neokratos_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
