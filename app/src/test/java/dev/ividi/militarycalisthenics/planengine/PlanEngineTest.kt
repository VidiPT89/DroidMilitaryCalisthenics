package dev.ividi.militarycalisthenics.planengine

import dev.ividi.militarycalisthenics.model.Equipment
import dev.ividi.militarycalisthenics.model.FitnessLevel
import dev.ividi.militarycalisthenics.model.Goal
import dev.ividi.militarycalisthenics.model.Sex
import dev.ividi.militarycalisthenics.model.UserProfile
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
        equipment: Set<Equipment> = setOf(Equipment.BODYWEIGHT_ONLY)
    ) = UserProfile(
        weightKg = weightKg,
        heightCm = heightCm,
        age = age,
        sex = Sex.UNSPECIFIED,
        level = level,
        goal = goal,
        daysPerWeek = daysPerWeek,
        equipment = equipment
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

                                assertEquals(6, plan.weeks.size)
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
}
