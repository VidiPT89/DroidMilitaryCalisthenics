package dev.ividi.militarycalisthenics.ui.components

import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the "frozen" stick-figure bug: every demo category must move.
 * Isometric holds (plank) get a lower bar since their real-life motion is a subtle
 * engagement wobble, not a full rep — but zero motion is still treated as a bug.
 */
class ExerciseDemoShapesTest {

    private fun poseDistance(category: ExerciseDemoCategory): Double {
        val a = category.poseA
        val b = category.poseB
        val joints = listOf(
            a.head to b.head, a.neck to b.neck, a.hip to b.hip,
            a.elbow to b.elbow, a.hand to b.hand, a.knee to b.knee, a.foot to b.foot
        )
        return joints.sumOf { (p1, p2) -> hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()) }
    }

    private val isometricCategories = setOf(ExerciseDemoCategory.PLANK)

    @Test
    fun everyCategoryHasNonZeroMotionBetweenKeyframes() {
        for (category in ExerciseDemoCategory.entries) {
            val distance = poseDistance(category)
            val minDistance = if (category in isometricCategories) 0.02 else 0.15
            assertTrue(
                "${category.name} looks frozen: total keyframe distance $distance is below $minDistance",
                distance >= minDistance
            )
        }
    }

    @Test
    fun interpolationAtMidpointDiffersFromBothKeyframes() {
        for (category in ExerciseDemoCategory.entries) {
            if (category in isometricCategories) continue
            val mid = lerpPose(category.poseA, category.poseB, 0.5f)
            assertTrue("${category.name} midpoint pose equals poseA", mid != category.poseA)
            assertTrue("${category.name} midpoint pose equals poseB", mid != category.poseB)
        }
    }

    /**
     * Regression guard for exercises collapsing onto a visually identical shared pose
     * (e.g. several cool-down stretches all rendering as the same generic silhouette).
     * Compares the midpoint silhouette of every category against every other one.
     */
    @Test
    fun noTwoCategoriesShareANearIdenticalSilhouette() {
        val categories = ExerciseDemoCategory.entries
        for (i in categories.indices) {
            for (j in i + 1 until categories.size) {
                val a = categories[i]
                val b = categories[j]
                val midA = lerpPose(a.poseA, a.poseB, 0.5f)
                val midB = lerpPose(b.poseA, b.poseB, 0.5f)
                val joints = listOf(
                    midA.head to midB.head, midA.neck to midB.neck, midA.hip to midB.hip,
                    midA.elbow to midB.elbow, midA.hand to midB.hand, midA.knee to midB.knee, midA.foot to midB.foot
                )
                val distance = joints.sumOf { (p1, p2) -> hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble()) }
                assertTrue(
                    "${a.name} and ${b.name} look nearly identical: silhouette distance $distance",
                    distance >= 0.12
                )
            }
        }
    }
}
