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
        return (0 until profile.daysPerWeek).map { dayIndex ->
            DailyWorkout(
                dayIndex = dayIndex,
                title = dayTitle(profile.goal, dayIndex),
                blocks = listOf(
                    warmUpBlock(),
                    strengthBlock(profile, multiplier, dayIndex),
                    circuitBlock(profile, multiplier, weekIndex),
                    coreBlock(multiplier),
                    coolDownBlock()
                )
            )
        }
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

    private fun warmUpBlock() = TrainingBlock(
        type = BlockType.WARM_UP,
        exercises = listOf(
            ExerciseSet("Jumping Jacks", seconds = 60, sets = 1),
            ExerciseSet("Arm Circles", seconds = 30, sets = 1),
            ExerciseSet("Bodyweight Squats", reps = 15, sets = 1),
            ExerciseSet("Hip Openers", seconds = 30, sets = 1)
        )
    )

    private fun strengthBlock(profile: UserProfile, multiplier: Double, dayIndex: Int): TrainingBlock {
        val hasBar = Equipment.PULL_UP_BAR in profile.equipment
        val hasParallettes = Equipment.PARALLETTES in profile.equipment
        val pushReps = scale(12, multiplier)
        val squatReps = scale(18, multiplier)
        val sets = scaleSets(3, multiplier)

        val exercises = mutableListOf(
            ExerciseSet("Push-ups", reps = pushReps, sets = sets),
            ExerciseSet("Bodyweight Squats", reps = squatReps, sets = sets)
        )
        if (hasBar) {
            exercises += ExerciseSet("Pull-ups", reps = scale(6, multiplier), sets = sets)
        } else {
            exercises += ExerciseSet("Inverted Rows / Table Rows", reps = scale(10, multiplier), sets = sets)
        }
        if (hasParallettes) {
            exercises += ExerciseSet("Parallel Bar Dips", reps = scale(8, multiplier), sets = sets)
        } else {
            exercises += ExerciseSet("Bench Dips", reps = scale(10, multiplier), sets = sets)
        }
        exercises += ExerciseSet("Lunges", reps = scale(12, multiplier), sets = sets)

        return TrainingBlock(type = BlockType.STRENGTH, exercises = exercises)
    }

    private fun circuitBlock(profile: UserProfile, multiplier: Double, weekIndex: Int): TrainingBlock {
        val rounds = scaleSets(3, multiplier)
        val exercises = when (profile.goal) {
            Goal.MILITARY_ENDURANCE -> listOf(
                ExerciseSet("Burpees", reps = scale(10, multiplier), sets = rounds),
                ExerciseSet("Mountain Climbers", seconds = 40, sets = rounds),
                ExerciseSet("Sprint Intervals", seconds = 30, sets = rounds),
                ExerciseSet("High Knees", seconds = 30, sets = rounds)
            )
            Goal.FAT_LOSS -> listOf(
                ExerciseSet("Burpees", reps = scale(8, multiplier), sets = rounds),
                ExerciseSet("Jump Squats", reps = scale(12, multiplier), sets = rounds),
                ExerciseSet("Mountain Climbers", seconds = 40, sets = rounds)
            )
            Goal.STRENGTH_MASS -> listOf(
                ExerciseSet("Pike Push-ups", reps = scale(8, multiplier), sets = rounds),
                ExerciseSet("Explosive Push-ups", reps = scale(6, multiplier), sets = rounds)
            )
            Goal.MOBILITY -> listOf(
                ExerciseSet("World's Greatest Stretch", reps = 6, sets = rounds),
                ExerciseSet("Cat-Cow", reps = 10, sets = rounds),
                ExerciseSet("Bodyweight Good Mornings", reps = 10, sets = rounds)
            )
        }
        return TrainingBlock(type = BlockType.CIRCUIT, exercises = exercises)
    }

    private fun coreBlock(multiplier: Double) = TrainingBlock(
        type = BlockType.CORE,
        exercises = listOf(
            ExerciseSet("Plank", seconds = scale(45, multiplier), sets = 3),
            ExerciseSet("Leg Raises", reps = scale(12, multiplier), sets = 3),
            ExerciseSet("Russian Twists", reps = scale(20, multiplier), sets = 3)
        )
    )

    private fun coolDownBlock() = TrainingBlock(
        type = BlockType.COOL_DOWN,
        exercises = listOf(
            ExerciseSet("Deep Breathing", seconds = 60, sets = 1),
            ExerciseSet("Hamstring Stretch", seconds = 30, sets = 1),
            ExerciseSet("Shoulder Stretch", seconds = 30, sets = 1)
        )
    )

    private fun scale(base: Int, multiplier: Double): Int =
        (base * multiplier).roundToInt().coerceAtLeast(1)

    private fun scaleSets(base: Int, multiplier: Double): Int =
        (base * multiplier).roundToInt().coerceIn(2, 5)
}
