package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.Equipment
import com.example.neokratos.data.local.MuscleGroup

/**
 * Entity per la libreria globale degli esercizi.
 *
 * Questa tabella contiene TUTTI gli esercizi disponibili:
 * - Esercizi importati da wger.de (isCustom = false)
 * - Esercizi creati dall'utente (isCustom = true)
 *
 * È la "single source of truth" per gli esercizi:
 * - I template fanno riferimento a questa tabella (via exerciseId)
 * - Le sessioni workout fanno riferimento a questa tabella (via exerciseId)
 * - Gli analytics aggregano dati per exerciseId
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // ===== BASIC INFO =====

    /**
     * Nome dell'esercizio
     * Esempi: "Bench Press", "Squat", "Deadlift"
     */
    val name: String,

    /**
     * Descrizione tecnica dell'esercizio (opzionale)
     * Viene popolata da wger o inserita dall'utente
     */
    val description: String? = null,

    // ===== CLASSIFICATION =====

    /**
     * Categoria macro per filtri veloci
     * Esempio: LEGS, CHEST, BACK
     */
    val category: ExerciseCategory,

    /**
     * Gruppo muscolare primario allenato
     * Esempio: QUADRICEPS per lo squat
     */
    val primaryMuscleGroup: MuscleGroup,

    /**
     * Gruppi muscolari secondari coinvolti (opzionale)
     * Esempio: per bench press → [DELTS_ANTERIOR, TRICEPS]
     *
     * Lista vuota = nessun muscolo secondario
     */
    val secondaryMuscleGroups: List<MuscleGroup> = emptyList(),

    // ===== EQUIPMENT =====

    /**
     * Attrezzatura necessaria per l'esercizio
     * Esempio: BARBELL, DUMBBELL, BODYWEIGHT
     */
    val equipment: Equipment,

    // ===== SOURCE TRACKING =====

    /**
     * Flag per distinguere esercizi custom da quelli importati
     * - true = creato dall'utente manualmente
     * - false = importato da wger.de
     */
    val isCustom: Boolean = false,

    /**
     * ID originale da wger.de (null per esercizi custom)
     * Serve per:
     * - Evitare duplicati durante import
     * - Future sync/update da wger
     */
    val wgerId: Int? = null,

    // ===== METADATA =====

    /**
     * Timestamp creazione (millisecondi da epoch)
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Flag preferito (per lista "frequently used")
     */
    val isFavorite: Boolean = false,

    /**
     * Contatore utilizzi (incrementato ogni volta che usi l'esercizio)
     * Serve per:
     * - Lista "most used exercises"
     * - Ordinamento suggerimenti
     */
    val usageCount: Int = 0
)

/**
 * Extension functions per utility comuni
 */

/**
 * Ritorna tutti i muscle groups coinvolti (primary + secondary)
 * Utile per muscle balance analytics
 */
fun ExerciseEntity.getAllMuscleGroups(): List<MuscleGroup> {
    return listOf(primaryMuscleGroup) + secondaryMuscleGroups
}

/**
 * Verifica se l'esercizio allena un muscolo specifico
 */
fun ExerciseEntity.trainsMuscle(muscle: MuscleGroup): Boolean {
    return primaryMuscleGroup == muscle || secondaryMuscleGroups.contains(muscle)
}

/**
 * Ritorna una stringa human-readable per i muscle groups
 * Esempio: "Quadriceps (Glutes, Hamstrings)"
 */
fun ExerciseEntity.getMuscleGroupsDisplay(): String {
    val primary = primaryMuscleGroup.name.lowercase().replace("_", " ")
    val secondary = secondaryMuscleGroups
        .joinToString(", ") { it.name.lowercase().replace("_", " ") }

    return if (secondary.isNotEmpty()) {
        "$primary ($secondary)"
    } else {
        primary
    }
}