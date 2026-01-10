package com.example.neokratos.fake

import com.example.neokratos.data.dao.WorkoutDao
import com.example.neokratos.data.model.WorkoutEntity
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

class FakeWorkoutDao : WorkoutDao {

    private val data = mutableListOf<WorkoutEntity>()
    private val idGenerator = AtomicLong(0L)
    private val delayMs = 5L

    override suspend fun insert(workout: WorkoutEntity) {
        delay(delayMs)

        val entity = workout.copy(
            id = if (workout.id == 0L) idGenerator.incrementAndGet() else workout.id
        )

        data.removeAll { it.id == entity.id }
        data.add(entity)
    }

    override suspend fun deleteById(id: Long) {
        delay(delayMs)
        data.removeIf { it.id == id }
    }

    override suspend fun getById(id: Long): WorkoutEntity? {
        delay(delayMs)
        return data.find { it.id == id }
    }

    override suspend fun getAll(): List<WorkoutEntity> {
        delay(delayMs)
        return data.sortedBy { it.createdAt }
    }

    // ==== TEST HELPERS ====

    fun seed(vararg workouts: WorkoutEntity) {
        data.clear()
        data.addAll(workouts)
    }

    fun clear() = data.clear()
}
