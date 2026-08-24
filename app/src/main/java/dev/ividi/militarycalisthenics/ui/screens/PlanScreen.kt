package dev.ividi.militarycalisthenics.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.ividi.militarycalisthenics.model.TrainingBlock
import dev.ividi.militarycalisthenics.model.TrainingPlan
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.CompletionBadge
import dev.ividi.militarycalisthenics.ui.components.ExerciseDetailDialog
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
    onToggleCompleted: (weekIndex: Int, dayIndex: Int) -> Unit,
    onOpenSettings: () -> Unit
) {
    var selectedWeek by remember { mutableIntStateOf(0) }
    var demoExercise by remember { mutableStateOf<ExerciseSet?>(null) }
    val week = plan.weeks.getOrNull(selectedWeek) ?: plan.weeks.first()
    val completedCount = week.workouts.count { it.completed }
    val progress = if (week.workouts.isEmpty()) 0f else completedCount.toFloat() / week.workouts.size
    val context = LocalContext.current

    demoExercise?.let { exercise ->
        ExerciseDetailDialog(exercise = exercise, lang = lang, onDismiss = { demoExercise = null })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("your_plan", lang), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Row {
                IconButton(onClick = { sharePlanAsPdf(context, week, lang) }) {
                    Icon(Icons.Filled.Share, contentDescription = t("share_plan", lang), tint = AccentOrange)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = t("settings", lang), tint = AccentOrange)
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(plan.weeks) { w ->
                SelectableChip("${t("week", lang)} ${w.weekIndex + 1}", w.weekIndex == selectedWeek) {
                    selectedWeek = w.weekIndex
                }
            }
        }

        SectionCard(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ProgressRing(progress = progress)
                Column {
                    Text("${t("week", lang)} ${week.weekIndex + 1}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("$completedCount / ${week.workouts.size}", color = TextDim, fontSize = 14.sp)
                }
            }
        }

        AnimatedContent(
            targetState = selectedWeek,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "weekContent"
        ) { _ ->
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(week.workouts) { workout ->
                    WorkoutCard(
                        workout = workout,
                        lang = lang,
                        onToggle = { onToggleCompleted(week.weekIndex, workout.dayIndex) },
                        onExerciseClick = { demoExercise = it }
                    )
                }
                item { Column(Modifier.padding(bottom = 24.dp)) {} }
            }
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: DailyWorkout,
    lang: Lang,
    onToggle: () -> Unit,
    onExerciseClick: (ExerciseSet) -> Unit
) {
    val context = LocalContext.current
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("${t("day", lang)} ${workout.dayIndex + 1}", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(workout.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shareAsText(context, workout.title, workout.toShareText(lang)) }) {
                        Icon(Icons.Filled.Share, contentDescription = t("share_plan", lang), tint = AccentOrange, modifier = Modifier.size(18.dp))
                    }
                    CompletionBadge(completed = workout.completed, lang = lang)
                }
            }

            workout.blocks.forEach { block -> BlockRow(block, lang, onExerciseClick) }

            SelectableChip(
                text = t("mark_done", lang),
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
                else -> "${ex.sets} ${t("sets", lang)}"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExerciseClick(ex) }
                    .padding(vertical = 2.dp)
            ) {
                Icon(Icons.Filled.PlayCircleOutline, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                Text("${ex.name} — $amount", color = TextDim, fontSize = 13.sp)
            }
        }
    }
}
