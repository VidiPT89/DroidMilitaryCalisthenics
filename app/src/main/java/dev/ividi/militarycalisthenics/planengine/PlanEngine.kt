package dev.ividi.militarycalisthenics.planengine

import dev.ividi.militarycalisthenics.model.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Generates a periodized weekly calisthenics plan from a user profile.
 * Mirrors the shared algorithm in docs/plan-engine-spec.md and iOS's
 * `PlanEngine`/`ExerciseCatalog` exactly — same exercise catalog, same
 * intensity/scale formulas, same day-pattern filtering, same rotation.
 */
object PlanEngine {

    private data class CatalogExercise(
        val name: String,
        val block: BlockType,
        val baseReps: Int? = null,
        val baseSeconds: Int? = null,
        val minLevel: FitnessLevel = FitnessLevel.BEGINNER,
        val skipOverForty: Boolean = false,
        val requires: Equipment? = null,
        val pattern: MovementPattern? = null
    )

    private object Catalog {
        val warmup = listOf(
            CatalogExercise("Jumping Jacks", BlockType.WARM_UP, baseSeconds = 40),
            CatalogExercise("Arm Circles", BlockType.WARM_UP, baseSeconds = 30),
            CatalogExercise("Leg Swings", BlockType.WARM_UP, baseSeconds = 30),
            CatalogExercise("High Knees", BlockType.WARM_UP, baseSeconds = 30),
            CatalogExercise("Light Squats", BlockType.WARM_UP, baseReps = 12)
        )

        val strengthBodyweight = listOf(
            CatalogExercise("Push-ups", BlockType.STRENGTH, baseReps = 12, pattern = MovementPattern.PUSH),
            CatalogExercise("Wide Push-ups", BlockType.STRENGTH, baseReps = 10, pattern = MovementPattern.PUSH),
            CatalogExercise("Diamond Push-ups", BlockType.STRENGTH, baseReps = 8, minLevel = FitnessLevel.INTERMEDIATE, pattern = MovementPattern.PUSH),
            CatalogExercise("Pike Push-ups", BlockType.STRENGTH, baseReps = 8, minLevel = FitnessLevel.INTERMEDIATE, pattern = MovementPattern.PUSH),
            CatalogExercise("Squats", BlockType.STRENGTH, baseReps = 18, pattern = MovementPattern.LEGS),
            CatalogExercise("Lunges", BlockType.STRENGTH, baseReps = 12, pattern = MovementPattern.LEGS),
            CatalogExercise("Glute Bridges", BlockType.STRENGTH, baseReps = 15, pattern = MovementPattern.LEGS)
        )

        /** Bodyweight-only pull option (no bar needed) — without it, users on
         * BODYWEIGHT_ONLY equipment had zero pull-pattern exercises available. */
        val strengthPullFallback = listOf(
            CatalogExercise("Inverted Rows (table)", BlockType.STRENGTH, baseReps = 10, pattern = MovementPattern.PULL)
        )

        val strengthPullBar = listOf(
            CatalogExercise("Pull-ups", BlockType.STRENGTH, baseReps = 6, minLevel = FitnessLevel.INTERMEDIATE, requires = Equipment.PULL_UP_BAR, pattern = MovementPattern.PULL),
            CatalogExercise("Chin-ups", BlockType.STRENGTH, baseReps = 6, requires = Equipment.PULL_UP_BAR, pattern = MovementPattern.PULL),
            CatalogExercise("Negative Pull-ups", BlockType.STRENGTH, baseReps = 5, requires = Equipment.PULL_UP_BAR, pattern = MovementPattern.PULL),
            CatalogExercise("Hanging Leg Raises", BlockType.CORE, baseReps = 10, minLevel = FitnessLevel.INTERMEDIATE, requires = Equipment.PULL_UP_BAR)
        )

        val strengthParallettes = listOf(
            CatalogExercise("Dips", BlockType.STRENGTH, baseReps = 10, requires = Equipment.PARALLETTES, pattern = MovementPattern.PUSH),
            CatalogExercise("L-sit", BlockType.CORE, baseSeconds = 15, minLevel = FitnessLevel.ADVANCED, requires = Equipment.PARALLETTES)
        )

        val circuitMilitaryEndurance = listOf(
            CatalogExercise("Burpees", BlockType.CIRCUIT, baseReps = 12, skipOverForty = true),
            CatalogExercise("Mountain Climbers", BlockType.CIRCUIT, baseSeconds = 30),
            CatalogExercise("Sprints", BlockType.CIRCUIT, baseSeconds = 20),
            CatalogExercise("High Knees (circuit)", BlockType.CIRCUIT, baseSeconds = 30),
            CatalogExercise("Shuttle Runs", BlockType.CIRCUIT, baseSeconds = 30),
            CatalogExercise("Bear Crawl", BlockType.CIRCUIT, baseSeconds = 30)
        )

        val circuitFatLoss = listOf(
            CatalogExercise("Burpees", BlockType.CIRCUIT, baseReps = 12, skipOverForty = true),
            CatalogExercise("Jump Squats", BlockType.CIRCUIT, baseReps = 14, skipOverForty = true),
            CatalogExercise("Mountain Climbers", BlockType.CIRCUIT, baseSeconds = 30),
            CatalogExercise("High Knees (circuit)", BlockType.CIRCUIT, baseSeconds = 30),
            CatalogExercise("Bear Crawl", BlockType.CIRCUIT, baseSeconds = 30)
        )

        // Deliberately does not reuse Pike/Diamond Push-ups (already in
        // strengthBodyweight, allowed on any day's strength block by default)
        // — sharing a name between the strength and circuit pools let the
        // same exercise appear twice in one day's workout.
        val circuitStrengthMass = listOf(
            CatalogExercise("Mountain Climbers", BlockType.CIRCUIT, baseSeconds = 30),
            CatalogExercise("Explosive Push-ups", BlockType.CIRCUIT, baseReps = 6, minLevel = FitnessLevel.INTERMEDIATE),
            CatalogExercise("Jump Squats", BlockType.CIRCUIT, baseReps = 14, skipOverForty = true)
        )

        fun circuitPool(goal: Goal): List<CatalogExercise> = when (goal) {
            Goal.MILITARY_ENDURANCE -> circuitMilitaryEndurance
            Goal.FAT_LOSS -> circuitFatLoss
            Goal.STRENGTH_MASS -> circuitStrengthMass
            Goal.MOBILITY -> emptyList()
        }

        val core = listOf(
            CatalogExercise("Plank", BlockType.CORE, baseSeconds = 30),
            CatalogExercise("Side Plank", BlockType.CORE, baseSeconds = 20),
            CatalogExercise("Mountain Climbers (core)", BlockType.CORE, baseSeconds = 25),
            CatalogExercise("Russian Twists", BlockType.CORE, baseReps = 20),
            CatalogExercise("Leg Raises (floor)", BlockType.CORE, baseReps = 12),
            CatalogExercise("Bicycle Crunches", BlockType.CORE, baseReps = 20),
            CatalogExercise("Superman Hold", BlockType.CORE, baseSeconds = 20)
        )

        val mobility = listOf(
            CatalogExercise("Hip Openers", BlockType.CORE, baseSeconds = 30),
            CatalogExercise("Cat-Cow", BlockType.CORE, baseSeconds = 30),
            CatalogExercise("Shoulder Circles", BlockType.CORE, baseSeconds = 30),
            CatalogExercise("Thoracic Rotations", BlockType.CORE, baseSeconds = 30),
            CatalogExercise("Ankle Circles", BlockType.CORE, baseSeconds = 20),
            CatalogExercise("Deep Squat Hold", BlockType.CORE, baseSeconds = 30)
        )

        val cooldown = listOf(
            CatalogExercise("Hamstring Stretch", BlockType.COOL_DOWN, baseSeconds = 30),
            CatalogExercise("Quad Stretch", BlockType.COOL_DOWN, baseSeconds = 30),
            CatalogExercise("Child's Pose", BlockType.COOL_DOWN, baseSeconds = 40),
            CatalogExercise("Deep Breathing", BlockType.COOL_DOWN, baseSeconds = 60)
        )

        fun availableStrength(equipment: Set<Equipment>): List<CatalogExercise> {
            var pool = strengthBodyweight
            pool = if (Equipment.PULL_UP_BAR in equipment) pool + strengthPullBar else pool + strengthPullFallback
            if (Equipment.PARALLETTES in equipment) pool = pool + strengthParallettes
            return pool
        }
    }

    fun generate(profile: UserProfile): TrainingPlan {
        val totalWeeks = profile.level.progressionWeeks
        val progressionStep = if (profile.age > 40) 0.025 else 0.05
        val intensity = intensityFactor(profile)
        val dayLabels = splitLabels(profile.daysPerWeek)

        val weeks = (0 until totalWeeks).map { w ->
            val isDeload = (w + 1) % 4 == 0
            val scale = if (isDeload) 0.6 else 1.0 + progressionStep * w
            val days = dayLabels.mapIndexed { index, label ->
                buildDay(label, index, w, profile, intensity, scale)
            }
            WeeklyPlan(weekIndex = w, workouts = days, isDeload = isDeload)
        }
        return TrainingPlan(profile = profile, weeks = weeks)
    }

    private fun intensityFactor(profile: UserProfile): Double {
        var factor = levelIntensityFactor(profile.level)
        val bmi = profile.bmi
        if (bmi > 27) factor -= 0.1 else if (bmi < 18.5) factor += 0.1
        if (profile.age > 40) factor *= 0.9
        factor *= sexMultiplier(profile.sex)
        return factor
    }

    private fun levelIntensityFactor(level: FitnessLevel) = when (level) {
        FitnessLevel.BEGINNER -> 0.7
        FitnessLevel.INTERMEDIATE -> 0.9
        FitnessLevel.ADVANCED -> 1.15
    }

    /** Average upper-body strength/endurance calibration by sex, per docs/plan-engine-spec.md. */
    private fun sexMultiplier(sex: Sex) = when (sex) {
        Sex.MALE -> 1.0
        Sex.FEMALE -> 0.9
        Sex.UNSPECIFIED -> 1.0
    }

    private fun splitLabels(daysPerWeek: Int): List<String> = when (daysPerWeek) {
        3 -> listOf("Full Body I", "Full Body II", "Full Body III")
        4 -> listOf("Upper Body", "Lower Body", "Full Body", "Conditioning")
        5 -> listOf("Upper Body", "Lower Body", "Push", "Pull", "Conditioning")
        else -> listOf("Upper Body", "Lower Body", "Push", "Pull", "Conditioning", "Mobility")
    }

    private fun buildDay(label: String, splitIndex: Int, weekIndex: Int, profile: UserProfile, intensity: Double, scale: Double): DailyWorkout {
        val warmupBlock = block(BlockType.WARM_UP, Catalog.warmup.take(3), intensity, scale, rest = 15)

        val hasCircuit = includeCircuit(profile.goal, label)
        val budget = SessionBudget(profile.sessionMinutes, hasCircuit)

        // Only pattern-tagged (push/pull/legs) entries belong in the strength
        // block. `availableStrength` also carries bonus core exercises that
        // unlock with equipment (Hanging Leg Raises, L-sit) — those are CORE
        // and have no pattern, so without this filter they'd bypass the
        // day-label filtering below entirely.
        val fullStrengthPool = Catalog.availableStrength(profile.equipment)
            .filter { it.pattern != null }
            .filter { levelAllows(it.minLevel, profile.level) }
        val dayPatterns = dayPatternFocus(label)
        val strengthPool = filteredPool(fullStrengthPool, dayPatterns)
        val strengthRest = if (profile.goal == Goal.STRENGTH_MASS) 75 else 45
        val strengthCount = exerciseCount(strengthPool, budget.strengthSeconds, sets = 4, restSeconds = strengthRest, intensity = intensity, scale = scale)
        val strengthExercises = pick(strengthPool, splitIndex, weekIndex, strengthCount)
        val strengthBlock = block(BlockType.STRENGTH, strengthExercises, intensity, scale, rest = strengthRest)

        val blocks = mutableListOf(warmupBlock, strengthBlock)

        if (hasCircuit) {
            val goalCircuitPool = Catalog.circuitPool(profile.goal)
                .filter { !(it.skipOverForty && profile.age > 40) }
            val levelFiltered = goalCircuitPool.filter { levelAllows(it.minLevel, profile.level) }
            // strengthMass's conditioning circuit is all intermediate+ (pike/
            // explosive/diamond push-ups) — without this fallback, a beginner
            // would get an empty circuit block on that day.
            val circuitPool = levelFiltered.ifEmpty { goalCircuitPool }
            val circuitRest = if (profile.goal == Goal.FAT_LOSS) 30 else 40
            val circuitCount = exerciseCount(circuitPool, budget.circuitSeconds, sets = 3, restSeconds = circuitRest, intensity = intensity, scale = scale)
            val circuitExercises = pick(circuitPool, splitIndex, weekIndex, circuitCount)
            blocks += block(BlockType.CIRCUIT, circuitExercises, intensity, scale, rest = circuitRest)
        }

        var corePool = if (profile.goal == Goal.MOBILITY) Catalog.mobility else Catalog.core
        if (profile.goal != Goal.MOBILITY) {
            // Bonus core exercises that unlock with equipment (hanging leg
            // raises need a bar, L-sit needs parallettes) — these train core,
            // not a strength pattern, so they live in the core pool.
            if (Equipment.PULL_UP_BAR in profile.equipment) {
                corePool = corePool + Catalog.strengthPullBar.filter { it.block == BlockType.CORE && levelAllows(it.minLevel, profile.level) }
            }
            if (Equipment.PARALLETTES in profile.equipment) {
                corePool = corePool + Catalog.strengthParallettes.filter { it.block == BlockType.CORE && levelAllows(it.minLevel, profile.level) }
            }
        }
        val coreCount = exerciseCount(corePool, budget.coreSeconds, sets = 3, restSeconds = 20, intensity = intensity, scale = scale)
        val coreExercises = pick(corePool, splitIndex, weekIndex, coreCount)
        blocks += block(BlockType.CORE, coreExercises, intensity, scale, rest = 20)

        blocks += block(BlockType.COOL_DOWN, Catalog.cooldown.take(3), intensity, scale, rest = 10)

        return DailyWorkout(dayIndex = splitIndex, title = label, blocks = blocks)
    }

    /**
     * Which movement patterns a day's strength block should draw from,
     * based on the day's own label (upper/lower/push/pull/full body).
     * This is what makes "Lower Body" actually mean lower body instead of
     * the label being purely cosmetic.
     */
    private fun dayPatternFocus(label: String): Set<MovementPattern> = when (label) {
        "Upper Body" -> setOf(MovementPattern.PUSH, MovementPattern.PULL)
        "Lower Body" -> setOf(MovementPattern.LEGS)
        "Push" -> setOf(MovementPattern.PUSH)
        "Pull" -> setOf(MovementPattern.PULL)
        else -> setOf(MovementPattern.PUSH, MovementPattern.PULL, MovementPattern.LEGS)
    }

    /**
     * Restricts `pool` to exercises whose pattern is allowed for the day
     * (non-strength exercises, with a null pattern, always pass through).
     * Falls back to the unfiltered pool if filtering would leave it empty,
     * so a day never ends up with zero strength exercises.
     */
    private fun filteredPool(pool: List<CatalogExercise>, patterns: Set<MovementPattern>): List<CatalogExercise> {
        val filtered = pool.filter { entry -> entry.pattern == null || entry.pattern in patterns }
        return filtered.ifEmpty { pool }
    }

    /**
     * Splits the time left after warm-up/cool-down across the variable
     * blocks, per docs/plan-engine-spec.md "Session duration budget".
     */
    private class SessionBudget(sessionMinutes: Int, hasCircuit: Boolean) {
        val strengthSeconds: Int
        val coreSeconds: Int
        val circuitSeconds: Int

        init {
            val warmupSeconds = 180
            val cooldownSeconds = 120
            val total = (sessionMinutes * 60 - warmupSeconds - cooldownSeconds).coerceAtLeast(0)
            if (hasCircuit) {
                strengthSeconds = (total * 0.5).toInt()
                coreSeconds = (total * 0.2).toInt()
                circuitSeconds = (total * 0.3).toInt()
            } else {
                strengthSeconds = (total * 0.65).toInt()
                coreSeconds = (total * 0.35).toInt()
                circuitSeconds = 0
            }
        }
    }

    /**
     * Largest exercise count from `pool` whose estimated total time (sets *
     * (work + rest), see spec) fits `budgetSeconds`, clamped to 1...pool.size.
     */
    private fun exerciseCount(pool: List<CatalogExercise>, budgetSeconds: Int, sets: Int, restSeconds: Int, intensity: Double, scale: Double): Int {
        if (pool.isEmpty()) return 0
        val totalWorkSeconds = pool.sumOf { entry ->
            when {
                entry.baseReps != null -> entry.baseReps * 3.0 * intensity * scale
                entry.baseSeconds != null -> entry.baseSeconds * intensity * scale
                else -> 0.0
            }
        }
        val avgWorkSeconds = totalWorkSeconds / pool.size
        val perExerciseSeconds = sets * (avgWorkSeconds + restSeconds)
        if (perExerciseSeconds <= 0) return pool.size
        val count = (budgetSeconds / perExerciseSeconds).toInt()
        return count.coerceIn(1, pool.size)
    }

    /** Matches iOS's `PlanEngine.includeCircuit`. */
    private fun includeCircuit(goal: Goal, label: String): Boolean = when (goal) {
        Goal.FAT_LOSS, Goal.MILITARY_ENDURANCE -> true
        Goal.STRENGTH_MASS -> label == "Conditioning"
        Goal.MOBILITY -> false
    }

    private fun levelAllows(required: FitnessLevel, userLevel: FitnessLevel) = userLevel.ordinal >= required.ordinal

    /**
     * Rotates `pool` by both the day's position in the split and the week
     * index, so the exact same exercises don't repeat identically every
     * week of a 4-8 week plan.
     */
    private fun pick(pool: List<CatalogExercise>, splitIndex: Int, weekIndex: Int, count: Int): List<CatalogExercise> {
        if (pool.isEmpty()) return emptyList()
        val offset = (splitIndex + weekIndex) % pool.size
        val rotated = pool.subList(offset, pool.size) + pool.subList(0, offset)
        return rotated.take(count)
    }

    private fun block(kind: BlockType, catalog: List<CatalogExercise>, intensity: Double, scale: Double, rest: Int): TrainingBlock {
        val exercises = catalog.map { entry ->
            val sets = when (kind) {
                BlockType.WARM_UP, BlockType.COOL_DOWN -> 1
                BlockType.STRENGTH -> 4
                else -> 3
            }
            val reps = entry.baseReps?.let { max(4, (it * intensity * scale).roundToInt()) }
            val seconds = entry.baseSeconds?.let { max(10, (it * intensity * scale).roundToInt()) }
            ExerciseSet(entry.name, reps = reps, seconds = seconds, sets = sets, restSeconds = rest)
        }
        return TrainingBlock(type = kind, exercises = exercises)
    }
}
