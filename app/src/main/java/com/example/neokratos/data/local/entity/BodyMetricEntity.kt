package com.example.neokratos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for tracking body metrics over time.
 *
 * Tracks:
 * - Bodyweight
 * - Body measurements (chest, waist, arms, etc.)
 * - Progress photos (URI only, images stored externally)
 *
 * Use cases:
 * - Weight tracking graphs
 * - Body composition progress
 * - Before/after comparisons
 * - BMI calculation
 *
 * Concepts:
 * - Time-series data (one measurement per timestamp)
 * - Multiple metric types in same entity
 * - External file references (photo URIs)
 */
@Entity(
    tableName = "body_metrics",
    indices = [
        Index(value = ["timestamp"]), // For time-based queries
        Index(value = ["type"])        // For filtering by metric type
    ]
)
data class BodyMetricEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Type of metric being tracked.
     */
    val type: BodyMetricType,

    /**
     * Numeric value of the metric.
     *
     * Examples:
     * - WEIGHT: 75.5 (kg)
     * - WAIST: 85.0 (cm)
     * - CHEST: 105.0 (cm)
     *
     * Nullable for PHOTO type (no numeric value).
     */
    val value: Float? = null,

    /**
     * Unit of measurement.
     * "kg" for weight, "cm" for measurements, null for photos.
     */
    val unit: String? = null,

    /**
     * Timestamp of measurement (milliseconds since epoch).
     * Used for:
     * - Chronological ordering
     * - Time-series graphs
     * - Progress tracking
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Optional notes for this measurement.
     * Examples:
     * - "After morning workout"
     * - "Before breakfast"
     * - "Start of cut"
     */
    val notes: String? = null,

    /**
     * Photo URI for progress photos.
     * Only used when type = PHOTO.
     *
     * Examples:
     * - "content://media/external/images/media/12345"
     * - File URI from camera or gallery
     *
     * NOTE: Actual image stored externally, we just keep reference.
     */
    val photoUri: String? = null
)

/**
 * Types of body metrics we can track.
 */
enum class BodyMetricType {
    // Weight
    WEIGHT,

    // Body measurements
    NECK,
    CHEST,
    WAIST,
    HIPS,
    BICEP_LEFT,
    BICEP_RIGHT,
    FOREARM_LEFT,
    FOREARM_RIGHT,
    THIGH_LEFT,
    THIGH_RIGHT,
    CALF_LEFT,
    CALF_RIGHT,
    SHOULDERS,

    // Progress photos
    PHOTO_FRONT,
    PHOTO_SIDE,
    PHOTO_BACK;

    companion object {
        /**
         * Check if this type is a photo type.
         */
        fun isPhotoType(type: BodyMetricType): Boolean {
            return type in listOf(PHOTO_FRONT, PHOTO_SIDE, PHOTO_BACK)
        }

        /**
         * Get display name for UI.
         */
        fun getDisplayName(type: BodyMetricType): String {
            return when (type) {
                WEIGHT -> "Weight"
                NECK -> "Neck"
                CHEST -> "Chest"
                WAIST -> "Waist"
                HIPS -> "Hips"
                BICEP_LEFT -> "Bicep (L)"
                BICEP_RIGHT -> "Bicep (R)"
                FOREARM_LEFT -> "Forearm (L)"
                FOREARM_RIGHT -> "Forearm (R)"
                THIGH_LEFT -> "Thigh (L)"
                THIGH_RIGHT -> "Thigh (R)"
                CALF_LEFT -> "Calf (L)"
                CALF_RIGHT -> "Calf (R)"
                SHOULDERS -> "Shoulders"
                PHOTO_FRONT -> "Photo (Front)"
                PHOTO_SIDE -> "Photo (Side)"
                PHOTO_BACK -> "Photo (Back)"
            }
        }
    }
}

/**
 * Extension functions for convenience.
 */

/**
 * Check if this is a photo metric.
 */
fun BodyMetricEntity.isPhoto(): Boolean {
    return BodyMetricType.isPhotoType(type)
}

/**
 * Get display value with unit.
 * Example: "75.5 kg", "85 cm"
 */
fun BodyMetricEntity.getDisplayValue(): String {
    return if (value != null && unit != null) {
        "${String.format("%.1f", value)} $unit"
    } else if (isPhoto()) {
        "Photo"
    } else {
        "N/A"
    }
}

/**
 * Get formatted date.
 */
fun BodyMetricEntity.getFormattedDate(): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * Calculate BMI from weight and height.
 *
 * Formula: BMI = weight (kg) / (height (m))²
 *
 * NOTE: Height is not stored in BodyMetricEntity.
 * You need to pass it separately.
 */
fun calculateBMI(weightKg: Float, heightCm: Float): Float {
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

/**
 * Get BMI category.
 */
fun getBMICategory(bmi: Float): String {
    return when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }
}