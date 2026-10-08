package dev.ividi.militarycalisthenics.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import dev.ividi.militarycalisthenics.model.estimatedMinutes
import dev.ividi.militarycalisthenics.model.exerciseCount
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ividi.militarycalisthenics.model.BlockType
import dev.ividi.militarycalisthenics.model.DailyWorkout
import dev.ividi.militarycalisthenics.model.ExerciseSet
import dev.ividi.militarycalisthenics.model.FitnessLevel
import dev.ividi.militarycalisthenics.model.TrainingBlock
import dev.ividi.militarycalisthenics.model.TrainingPlan
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.CompletionBadge
import dev.ividi.militarycalisthenics.ui.components.ExerciseDetailDialog
import dev.ividi.militarycalisthenics.ui.components.PlanCompleteDialog
import dev.ividi.militarycalisthenics.ui.components.PrimaryButton
import dev.ividi.militarycalisthenics.ui.components.ProgressRing
import dev.ividi.militarycalisthenics.ui.components.SectionCard
import dev.ividi.militarycalisthenics.ui.components.SelectableChip
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary
import dev.ividi.militarycalisthenics.util.sharePlanAsPdf
import dev.ividi.militarycalisthenics.util.shareAsText
import dev.ividi.militarycalisthenics.util.toShareText

@Composable
fun PlanScreen(
    plan: TrainingPlan,
    lang: Lang,
    selectedWeek: Int,
    onSelectWeek: (Int) -> Unit,
    shouldShowPlanComplete: Boolean,
    nextLevel: FitnessLevel?,
    onToggleCompleted: (weekIndex: Int, dayIndex: Int) -> Unit,
    onRepeatPlan: () -> Unit,
    onLevelUp: () -> Unit,
    onDismissPlanComplete: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartWorkout: (weekIndex: Int, day: DailyWorkout) -> Unit
) {
    var demoExercise by remember { mutableStateOf<ExerciseSet?>(null) }
    val week = plan.weeks.getOrNull(selectedWeek) ?: plan.weeks.firstOrNull() ?: return
    val completedCount = week.workouts.count { it.completed }
    val progress = if (week.workouts.isEmpty()) 0f else completedCount.toFloat() / week.workouts.size
    val context = LocalContext.current

    demoExercise?.let { exercise ->
        ExerciseDetailDialog(exercise = exercise, lang = lang, onDismiss = { demoExercise = null })
    }

    // `shouldShowPlanComplete` is already false once acknowledged (tracked in the
    // ViewModel/persisted, not local state), so it won't reappear on recomposition —
    // e.g. switching tabs and back — the way a purely local `remember` flag would.
    if (shouldShowPlanComplete) {
        PlanCompleteDialog(
            lang = lang,
            nextLevel = nextLevel,
            onRepeat = { onRepeatPlan() },
            onLevelUp = { onLevelUp() },
            onDismiss = onDismissPlanComplete
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(t("your_plan", lang), modifier = Modifier.weight(1f), color = TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 26.sp)
                IconButton(onClick = { sharePlanAsPdf(context, week, lang) }) {
                    Icon(Icons.Filled.Share, contentDescription = t("share_plan", lang), tint = AccentOrange)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = t("settings", lang), tint = AccentOrange)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(plan.weeks) { w ->
                    SelectableChip("${t("week", lang)} ${w.weekIndex + 1}", w.weekIndex == week.weekIndex) {
                        onSelectWeek(w.weekIndex)
                    }
                }
            }
        }
        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProgressRing(progress = progress, sizeDp = 72)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${t("week", lang)} ${week.weekIndex + 1}", color = TextPrimary,
                            fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("$completedCount / ${week.workouts.size} ${t("workouts_completed", lang)}", color = TextDim, fontSize = 14.sp)
                        if (week.isDeload) Text(t("deload_week", lang), color = AccentOrange, fontSize = 14.sp)
                    }
                }
            }
        }
        items(week.workouts, key = { "${week.weekIndex}-${it.dayIndex}" }) { workout ->
            WorkoutCard(
                workout = workout, lang = lang,
                onToggle = { onToggleCompleted(week.weekIndex, workout.dayIndex) },
                onExerciseClick = { demoExercise = it },
                onStartWorkout = { onStartWorkout(week.weekIndex, workout) }
            )
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: DailyWorkout,
    lang: Lang,
    onToggle: () -> Unit,
    onExerciseClick: (ExerciseSet) -> Unit,
    onStartWorkout: () -> Unit
) {
    val context = LocalContext.current
    var expanded by rememberSaveable(workout.dayIndex) { mutableStateOf(false) }
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${t("day", lang)} ${workout.dayIndex + 1}", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(t(workout.title, lang), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shareAsText(context, t(workout.title, lang), workout.toShareText(lang)) }) {
                        Icon(Icons.Filled.Share, contentDescription = t("share_plan", lang), tint = AccentOrange, modifier = Modifier.size(18.dp))
                    }
                    CompletionBadge(completed = workout.completed, lang = lang)
                }
            }

            Text("~${workout.estimatedMinutes} min · ${workout.exerciseCount} ${t("exercises", lang)}", color = TextDim, fontSize = 15.sp)
            Text(t("duration_estimate", lang), color = TextDim, fontSize = 12.sp)
            TextButton(onClick = { expanded = !expanded }) {
                Text(t(if (expanded) "hide_exercises" else "show_exercises", lang))
            }
            if (expanded) workout.blocks.forEach { block -> BlockRow(block, lang, onExerciseClick) }

            PrimaryButton(t("start_workout", lang), modifier = Modifier.fillMaxWidth(), onClick = onStartWorkout)

            SelectableChip(
                text = t(if (workout.completed) "mark_pending" else "mark_done", lang),
                selected = workout.completed,
                onClick = onToggle
            )
        }
    }
}

@Composable
private fun BlockRow(block: TrainingBlock, lang: Lang, onExerciseClick: (ExerciseSet) -> Unit) {
    val label = when (block.type) {
        BlockType.WARM_UP -> t("warm_up", lang)
        BlockType.STRENGTH -> t("strength", lang)
        BlockType.CIRCUIT -> t("circuit", lang)
        BlockType.CORE -> t("core", lang)
        BlockType.COOL_DOWN -> t("cool_down", lang)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = AccentOrange, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        block.exercises.forEach { ex ->
            val amount = when {
                ex.reps != null -> "${ex.sets}x${ex.reps} ${t("reps", lang)}"
                ex.seconds != null -> "${ex.sets}x${ex.seconds}${t("seconds", lang)}"
                else -> "${ex.sets} ${t(if (ex.sets == 1) "set" else "sets", lang)}"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExerciseClick(ex) }
                    .padding(vertical = 12.dp)
            ) {
                Icon(Icons.Filled.PlayCircleOutline, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(t(ex.name, lang), color = TextPrimary, fontSize = 15.sp)
                    Text("$amount · ${ex.restSeconds}s ${t("rest", lang)}", color = TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}
