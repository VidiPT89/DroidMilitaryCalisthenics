package dev.ividi.militarycalisthenics.planengine

import dev.ividi.militarycalisthenics.model.*
import org.junit.Assert.*
import org.junit.Test

class PlanOverviewTest {
    private val profile = UserProfile(75.0, 175.0, 28, level = FitnessLevel.BEGINNER,
        goal = Goal.FAT_LOSS, daysPerWeek = 4)

    @Test
    fun `last week alone does not complete plan`() {
        val plan = PlanEngine.generate(profile)
        val onlyLast = plan.copy(weeks = plan.weeks.map { week ->
            week.copy(workouts = week.workouts.map { it.copy(completed = week == plan.weeks.last()) })
        })
        assertFalse(onlyLast.isComplete)
        val complete = plan.copy(weeks = plan.weeks.map { week ->
            week.copy(workouts = week.workouts.map { it.copy(completed = true) })
        })
        assertTrue(complete.isComplete)
        assertFalse(plan.isComplete)
    }

    @Test
    fun `empty plans do not complete`() {
        assertFalse(TrainingPlan(profile, emptyList()).isComplete)
        assertFalse(TrainingPlan(profile, listOf(WeeklyPlan(0, emptyList()))).isComplete)
    }

    @Test
    fun `duration follows session and excludes final rest`() {
        val day = DailyWorkout(0, "day", listOf(TrainingBlock(BlockType.STRENGTH, listOf(
            ExerciseSet("reps", reps = 10, sets = 2, restSeconds = 20),
            ExerciseSet("hold", seconds = 45, sets = 1, restSeconds = 10)
        ))))
        assertEquals(2, day.exerciseCount)
        assertEquals(145, day.estimatedDurationSeconds)
        assertEquals(3, day.estimatedMinutes)
    }

    @Test
    fun `empty workout has no duration`() {
        val day = DailyWorkout(0, "empty", emptyList())
        assertEquals(0, day.exerciseCount)
        assertEquals(0, day.estimatedMinutes)
    }
}
