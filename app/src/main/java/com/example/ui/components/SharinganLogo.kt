package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.SharinganDarkRed
import com.example.ui.theme.SharinganGlow
import com.example.ui.theme.SharinganRed
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SharinganLogoImage(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isRotating: Boolean = false,
    showGlow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sharingan_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showGlow) {
                    Modifier.shadow(8.dp, CircleShape, spotColor = SharinganGlow)
                } else Modifier
            )
            .clip(CircleShape)
            .border(2.dp, SharinganRed, CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.aminos_logo),
            contentDescription = "AmInoS Naruto Sharingan Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .testTag("sharingan_logo_image")
                .then(if (isRotating) Modifier.rotate(rotation) else Modifier)
        )
    }
}

/**
 * Procedural Vector Drawing of the Iconic Naruto Sharingan Eye
 * with 3 Tomoe swirling symmetrically around the pupil.
 */
@Composable
fun NarutoSharinganEye(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isSpinning: Boolean = false,
    tomoeCount: Int = 3
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eye_spin")
    val angleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eye_rotation"
    )

    val currentAngle = if (isSpinning) angleOffset else 0f

    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, spotColor = SharinganGlow)
            .clip(CircleShape)
            .border(3.dp, Color.Black, CircleShape)
            .background(Color.Black)
            .testTag("naruto_sharingan_eye"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Crimson Iris Gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF1744),
                        SharinganRed,
                        SharinganDarkRed,
                        Color(0xFF4A0007)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Outer Iris Boundary Ring
            drawCircle(
                color = Color.Black,
                radius = radius * 0.96f,
                center = center,
                style = Stroke(width = radius * 0.08f)
            )

            // Inner Thin Orbit Ring Connecting Tomoe
            val orbitRadius = radius * 0.52f
            drawCircle(
                color = Color.Black.copy(alpha = 0.85f),
                radius = orbitRadius,
                center = center,
                style = Stroke(width = radius * 0.05f)
            )

            // Central Pupil
            val pupilRadius = radius * 0.22f
            drawCircle(
                color = Color.Black,
                radius = pupilRadius,
                center = center
            )

            // Draw Tomoe Marks
            val angleStep = (360f / tomoeCount)
            for (i in 0 until tomoeCount) {
                val rad = Math.toRadians((i * angleStep + currentAngle).toDouble())
                val tomoeCenter = Offset(
                    (center.x + orbitRadius * cos(rad)).toFloat(),
                    (center.y + orbitRadius * sin(rad)).toFloat()
                )

                val tomoeHeadRadius = radius * 0.13f
                drawCircle(
                    color = Color.Black,
                    radius = tomoeHeadRadius,
                    center = tomoeCenter
                )

                // Tomoe curved tail
                val tailAngle = rad + Math.toRadians(70.0)
                val tailTip = Offset(
                    (tomoeCenter.x + tomoeHeadRadius * 1.8f * cos(tailAngle)).toFloat(),
                    (tomoeCenter.y + tomoeHeadRadius * 1.8f * sin(tailAngle)).toFloat()
                )

                val tailPath = Path().apply {
                    moveTo(tomoeCenter.x, tomoeCenter.y)
                    quadraticTo(
                        (tomoeCenter.x + tomoeHeadRadius * 1.4f * cos(rad + Math.toRadians(35.0))).toFloat(),
                        (tomoeCenter.y + tomoeHeadRadius * 1.4f * sin(rad + Math.toRadians(35.0))).toFloat(),
                        tailTip.x,
                        tailTip.y
                    )
                    close()
                }
                drawPath(tailPath, Color.Black)
            }
        }
    }
}
