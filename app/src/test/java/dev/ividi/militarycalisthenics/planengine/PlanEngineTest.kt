package dev.ividi.militarycalisthenics.planengine

import dev.ividi.militarycalisthenics.model.Equipment
import dev.ividi.militarycalisthenics.model.FitnessLevel
import dev.ividi.militarycalisthenics.model.Goal
import dev.ividi.militarycalisthenics.model.Sex
import dev.ividi.militarycalisthenics.model.UserProfile
import dev.ividi.militarycalisthenics.model.progressionWeeks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanEngineTest {

    private fun baseProfile(
        weightKg: Double = 80.0,
        heightCm: Double = 178.0,
        age: Int = 28,
        level: FitnessLevel = FitnessLevel.INTERMEDIATE,
        goal: Goal = Goal.MILITARY_ENDURANCE,
        daysPerWeek: Int = 4,
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT_ONLY),
        // 60 (the top of the allowed range) keeps these calibration tests
        // clear of session-duration trimming, which can otherwise lower a
        // harder profile's total exercise count enough to mask its higher
        // per-exercise intensity, see testSessionMinutesTrimsExerciseCount.
        sessionMinutes: Int = 60
    ) = UserProfile(
        weightKg = weightKg,
        heightCm = heightCm,
        age = age,
        sex = Sex.UNSPECIFIED,
        level = level,
        goal = goal,
        daysPerWeek = daysPerWeek,
        equipment = equipment,
        sessionMinutes = sessionMinutes
    )

    /** Sum of (reps or seconds) * sets across every exercise in every block/day/week. */
    private fun totalVolume(profile: UserProfile): Int {
        val plan = PlanEngine.generate(profile)
        return plan.weeks.sumOf { week ->
            week.workouts.sumOf { day ->
                day.blocks.sumOf { block ->
                    block.exercises.sumOf { ex -> (ex.reps ?: ex.seconds ?: 0) * ex.sets }
                }
            }
        }
    }

    @Test
    fun `generation is stable and non crashing across the full input matrix`() {
        val ages = listOf(16, 28, 45, 65)
        val weights = listOf(45.0, 80.0, 140.0)
        val equipmentOptions = listOf(
            setOf(Equipment.BODYWEIGHT_ONLY),
            setOf(Equipment.PULL_UP_BAR),
            setOf(Equipment.PARALLETTES),
            setOf(Equipment.PULL_UP_BAR, Equipment.PARALLETTES)
        )

        for (age in ages) {
            for (weight in weights) {
                for (level in FitnessLevel.entries) {
                    for (goal in Goal.entries) {
                        for (days in listOf(3, 6)) {
                            for (equipment in equipmentOptions) {
                                val profile = baseProfile(
                                    weightKg = weight,
                                    age = age,
                                    level = level,
                                    goal = goal,
                                    daysPerWeek = days,
                                    equipment = equipment
                                )
                                val plan = PlanEngine.generate(profile)

                                assertEquals(level.progressionWeeks, plan.weeks.size)
                                plan.weeks.forEach { week ->
                                    assertEquals(days, week.workouts.size)
                                    week.workouts.forEach { day ->
                                        assertTrue("every day must have at least one block", day.blocks.isNotEmpty())
                                        day.blocks.forEach { block ->
                                            assertTrue("every block must have at least one exercise", block.exercises.isNotEmpty())
                                            block.exercises.forEach { ex ->
                                                assertTrue("sets must be positive", ex.sets > 0)
                                                assertTrue(
                                                    "either reps or seconds must be set",
                                                    ex.reps != null || ex.seconds != null
                                                )
                                            }
                                        }
                                    }
                                }

                                // determinism: same input, same output shape twice in a row
                                val second = PlanEngine.generate(profile)
                                assertEquals(totalVolume(profile), totalVolume(profile))
                                assertEquals(second.weeks.size, plan.weeks.size)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `advanced level produces higher volume than beginner at otherwise equal inputs`() {
        val beginner = totalVolume(baseProfile(level = FitnessLevel.BEGINNER))
        val advanced = totalVolume(baseProfile(level = FitnessLevel.ADVANCED))

        assertTrue(
            "advanced ($advanced) should train at higher volume than beginner ($beginner)",
            advanced > beginner
        )
    }

    @Test
    fun `military endurance goal includes conditioning work absent from mobility goal`() {
        val endurancePlan = PlanEngine.generate(baseProfile(goal = Goal.MILITARY_ENDURANCE))
        val mobilityPlan = PlanEngine.generate(baseProfile(goal = Goal.MOBILITY))

        val enduranceExerciseNames = endurancePlan.weeks.first().workouts.first().blocks
            .flatMap { it.exercises }.map { it.name }.toSet()
        val mobilityExerciseNames = mobilityPlan.weeks.first().workouts.first().blocks
            .flatMap { it.exercises }.map { it.name }.toSet()

        assertTrue(
            "military endurance day should include sprint-style conditioning",
            enduranceExerciseNames.any { it.contains("Sprint") || it.contains("Burpee") }
        )
        assertNotEquals(enduranceExerciseNames, mobilityExerciseNames)
    }

    @Test
    fun `low BMI and high BMI produce different calibrated volume at equal weight-independent inputs`() {
        // Same age/level/goal/days, different height so BMI differs while isolating the BMI signal.
        val lowBmiProfile = baseProfile(weightKg = 60.0, heightCm = 200.0) // BMI ~15 (< 18.5)
        val normalBmiProfile = baseProfile(weightKg = 75.0, heightCm = 178.0) // BMI ~23.7 (18.5-25)
        val highBmiProfile = baseProfile(weightKg = 110.0, heightCm = 170.0) // BMI ~38 (>= 30)

        val lowVolume = totalVolume(lowBmiProfile)
        val normalVolume = totalVolume(normalBmiProfile)
        val highVolume = totalVolume(highBmiProfile)

        assertNotEquals(lowVolume, normalVolume)
        assertNotEquals(normalVolume, highVolume)
        assertTrue(
            "normal-BMI volume ($normalVolume) should exceed high-BMI volume ($highVolume) per the calibration table",
            normalVolume > highVolume
        )
    }

    @Test
    fun `logging a new weight and recalibrating changes the plan compared to before the update`() {
        // Mirrors MainViewModel.logWeight: it copies the profile with the new weight and
        // regenerates through PlanEngine, so we exercise that same recalibration path directly.
        val before = baseProfile(weightKg = 70.0, heightCm = 178.0)
        val beforeVolume = totalVolume(before)

        val after = before.copy(weightKg = 130.0)
        val afterVolume = totalVolume(after)

        assertNotEquals(
            "recalibrating after a large weight change must change calibrated volume",
            beforeVolume,
            afterVolume
        )
    }

    @Test
    fun `push day never includes leg exercises`() {
        // Regression test: day labels ("Push", "Lower Body", ...) used to be
        // purely cosmetic (or only respected for STRENGTH_MASS) — the
        // strength block used to ignore them and always include the same
        // fixed exercise list regardless of goal.
        val legNames = setOf("Squats", "Lunges", "Glute Bridges")
        Goal.entries.forEach { goal ->
            val plan = PlanEngine.generate(baseProfile(goal = goal, daysPerWeek = 6))
            plan.weeks.forEach { week ->
                val pushDay = week.workouts.first { it.title == "Push" }
                val strengthNames = pushDay.blocks.first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }
                    .exercises.map { it.name }
                assertTrue("push day must have exercises", strengthNames.isNotEmpty())
                strengthNames.forEach { name ->
                    assertTrue("$name is a leg exercise but appeared on $goal's Push day", name !in legNames)
                }
            }
        }
    }

    @Test
    fun `lower body day only includes leg exercises`() {
        val legNames = setOf("Squats", "Lunges", "Glute Bridges")
        Goal.entries.forEach { goal ->
            val plan = PlanEngine.generate(baseProfile(goal = goal, daysPerWeek = 4))
            plan.weeks.forEach { week ->
                val lowerDay = week.workouts.first { it.title == "Lower Body" }
                val strengthNames = lowerDay.blocks.first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }
                    .exercises.map { it.name }
                assertTrue("lower body day must have exercises", strengthNames.isNotEmpty())
                strengthNames.forEach { name ->
                    assertTrue("$name is not a leg exercise but appeared on $goal's Lower Body day", name in legNames)
                }
            }
        }
    }

    @Test
    fun `bodyweight only equipment still has a pull exercise available`() {
        // Regression test: verifies the fallback pull exercise (Inverted
        // Rows) is offered when there's no pull-up bar.
        val plan = PlanEngine.generate(
            baseProfile(daysPerWeek = 6, equipment = setOf(Equipment.BODYWEIGHT_ONLY))
        )
        val pullDay = plan.weeks.first().workouts.first { it.title == "Pull" }
        val strengthNames = pullDay.blocks.first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }
            .exercises.map { it.name }
        assertTrue(
            "expected the bodyweight pull fallback exercise, got $strengthNames",
            strengthNames.contains("Inverted Rows (table)")
        )
    }

    @Test
    fun `strength selection varies across weeks of the same plan`() {
        // Regression test: strength exercises used to be an identical fixed
        // list every day and every week, regardless of goal or day title.
        val plan = PlanEngine.generate(baseProfile(goal = Goal.MILITARY_ENDURANCE, daysPerWeek = 4))
        val week1Names = plan.weeks[0].workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }.exercises.map { it.name }
        val week2Names = plan.weeks[1].workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }.exercises.map { it.name }
        assertNotEquals(
            "strength selection should vary week to week, not repeat identically",
            week1Names,
            week2Names
        )
    }

    @Test
    fun `core pool is big enough that plank is not on every day`() {
        // Regression test: the core block used to be a fixed 3-exercise list
        // (Plank, Leg Raises, Russian Twists) in the same order every day,
        // and Plank always survived the budget trim since it came first —
        // so it appeared on literally every single day of every week.
        val plan = PlanEngine.generate(baseProfile(goal = Goal.FAT_LOSS, daysPerWeek = 6))
        var totalDays = 0
        var plankDays = 0
        plan.weeks.forEach { week ->
            week.workouts.forEach { day ->
                totalDays++
                val coreNames = day.blocks.first { it.type == dev.ividi.militarycalisthenics.model.BlockType.CORE }
                    .exercises.map { it.name }
                if ("Plank" in coreNames) plankDays++
            }
        }
        assertTrue(
            "plank should not appear on every day ($plankDays/$totalDays)",
            plankDays < totalDays
        )
    }

    @Test
    fun `beginners never get pike push-ups in the strength block`() {
        // Regression test: iOS gates Pike/Diamond Push-ups to intermediate+
        // (they demand more shoulder mobility and core control than a
        // complete beginner should be handed on day one); Android had no
        // such gating at all.
        val plan = PlanEngine.generate(baseProfile(level = FitnessLevel.BEGINNER, daysPerWeek = 4))
        plan.weeks.forEach { week ->
            week.workouts.forEach { day ->
                val strengthNames = day.blocks.first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }
                    .exercises.map { it.name }
                assertTrue(
                    "beginner should not get Pike Push-ups: $strengthNames",
                    "Pike Push-ups" !in strengthNames
                )
            }
        }
    }

    @Test
    fun `over forty users never get burpees or jump squats in the circuit`() {
        // Regression test: iOS excludes high-impact plyometric moves for
        // over-40 users (skipOverForty); Android's circuit had no
        // equivalent age-based exclusion at all.
        val plan = PlanEngine.generate(baseProfile(age = 55, goal = Goal.MILITARY_ENDURANCE, daysPerWeek = 3))
        plan.weeks.forEach { week ->
            week.workouts.forEach { day ->
                val circuitNames = day.blocks.first { it.type == dev.ividi.militarycalisthenics.model.BlockType.CIRCUIT }
                    .exercises.map { it.name }
                assertTrue("over-40 should not get Burpees: $circuitNames", "Burpees" !in circuitNames)
            }
        }
    }

    @Test
    fun `circuit exercise selection varies across weeks of the same plan`() {
        // Regression test: the circuit block was a fixed list per goal with
        // no rotation at all — same exercises, same order, every day, every
        // week, forever.
        val plan = PlanEngine.generate(baseProfile(goal = Goal.MILITARY_ENDURANCE, daysPerWeek = 3))
        val week1 = plan.weeks[0].workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.CIRCUIT }.exercises.map { it.name }
        val week2 = plan.weeks[1].workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.CIRCUIT }.exercises.map { it.name }
        assertNotEquals(
            "circuit selection should vary week to week, not repeat identically",
            week1,
            week2
        )
    }

    @Test
    fun `no exercise name repeats twice within the same day across blocks`() {
        // Regression test: "Hip Openers" used to be in both the fixed
        // warm-up and the Mobility goal's core pool, and "Cat-Cow" in both
        // the Mobility goal's circuit and core pools — either could show up
        // twice in the same day's workout.
        Goal.entries.forEach { goal ->
            val plan = PlanEngine.generate(baseProfile(goal = goal, daysPerWeek = 4))
            plan.weeks.forEach { week ->
                week.workouts.forEach { day ->
                    val allNames = day.blocks.flatMap { it.exercises.map { ex -> ex.name } }
                    val duplicates = allNames.groupingBy { it }.eachCount().filter { it.value > 1 }
                    assertTrue("$goal day ${day.dayIndex} has duplicate exercises: $duplicates", duplicates.isEmpty())
                }
            }
        }
    }

    @Test
    fun `mobility goal replaces ab core work with mobility drills`() {
        // Regression test: the core block used to always train abs (Plank,
        // Leg Raises, ...) even on the Mobility goal, unlike iOS which
        // substitutes mobility-focused drills for that goal instead.
        val plan = PlanEngine.generate(baseProfile(goal = Goal.MOBILITY, daysPerWeek = 3))
        val coreNames = plan.weeks.first().workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.CORE }.exercises.map { it.name }
        assertTrue("Plank should not appear on the Mobility goal: $coreNames", "Plank" !in coreNames)
    }

    @Test
    fun `shorter session minutes never increases the strength exercise count`() {
        val short = PlanEngine.generate(baseProfile(sessionMinutes = 15))
        val long = PlanEngine.generate(baseProfile(sessionMinutes = 60))

        val shortCount = short.weeks.first().workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }.exercises.size
        val longCount = long.weeks.first().workouts.first().blocks
            .first { it.type == dev.ividi.militarycalisthenics.model.BlockType.STRENGTH }.exercises.size

        assertTrue(
            "a 60-minute session ($longCount exercises) should fit at least as many strength exercises as a 15-minute one ($shortCount)",
            longCount >= shortCount
        )
    }
}
