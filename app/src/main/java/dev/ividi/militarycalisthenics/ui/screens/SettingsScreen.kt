package dev.ividi.militarycalisthenics.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.components.SectionCard
import dev.ividi.militarycalisthenics.ui.components.SelectableChip
import dev.ividi.militarycalisthenics.ui.t
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.BgPanel2
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    lang: Lang,
    onLangChange: (Lang) -> Unit,
    onResetProfile: () -> Unit,
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = AccentOrange)
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t("edit_profile", lang), color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    SelectableChip(t("edit_profile", lang), selected = false, onClick = onResetProfile)
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
        Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = AccentOrange)
    }
}
