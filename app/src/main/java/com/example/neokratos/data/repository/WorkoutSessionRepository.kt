package com.example.neokratos.data.repository

import com.example.neokratos.data.local.dao.SessionExerciseDao
import com.example.neokratos.data.local.dao.SetLogDao
import com.example.neokratos.data.local.dao.WorkoutSessionDao
import com.example.neokratos.data.local.entity.SessionExerciseEntity
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import com.example.neokratos.data.local.relations.SessionComplete
import com.example.neokratos.data.local.relations.SessionWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * Repository for WorkoutSession operations.
 *
 * This is the single source of truth for active workouts and workout history.
 * Handles all business logic for:
 * - Starting/ending workouts
 * - Adding exercises to sessions
 * - Logging sets
 * - Calculating stats (volume, RPE, etc.)
 */
class WorkoutSessionRepository(
    private val workoutSessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: SessionExerciseDao,
    private val setLogDao: SetLogDao
) {

    // ===== EXPOSED FLOWS =====

    /**
     * All completed workouts, ordered by date (newest first).
     */
    val allWorkouts: Flow<List<WorkoutSessionEntity>> =
        workoutSessionDao.getAllWorkouts()

    /**
     * Currently active workout (in-progress).
     * null if no workout is active.
     */
    val activeWorkout: Flow<WorkoutSessionEntity?> =
        workoutSessionDao.getActiveWorkoutFlow()

    /**
     * Active workout with complete details (exercises + sets).
     * THIS IS THE MAIN FLOW FOR ACTIVE WORKOUT SCREEN.
     */
    val activeWorkoutComplete: Flow<SessionComplete?> =
        workoutSessionDao.getActiveWorkoutComplete()

    /**
     * All completed workouts with full details.
     * Used for history screen.
     */
    val completedWorkoutsComplete: Flow<List<SessionComplete>> =
        workoutSessionDao.getAllCompletedComplete()

    // ===== WORKOUT SESSION OPERATIONS =====

    /**
     * Start a new workout session.
     *
     * @param templateId Optional template to base workout on
     * @param name Optional custom name for session
     * @return The ID of the created session
     */
    suspend fun startWorkout(
        templateId: Long? = null,
        name: String? = null
    ): Long {
        // Check if there's already an active workout
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

    /**
     * End the active workout.
     * Calculates final stats (volume, sets, avg RPE) and sets endTime.
     *
     * @param sessionId The session to complete
     */
    suspend fun completeWorkout(sessionId: Long) {
        val session = workoutSessionDao.getSessionComplete(sessionId)
            ?: throw IllegalArgumentException("Session $sessionId not found")

        // Calculate stats
        val totalVolume = calculateTotalVolume(sessionId)
        val totalSets = calculateTotalSets(sessionId)
        val averageRPE = calculateAverageRPE(sessionId)

        // Mark as completed
        workoutSessionDao.completeWorkout(
            sessionId = sessionId,
            endTime = System.currentTimeMillis(),
            totalVolume = totalVolume,
            totalSets = totalSets,
            averageRPE = averageRPE
        )
    }

    /**
     * Cancel/delete the active workout.
     * Removes session and all its data.
     */
    suspend fun cancelWorkout(sessionId: Long) {
        workoutSessionDao.deleteById(sessionId)
    }

    /**
     * Update session notes.
     */
    suspend fun updateSessionNotes(sessionId: Long, notes: String?) {
        workoutSessionDao.updateNotes(sessionId, notes)
    }

    /**
     * Update session name.
     */
    suspend fun updateSessionName(sessionId: Long, name: String?) {
        workoutSessionDao.updateName(sessionId, name)
    }

    /**
     * Update bodyweight for session.
     */
    suspend fun updateBodyweight(sessionId: Long, bodyweight: Float?) {
        workoutSessionDao.updateBodyweight(sessionId, bodyweight)
    }

    // ===== SESSION EXERCISE OPERATIONS =====

    /**
     * Add an exercise to the active workout.
     *
     * @param sessionId The workout session
     * @param exerciseId Exercise from library
     * @param order Position in workout (0 = first)
     * @return The ID of the created SessionExercise
     */
    suspend fun addExerciseToSession(
        sessionId: Long,
        exerciseId: Long,
        order: Int? = null
    ): Long {
        // If no order specified, add at end
        val finalOrder = order ?: sessionExerciseDao.getTotalCount(sessionId)

        val sessionExercise = SessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            order = finalOrder,
            startTime = System.currentTimeMillis()
        )

        return sessionExerciseDao.insert(sessionExercise)
    }

    /**
     * Remove an exercise from the session.
     * Also removes all its sets (cascade).
     */
    suspend fun removeExerciseFromSession(sessionExerciseId: Long) {
        sessionExerciseDao.deleteById(sessionExerciseId)
    }

    /**
     * Mark exercise as completed.
     */
    suspend fun completeExercise(sessionExerciseId: Long) {
        sessionExerciseDao.markCompleted(sessionExerciseId)
    }

    /**
     * Reorder exercises in session.
     *
     * @param updates List of (sessionExerciseId, newOrder) pairs
     */
    suspend fun reorderExercises(updates: List<Pair<Long, Int>>) {
        sessionExerciseDao.reorderExercises(updates)
    }

    /**
     * Update exercise notes.
     */
    suspend fun updateExerciseNotes(sessionExerciseId: Long, notes: String?) {
        sessionExerciseDao.updateNotes(sessionExerciseId, notes)
    }

    // ===== SET LOGGING OPERATIONS =====

    /**
     * Log a set for an exercise.
     *
     * This is the core function called during workout when user completes a set.
     *
     * @param sessionExerciseId Which exercise in the session
     * @param weight Weight lifted (kg)
     * @param reps Reps completed
     * @param rpe Rate of Perceived Exertion (1-10, nullable)
     * @param restSeconds Rest time after this set (nullable)
     * @param completed Whether set was actually done (default true)
     * @return The ID of the logged set
     */
    suspend fun logSet(
        sessionExerciseId: Long,
        weight: Float,
        reps: Int,
        rpe: Float? = null,
        restSeconds: Int? = null,
        completed: Boolean = true,
        notes: String? = null
    ): Long {
        // Get next set number
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

    /**
     * Update an existing set.
     * Used for editing sets after they're logged.
     */
    suspend fun updateSet(setLog: SetLogEntity) {
        setLogDao.update(setLog)
    }

    /**
     * Delete a set.
     * Used for removing accidental entries.
     */
    suspend fun deleteSet(setId: Long) {
        setLogDao.deleteById(setId)
    }

    // ===== QUERIES =====

    /**
     * Get complete session by ID.
     */
    suspend fun getSessionComplete(sessionId: Long): SessionComplete? {
        return workoutSessionDao.getSessionComplete(sessionId)
    }

    /**
     * Get complete session as Flow (reactive).
     */
    fun getSessionCompleteFlow(sessionId: Long): Flow<SessionComplete?> {
        return workoutSessionDao.getSessionCompleteFlow(sessionId)
    }

    /**
     * Get session with exercises (no set details).
     */
    suspend fun getSessionWithExercises(sessionId: Long): SessionWithExercises? {
        return workoutSessionDao.getSessionWithExercises(sessionId)
    }

    /**
     * Check if there's an active workout.
     */
    suspend fun hasActiveWorkout(): Boolean {
        return workoutSessionDao.hasActiveWorkout()
    }

    /**
     * Get active workout ID (null if none).
     */
    suspend fun getActiveWorkoutId(): Long? {
        return workoutSessionDao.getActiveWorkout()?.id
    }

    /**
     * Get exercises for a session as Flow.
     */
    fun getExercisesForSession(sessionId: Long): Flow<List<SessionExerciseEntity>> {
        return sessionExerciseDao.getForSession(sessionId)
    }

    /**
     * Get sets for an exercise as Flow.
     */
    fun getSetsForExercise(sessionExerciseId: Long): Flow<List<SetLogEntity>> {
        return setLogDao.getSetsForExercise(sessionExerciseId)
    }

    /**
     * Get previous sets for an exercise (from last workout).
     * Used for "Last time you did this" comparison.
     */
    suspend fun getPreviousSetsForExercise(
        exerciseId: Long,
        currentSessionId: Long
    ): List<SetLogEntity> {
        // Get most recent session for this exercise (excluding current)
        val previousSession = workoutSessionDao.getAllWorkouts()
        // This is a simplified version - in real implementation,
        // you'd query for most recent session with this exercise
        // For now, return empty list
        return emptyList()
    }

    // ===== STATS CALCULATIONS =====

    /**
     * Calculate total volume for a session (sum of weight × reps).
     */
    private suspend fun calculateTotalVolume(sessionId: Long): Float {
        val exercises = sessionExerciseDao.getForSessionOneShot(sessionId)
        var totalVolume = 0f

        exercises.forEach { exercise ->
            val volume = setLogDao.getTotalVolume(exercise.id)
            totalVolume += volume ?: 0f
        }

        return totalVolume
    }

    /**
     * Calculate total number of completed sets in session.
     */
    private suspend fun calculateTotalSets(sessionId: Long): Int {
        val exercises = sessionExerciseDao.getForSessionOneShot(sessionId)
        var totalSets = 0

        exercises.forEach { exercise ->
            totalSets += setLogDao.getCompletedSetCount(exercise.id)
        }

        return totalSets
    }

    /**
     * Calculate average RPE across all sets in session.
     */
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

    // ===== HISTORY & ANALYTICS =====

    /**
     * Get all sessions for a specific template.
     */
    fun getSessionsForTemplate(templateId: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutSessionDao.getSessionsForTemplate(templateId)
    }

    /**
     * Get sessions in a date range.
     */
    fun getSessionsInRange(startTime: Long, endTime: Long): Flow<List<WorkoutSessionEntity>> {
        return workoutSessionDao.getSessionsInRange(startTime, endTime)
    }

    /**
     * Get total completed workout count.
     */
    suspend fun getCompletedWorkoutCount(): Int {
        return workoutSessionDao.getCompletedCount()
    }

    /**
     * Get total volume all time.
     */
    suspend fun getTotalVolumeAllTime(): Float {
        return workoutSessionDao.getTotalVolumeAllTime() ?: 0f
    }

    /**
     * Get average workout duration.
     */
    suspend fun getAverageDuration(): Long? {
        return workoutSessionDao.getAverageDuration()
    }

    // ===== TEMPLATE INTEGRATION =====

    /**
     * Start workout from a template.
     * Creates session and copies all exercises from template.
     *
     * @param templateId The template to use
     * @return The ID of the created session
     */
    suspend fun startWorkoutFromTemplate(templateId: Long): Long {
        // This will be implemented when we integrate templates
        // For now, just start empty workout
        return startWorkout(templateId = templateId)
    }
}