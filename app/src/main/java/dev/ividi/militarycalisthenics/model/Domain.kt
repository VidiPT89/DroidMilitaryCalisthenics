package dev.ividi.militarycalisthenics.model

import kotlinx.serialization.Serializable

@Serializable
enum class Sex { MALE, FEMALE, UNSPECIFIED }

@Serializable
enum class FitnessLevel { BEGINNER, INTERMEDIATE, ADVANCED }

/** Total weeks in a generated plan for this level — matches iOS's `FitnessLevel.progressionWeeks`. */
val FitnessLevel.progressionWeeks: Int
    get() = when (this) {
        FitnessLevel.BEGINNER -> 4
        FitnessLevel.INTERMEDIATE -> 6
        FitnessLevel.ADVANCED -> 8
    }

/** The level reached after finishing this one's full plan, or null once already at ADVANCED. */
val FitnessLevel.next: FitnessLevel?
    get() = when (this) {
        FitnessLevel.BEGINNER -> FitnessLevel.INTERMEDIATE
        FitnessLevel.INTERMEDIATE -> FitnessLevel.ADVANCED
        FitnessLevel.ADVANCED -> null
    }

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
    val equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT_ONLY),
    val sessionMinutes: Int = 30
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
        val SESSION_MINUTES_RANGE = 15..60
    }
}

@Serializable
enum class BlockType { WARM_UP, STRENGTH, CIRCUIT, CORE, COOL_DOWN }

/**
 * Movement pattern a strength exercise trains — used to make exercise
 * selection respect the day's own focus (e.g. a "Push" day shouldn't be
 * filled with squats).
 */
enum class MovementPattern { PUSH, PULL, LEGS }

@Serializable
data class ExerciseSet(
    val name: String,
    val reps: Int? = null,
    val seconds: Int? = null,
    val sets: Int,
    val restSeconds: Int = 30
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

@Serializable
data class WeightEntry(
    val timestampMillis: Long,
    val weightKg: Double
)
