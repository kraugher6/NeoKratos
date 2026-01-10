package com.example.neokratos.data.local.entity

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests per SetLogEntity e le sue extension functions.
 *
 * COME LANCIARE I TEST:
 *
 * Metodo 1 - Da Android Studio:
 *   1. Click destro su questo file
 *   2. Seleziona "Run 'SetLogEntityTest'"
 *   3. I test girano sulla JVM (velocissimi, ~2 secondi)
 *
 * Metodo 2 - Da Terminale:
 *   ./gradlew test --tests "*.SetLogEntityTest"
 *
 * Metodo 3 - Tutti i test del progetto:
 *   ./gradlew test
 *
 * RISULTATI:
 *   - ✓ Verde = test passato
 *   - ✗ Rosso = test fallito (vedi messaggio di errore)
 *   - Report HTML: app/build/reports/tests/testDebugUnitTest/index.html
 */
class SetLogEntityTest {

    // ===== VOLUME CALCULATION =====

    @Test
    fun `getVolume calcola correttamente peso x reps`() {
        // Arrange: Prepara i dati di test
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 10
        )

        // Act: Esegui la funzione da testare
        val volume = set.getVolume()

        // Assert: Verifica che il risultato sia corretto
        assertEquals(1000f, volume, 0.01f) // 100 * 10 = 1000
    }

    @Test
    fun `getVolume con peso zero ritorna zero`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 0f,
            reps = 10
        )

        assertEquals(0f, set.getVolume(), 0.01f)
    }

    @Test
    fun `getVolume con reps zero ritorna zero`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 0
        )

        assertEquals(0f, set.getVolume(), 0.01f)
    }

    @Test
    fun `getVolume con pesi decimali funziona correttamente`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 22.5f, // Peso con decimali (es. dischi da 2.5kg)
            reps = 8
        )

        assertEquals(180f, set.getVolume(), 0.01f) // 22.5 * 8 = 180
    }

    // ===== 1RM ESTIMATION (Epley Formula) =====

    @Test
    fun `estimateOneRepMax usa formula Epley correttamente`() {
        // Formula: weight * (1 + reps/30)
        // 100kg x 8 reps → 100 * (1 + 8/30) ≈ 126.67kg
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8
        )

        val oneRM = set.estimateOneRepMax()

        assertEquals(126.67f, oneRM, 0.1f)
    }

    @Test
    fun `estimateOneRepMax con 1 rep ritorna peso leggermente maggiore`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 150f,
            reps = 1
        )

        // 150 * (1 + 1/30) = 150 * 1.033 = 155
        assertEquals(155f, set.estimateOneRepMax(), 0.1f)
    }

    @Test
    fun `estimateOneRepMax con 0 reps ritorna zero`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 0
        )

        assertEquals(0f, set.estimateOneRepMax(), 0.01f)
    }

    @Test
    fun `estimateOneRepMax con 12 reps stima correttamente`() {
        // 100 * (1 + 12/30) = 100 * 1.4 = 140
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 12
        )

        assertEquals(140f, set.estimateOneRepMax(), 0.1f)
    }

    // ===== 1RM ESTIMATION WITH RPE =====

    @Test
    fun `estimateOneRepMaxWithRPE aggiusta per reps in reserve`() {
        // 100kg x 8 @ RPE 8 = 2 RIR (reps in reserve)
        // Significa che potevi fare 10 reps totali
        // 1RM = 100 * (1 + 10/30) ≈ 133.33kg
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 8f
        )

        val oneRM = set.estimateOneRepMaxWithRPE()

        assertEquals(133.33f, oneRM, 0.1f)
    }

    @Test
    fun `estimateOneRepMaxWithRPE con RPE 10 non aggiunge reps`() {
        // RPE 10 = 0 RIR, quindi stima è uguale a quella standard
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 10f
        )

        val oneRM = set.estimateOneRepMaxWithRPE()
        val oneRMStandard = set.estimateOneRepMax()

        assertEquals(oneRMStandard, oneRM, 0.01f)
    }

    @Test
    fun `estimateOneRepMaxWithRPE senza RPE usa formula standard`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = null // RPE non fornito
        )

        val oneRMStandard = set.estimateOneRepMax()
        val oneRMWithRPE = set.estimateOneRepMaxWithRPE()

        assertEquals(oneRMStandard, oneRMWithRPE, 0.01f)
    }

    @Test
    fun `estimateOneRepMaxWithRPE con RPE 7 aggiunge 3 reps`() {
        // RPE 7 = 3 RIR
        // 100kg x 8 + 3 RIR = come se avessi fatto 11 reps
        // 100 * (1 + 11/30) ≈ 136.67
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 7f
        )

        assertEquals(136.67f, set.estimateOneRepMaxWithRPE(), 0.1f)
    }

    // ===== RPE-BASED CLASSIFICATIONS =====

    @Test
    fun `isHeavySet ritorna true per RPE 9`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 9f
        )

        assertTrue(set.isHeavySet())
    }

    @Test
    fun `isHeavySet ritorna true per RPE 8_5 (soglia)`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 8.5f
        )

        assertTrue(set.isHeavySet())
    }

    @Test
    fun `isHeavySet ritorna false per RPE 8`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 8f
        )

        assertFalse(set.isHeavySet())
    }

    @Test
    fun `isHeavySet ritorna false per RPE 7`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 7f
        )

        assertFalse(set.isHeavySet())
    }

    @Test
    fun `isHeavySet ritorna false quando RPE è null`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = null
        )

        assertFalse(set.isHeavySet())
    }

    @Test
    fun `hasGoodForm ritorna true per RPE 8 (soglia)`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 8f
        )

        assertTrue(set.hasGoodForm())
    }

    @Test
    fun `hasGoodForm ritorna true per RPE 7_5`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 7.5f
        )

        assertTrue(set.hasGoodForm())
    }

    @Test
    fun `hasGoodForm ritorna false per RPE 9`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 9f
        )

        assertFalse(set.hasGoodForm())
    }

    @Test
    fun `hasGoodForm ritorna true quando RPE è null (beneficio del dubbio)`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = null
        )

        assertTrue(set.hasGoodForm())
    }

    // ===== REST TIME DISPLAY =====

    @Test
    fun `getRestTimeDisplay formatta 90 secondi come 1_30`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            restSeconds = 90
        )

        assertEquals("1:30", set.getRestTimeDisplay())
    }

    @Test
    fun `getRestTimeDisplay formatta 120 secondi come 2_00`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            restSeconds = 120
        )

        assertEquals("2:00", set.getRestTimeDisplay())
    }

    @Test
    fun `getRestTimeDisplay formatta 65 secondi come 1_05`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            restSeconds = 65
        )

        assertEquals("1:05", set.getRestTimeDisplay())
    }

    @Test
    fun `getRestTimeDisplay con null mostra No rest`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            restSeconds = null
        )

        assertEquals("No rest", set.getRestTimeDisplay())
    }

    @Test
    fun `getRestTimeDisplay con 0 secondi mostra 0_00`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            restSeconds = 0
        )

        assertEquals("0:00", set.getRestTimeDisplay())
    }

    // ===== RPE DISPLAY =====

    @Test
    fun `getRPEDisplay formatta decimale correttamente`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 8.5f
        )

        assertEquals("8.5", set.getRPEDisplay())
    }

    @Test
    fun `getRPEDisplay formatta intero con un decimale`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = 9f
        )

        assertEquals("9.0", set.getRPEDisplay())
    }

    @Test
    fun `getRPEDisplay con null mostra N_A`() {
        val set = SetLogEntity(
            sessionExerciseId = 1,
            setNumber = 1,
            weight = 100f,
            reps = 8,
            rpe = null
        )

        assertEquals("N/A", set.getRPEDisplay())
    }
}