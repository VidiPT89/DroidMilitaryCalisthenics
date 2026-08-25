package dev.ividi.militarycalisthenics.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.ividi.militarycalisthenics.model.FitnessLevel
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary

/**
 * Shown when the user finishes the final week of their plan. Offers the two
 * paths the plan engine already supports without re-onboarding: repeating
 * the same level or moving to the next one. See
 * docs/plan-engine-spec.md "Plan completion".
 */
@Composable
fun PlanCompleteDialog(
    lang: Lang,
    nextLevel: FitnessLevel?,
    onRepeat: () -> Unit,
    onLevelUp: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = t("plan_complete_title", lang),
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = t("plan_complete_subtitle", lang),
                    color = TextDim,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                if (nextLevel != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SelectableChip(
                            text = "${t("plan_complete_level_up", lang)} ${t(levelKey(nextLevel), lang)}",
                            selected = true,
                            onClick = onLevelUp
                        )
                        Text(t("plan_complete_level_up_subtitle", lang), color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                } else {
                    Text(
                        text = t("plan_complete_max_level", lang),
                        color = TextDim,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SelectableChip(
                        text = t("plan_complete_repeat", lang),
                        selected = nextLevel == null,
                        onClick = onRepeat
                    )
                    Text(t("plan_complete_repeat_subtitle", lang), color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                Text(
                    text = t("plan_complete_dismiss", lang),
                    color = TextDim,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(onClick = onDismiss)
                )
            }
        }
    }
}

private fun levelKey(level: FitnessLevel): String = when (level) {
    FitnessLevel.BEGINNER -> "level_beginner"
    FitnessLevel.INTERMEDIATE -> "level_intermediate"
    FitnessLevel.ADVANCED -> "level_advanced"
}
