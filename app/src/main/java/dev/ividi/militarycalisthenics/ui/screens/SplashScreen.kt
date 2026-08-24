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
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = w * 0.075f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Military rank star, top third.
                    val star = Path().apply {
                        val cx = w * 0.5f
                        val cy = h * 0.24f
                        val outerR = w * 0.16f
                        val innerR = w * 0.065f
                        for (i in 0 until 10) {
                            val angle = (-Math.PI / 2 + i * Math.PI / 5).toFloat()
                            val r = if (i % 2 == 0) outerR else innerR
                            val x = cx + r * kotlin.math.cos(angle)
                            val y = cy + r * kotlin.math.sin(angle)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(star, color = BgBase)

                    // Push-up figure, side view: head, torso to hip, bent support arm.
                    drawCircle(BgBase, radius = w * 0.09f, center = Offset(w * 0.28f, h * 0.55f))
                    drawLine(BgBase, Offset(w * 0.30f, h * 0.55f), Offset(w * 0.66f, h * 0.62f), strokeWidth = stroke.width, cap = stroke.cap)
                    drawLine(BgBase, Offset(w * 0.66f, h * 0.62f), Offset(w * 0.84f, h * 0.56f), strokeWidth = stroke.width, cap = stroke.cap)
                    drawLine(BgBase, Offset(w * 0.38f, h * 0.57f), Offset(w * 0.36f, h * 0.72f), strokeWidth = stroke.width, cap = stroke.cap)
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
