package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Rappresenta un singolo set all'interno di un TemplateExercise.
 * Permette set eterogenei (piramidale, dropset, etc.)
 */
@Entity(
    tableName = "template_sets",
    foreignKeys = [
        ForeignKey(
            entity = TemplateExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["templateExerciseId"])
    ]
)
data class TemplateSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val templateExerciseId: Long,

    val setNumber: Int,

    val targetRepsMin: Int = 8,

    val targetRepsMax: Int = 12,

    val targetWeight: Float? = null,

    val targetRPE: Float? = null,

    val restSeconds: Int? = null,

    val notes: String? = null
)

fun TemplateSetEntity.getRepRangeDisplay(): String {
    return if (targetRepsMin == targetRepsMax) {
        "$targetRepsMin"
    } else {
        "$targetRepsMin-$targetRepsMax"
    }
}

fun TemplateSetEntity.getWeightDisplay(): String {
    return targetWeight?.let { "${it}kg" } ?: "—"
}

fun TemplateSetEntity.getRPEDisplay(): String {
    return targetRPE?.let { "@${it}" } ?: ""
}

fun TemplateSetEntity.getFullDisplay(): String {
    return buildString {
        append(getRepRangeDisplay())
        if (targetWeight != null) {
            append(" × ${targetWeight}kg")
        }
        if (targetRPE != null) {
            append(" @${targetRPE}")
        }
    }
}