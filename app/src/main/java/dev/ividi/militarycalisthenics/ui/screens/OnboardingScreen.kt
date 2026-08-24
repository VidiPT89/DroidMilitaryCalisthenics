package dev.ividi.militarycalisthenics.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ividi.militarycalisthenics.model.Equipment
import dev.ividi.militarycalisthenics.model.FitnessLevel
import dev.ividi.militarycalisthenics.model.Goal
import dev.ividi.militarycalisthenics.model.Sex
import dev.ividi.militarycalisthenics.model.UserProfile
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.PrimaryButton
import dev.ividi.militarycalisthenics.ui.components.SectionCard
import dev.ividi.militarycalisthenics.ui.components.SelectableChip
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary

@Composable
fun OnboardingScreen(lang: Lang, onSubmit: (UserProfile) -> Unit) {
    var weight by remember { mutableStateOf("75") }
    var height by remember { mutableStateOf("175") }
    var age by remember { mutableStateOf("28") }
    var sex by remember { mutableStateOf(Sex.UNSPECIFIED) }
    var level by remember { mutableStateOf(FitnessLevel.INTERMEDIATE) }
    var goal by remember { mutableStateOf(Goal.MILITARY_ENDURANCE) }
    var days by remember { mutableStateOf(4) }
    var equipment by remember { mutableStateOf(setOf(Equipment.BODYWEIGHT_ONLY)) }
    var error by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = TextDim,
        cursorColor = AccentOrange,
        focusedLabelColor = AccentOrange,
        unfocusedLabelColor = TextDim
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t("onboarding_title", lang), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Text(t("onboarding_subtitle", lang), color = TextDim, fontSize = 14.sp)
            }
        }

        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(t("weight", lang)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = height, onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(t("height", lang)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = age, onValueChange = { age = it.filter { c -> c.isDigit() } },
                        label = { Text(t("age", lang)) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors, modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            LabeledChipGroup(t("sex", lang)) {
                SelectableChip(t("sex_male", lang), sex == Sex.MALE) { sex = Sex.MALE }
                SelectableChip(t("sex_female", lang), sex == Sex.FEMALE) { sex = Sex.FEMALE }
                SelectableChip(t("sex_unspecified", lang), sex == Sex.UNSPECIFIED) { sex = Sex.UNSPECIFIED }
            }
        }

        item {
            LabeledChipGroup(t("level", lang)) {
                SelectableChip(t("level_beginner", lang), level == FitnessLevel.BEGINNER) { level = FitnessLevel.BEGINNER }
                SelectableChip(t("level_intermediate", lang), level == FitnessLevel.INTERMEDIATE) { level = FitnessLevel.INTERMEDIATE }
                SelectableChip(t("level_advanced", lang), level == FitnessLevel.ADVANCED) { level = FitnessLevel.ADVANCED }
            }
        }

        item {
            LabeledChipGroup(t("goal", lang)) {
                SelectableChip(t("goal_fat_loss", lang), goal == Goal.FAT_LOSS) { goal = Goal.FAT_LOSS }
                SelectableChip(t("goal_strength_mass", lang), goal == Goal.STRENGTH_MASS) { goal = Goal.STRENGTH_MASS }
                SelectableChip(t("goal_military_endurance", lang), goal == Goal.MILITARY_ENDURANCE) { goal = Goal.MILITARY_ENDURANCE }
                SelectableChip(t("goal_mobility", lang), goal == Goal.MOBILITY) { goal = Goal.MOBILITY }
            }
        }

        item {
            LabeledChipGroup(t("days_per_week", lang)) {
                (3..6).forEach { d ->
                    SelectableChip(d.toString(), days == d) { days = d }
                }
            }
        }

        item {
            LabeledChipGroup(t("equipment", lang)) {
                SelectableChip(t("equipment_bodyweight", lang), equipment.contains(Equipment.BODYWEIGHT_ONLY)) {
                    equipment = setOf(Equipment.BODYWEIGHT_ONLY)
                }
                SelectableChip(t("equipment_bar", lang), equipment.contains(Equipment.PULL_UP_BAR)) {
                    equipment = (equipment - Equipment.BODYWEIGHT_ONLY) + Equipment.PULL_UP_BAR
                }
                SelectableChip(t("equipment_parallettes", lang), equipment.contains(Equipment.PARALLETTES)) {
                    equipment = (equipment - Equipment.BODYWEIGHT_ONLY) + Equipment.PARALLETTES
                }
            }
        }

        error?.let { message ->
            item { Text(message, color = dev.ividi.militarycalisthenics.ui.theme.ColorError, fontSize = 13.sp) }
        }

        item {
            PrimaryButton(t("generate_plan", lang), modifier = Modifier.fillMaxWidth()) {
                val w = weight.toDoubleOrNull()
                val h = height.toDoubleOrNull()
                val a = age.toIntOrNull()
                if (w == null || h == null || a == null ||
                    w !in UserProfile.WEIGHT_RANGE || h !in UserProfile.HEIGHT_RANGE || a !in UserProfile.AGE_RANGE
                ) {
                    error = t("error_range", lang)
                    return@PrimaryButton
                }
                error = null
                onSubmit(
                    UserProfile(
                        weightKg = w, heightCm = h, age = a, sex = sex, level = level,
                        goal = goal, daysPerWeek = days,
                        equipment = if (equipment.isEmpty()) setOf(Equipment.BODYWEIGHT_ONLY) else equipment
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabeledChipGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}
