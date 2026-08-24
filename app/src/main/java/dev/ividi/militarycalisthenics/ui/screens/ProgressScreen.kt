package dev.ividi.militarycalisthenics.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ividi.militarycalisthenics.model.WeightEntry
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.PrimaryButton
import dev.ividi.militarycalisthenics.ui.components.SectionCard
import dev.ividi.militarycalisthenics.ui.components.WeightTrendChart
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.ColorOk
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
    lang: Lang,
    weightHistory: List<WeightEntry>,
    onLogWeight: (Double) -> Unit,
    onDeleteWeightEntry: (Long) -> Unit,
    onBack: () -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var justLogged by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("back", lang), tint = AccentOrange)
                }
                Text(t("progress", lang), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
        }

        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(t("log_weight", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(t("log_weight_subtitle", lang), color = TextDim, fontSize = 13.sp)
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it.filter { c -> c.isDigit() || c == '.' }; justLogged = false },
                        label = { Text(t("weight", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryButton(text = t("save", lang), modifier = Modifier.fillMaxWidth()) {
                        val value = weightInput.toDoubleOrNull()
                        if (value != null && value in 30.0..250.0) {
                            onLogWeight(value)
                            weightInput = ""
                            justLogged = true
                        }
                    }
                    if (justLogged) {
                        Text(t("plan_recalibrated", lang), color = ColorOk, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(t("weight_history", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (weightHistory.size < 2) {
                        Text(t("no_weight_history", lang), color = TextDim, fontSize = 13.sp)
                    } else {
                        WeightTrendChart(entries = weightHistory)
                    }
                }
            }
        }

        if (weightHistory.isNotEmpty()) {
            items(weightHistory.sortedByDescending { it.timestampMillis }, key = { it.timestampMillis }) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dateFormat.format(Date(entry.timestampMillis)), color = TextDim, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${entry.weightKg} kg", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        IconButton(onClick = { onDeleteWeightEntry(entry.timestampMillis) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = t("delete", lang), tint = TextDim, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
