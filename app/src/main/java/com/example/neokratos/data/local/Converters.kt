package com.example.neokratos.data.local

import androidx.room.TypeConverter

// ============================================
// ENUMS - Categorie fisse type-safe
// ============================================

/**
 * Categorie macro per gli esercizi.
 * Usate per filtri veloci tipo "mostra solo esercizi gambe"
 */
enum class ExerciseCategory {
    ARMS,       // Bicipiti, tricipiti, avambracci
    LEGS,       // Quadricipiti, femorali, polpacci
    CHEST,      // Pettorali
    BACK,       // Dorsali, trapezi, lombari
    SHOULDERS,  // Deltoidi
    CORE,       // Addominali, obliqui
    CARDIO,     // Corsa, bike, rowing
    OTHER;      // Esercizi misti o non classificabili

    companion object {
        fun fromString(value: String): ExerciseCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}

/**
 * Gruppi muscolari specifici.
 * Più granulare di Category, per analytics precise (muscle balance)
 */
enum class MuscleGroup {
    // Arms
    BICEPS, TRICEPS, FOREARMS,

    // Legs
    QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, ADDUCTORS,

    // Chest
    PECTORALS_UPPER, PECTORALS_MIDDLE, PECTORALS_LOWER,

    // Back
    LATS, TRAPS, RHOMBOIDS, LOWER_BACK, ERECTOR_SPINAE,

    // Shoulders
    DELTS_ANTERIOR, DELTS_LATERAL, DELTS_POSTERIOR,

    // Core
    ABS, OBLIQUES, SERRATUS,

    // Other
    FULL_BODY, CARDIOVASCULAR, OTHER;

    companion object {
        fun fromString(value: String): MuscleGroup {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}

/**
 * Equipment necessario per l'esercizio
 */
enum class Equipment {
    BARBELL,        // Bilanciere
    DUMBBELL,       // Manubri
    KETTLEBELL,     // Kettlebell
    MACHINE,        // Macchina guidata
    CABLE,          // Cavi
    BODYWEIGHT,     // Corpo libero
    BANDS,          // Elastici
    OTHER;          // Altro

    companion object {
        fun fromString(value: String): Equipment {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: OTHER
        }
    }
}

// ============================================
// TYPE CONVERTERS
// ============================================

/**
 * Converters per Room Database.
 * Room non sa gestire Enum e List nativamente, quindi gli diciamo come convertirli.
 *
 * @TypeConverter = annotazione che dice a Room "usa questa funzione per convertire"
 */
class Converters {

    // ===== List<String> conversions =====

    /**
     * Converte List<String> → String per salvarla nel DB
     * Esempio: ["triceps", "shoulders"] → "triceps,shoulders"
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    /**
     * Converte String → List<String> quando leggi dal DB
     * Esempio: "triceps,shoulders" → ["triceps", "shoulders"]
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { it.trim() }
    }

    // ===== ExerciseCategory conversions =====

    /**
     * Converte ExerciseCategory → String per salvarla nel DB
     * Esempio: ExerciseCategory.LEGS → "LEGS"
     */
    @TypeConverter
    fun fromExerciseCategory(category: ExerciseCategory): String {
        return category.name
    }

    /**
     * Converte String → ExerciseCategory quando leggi dal DB
     * Esempio: "LEGS" → ExerciseCategory.LEGS
     */
    @TypeConverter
    fun toExerciseCategory(value: String): ExerciseCategory {
        return ExerciseCategory.fromString(value)
    }

    // ===== MuscleGroup conversions =====

    @TypeConverter
    fun fromMuscleGroup(muscle: MuscleGroup): String {
        return muscle.name
    }

    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup {
        return MuscleGroup.fromString(value)
    }

    // ===== Equipment conversions =====

    @TypeConverter
    fun fromEquipment(equipment: Equipment): String {
        return equipment.name
    }

    @TypeConverter
    fun toEquipment(value: String): Equipment {
        return Equipment.fromString(value)
    }

    // ===== List<MuscleGroup> conversions =====
    // Per secondary muscle groups

    @TypeConverter
    fun fromMuscleGroupList(muscles: List<MuscleGroup>?): String? {
        return muscles?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toMuscleGroupList(value: String?): List<MuscleGroup>? {
        return value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map { MuscleGroup.fromString(it.trim()) }
    }
}