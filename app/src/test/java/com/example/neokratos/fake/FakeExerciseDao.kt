package com.example.neokratos.fake

import com.example.neokratos.data.local.dao.ExerciseDao
import com.example.neokratos.data.model.ExerciseEntity
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

class FakeExerciseDao : ExerciseDao {

    private val data = mutableListOf<ExerciseEntity>()
    private val idGenerator = AtomicLong(0L)
    private val delayMs = 5L

    override suspend fun insert(exercise: ExerciseEntity) {
        delay(delayMs)

        val entity = exercise.copy(
            id = if (exercise.id == 0L) idGenerator.incrementAndGet() else exercise.id
        )

        data.removeAll { it.id == entity.id }
        data.add(entity)
    }

    override suspend fun deleteById(id: Long) {
        delay(delayMs)
        data.removeIf { it.id == id }
    }

    override suspend fun getBySessionId(sessionId: Long): List<ExerciseEntity> {
        delay(delayMs)
        return data
            .filter { it.sessionId == sessionId }
            .sortedBy { it.name }
    }

    // ==== TEST HELPERS ====

    fun seed(vararg exercises: ExerciseEntity) {
        data.clear()
        data.addAll(exercises)
    }

    fun clear() = data.clear()
}
