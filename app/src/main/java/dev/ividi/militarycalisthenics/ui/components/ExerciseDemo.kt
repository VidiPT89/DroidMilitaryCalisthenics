package dev.ividi.militarycalisthenics.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.ividi.militarycalisthenics.model.ExerciseSet
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.BgPanel2
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary

/** A small looping stick-figure how-to animation, drawn entirely in code — no bundled media. */
@Composable
fun ExerciseDemo(category: ExerciseDemoCategory, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "exerciseDemo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "poseT"
    )
    val pose = lerpPose(category.poseA, category.poseB, t)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BgPanel2)
    ) {
        drawStickFigure(pose, category.symmetric)
    }
}

private fun DrawScope.drawStickFigure(pose: StickPose, symmetric: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = size.minDimension * 0.045f
    val headRadius = size.minDimension * 0.09f

    fun px(o: Offset) = Offset(o.x * w, o.y * h)

    val head = px(pose.head)
    val neck = px(pose.neck)
    val hip = px(pose.hip)
    val elbow = px(pose.elbow)
    val hand = px(pose.hand)
    val knee = px(pose.knee)
    val foot = px(pose.foot)

    fun line(a: Offset, b: Offset) = drawLine(AccentOrange, a, b, strokeWidth = stroke, cap = StrokeCap.Round)

    line(neck, hip)
    line(neck, elbow); line(elbow, hand)
    line(hip, knee); line(knee, foot)
    drawCircle(AccentOrange, radius = headRadius, center = head)

    if (symmetric) {
        val mirroredHand = Offset(2 * neck.x - hand.x, hand.y)
        val mirroredElbow = Offset(2 * neck.x - elbow.x, elbow.y)
        val mirroredFoot = Offset(2 * hip.x - foot.x, foot.y)
        val mirroredKnee = Offset(2 * hip.x - knee.x, knee.y)
        line(neck, mirroredElbow); line(mirroredElbow, mirroredHand)
        line(hip, mirroredKnee); line(mirroredKnee, mirroredFoot)
    }
}

/** Full detail view: larger demo + exercise name + a short coaching cue, in the user's chosen language. */
@Composable
fun ExerciseDetailDialog(exercise: ExerciseSet, lang: Lang, onDismiss: () -> Unit) {
    val category = categoryForExerciseName(exercise.name)
    Dialog(onDismissRequest = onDismiss) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(exercise.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp, textAlign = TextAlign.Center)
                ExerciseDemo(category, modifier = Modifier.size(200.dp))
                Text(
                    text = t(cueKeyForCategory(category), lang),
                    color = TextDim,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                SelectableChip(text = t("close", lang), selected = true, onClick = onDismiss)
            }
        }
    }
}
