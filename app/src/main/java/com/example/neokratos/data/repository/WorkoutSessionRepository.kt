package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.SessionExerciseDao
import com.example.neokratos.data.local.dao.SetLogDao
import com.example.neokratos.data.local.dao.TemplateExerciseDao
import com.example.neokratos.data.local.dao.WorkoutSessionDao
import com.example.neokratos.data.local.entity.SessionExerciseEntity
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.SessionWithExercises
import kotlinx.coroutines.flow.Flow

class WorkoutSessionRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val setLogDao: SetLogDao,
    private val templateExerciseDao: TemplateExerciseDao? = null
) {

    val allWorkouts: Flow<List<WorkoutSessionEntity>> =
        workoutSessionDao.getAllWorkouts()

    val activeWorkout: Flow<WorkoutSessionEntity?> =
        workoutSessionDao.getActiveWorkoutFlow()

    val activeWorkoutComplete: Flow<SessionComplete?> =
        workoutSessionDao.getActiveWorkoutComplete()

    val completedWorkoutsComplete: Flow<List<SessionComplete>> =
        workoutSessionDao.getAllCompletedComplete()

    suspend fun startWorkout(
        templateId: Long? = null,
        name: String? = null
    ): Long {
        val existing = workoutSessionDao.getActiveWorkout()
        if (existing != null) {
            throw IllegalStateException("Cannot start new workout: workout ${existing.id} is already active")
        }

        val session = WorkoutSessionEntity(
            templateId = templateId,
            name = name,
            startTime = System.currentTimeMillis(),
            endTime = null
        )

        return workoutSessionDao.insertWorkout(session)
    }

    suspend fun startWorkoutFromTemplate(templateId: Long): Long {
        val sessionId = startWorkout(templateId = templateId)

        if (templateExerciseDao != null) {
            val templateExercises = templateExerciseDao.getForCloning(templateId)

            templateExercises.forEach { templateExercise ->
                // Aggiungi l'esercizio alla sessione
                val sessionExerciseId = addExerciseToSession(
                    sessionId = sessionId,
                    exerciseId = templateExercise.exerciseId,
                    order = templateExercise.order
                )

                // NUOVA FUNZIONALITÀ: Replica i set dal template
                // Crea set placeholder con i parametri del template
                val setLogs = mutableListOf<SetLogEntity>()

                for (setNumber in 1..templateExercise.targetSets) {
                    val setLog = SetLogEntity(
                        sessionExerciseId = sessionExerciseId,
                        setNumber = setNumber,
                        weight = 0f, // Da compilare dall'utente
                        reps = templateExercise.targetRepsMin, // Usa il minimo come default
                        rpe = null, // Da compilare dall'utente
                        restSeconds = templateExercise.restSeconds,
                        timestamp = System.currentTimeMillis(),
                        completed = false, // Set non completato
                        notes = null
                    )
                    setLogs.add(setLog)
                }

                // Inserisci tutti i set placeholder
                if (setLogs.isNotEmpty()) {
                    setLogDao.insertAll(setLogs)
                }
            }
        }

        return sessionId
    }

    suspend fun completeWorkout(sessionId: Long) {
        val session = workoutSessionDao.getSessionComplete(sessionId)
            ?: throw IllegalArgumentException("Session $sessionId not found")

        val totalVolume = calculateTotalVolume(sessionId)
        val totalSets = calculateTotalSets(sessionId)
        val averageRPE = calculateAverageRPE(sessionId)

        workoutSessionDao.completeWorkout(
            sessionId = sessionId,
            endTime = System.currentTimeMillis(),
            totalVolume = totalVolume,
            totalSets = totalSets,
            averageRPE = averageRPE
        )
    }

    suspend fun cancelWorkout(sessionId: Long) {
        workoutSessionDao.deleteById(sessionId)
    }

    suspend fun updateSessionNotes(sessionId: Long, notes: String?) {
        workoutSessionDao.updateNotes(sessionId, notes)
    }

    suspend fun updateSessionName(sessionId: Long, name: String?) {
        workoutSessionDao.updateName(sessionId, name)
    }

    suspend fun updateBodyweight(sessionId: Long, bodyweight: Float?) {
        workoutSessionDao.updateBodyweight(sessionId, bodyweight)
    }

    suspend fun addExerciseToSession(
        sessionId: Long,
        exerciseId: Long,
        order: Int? = null
    ): Long {
        val finalOrder = order ?: sessionExerciseDao.getTotalCount(sessionId)

        val sessionExercise = SessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            order = finalOrder,
            startTime = System.currentTimeMillis()
        )

        return sessionExerciseDao.insert(sessionExercise)
    }

    suspend fun removeExerciseFromSession(sessionExerciseId: Long) {
        sessionExerciseDao.deleteById(sessionExerciseId)
    }

    suspend fun completeExercise(sessionExerciseId: Long) {
        sessionExerciseDao.markCompleted(sessionExerciseId)
    }

    suspend fun reorderExercises(updates: List<Pair<Long, Int>>) {
        sessionExerciseDao.reorderExercises(updates)
    }

    suspend fun updateExerciseNotes(sessionExerciseId: Long, notes: String?) {
        sessionExerciseDao.updateNotes(sessionExerciseId, notes)
    }

    suspend fun logSet(
        sessionExerciseId: Long,
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        restSeconds: Int? = null,
        completed: Boolean = true,
        notes: String? = null
    ): Long {
        val currentSetCount = setLogDao.getSetCount(sessionExerciseId)
        val setNumber = currentSetCount + 1

        val setLog = SetLogEntity(
            sessionExerciseId = sessionExerciseId,
            setNumber = setNumber,
            weight = weight,
            reps = reps,
            rpe = rpe,
            restSeconds = restSeconds,
            timestamp = System.currentTimeMillis(),
            completed = completed,
            notes = notes
        )

        return setLogDao.insert(setLog)
    }

    suspend fun updateSet(setLog: SetLogEntity) {
        setLogDao.update(setLog)
    }

    suspend fun deleteSet(setId: Long) {
        setLogDao.deleteById(setId)
    }

    suspend fun getSessionComplete(sessionId: Long): SessionComplete? {
        return workoutSessionDao.getSessionComplete(sessionId)
    }

    fun getSessionCompleteFlow(sessionId: Long): Flow<SessionComplete?> {
        return workoutSessionDao.getSessionCompleteFlow(sessionId)
    }

    suspend fun getSessionWithExercises(sessionId: Long): SessionWithExercises? {
        return workoutSessionDao.getSessionWithExercises(sessionId)
    }

    suspend fun hasActiveWorkout(): Boolean {
        return workoutSessionDao.hasActiveWorkout()
    }

    suspend fun getActiveWorkoutId(): Long? {
        return workoutSessionDao.getActiveWorkout()?.id
    }

    fun getExercisesForSession(sessionId: Long): Flow<List<SessionExerciseEntity>> {
        return sessionExerciseDao.getForSession(sessionId)
    }

    fun getSetsForExercise(sessionExerciseId: Long): Flow<List<SetLogEntity>> {
        return setLogDao.getSetsForExercise(sessionExerciseId)
    }

    suspend fun getPreviousSetsForExercise(
        exerciseId: Long,
        currentSessionId: Long
    ): List<SetLogEntity> {
        return emptyList()
    }

    private suspend fun calculateTotalVolume(sessionId: Long): Float {
        val exercises = sessionExerciseDao.getForSessionOneShot(sessionId)
        var totalVolume = 0f

        exercises.forEach { exercise ->
            val volume = setLogDao.getTotalVolume(exercise.id)
            totalVolume += volume ?: 0f
        }

        return totalVolume
    }

    private suspend fun calculateTotalSets(sessionId: Long): Int {
        val exercises = sessionExerciseDao.getForSessionOneShot(sessionId)
        var totalSets = 0

        exercises.forEach { exercise ->
            totalSets += setLogDao.getCompletedSetCount(exercise.id)
        }

        return totalSets
    }

    private suspend fun calculateAverageRPE(sessionId: Long): Float? {
        val exercises = sessionExerciseDao.getForSessionOneShot(sessionId)
        val allRPEs = mutableListOf<Float>()

        exercises.forEach { exercise ->
            val avgRPE = setLogDao.getAverageRPE(exercise.id)
            avgRPE?.let { allRPEs.add(it) }
        }

        return if (allRPEs.isNotEmpty()) {
            allRPEs.average().toFloat()
        } else {
            null
        }
    }

    fun getSessionsForTemplate(templateId: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutSessionDao.getSessionsForTemplate(templateId)
    }

    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutSessionDao.getSessionsInRange(startTime, endTime)
    }

    suspend fun getCompletedWorkoutCount(): Int {
        return workoutSessionDao.getCompletedCount()
    }

    suspend fun getTotalVolumeAllTime(): Float {
        return workoutSessionDao.getTotalVolumeAllTime() ?: 0f
    }

    suspend fun getAverageDuration(): Long? {
        return workoutSessionDao.getAverageDuration()
    }
}