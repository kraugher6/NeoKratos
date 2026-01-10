package com.example.neokratos.fake

import com.example.neokratos.data.dao.SessionDao
import com.example.neokratos.data.model.SessionEntity
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong

class FakeSessionDao : SessionDao {

    private val data = mutableListOf<SessionEntity>()
    private val idGenerator = AtomicLong(0L)
    private val delayMs = 5L

    override suspend fun insert(session: SessionEntity) {
        delay(delayMs)

        val entity = session.copy(
            id = if (session.id == 0L) idGenerator.incrementAndGet() else session.id
        )

        data.removeAll { it.id == entity.id }
        data.add(entity)
    }

    override suspend fun deleteById(id: Long) {
        delay(delayMs)
        data.removeIf { it.id == id }
    }

    override suspend fun getByWorkoutId(workoutId: Long): List<SessionEntity> {
        delay(delayMs)
        return data
            .filter { it.workoutId == workoutId }
            .sortedBy { it.date }
    }

    override suspend fun getById(id: Long): SessionEntity? {
        delay(delayMs)
        return data.find { it.id == id }
    }

    // ==== TEST HELPERS ====

    fun seed(vararg sessions: SessionEntity) {
        data.clear()
        data.addAll(sessions)
    }

    fun clear() = data.clear()
}
