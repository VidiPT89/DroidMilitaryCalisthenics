package dev.ividi.militarycalisthenics.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp

/**
 * A minimal 7-point stick figure pose, expressed as fractions (0f..1f) of a square box.
 * Two poses per exercise are interpolated to produce a looping how-to animation.
 */
data class StickPose(
    val head: Offset,
    val neck: Offset,
    val hip: Offset,
    val elbow: Offset,
    val hand: Offset,
    val knee: Offset,
    val foot: Offset
)

fun lerpPose(a: StickPose, b: StickPose, t: Float): StickPose = StickPose(
    head = lerp(a.head, b.head, t),
    neck = lerp(a.neck, b.neck, t),
    hip = lerp(a.hip, b.hip, t),
    elbow = lerp(a.elbow, b.elbow, t),
    hand = lerp(a.hand, b.hand, t),
    knee = lerp(a.knee, b.knee, t),
    foot = lerp(a.foot, b.foot, t)
)

enum class ExerciseDemoCategory(val poseA: StickPose, val poseB: StickPose, val symmetric: Boolean = false) {
    PUSH_UP(
        poseA = StickPose(Offset(.18f, .42f), Offset(.28f, .45f), Offset(.62f, .48f), Offset(.28f, .60f), Offset(.30f, .75f), Offset(.80f, .50f), Offset(.92f, .52f)),
        poseB = StickPose(Offset(.18f, .55f), Offset(.28f, .58f), Offset(.62f, .52f), Offset(.22f, .60f), Offset(.30f, .75f), Offset(.80f, .52f), Offset(.92f, .52f))
    ),
    SQUAT(
        poseA = StickPose(Offset(.50f, .15f), Offset(.50f, .25f), Offset(.50f, .45f), Offset(.35f, .30f), Offset(.30f, .40f), Offset(.50f, .65f), Offset(.50f, .85f)),
        poseB = StickPose(Offset(.50f, .35f), Offset(.50f, .42f), Offset(.50f, .60f), Offset(.65f, .42f), Offset(.72f, .40f), Offset(.40f, .68f), Offset(.50f, .85f))
    ),
    PULL_UP(
        poseA = StickPose(Offset(.50f, .35f), Offset(.50f, .42f), Offset(.50f, .65f), Offset(.35f, .30f), Offset(.35f, .15f), Offset(.50f, .80f), Offset(.50f, .92f)),
        poseB = StickPose(Offset(.50f, .20f), Offset(.50f, .28f), Offset(.50f, .55f), Offset(.30f, .22f), Offset(.35f, .15f), Offset(.50f, .72f), Offset(.50f, .88f))
    ),
    ROW(
        poseA = StickPose(Offset(.20f, .50f), Offset(.30f, .50f), Offset(.62f, .48f), Offset(.35f, .35f), Offset(.40f, .20f), Offset(.80f, .48f), Offset(.92f, .46f)),
        poseB = StickPose(Offset(.20f, .38f), Offset(.30f, .40f), Offset(.62f, .44f), Offset(.35f, .30f), Offset(.40f, .20f), Offset(.80f, .46f), Offset(.92f, .46f))
    ),
    DIP(
        poseA = StickPose(Offset(.50f, .20f), Offset(.50f, .28f), Offset(.50f, .48f), Offset(.35f, .30f), Offset(.35f, .35f), Offset(.50f, .68f), Offset(.50f, .85f)),
        poseB = StickPose(Offset(.50f, .32f), Offset(.50f, .40f), Offset(.50f, .58f), Offset(.30f, .42f), Offset(.35f, .35f), Offset(.50f, .72f), Offset(.50f, .85f))
    ),
    LUNGE(
        poseA = StickPose(Offset(.50f, .15f), Offset(.50f, .25f), Offset(.50f, .45f), Offset(.40f, .35f), Offset(.35f, .45f), Offset(.50f, .65f), Offset(.50f, .85f)),
        poseB = StickPose(Offset(.55f, .20f), Offset(.55f, .30f), Offset(.50f, .50f), Offset(.40f, .40f), Offset(.35f, .50f), Offset(.65f, .70f), Offset(.75f, .85f))
    ),
    BURPEE(
        poseA = StickPose(Offset(.20f, .50f), Offset(.30f, .50f), Offset(.55f, .50f), Offset(.30f, .62f), Offset(.32f, .75f), Offset(.75f, .50f), Offset(.90f, .50f)),
        poseB = StickPose(Offset(.50f, .12f), Offset(.50f, .22f), Offset(.50f, .45f), Offset(.40f, .10f), Offset(.35f, .02f), Offset(.50f, .60f), Offset(.50f, .85f))
    ),
    MOUNTAIN_CLIMBER(
        poseA = StickPose(Offset(.20f, .42f), Offset(.30f, .45f), Offset(.60f, .48f), Offset(.28f, .58f), Offset(.30f, .72f), Offset(.78f, .48f), Offset(.92f, .50f)),
        poseB = StickPose(Offset(.20f, .42f), Offset(.30f, .45f), Offset(.60f, .48f), Offset(.28f, .58f), Offset(.30f, .72f), Offset(.68f, .40f), Offset(.70f, .48f))
    ),
    SPRINT(
        poseA = StickPose(Offset(.50f, .15f), Offset(.50f, .25f), Offset(.50f, .48f), Offset(.42f, .32f), Offset(.38f, .20f), Offset(.42f, .60f), Offset(.40f, .85f)),
        poseB = StickPose(Offset(.52f, .16f), Offset(.52f, .26f), Offset(.50f, .48f), Offset(.58f, .35f), Offset(.62f, .45f), Offset(.58f, .40f), Offset(.55f, .55f))
    ),
    PLANK(
        poseA = StickPose(Offset(.18f, .42f), Offset(.28f, .45f), Offset(.60f, .46f), Offset(.28f, .58f), Offset(.30f, .72f), Offset(.78f, .47f), Offset(.92f, .48f)),
        poseB = StickPose(Offset(.18f, .43f), Offset(.28f, .46f), Offset(.60f, .47f), Offset(.28f, .59f), Offset(.30f, .73f), Offset(.78f, .48f), Offset(.92f, .49f))
    ),
    LEG_RAISE(
        poseA = StickPose(Offset(.15f, .50f), Offset(.25f, .50f), Offset(.45f, .50f), Offset(.20f, .55f), Offset(.15f, .60f), Offset(.65f, .55f), Offset(.85f, .58f)),
        poseB = StickPose(Offset(.15f, .50f), Offset(.25f, .50f), Offset(.45f, .50f), Offset(.20f, .55f), Offset(.15f, .60f), Offset(.50f, .30f), Offset(.55f, .12f))
    ),
    TWIST(
        poseA = StickPose(Offset(.20f, .38f), Offset(.30f, .46f), Offset(.55f, .60f), Offset(.38f, .52f), Offset(.25f, .48f), Offset(.75f, .60f), Offset(.90f, .55f)),
        poseB = StickPose(Offset(.40f, .36f), Offset(.38f, .45f), Offset(.55f, .60f), Offset(.45f, .50f), Offset(.55f, .42f), Offset(.75f, .60f), Offset(.90f, .55f))
    ),
    JUMPING_JACK(
        poseA = StickPose(Offset(.50f, .15f), Offset(.50f, .25f), Offset(.50f, .50f), Offset(.50f, .40f), Offset(.50f, .55f), Offset(.50f, .68f), Offset(.50f, .85f)),
        poseB = StickPose(Offset(.50f, .15f), Offset(.50f, .25f), Offset(.50f, .50f), Offset(.35f, .15f), Offset(.20f, .05f), Offset(.38f, .68f), Offset(.25f, .85f)),
        symmetric = true
    ),
    STRETCH_GENERIC(
        poseA = StickPose(Offset(.50f, .15f), Offset(.50f, .25f), Offset(.50f, .50f), Offset(.40f, .15f), Offset(.35f, .05f), Offset(.50f, .68f), Offset(.50f, .85f)),
        poseB = StickPose(Offset(.52f, .16f), Offset(.52f, .26f), Offset(.50f, .50f), Offset(.62f, .16f), Offset(.68f, .08f), Offset(.50f, .68f), Offset(.50f, .85f))
    );
}

/** Maps a plan-engine exercise name (English, as generated) to its closest demo category. */
fun categoryForExerciseName(name: String): ExerciseDemoCategory {
    val n = name.lowercase()
    return when {
        "push-up" in n -> ExerciseDemoCategory.PUSH_UP
        "squat" in n -> ExerciseDemoCategory.SQUAT
        "pull-up" in n -> ExerciseDemoCategory.PULL_UP
        "row" in n -> ExerciseDemoCategory.ROW
        "dip" in n -> ExerciseDemoCategory.DIP
        "lunge" in n -> ExerciseDemoCategory.LUNGE
        "burpee" in n -> ExerciseDemoCategory.BURPEE
        "mountain climber" in n -> ExerciseDemoCategory.MOUNTAIN_CLIMBER
        "high knees" in n || "sprint" in n -> ExerciseDemoCategory.SPRINT
        "plank" in n -> ExerciseDemoCategory.PLANK
        "leg raise" in n -> ExerciseDemoCategory.LEG_RAISE
        "twist" in n -> ExerciseDemoCategory.TWIST
        "jumping jack" in n -> ExerciseDemoCategory.JUMPING_JACK
        else -> ExerciseDemoCategory.STRETCH_GENERIC
    }
}

/** Localization key for the short coaching cue shown under an exercise's demo. */
fun cueKeyForCategory(category: ExerciseDemoCategory): String = when (category) {
    ExerciseDemoCategory.PUSH_UP -> "cue_push_up"
    ExerciseDemoCategory.SQUAT -> "cue_squat"
    ExerciseDemoCategory.PULL_UP -> "cue_pull_up"
    ExerciseDemoCategory.ROW -> "cue_row"
    ExerciseDemoCategory.DIP -> "cue_dip"
    ExerciseDemoCategory.LUNGE -> "cue_lunge"
    ExerciseDemoCategory.BURPEE -> "cue_burpee"
    ExerciseDemoCategory.MOUNTAIN_CLIMBER -> "cue_mountain_climber"
    ExerciseDemoCategory.SPRINT -> "cue_sprint"
    ExerciseDemoCategory.PLANK -> "cue_plank"
    ExerciseDemoCategory.LEG_RAISE -> "cue_leg_raise"
    ExerciseDemoCategory.TWIST -> "cue_twist"
    ExerciseDemoCategory.JUMPING_JACK -> "cue_jumping_jack"
    ExerciseDemoCategory.STRETCH_GENERIC -> "cue_stretch"
}
