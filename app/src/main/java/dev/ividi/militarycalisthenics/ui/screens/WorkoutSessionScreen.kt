package dev.ividi.militarycalisthenics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ividi.militarycalisthenics.model.BlockType
import dev.ividi.militarycalisthenics.model.DailyWorkout
import dev.ividi.militarycalisthenics.model.ExerciseSet
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.PrimaryButton
import dev.ividi.militarycalisthenics.ui.components.SectionCard
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.BgBase
import dev.ividi.militarycalisthenics.ui.theme.ColorError
import dev.ividi.militarycalisthenics.ui.theme.ColorOk
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary
import kotlinx.coroutines.delay

private data class WorkoutStep(
    val blockType: BlockType,
    val exercise: ExerciseSet,
    val isRest: Boolean,
    val setIndex: Int
)

/** Flattens every block into sequential (set, rest) steps, skipping the rest after the last set of the day. */
private fun buildSteps(day: DailyWorkout): List<WorkoutStep> {
    val result = mutableListOf<WorkoutStep>()
    val lastBlock = day.blocks.lastOrNull()
    for (block in day.blocks) {
        val lastExercise = block.exercises.lastOrNull()
        for (exercise in block.exercises) {
            for (set in 0 until maxOf(exercise.sets, 1)) {
                result += WorkoutStep(block.type, exercise, isRest = false, setIndex = set)
                val isVeryLast = block === lastBlock && exercise === lastExercise && set == exercise.sets - 1
                if (!isVeryLast) {
                    result += WorkoutStep(block.type, exercise, isRest = true, setIndex = set)
                }
            }
        }
    }
    return result
}

/**
 * Full-screen guided session that walks through a day's workout step by
 * step: timed exercises count down automatically, rep-based ones wait for
 * a manual "Done" tap, and rest counts down between them. See
 * docs/plan-engine-spec.md "Guided workout session (timer)".
 */
@Composable
fun WorkoutSessionScreen(lang: Lang, day: DailyWorkout, onExit: () -> Unit, onFinish: () -> Unit) {
    val steps = remember(day) { buildSteps(day) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var remainingSeconds by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var confirmingExit by remember { mutableStateOf(false) }
    val currentStep = steps.getOrNull(currentIndex)

    LaunchedEffect(currentIndex) {
        val step = steps.getOrNull(currentIndex) ?: run { onFinish(); return@LaunchedEffect }
        val seconds = if (step.isRest) step.exercise.restSeconds else step.exercise.seconds
        if (seconds == null) {
            remainingSeconds = 0
            return@LaunchedEffect
        }
        remainingSeconds = seconds
        while (remainingSeconds > 0) {
            delay(1000)
            if (!isPaused) remainingSeconds -= 1
        }
        currentIndex += 1
    }

    Box(modifier = Modifier.fillMaxSize().background(BgBase)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { confirmingExit = true }) {
                    Icon(Icons.Filled.Close, contentDescription = t("session_exit_confirm_action", lang), tint = TextDim)
                }
                if (currentStep != null) {
                    Text(blockLabel(currentStep.blockType, lang), color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Box(modifier = Modifier.padding(24.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep != null) {
                    if (currentStep.isRest) {
                        Text(t("session_rest", lang), color = ColorOk, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(timeString(remainingSeconds), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 56.sp)
                    } else {
                        Text(t(currentStep.exercise.name, lang), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 26.sp)
                        Text(
                            "${t("session_set", lang)} ${currentStep.setIndex + 1}/${currentStep.exercise.sets}",
                            color = TextDim, fontSize = 14.sp
                        )
                        if (currentStep.exercise.seconds != null) {
                            Text(timeString(remainingSeconds), color = AccentOrange, fontWeight = FontWeight.Black, fontSize = 56.sp)
                        } else if (currentStep.exercise.reps != null) {
                            Text("${currentStep.exercise.reps} ${t("reps", lang)}", color = AccentOrange, fontWeight = FontWeight.Black, fontSize = 44.sp)
                        }
                    }
                }
            }

            if (currentStep != null) {
                when {
                    currentStep.isRest -> {
                        PrimaryButton(t("session_skip_rest", lang), modifier = Modifier.fillMaxWidth()) { currentIndex += 1 }
                    }
                    currentStep.exercise.reps != null -> {
                        PrimaryButton(t("session_done", lang), modifier = Modifier.fillMaxWidth()) { currentIndex += 1 }
                    }
                    else -> {
                        Text(
                            if (isPaused) t("session_resume", lang) else t("session_pause", lang),
                            color = TextDim,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable { isPaused = !isPaused }
                        )
                    }
                }
            }
        }

        // In-view confirmation instead of a native dialog, so it never blocks
        // the countdown coroutine while awaiting a result.
        if (confirmingExit) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(t("session_exit_confirm", lang), color = TextPrimary, fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(
                                t("session_exit_cancel", lang), color = TextDim, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { confirmingExit = false }
                            )
                            Text(
                                t("session_exit_confirm_action", lang), color = ColorError, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(onClick = onExit)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun blockLabel(type: BlockType, lang: Lang): String = when (type) {
    BlockType.WARM_UP -> t("warm_up", lang)
    BlockType.STRENGTH -> t("strength", lang)
    BlockType.CIRCUIT -> t("circuit", lang)
    BlockType.CORE -> t("core", lang)
    BlockType.COOL_DOWN -> t("cool_down", lang)
}

private fun timeString(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
