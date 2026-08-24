package dev.ividi.militarycalisthenics.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.ividi.militarycalisthenics.notifications.ReminderScheduler
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.SectionCard
import dev.ividi.militarycalisthenics.ui.components.SelectableChip
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.BgPanel2
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary
import dev.ividi.militarycalisthenics.ui.theme.ThemeMode

private val REMINDER_HOURS = listOf(6, 8, 12, 18, 20)

@Composable
fun SettingsScreen(
    lang: Lang,
    onLangChange: (Lang) -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onResetProfile: () -> Unit,
    onRegeneratePlan: () -> Unit,
    onOpenProgress: () -> Unit,
    onBack: () -> Unit,
    remindersEnabled: Boolean,
    reminderHour: Int,
    onRemindersChange: (enabled: Boolean, hour: Int) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            permissionDenied = false
            ReminderScheduler.schedule(context, reminderHour, lang = lang)
            onRemindersChange(true, reminderHour)
        } else {
            permissionDenied = true
        }
    }

    fun enableReminders(hour: Int) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            ReminderScheduler.schedule(context, hour, lang = lang)
            onRemindersChange(true, hour)
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun disableReminders() {
        ReminderScheduler.cancel(context)
        onRemindersChange(false, reminderHour)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("back", lang), tint = AccentOrange)
            }
            Text(t("settings", lang), color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }

        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(t("language", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectableChip("PT-PT", lang == Lang.PT) { onLangChange(Lang.PT) }
                        SelectableChip("EN", lang == Lang.EN) { onLangChange(Lang.EN) }
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(t("theme", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectableChip(t("theme_dark", lang), themeMode == ThemeMode.DARK) { onThemeModeChange(ThemeMode.DARK) }
                        SelectableChip(t("theme_light", lang), themeMode == ThemeMode.LIGHT) { onThemeModeChange(ThemeMode.LIGHT) }
                        SelectableChip(t("theme_system", lang), themeMode == ThemeMode.SYSTEM) { onThemeModeChange(ThemeMode.SYSTEM) }
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t("progress", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    SelectableChip(t("log_weight", lang), selected = false, onClick = onOpenProgress)
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t("reminders", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text(t("reminders_subtitle", lang), color = TextDim, fontSize = 12.sp)
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { checked -> if (checked) enableReminders(reminderHour) else disableReminders() },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentOrange)
                        )
                    }
                    if (remindersEnabled) {
                        Text(t("reminder_time", lang), color = TextDim, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            REMINDER_HOURS.forEach { hour ->
                                SelectableChip("%02d:00".format(hour), reminderHour == hour) {
                                    enableReminders(hour)
                                }
                            }
                        }
                    }
                    if (permissionDenied) {
                        Text(t("notifications_denied", lang), color = TextDim, fontSize = 12.sp)
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t("profile_section", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(t("regenerate_plan_subtitle", lang), color = TextDim, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SelectableChip(t("regenerate_plan", lang), selected = false, onClick = onRegeneratePlan)
                        SelectableChip(t("edit_profile", lang), selected = false, onClick = onResetProfile)
                    }
                }
            }

            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t("about", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(t("developed_by", lang), color = TextDim, fontSize = 14.sp)
                    LinkRow(t("website", lang), "https://ividi.dev/") { uriHandler.openUri("https://ividi.dev/") }
                    LinkRow(t("github", lang), "https://github.com/VidiPT89/") { uriHandler.openUri("https://github.com/VidiPT89/") }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgPanel2)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(url, color = TextDim, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AccentOrange)
    }
}
