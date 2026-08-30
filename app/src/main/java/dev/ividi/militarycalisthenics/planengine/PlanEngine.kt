package dev.ividi.militarycalisthenics.planengine

import dev.ividi.militarycalisthenics.model.*
import kotlin.math.roundToInt

/**
 * Generates a periodized calisthenics plan from a user profile.
 * Volume/intensity is calibrated from level, goal, age and BMI signal;
 * BMI never gates or diagnoses, it only nudges starting volume.
 */
object PlanEngine {

    fun generate(profile: UserProfile): TrainingPlan {
        val baseMultiplier = levelMultiplier(profile.level) * ageMultiplier(profile.age) * bmiMultiplier(profile.bmi) * sexMultiplier(profile.sex)
        val weeks = (0 until profile.level.progressionWeeks).map { weekIndex ->
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
        val labels = splitLabels(profile.daysPerWeek)
        return labels.mapIndexed { dayIndex, label ->
            val blocks = mutableListOf(warmUpBlock(), strengthBlock(profile, multiplier, dayIndex, weekIndex, label, budget.strengthSeconds))
            if (includeCircuit(profile.goal, label)) {
                blocks += circuitBlock(profile, multiplier, dayIndex, weekIndex, budget.circuitSeconds)
            }
            blocks += coreBlock(profile.goal, multiplier, dayIndex, weekIndex, budget.coreSeconds)
            blocks += coolDownBlock()
            DailyWorkout(dayIndex = dayIndex, title = label, blocks = blocks)
        }
    }

    /**
     * Splits days by a fixed, goal-independent pattern based on
     * `daysPerWeek` — matches iOS's `PlanEngine.splitLabels` exactly, so
     * both platforms use the same day-naming scheme and the same
     * movement-pattern filtering rule below, instead of Android's previous
     * per-goal title lists (Push Strength, Selection Prep, ...) that only
     * actually constrained exercise selection for the strengthMass goal.
     */
    private fun splitLabels(daysPerWeek: Int): List<String> = when (daysPerWeek) {
        3 -> listOf("Full Body I", "Full Body II", "Full Body III")
        4 -> listOf("Upper Body", "Lower Body", "Full Body", "Conditioning")
        5 -> listOf("Upper Body", "Lower Body", "Push", "Pull", "Conditioning")
        else -> listOf("Upper Body", "Lower Body", "Push", "Pull", "Conditioning", "Mobility")
    }

    /** Matches iOS's `PlanEngine.includeCircuit`. */
    private fun includeCircuit(goal: Goal, label: String): Boolean = when (goal) {
        Goal.FAT_LOSS, Goal.MILITARY_ENDURANCE -> true
        Goal.STRENGTH_MASS -> label == "Conditioning"
        Goal.MOBILITY -> false
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

    /**
     * Which movement patterns a day's strength block should draw from,
     * based on the day's own label — matches iOS's `PlanEngine.patterns(forLabel:)`.
     * This is what makes "Lower Body" actually mean lower body instead of
     * the label being purely cosmetic, for every goal (not just strengthMass).
     */
    private fun dayPatternFocus(label: String): Set<MovementPattern> = when (label) {
        "Upper Body" -> setOf(MovementPattern.PUSH, MovementPattern.PULL)
        "Lower Body" -> setOf(MovementPattern.LEGS)
        "Push" -> setOf(MovementPattern.PUSH)
        "Pull" -> setOf(MovementPattern.PULL)
        else -> setOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.LEGS)
    }

    private fun warmUpBlock() = TrainingBlock(
        type = BlockType.WARM_UP,
        exercises = listOf(
            ExerciseSet("Jumping Jacks", seconds = 60, sets = 1, restSeconds = 15),
            ExerciseSet("Arm Circles", seconds = 30, sets = 1, restSeconds = 15),
            ExerciseSet("Light Squats", reps = 15, sets = 1, restSeconds = 15),
            ExerciseSet("Hip Openers", seconds = 30, sets = 1, restSeconds = 15)
        )
    )

    /** A strength-pool candidate, before scaling — carries the pattern used for day filtering. */
    private data class StrengthCandidate(
        val name: String,
        val pattern: MovementPattern,
        val baseReps: Int,
        val minLevel: FitnessLevel = FitnessLevel.BEGINNER
    )

    private fun levelAllows(required: FitnessLevel, userLevel: FitnessLevel) = userLevel.ordinal >= required.ordinal

    private fun strengthPool(profile: UserProfile): List<StrengthCandidate> {
        val hasBar = Equipment.PULL_UP_BAR in profile.equipment
        val hasParallettes = Equipment.PARALLETTES in profile.equipment

        val push = mutableListOf(
            StrengthCandidate("Push-ups", MovementPattern.PUSH, 12),
            StrengthCandidate("Wide Push-ups", MovementPattern.PUSH, 10),
            StrengthCandidate("Pike Push-ups", MovementPattern.PUSH, 8, minLevel = FitnessLevel.INTERMEDIATE)
        )
        push += if (hasParallettes) {
            StrengthCandidate("Parallel Bar Dips", MovementPattern.PUSH, 8)
        } else {
            StrengthCandidate("Bench Dips", MovementPattern.PUSH, 10)
        }

        val pull = if (hasBar) {
            listOf(
                StrengthCandidate("Pull-ups", MovementPattern.PULL, 6, minLevel = FitnessLevel.INTERMEDIATE),
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

    private fun strengthBlock(profile: UserProfile, multiplier: Double, dayIndex: Int, weekIndex: Int, label: String, budgetSeconds: Int): TrainingBlock {
        val sets = scaleSets(3, multiplier)
        val restSeconds = if (profile.goal == Goal.STRENGTH_MASS) 75 else 45

        val allowedPatterns = dayPatternFocus(label)
        val fullPool = strengthPool(profile)
        val patternFiltered = fullPool.filter { it.pattern in allowedPatterns }.ifEmpty { fullPool }
        val levelFiltered = patternFiltered.filter { levelAllows(it.minLevel, profile.level) }.ifEmpty { patternFiltered }
        val rotated = rotate(levelFiltered, dayIndex + weekIndex)

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

    /** A circuit-pool candidate, before scaling. */
    private data class CircuitCandidate(
        val name: String,
        val baseReps: Int? = null,
        val baseSeconds: Int? = null,
        val minLevel: FitnessLevel = FitnessLevel.BEGINNER,
        val skipOverForty: Boolean = false
    )

    private fun circuitPool(goal: Goal): List<CircuitCandidate> = when (goal) {
        Goal.MILITARY_ENDURANCE -> listOf(
            CircuitCandidate("Burpees", baseReps = 10, skipOverForty = true),
            CircuitCandidate("Mountain Climbers", baseSeconds = 40),
            CircuitCandidate("Sprint Intervals", baseSeconds = 30),
            CircuitCandidate("High Knees", baseSeconds = 30),
            CircuitCandidate("Shuttle Runs", baseSeconds = 30),
            CircuitCandidate("Bear Crawl", baseSeconds = 30)
        )
        Goal.FAT_LOSS -> listOf(
            CircuitCandidate("Burpees", baseReps = 8, skipOverForty = true),
            CircuitCandidate("Jump Squats", baseReps = 12, skipOverForty = true),
            CircuitCandidate("Mountain Climbers", baseSeconds = 40),
            CircuitCandidate("High Knees", baseSeconds = 30),
            CircuitCandidate("Bear Crawl", baseSeconds = 30)
        )
        // Deliberately does not reuse "Pike Push-ups"/"Diamond Push-ups" —
        // they're already in strengthPool's push list, and sharing a name
        // between the strength and circuit pools let the same exercise
        // appear twice in one day's workout (e.g. on a "Push Strength" day).
        Goal.STRENGTH_MASS -> listOf(
            CircuitCandidate("Mountain Climbers", baseSeconds = 30),
            CircuitCandidate("Explosive Push-ups", baseReps = 6, minLevel = FitnessLevel.INTERMEDIATE),
            CircuitCandidate("Jump Squats", baseReps = 14, skipOverForty = true)
        )
        Goal.MOBILITY -> listOf(
            // "Cat-Cow" deliberately lives only in the Mobility goal's core
            // pool (see coreBlock's mobilityPool) — having it here too meant
            // it could appear twice in the same day's workout.
            CircuitCandidate("World's Greatest Stretch", baseReps = 6),
            CircuitCandidate("Bodyweight Good Mornings", baseReps = 10)
        )
    }

    private fun circuitBlock(profile: UserProfile, multiplier: Double, dayIndex: Int, weekIndex: Int, budgetSeconds: Int): TrainingBlock {
        val rounds = scaleSets(3, multiplier)
        val restSeconds = if (profile.goal == Goal.FAT_LOSS) 30 else 40
        val isOverForty = profile.age > 40

        val fullPool = circuitPool(profile.goal)
        val ageFiltered = fullPool.filter { !(it.skipOverForty && isOverForty) }.ifEmpty { fullPool }
        val levelFiltered = ageFiltered.filter { levelAllows(it.minLevel, profile.level) }.ifEmpty { ageFiltered }
        val rotated = rotate(levelFiltered, dayIndex + weekIndex)

        val exercises = rotated.map { candidate ->
            ExerciseSet(
                candidate.name,
                reps = candidate.baseReps?.let { scale(it, multiplier) },
                seconds = candidate.baseSeconds,
                sets = rounds,
                restSeconds = restSeconds
            )
        }
        return TrainingBlock(type = BlockType.CIRCUIT, exercises = trimToBudget(exercises, budgetSeconds))
    }

    private fun coreBlock(goal: Goal, multiplier: Double, dayIndex: Int, weekIndex: Int, budgetSeconds: Int): TrainingBlock {
        // A pool of just Plank/Leg Raises/Russian Twists, always in this
        // fixed order, used to mean Plank appeared in nearly every workout
        // (it always survived the budget trim since it came first). A wider
        // pool plus rotation spreads exercises out instead.
        val abPool = listOf(
            ExerciseSet("Plank", seconds = scale(45, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Side Plank", seconds = scale(30, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Mountain Climbers (core)", seconds = scale(35, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Russian Twists", reps = scale(20, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Leg Raises", reps = scale(12, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Bicycle Crunches", reps = scale(20, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Superman Hold", seconds = scale(20, multiplier), sets = 3, restSeconds = 20)
        )
        // On the Mobility goal, swap ab-focused core work for mobility
        // drills — matches iOS, which substitutes its core block the same
        // way for that goal instead of always training abs.
        // "Hip Openers" deliberately excluded here — it's already in every
        // day's fixed warm-up block (see warmUpBlock), so including it here
        // too would let it appear twice in the same Mobility-goal workout.
        val mobilityPool = listOf(
            ExerciseSet("Cat-Cow", reps = scale(10, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Shoulder Circles", seconds = scale(30, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Thoracic Rotations", seconds = scale(30, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Ankle Circles", seconds = scale(20, multiplier), sets = 3, restSeconds = 20),
            ExerciseSet("Deep Squat Hold", seconds = scale(30, multiplier), sets = 3, restSeconds = 20)
        )
        val pool = if (goal == Goal.MOBILITY) mobilityPool else abPool
        val rotated = rotate(pool, dayIndex + weekIndex)
        return TrainingBlock(type = BlockType.CORE, exercises = trimToBudget(rotated, budgetSeconds))
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
