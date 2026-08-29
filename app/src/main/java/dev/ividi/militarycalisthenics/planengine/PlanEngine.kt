package dev.ividi.militarycalisthenics.planengine

import dev.ividi.militarycalisthenics.model.*
import kotlin.math.roundToInt

/**
 * Generates a periodized calisthenics plan from a user profile.
 * Volume/intensity is calibrated from level, goal, age and BMI signal;
 * BMI never gates or diagnoses, it only nudges starting volume.
 */
object PlanEngine {

    private const val WEEK_COUNT = 6

    fun generate(profile: UserProfile): TrainingPlan {
        val baseMultiplier = levelMultiplier(profile.level) * ageMultiplier(profile.age) * bmiMultiplier(profile.bmi) * sexMultiplier(profile.sex)
        val weeks = (0 until WEEK_COUNT).map { weekIndex ->
            val progression = 1.0 + weekIndex * 0.08
            WeeklyPlan(
                weekIndex = weekIndex,
                workouts = buildWeekWorkouts(profile, baseMultiplier * progression, weekIndex)
            )
        }
        return TrainingPlan(profile = profile, weeks = weeks)
    }

    private fun levelMultiplier(level: FitnessLevel) = when (level) {
        FitnessLevel.BEGINNER -> 0.7
        FitnessLevel.INTERMEDIATE -> 1.0
        FitnessLevel.ADVANCED -> 1.3
    }

    private fun ageMultiplier(age: Int) = when {
        age < 25 -> 1.1
        age < 40 -> 1.0
        age < 55 -> 0.85
        else -> 0.7
    }

    private fun bmiMultiplier(bmi: Double) = when {
        bmi < 18.5 -> 0.9
        bmi < 25.0 -> 1.0
        bmi < 30.0 -> 0.9
        else -> 0.75
    }

    /** Average upper-body strength/endurance calibration by sex, per docs/plan-engine-spec.md. */
    private fun sexMultiplier(sex: Sex) = when (sex) {
        Sex.MALE -> 1.0
        Sex.FEMALE -> 0.9
        Sex.UNSPECIFIED -> 1.0
    }

    private fun buildWeekWorkouts(profile: UserProfile, multiplier: Double, weekIndex: Int): List<DailyWorkout> {
        val budget = sessionBudget(profile.sessionMinutes)
        return (0 until profile.daysPerWeek).map { dayIndex ->
            DailyWorkout(
                dayIndex = dayIndex,
                title = dayTitle(profile.goal, dayIndex),
                blocks = listOf(
                    warmUpBlock(),
                    strengthBlock(profile, multiplier, dayIndex, weekIndex, budget.strengthSeconds),
                    circuitBlock(profile, multiplier, weekIndex, budget.circuitSeconds),
                    coreBlock(multiplier, budget.coreSeconds),
                    coolDownBlock()
                )
            )
        }
    }

    /**
     * Splits the time left after warm-up/cool-down across the variable
     * blocks, per docs/plan-engine-spec.md "Session duration budget".
     */
    private data class SessionBudget(val strengthSeconds: Int, val coreSeconds: Int, val circuitSeconds: Int)

    private fun sessionBudget(sessionMinutes: Int): SessionBudget {
        val warmupSeconds = 180
        val cooldownSeconds = 120
        val total = (sessionMinutes * 60 - warmupSeconds - cooldownSeconds).coerceAtLeast(0)
        return SessionBudget(
            strengthSeconds = (total * 0.5).toInt(),
            coreSeconds = (total * 0.2).toInt(),
            circuitSeconds = (total * 0.3).toInt()
        )
    }

    /** Largest prefix of `exercises` whose estimated total time fits `budgetSeconds`. */
    private fun trimToBudget(exercises: List<ExerciseSet>, budgetSeconds: Int): List<ExerciseSet> {
        if (exercises.isEmpty()) return exercises
        val avgSeconds = exercises.map { ex ->
            val work = ex.reps?.let { it * 3 } ?: (ex.seconds ?: 0)
            ex.sets * (work + ex.restSeconds)
        }.average()
        if (avgSeconds <= 0) return exercises
        val count = (budgetSeconds / avgSeconds).toInt().coerceIn(1, exercises.size)
        return exercises.take(count)
    }

    private fun dayTitle(goal: Goal, dayIndex: Int): String {
        val focuses = when (goal) {
            Goal.FAT_LOSS -> listOf("Full Body Burn", "Metabolic Circuit", "Core & Cardio", "Total Conditioning", "Endurance Push", "Active Recovery")
            Goal.STRENGTH_MASS -> listOf("Push Strength", "Pull Strength", "Legs & Core", "Upper Power", "Full Body Strength", "Grip & Core")
            Goal.MILITARY_ENDURANCE -> listOf("Selection Prep", "Ruck & Core", "Speed Endurance", "Combat Circuit", "Max Reps Test", "Recovery Mobility")
            Goal.MOBILITY -> listOf("Mobility Flow", "Control & Balance", "Light Strength", "Joint Health", "Full Body Flow", "Active Stretch")
        }
        return focuses[dayIndex % focuses.size]
    }

    /**
     * Which movement patterns a day's strength block should draw from. Only
     * STRENGTH_MASS's day titles (see [dayTitle]) actually promise a specific
     * focus ("Push Strength", "Legs & Core", ...) — this is what makes that
     * promise real instead of the title being purely cosmetic. Other goals'
     * titles are generic conditioning themes, so their strength block stays
     * balanced across all patterns.
     */
    private fun dayPatternFocus(goal: Goal, dayIndex: Int): Set<MovementPattern> {
        if (goal != Goal.STRENGTH_MASS) return setOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.LEGS)
        return when (dayIndex % 6) {
            0 -> setOf(MovementPattern.PUSH) // Push Strength
            1 -> setOf(MovementPattern.PULL) // Pull Strength
            2 -> setOf(MovementPattern.LEGS) // Legs & Core
            3 -> setOf(MovementPattern.PUSH, MovementPattern.PULL) // Upper Power
            4 -> setOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.LEGS) // Full Body Strength
            else -> setOf(MovementPattern.PULL) // Grip & Core
        }
    }

    private fun warmUpBlock() = TrainingBlock(
        type = BlockType.WARM_UP,
        exercises = listOf(
            ExerciseSet("Jumping Jacks", seconds = 60, sets = 1, restSeconds = 15),
            ExerciseSet("Arm Circles", seconds = 30, sets = 1, restSeconds = 15),
            ExerciseSet("Bodyweight Squats", reps = 15, sets = 1, restSeconds = 15),
            ExerciseSet("Hip Openers", seconds = 30, sets = 1, restSeconds = 15)
        )
    )

    /** A strength-pool candidate, before scaling — carries the pattern used for day filtering. */
    private data class StrengthCandidate(val name: String, val pattern: MovementPattern, val baseReps: Int)

    private fun strengthPool(profile: UserProfile): List<StrengthCandidate> {
        val hasBar = Equipment.PULL_UP_BAR in profile.equipment
        val hasParallettes = Equipment.PARALLETTES in profile.equipment

        val push = mutableListOf(
            StrengthCandidate("Push-ups", MovementPattern.PUSH, 12),
            StrengthCandidate("Wide Push-ups", MovementPattern.PUSH, 10),
            StrengthCandidate("Pike Push-ups", MovementPattern.PUSH, 8)
        )
        push += if (hasParallettes) {
            StrengthCandidate("Parallel Bar Dips", MovementPattern.PUSH, 8)
        } else {
            StrengthCandidate("Bench Dips", MovementPattern.PUSH, 10)
        }

        val pull = if (hasBar) {
            listOf(
                StrengthCandidate("Pull-ups", MovementPattern.PULL, 6),
                StrengthCandidate("Chin-ups", MovementPattern.PULL, 6)
            )
        } else {
            listOf(StrengthCandidate("Inverted Rows / Table Rows", MovementPattern.PULL, 10))
        }

        val legs = listOf(
            StrengthCandidate("Bodyweight Squats", MovementPattern.LEGS, 18),
            StrengthCandidate("Lunges", MovementPattern.LEGS, 12),
            StrengthCandidate("Glute Bridges", MovementPattern.LEGS, 15)
        )

        return push + pull + legs
    }

    private fun strengthBlock(profile: UserProfile, multiplier: Double, dayIndex: Int, weekIndex: Int, budgetSeconds: Int): TrainingBlock {
        val sets = scaleSets(3, multiplier)
        val restSeconds = if (profile.goal == Goal.STRENGTH_MASS) 75 else 45

        val allowedPatterns = dayPatternFocus(profile.goal, dayIndex)
        val fullPool = strengthPool(profile)
        val filtered = fullPool.filter { it.pattern in allowedPatterns }.ifEmpty { fullPool }
        val rotated = rotate(filtered, dayIndex + weekIndex)

        val exercises = rotated.map { candidate ->
            ExerciseSet(candidate.name, reps = scale(candidate.baseReps, multiplier), sets = sets, restSeconds = restSeconds)
        }

        return TrainingBlock(type = BlockType.STRENGTH, exercises = trimToBudget(exercises, budgetSeconds))
    }

    /** Rotates a list by `offset` positions, wrapping around — used so the same day/week doesn't always start on the same exercise. */
    private fun <T> rotate(list: List<T>, offset: Int): List<T> {
        if (list.isEmpty()) return list
        val shift = ((offset % list.size) + list.size) % list.size
        return list.subList(shift, list.size) + list.subList(0, shift)
    }

    private fun circuitBlock(profile: UserProfile, multiplier: Double, weekIndex: Int, budgetSeconds: Int): TrainingBlock {
        val rounds = scaleSets(3, multiplier)
        val restSeconds = if (profile.goal == Goal.FAT_LOSS) 30 else 40
        val exercises = when (profile.goal) {
            Goal.MILITARY_ENDURANCE -> listOf(
                ExerciseSet("Burpees", reps = scale(10, multiplier), sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Mountain Climbers", seconds = 40, sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Sprint Intervals", seconds = 30, sets = rounds, restSeconds = restSeconds),
                ExerciseSet("High Knees", seconds = 30, sets = rounds, restSeconds = restSeconds)
            )
            Goal.FAT_LOSS -> listOf(
                ExerciseSet("Burpees", reps = scale(8, multiplier), sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Jump Squats", reps = scale(12, multiplier), sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Mountain Climbers", seconds = 40, sets = rounds, restSeconds = restSeconds)
            )
            Goal.STRENGTH_MASS -> listOf(
                ExerciseSet("Pike Push-ups", reps = scale(8, multiplier), sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Explosive Push-ups", reps = scale(6, multiplier), sets = rounds, restSeconds = restSeconds)
            )
            Goal.MOBILITY -> listOf(
                ExerciseSet("World's Greatest Stretch", reps = 6, sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Cat-Cow", reps = 10, sets = rounds, restSeconds = restSeconds),
                ExerciseSet("Bodyweight Good Mornings", reps = 10, sets = rounds, restSeconds = restSeconds)
            )
        }
        return TrainingBlock(type = BlockType.CIRCUIT, exercises = trimToBudget(exercises, budgetSeconds))
    }

    private fun coreBlock(multiplier: Double, budgetSeconds: Int): TrainingBlock {
        val exercises = listOf(
            ExerciseSet("Plank", seconds = scale(45, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Leg Raises", reps = scale(12, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Russian Twists", reps = scale(20, multiplier), sets = 3, restSeconds = 20)
        )
        return TrainingBlock(type = BlockType.CORE, exercises = trimToBudget(exercises, budgetSeconds))
    }

    private fun coolDownBlock() = TrainingBlock(
        type = BlockType.COOL_DOWN,
        exercises = listOf(
            ExerciseSet("Deep Breathing", seconds = 60, sets = 1, restSeconds = 10),
            ExerciseSet("Hamstring Stretch", seconds = 30, sets = 1, restSeconds = 10),
            ExerciseSet("Shoulder Stretch", seconds = 30, sets = 1, restSeconds = 10)
        )
    )

    private fun scale(base: Int, multiplier: Double): Int =
        (base * multiplier).roundToInt().coerceAtLeast(1)

    private fun scaleSets(base: Int, multiplier: Double): Int =
        (base * multiplier).roundToInt().coerceIn(2, 5)
}
