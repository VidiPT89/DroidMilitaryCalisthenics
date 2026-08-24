package dev.ividi.militarycalisthenics.model

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { MALE, FEMALE, UNSPECIFIED }

@Serializable
enum class FitnessLevel { BEGINNER, INTERMEDIATE, ADVANCED }

@Serializable
enum class Goal { FAT_LOSS, STRENGTH_MASS, MILITARY_ENDURANCE, MOBILITY }

@Serializable
enum class Equipment { BODYWEIGHT_ONLY, PULL_UP_BAR, PARALLETTES }

@Serializable
data class UserProfile(
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val sex: Sex = Sex.UNSPECIFIED,
    val level: FitnessLevel,
    val goal: Goal,
    val daysPerWeek: Int,
    val equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT_ONLY)
) {
    val bmi: Double
        get() {
            val heightM = heightCm / 100.0
            return weightKg / (heightM * heightM)
        }

    companion object {
        val WEIGHT_RANGE = 30.0..250.0
        val HEIGHT_RANGE = 120.0..230.0
        val AGE_RANGE = 14..75
        val DAYS_RANGE = 3..6
    }
}

@Serializable
enum class BlockType { WARM_UP, STRENGTH, CIRCUIT, CORE, COOL_DOWN }

@Serializable
data class ExerciseSet(
    val name: String,
    val reps: Int? = null,
    val seconds: Int? = null,
    val sets: Int
)

@Serializable
data class TrainingBlock(
    val type: BlockType,
    val exercises: List<ExerciseSet>
)

@Serializable
data class DailyWorkout(
    val dayIndex: Int,
    val title: String,
    val blocks: List<TrainingBlock>,
    val completed: Boolean = false
)

@Serializable
data class WeeklyPlan(
    val weekIndex: Int,
    val workouts: List<DailyWorkout>
)

@Serializable
data class TrainingPlan(
    val profile: UserProfile,
    val weeks: List<WeeklyPlan>
)
