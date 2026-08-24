package dev.ividi.militarycalisthenics.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ividi.militarycalisthenics.ui.theme.AccentOrange
import dev.ividi.militarycalisthenics.ui.theme.AccentYellow
import dev.ividi.militarycalisthenics.ui.theme.BgBase
import dev.ividi.militarycalisthenics.ui.theme.TextDim
import dev.ividi.militarycalisthenics.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(1600)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulse)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(AccentOrange, AccentYellow))),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    val w = size.width
                    val h = size.height
                    val chevron = Path().apply {
                        moveTo(w * 0.53f, h * 0.06f)
                        lineTo(w * 0.86f, h * 0.40f)
                        lineTo(w * 0.72f, h * 0.40f)
                        lineTo(w * 0.53f, h * 0.20f)
                        lineTo(w * 0.34f, h * 0.40f)
                        lineTo(w * 0.20f, h * 0.40f)
                        close()
                    }
                    drawPath(chevron, color = BgBase)
                    drawRect(
                        color = BgBase,
                        topLeft = Offset(w * 0.30f, h * 0.50f),
                        size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.10f)
                    )
                    drawRect(
                        color = BgBase,
                        topLeft = Offset(w * 0.16f, h * 0.42f),
                        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.26f)
                    )
                    drawRect(
                        color = BgBase,
                        topLeft = Offset(w * 0.74f, h * 0.42f),
                        size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.26f)
                    )
                }
            }
            Text(
                text = "MILITARY CALISTHENICS",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 2.sp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text("Developed by David Arsénio Martins", color = TextDim, fontSize = 13.sp)
                Text("ividi.dev", color = AccentOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
