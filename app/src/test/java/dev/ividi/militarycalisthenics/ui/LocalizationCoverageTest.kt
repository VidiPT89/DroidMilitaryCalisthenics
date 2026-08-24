package dev.ividi.militarycalisthenics.ui

import dev.ividi.militarycalisthenics.model.Equipment
import dev.ividi.militarycalisthenics.model.FitnessLevel
import dev.ividi.militarycalisthenics.model.Goal
import dev.ividi.militarycalisthenics.model.Sex
import dev.ividi.militarycalisthenics.model.UserProfile
import dev.ividi.militarycalisthenics.planengine.PlanEngine
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationCoverageTest {

    /** Every day title and exercise name a generated plan can contain must have a PT/EN entry. */
    @Test
    fun everyGeneratedPlanStringHasATranslation() {
        val combos = listOf(
            UserProfile(80.0, 178.0, 28, Sex.UNSPECIFIED, FitnessLevel.BEGINNER, Goal.FAT_LOSS, 3, setOf(Equipment.BODYWEIGHT_ONLY)),
            UserProfile(95.0, 170.0, 45, Sex.UNSPECIFIED, FitnessLevel.INTERMEDIATE, Goal.STRENGTH_MASS, 5, setOf(Equipment.PULL_UP_BAR)),
            UserProfile(70.0, 185.0, 22, Sex.UNSPECIFIED, FitnessLevel.ADVANCED, Goal.MILITARY_ENDURANCE, 6, setOf(Equipment.PARALLETTES)),
            UserProfile(60.0, 160.0, 60, Sex.UNSPECIFIED, FitnessLevel.BEGINNER, Goal.MOBILITY, 3, setOf(Equipment.BODYWEIGHT_ONLY))
        )

        val missing = mutableSetOf<String>()
        combos.forEach { profile ->
            val plan = PlanEngine.generate(profile)
            plan.weeks.forEach { week ->
                week.workouts.forEach { day ->
                    if (!hasTranslation(day.title)) missing += day.title
                    day.blocks.forEach { block ->
                        block.exercises.forEach { ex ->
                            if (!hasTranslation(ex.name)) missing += ex.name
                        }
                    }
                }
            }
        }

        assertTrue("Missing translations for: $missing", missing.isEmpty())
    }
}
