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

    val isValid: Boolean
        get() = weightKg in WEIGHT_RANGE && heightCm in HEIGHT_RANGE &&
            age in AGE_RANGE && daysPerWeek in DAYS_RANGE &&
            sessionMinutes in SESSION_MINUTES_RANGE && equipment.isNotEmpty()

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
    val workouts: List<DailyWorkout>,
    val isDeload: Boolean = false
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


val DailyWorkout.exerciseCount: Int
    get() = blocks.sumOf { it.exercises.size }

val DailyWorkout.estimatedDurationSeconds: Int
    get() {
        val exercises = blocks.flatMap { it.exercises }
        val total = exercises.sumOf {
            val work = it.seconds ?: ((it.reps ?: 0) * 3)
            maxOf(1, it.sets) * (work + it.restSeconds)
        }
        return maxOf(0, total - (exercises.lastOrNull()?.restSeconds ?: 0))
    }

val DailyWorkout.estimatedMinutes: Int
    get() = (estimatedDurationSeconds + 59) / 60

val TrainingPlan.isComplete: Boolean
    get() = weeks.isNotEmpty() && weeks.all { week ->
        week.workouts.isNotEmpty() && week.workouts.all { it.completed }
    }


/** Preserve all week metadata, including deload, while updating completion. */
fun TrainingPlan.toggleWorkout(weekIndex: Int, dayIndex: Int, completed: Boolean? = null): TrainingPlan =
    copy(weeks = weeks.map { week ->
        if (week.weekIndex != weekIndex) week else week.copy(workouts = week.workouts.map { day ->
            if (day.dayIndex != dayIndex) day else day.copy(completed = completed ?: !day.completed)
        })
    })
