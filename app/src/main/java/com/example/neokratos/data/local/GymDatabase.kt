package com.example.neokratos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.neokratos.data.local.dao.BodyMetricDao
import com.example.neokratos.data.local.dao.ExerciseDao
import com.example.neokratos.data.local.dao.SessionExerciseDao
import com.example.neokratos.data.local.dao.SetLogDao
import com.example.neokratos.data.local.dao.TemplateExerciseDao
import com.example.neokratos.data.local.dao.WorkoutSessionDao
import com.example.neokratos.data.local.dao.WorkoutTemplateDao
import com.example.neokratos.data.local.entity.BodyMetricEntity
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.local.entity.SessionExerciseEntity
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.TemplateExerciseEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.entity.WorkoutTemplateEntity

/**
 * Database principale dell'app.
 *
 * IMPORTANTE: Quando aggiungi/modifichi entities, devi:
 * 1. Incrementare il numero di version
 * 2. Cancellare app e reinstallare (per ora, migrations dopo)
 */
@Database(
    entities = [
        ExerciseEntity::class,
        TemplateExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SetLogEntity::class,
        BodyMetricEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class) // AGGIUNTO: abilita i converters per Enum e List
abstract class GymDatabase : RoomDatabase() {

    // DAOs
    abstract fun exerciseDao(): ExerciseDao
    abstract fun templateExerciseDao(): TemplateExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun sessionExerciseDao(): SessionExerciseDao
    abstract fun setLogDao(): SetLogDao
    abstract fun bodyMetricDao(): BodyMetricDao

    companion object {
        @Volatile
        private var INSTANCE: GymDatabase? = null

        fun getInstance(context: Context): GymDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GymDatabase::class.java,
                    "neokratos_db"
                )
                    .fallbackToDestructiveMigration() // TEMPORANEO: ricrea DB se cambi schema
                    .build()
                    .also { INSTANCE = it }
            }
    }
}