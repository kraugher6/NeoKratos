package com.example.neokratos.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.AndroidJUnit4
import com.example.neokratos.data.local.Equipment
import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.local.MuscleGroup
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.local.entity.SessionExerciseEntity
import com.example.neokratos.data.local.entity.SetLogEntity
import com.example.neokratos.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests per SetLogDao.
 *
 * Questi test usano Robolectric per simulare Android senza emulatore.
 * Testano il database Room con query reali.
 *
 * COME LANCIARE:
 *   Click destro → Run 'SetLogDaoTest'
 *   O: ./gradlew test --tests "*.SetLogDaoTest"
 *
 * NOTA: Più lenti dei unit test (~10-15 secondi) ma girano comunque su JVM.
 */
@RunWith(AndroidJUnit4::class)
class SetLogDaoTest {

    private lateinit var database: GymDatabase
    private lateinit var setLogDao: SetLogDao
    private lateinit var sessionExerciseDao: SessionExerciseDao
    private lateinit var workoutSessionDao: WorkoutSessionDao
    private lateinit var exerciseDao: ExerciseDao

    /**
     * Setup: Crea database in-memory prima di ogni test.
     * In-memory = database temporaneo, viene cancellato dopo il test.
     */
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Crea database in-memory (non salva su disco)
        database = Room.inMemoryDatabaseBuilder(
            context,
            GymDatabase::class.java
        )
            .allowMainThreadQueries() // Per i test va bene
            .build()

        setLogDao = database.setLogDao()
        sessionExerciseDao = database.sessionExerciseDao()
        workoutSessionDao = database.workoutSessionDao()
        exerciseDao = database.exerciseDao()
    }

    /**
     * Teardown: Chiudi database dopo ogni test.
     */
    @After
    fun teardown() {
        database.close()
    }

    // ===== HELPER FUNCTIONS =====

    /**
     * Crea setup completo: Exercise → Session → SessionExercise.
     * Ritorna l'ID del SessionExercise per inserire set.
     */
    private suspend fun createTestSetup(): Long {
        // 1. Crea esercizio
        val exerciseId = exerciseDao.insert(
            ExerciseEntity(
                name = "Test Exercise",
                category = ExerciseCategory.CHEST,
                primaryMuscleGroup = MuscleGroup.PECTORALS_MIDDLE,
                equipment = Equipment.BARBELL
            )
        )

        // 2. Crea workout session
        val sessionId = workoutSessionDao.insertWorkout(
            WorkoutSessionEntity(
                startTime = System.currentTimeMillis()
            )
        )

        // 3. Crea session exercise
        val sessionExerciseId = sessionExerciseDao.insert(
            SessionExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                order = 0
            )
        )

        return sessionExerciseId
    }

    // ===== INSERT TESTS =====

    @Test
    fun `insert inserisce set e ritorna ID valido`() = runTest {
        val sessionExerciseId = createTestSetup()

        val setId = setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10
            )
        )

        // ID dovrebbe essere > 0
        assertTrue(setId > 0)
    }

    @Test
    fun `insertAll inserisce multipli set e ritorna lista IDs`() = runTest {
        val sessionExerciseId = createTestSetup()

        val sets = listOf(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10
            ),
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 2,
                weight = 100f,
                reps = 9
            ),
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 3,
                weight = 100f,
                reps = 8
            )
        )

        val ids = setLogDao.insertAll(sets)

        assertEquals(3, ids.size)
        assertTrue(ids.all { it > 0 })
    }

    // ===== QUERY TESTS =====

    @Test
    fun `getSetsForExercise ritorna set ordinati per setNumber`() = runTest {
        val sessionExerciseId = createTestSetup()

        // Inserisci set in ordine sparso
        setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 3,
                weight = 100f,
                reps = 8
            )
        )
        setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10
            )
        )
        setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 2,
                weight = 100f,
                reps = 9
            )
        )

        // Query dovrebbe ritornare in ordine 1, 2, 3
        val sets = setLogDao.getSetsForExercise(sessionExerciseId).first()

        assertEquals(3, sets.size)
        assertEquals(1, sets[0].setNumber)
        assertEquals(2, sets[1].setNumber)
        assertEquals(3, sets[2].setNumber)
    }

    @Test
    fun `getById ritorna il set corretto`() = runTest {
        val sessionExerciseId = createTestSetup()

        val setId = setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10,
                rpe = 8.5f
            )
        )

        val retrieved = setLogDao.getById(setId)

        assertNotNull(retrieved)
        assertEquals(100f, retrieved!!.weight, 0.01f)
        assertEquals(10, retrieved.reps)
        assertEquals(8.5f, retrieved.rpe!!, 0.01f)
    }

    // ===== UPDATE TESTS =====

    @Test
    fun `update modifica correttamente un set`() = runTest {
        val sessionExerciseId = createTestSetup()

        val setId = setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10
            )
        )

        // Modifica
        val originalSet = setLogDao.getById(setId)!!
        val updated = originalSet.copy(
            weight = 105f,
            reps = 12,
            rpe = 9f
        )
        setLogDao.update(updated)

        // Verifica
        val retrieved = setLogDao.getById(setId)!!
        assertEquals(105f, retrieved.weight, 0.01f)
        assertEquals(12, retrieved.reps)
        assertEquals(9f, retrieved.rpe!!, 0.01f)
    }

    // ===== DELETE TESTS =====

    @Test
    fun `deleteById rimuove il set`() = runTest {
        val sessionExerciseId = createTestSetup()

        val setId = setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10
            )
        )

        setLogDao.deleteById(setId)

        val retrieved = setLogDao.getById(setId)
        assertNull(retrieved)
    }

    @Test
    fun `deleteAllForSessionExercise rimuove tutti i set di un esercizio`() = runTest {
        val sessionExerciseId = createTestSetup()

        // Inserisci 3 set
        setLogDao.insertAll(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 1,
                    weight = 100f,
                    reps = 10
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 2,
                    weight = 100f,
                    reps = 9
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 3,
                    weight = 100f,
                    reps = 8
                )
            )
        )

        setLogDao.deleteAllForSessionExercise(sessionExerciseId)

        val sets = setLogDao.getSetsForExercise(sessionExerciseId).first()
        assertTrue(sets.isEmpty())
    }

    // ===== STATS TESTS =====

    @Test
    fun `getSetCount ritorna numero corretto di set`() = runTest {
        val sessionExerciseId = createTestSetup()

        setLogDao.insertAll(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 1,
                    weight = 100f,
                    reps = 10
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 2,
                    weight = 100f,
                    reps = 9
                )
            )
        )

        val count = setLogDao.getSetCount(sessionExerciseId)
        assertEquals(2, count)
    }

    @Test
    fun `getCompletedSetCount conta solo set completati`() = runTest {
        val sessionExerciseId = createTestSetup()

        setLogDao.insertAll(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 1,
                    weight = 100f,
                    reps = 10,
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 2,
                    weight = 100f,
                    reps = 9,
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 3,
                    weight = 100f,
                    reps = 8,
                    completed = false // Non completato
                )
            )
        )

        val completedCount = setLogDao.getCompletedSetCount(sessionExerciseId)
        assertEquals(2, completedCount)
    }

    @Test
    fun `getTotalVolume calcola somma corretta`() = runTest {
        val sessionExerciseId = createTestSetup()

        setLogDao.insertAll(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 1,
                    weight = 100f,
                    reps = 10, // 1000kg
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 2,
                    weight = 100f,
                    reps = 9,  // 900kg
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 3,
                    weight = 100f,
                    reps = 8,  // 800kg (ma non completato)
                    completed = false
                )
            )
        )

        val volume = setLogDao.getTotalVolume(sessionExerciseId)

        // Solo i primi 2 set: 1000 + 900 = 1900
        assertEquals(1900f, volume!!, 0.01f)
    }

    @Test
    fun `getAverageRPE calcola media correttamente`() = runTest {
        val sessionExerciseId = createTestSetup()

        setLogDao.insertAll(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 1,
                    weight = 100f,
                    reps = 10,
                    rpe = 8f,
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 2,
                    weight = 100f,
                    reps = 9,
                    rpe = 9f,
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 3,
                    weight = 100f,
                    reps = 8,
                    rpe = null, // Senza RPE, non conta
                    completed = true
                )
            )
        )

        val avgRPE = setLogDao.getAverageRPE(sessionExerciseId)

        // (8 + 9) / 2 = 8.5
        assertEquals(8.5f, avgRPE!!, 0.01f)
    }

    @Test
    fun `getMaxWeight ritorna peso massimo`() = runTest {
        val sessionExerciseId = createTestSetup()

        setLogDao.insertAll(
            listOf(
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 1,
                    weight = 100f,
                    reps = 10,
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 2,
                    weight = 105f, // MAX
                    reps = 8,
                    completed = true
                ),
                SetLogEntity(
                    sessionExerciseId = sessionExerciseId,
                    setNumber = 3,
                    weight = 95f,
                    reps = 12,
                    completed = true
                )
            )
        )

        val maxWeight = setLogDao.getMaxWeight(sessionExerciseId)
        assertEquals(105f, maxWeight!!, 0.01f)
    }

    // ===== EDGE CASES =====

    @Test
    fun `getSetsForExercise ritorna lista vuota se nessun set`() = runTest {
        val sessionExerciseId = createTestSetup()

        val sets = setLogDao.getSetsForExercise(sessionExerciseId).first()
        assertTrue(sets.isEmpty())
    }

    @Test
    fun `getTotalVolume ritorna null se nessun set completato`() = runTest {
        val sessionExerciseId = createTestSetup()

        val volume = setLogDao.getTotalVolume(sessionExerciseId)
        assertNull(volume)
    }

    @Test
    fun `getAverageRPE ritorna null se nessun set con RPE`() = runTest {
        val sessionExerciseId = createTestSetup()

        setLogDao.insert(
            SetLogEntity(
                sessionExerciseId = sessionExerciseId,
                setNumber = 1,
                weight = 100f,
                reps = 10,
                rpe = null,
                completed = true
            )
        )

        val avgRPE = setLogDao.getAverageRPE(sessionExerciseId)
        assertNull(avgRPE)
    }
}