package com.example.neokratos.data.local.entity

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests per WorkoutSessionEntity e le sue extension functions.
 */
class WorkoutSessionEntityTest {

    // ===== STATUS CHECKS =====

    @Test
    fun `isCompleted ritorna true quando endTime è non null`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = 2000L
        )

        assertTrue(session.isCompleted())
    }

    @Test
    fun `isCompleted ritorna false quando endTime è null`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = null
        )

        assertFalse(session.isCompleted())
    }

    @Test
    fun `isInProgress ritorna true quando endTime è null`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = null
        )

        assertTrue(session.isInProgress())
    }

    @Test
    fun `isInProgress ritorna false quando endTime è non null`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = 2000L
        )

        assertFalse(session.isInProgress())
    }

    // ===== DURATION CALCULATIONS =====

    @Test
    fun `getDurationSeconds calcola correttamente la differenza`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L, // 1 secondo
            endTime = 61000L   // 61 secondi
        )

        // (61000 - 1000) / 1000 = 60 secondi
        assertEquals(60L, session.getDurationSeconds())
    }

    @Test
    fun `getDurationSeconds ritorna null se workout non completato`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = null
        )

        assertNull(session.getDurationSeconds())
    }

    @Test
    fun `getDurationDisplay formatta ore_minuti_secondi correttamente`() {
        val session = WorkoutSessionEntity(
            startTime = 0L,
            endTime = 3665000L // 1 ora, 1 minuto, 5 secondi in millisecondi
        )

        assertEquals("1:01:05", session.getDurationDisplay())
    }

    @Test
    fun `getDurationDisplay formatta minuti_secondi quando sotto 1 ora`() {
        val session = WorkoutSessionEntity(
            startTime = 0L,
            endTime = 3305000L // 55 minuti, 5 secondi
        )

        assertEquals("55:05", session.getDurationDisplay())
    }

    @Test
    fun `getDurationDisplay mostra In progress per workout non completato`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = null
        )

        assertEquals("In progress", session.getDurationDisplay())
    }

    @Test
    fun `getDurationDisplay formatta correttamente workout di 2 ore esatte`() {
        val session = WorkoutSessionEntity(
            startTime = 0L,
            endTime = 7200000L // 2 ore = 7200 secondi
        )

        assertEquals("2:00:00", session.getDurationDisplay())
    }

    // ===== NAME DISPLAY =====

    @Test
    fun `getDisplayName usa custom name se presente`() {
        val session = WorkoutSessionEntity(
            name = "Heavy Squat Day",
            startTime = 1704470400000L // Una data specifica
        )

        assertEquals("Heavy Squat Day", session.getDisplayName())
    }

    @Test
    fun `getDisplayName genera nome con data se custom name è null`() {
        val session = WorkoutSessionEntity(
            name = null,
            startTime = 1704470400000L // Venerdì, 5 Gennaio 2024 12:00:00
        )

        val displayName = session.getDisplayName()

        // Verifica che contenga "Workout -" e una data
        assertTrue(displayName.startsWith("Workout - "))
        assertTrue(displayName.contains("Jan") || displayName.contains("Gen"))
    }

    // ===== STATS CALCULATIONS =====

    @Test
    fun `getAverageVolumePerSet calcola correttamente`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            totalVolume = 3000f,
            totalSets = 10
        )

        // 3000 / 10 = 300
        assertEquals(300f, session.getAverageVolumePerSet(), 0.01f)
    }

    @Test
    fun `getAverageVolumePerSet ritorna zero se totalSets è zero`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            totalVolume = 3000f,
            totalSets = 0
        )

        assertEquals(0f, session.getAverageVolumePerSet(), 0.01f)
    }

    @Test
    fun `getAverageVolumePerSet con decimali funziona correttamente`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            totalVolume = 2750f,
            totalSets = 11
        )

        // 2750 / 11 ≈ 250
        assertEquals(250f, session.getAverageVolumePerSet(), 0.1f)
    }

    // ===== DATE/TIME DISPLAY =====

    @Test
    fun `getDateDisplay formatta data correttamente`() {
        // 5 Gennaio 2024, 12:00:00
        val session = WorkoutSessionEntity(
            startTime = 1704470400000L
        )

        val dateDisplay = session.getDateDisplay()

        // Il formato dipende dal locale, ma dovrebbe contenere almeno questi elementi
        assertTrue(dateDisplay.contains("5") || dateDisplay.contains("Jan") || dateDisplay.contains("2024"))
    }

    @Test
    fun `getTimeDisplay formatta ora correttamente`() {
        // 5 Gennaio 2024, 14:30:00 (2:30 PM)
        val session = WorkoutSessionEntity(
            startTime = 1704479400000L
        )

        val timeDisplay = session.getTimeDisplay()

        // Dovrebbe contenere "2:30" o "14:30" a seconda del formato 12h/24h
        assertTrue(timeDisplay.contains("2:30") || timeDisplay.contains("14:30"))
    }

    // ===== EDGE CASES =====

    @Test
    fun `workout con tutti i campi null o default funziona`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L
        )

        // Non dovrebbe crashare
        assertNotNull(session)
        assertFalse(session.isCompleted())
        assertTrue(session.isInProgress())
        assertEquals(0f, session.totalVolume, 0.01f)
        assertEquals(0, session.totalSets)
    }

    @Test
    fun `workout con volume e sets alti funziona`() {
        val session = WorkoutSessionEntity(
            startTime = 1000L,
            endTime = 2000L,
            totalVolume = 50000f, // 50 tonnellate!
            totalSets = 200,
            averageRPE = 8.5f
        )

        assertTrue(session.isCompleted())
        assertEquals(250f, session.getAverageVolumePerSet(), 0.1f)
    }
}