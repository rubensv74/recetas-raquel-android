package com.rmm.recetasraquel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Neutral visual identifier for the EU Annex II safety groups used by the app.
 *
 * The pictogram identifies the group only; it intentionally does not encode
 * severity, certainty or clinical risk by colour. Those meanings belong to the
 * recipe safety presentation state and its accompanying text.
 */
@Composable
fun SafetyGroupPictogram(
    safetyGroupId: String,
    safetyGroupName: String,
    modifier: Modifier = Modifier,
) {
    val foreground = MaterialTheme.colorScheme.onSecondaryContainer
    val background = MaterialTheme.colorScheme.secondaryContainer
    val kind = safetyGroupPictogramKind(safetyGroupId)

    Surface(
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = "Grupo de seguridad alimentaria: $safetyGroupName" },
        shape = CircleShape,
        color = background,
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (kind) {
                SafetyGroupPictogramKind.SULPHITES -> Text(
                    text = "SO₂",
                    color = foreground,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )

                SafetyGroupPictogramKind.UNKNOWN -> Text(
                    text = "?",
                    color = foreground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                else -> Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    drawSafetyGroupSymbol(kind, foreground)
                }
            }
        }
    }
}

private enum class SafetyGroupPictogramKind {
    GLUTEN,
    CRUSTACEANS,
    EGGS,
    FISH,
    PEANUTS,
    SOYBEANS,
    MILK,
    NUTS,
    CELERY,
    MUSTARD,
    SESAME,
    SULPHITES,
    LUPIN,
    MOLLUSCS,
    UNKNOWN,
}

private fun safetyGroupPictogramKind(safetyGroupId: String): SafetyGroupPictogramKind = when (safetyGroupId) {
    "sg-eu-cereals-gluten" -> SafetyGroupPictogramKind.GLUTEN
    "sg-eu-crustaceans" -> SafetyGroupPictogramKind.CRUSTACEANS
    "sg-eu-eggs" -> SafetyGroupPictogramKind.EGGS
    "sg-eu-fish" -> SafetyGroupPictogramKind.FISH
    "sg-eu-peanuts" -> SafetyGroupPictogramKind.PEANUTS
    "sg-eu-soybeans" -> SafetyGroupPictogramKind.SOYBEANS
    "sg-eu-milk" -> SafetyGroupPictogramKind.MILK
    "sg-eu-nuts" -> SafetyGroupPictogramKind.NUTS
    "sg-eu-celery" -> SafetyGroupPictogramKind.CELERY
    "sg-eu-mustard" -> SafetyGroupPictogramKind.MUSTARD
    "sg-eu-sesame" -> SafetyGroupPictogramKind.SESAME
    "sg-eu-sulphites" -> SafetyGroupPictogramKind.SULPHITES
    "sg-eu-lupin" -> SafetyGroupPictogramKind.LUPIN
    "sg-eu-molluscs" -> SafetyGroupPictogramKind.MOLLUSCS
    else -> SafetyGroupPictogramKind.UNKNOWN
}

private fun DrawScope.drawSafetyGroupSymbol(
    kind: SafetyGroupPictogramKind,
    color: Color,
) {
    val w = size.width
    val h = size.height
    val stroke = (size.minDimension * 0.075f).coerceAtLeast(1f)
    val line = Stroke(width = stroke, cap = StrokeCap.Round)

    when (kind) {
        SafetyGroupPictogramKind.GLUTEN -> {
            drawLine(color, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.9f), stroke, StrokeCap.Round)
            listOf(0.28f, 0.44f, 0.60f, 0.76f).forEachIndexed { index, y ->
                val left = index % 2 == 0
                val cx = if (left) w * 0.34f else w * 0.66f
                drawOval(
                    color = color,
                    topLeft = Offset(cx - w * 0.12f, h * y - h * 0.075f),
                    size = Size(w * 0.24f, h * 0.15f),
                )
            }
        }

        SafetyGroupPictogramKind.CRUSTACEANS -> {
            drawOval(
                color = color,
                topLeft = Offset(w * 0.28f, h * 0.30f),
                size = Size(w * 0.42f, h * 0.34f),
                style = line,
            )
            drawCircle(color, radius = w * 0.045f, center = Offset(w * 0.61f, h * 0.40f))
            drawArc(color, 195f, 110f, false, Offset(w * 0.08f, h * 0.38f), Size(w * 0.36f, h * 0.36f), style = line)
            drawArc(color, 220f, 80f, false, Offset(w * 0.52f, h * 0.12f), Size(w * 0.34f, h * 0.30f), style = line)
            drawLine(color, Offset(w * 0.34f, h * 0.62f), Offset(w * 0.20f, h * 0.80f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.48f, h * 0.64f), Offset(w * 0.43f, h * 0.86f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.60f, h * 0.61f), Offset(w * 0.72f, h * 0.79f), stroke, StrokeCap.Round)
        }

        SafetyGroupPictogramKind.EGGS -> {
            val egg = Path().apply {
                moveTo(w * 0.50f, h * 0.10f)
                cubicTo(w * 0.30f, h * 0.22f, w * 0.20f, h * 0.54f, w * 0.24f, h * 0.72f)
                cubicTo(w * 0.28f, h * 0.90f, w * 0.72f, h * 0.90f, w * 0.76f, h * 0.72f)
                cubicTo(w * 0.80f, h * 0.54f, w * 0.70f, h * 0.22f, w * 0.50f, h * 0.10f)
                close()
            }
            drawPath(egg, color, style = line)
        }

        SafetyGroupPictogramKind.FISH -> {
            drawOval(
                color = color,
                topLeft = Offset(w * 0.18f, h * 0.30f),
                size = Size(w * 0.52f, h * 0.40f),
                style = line,
            )
            val tail = Path().apply {
                moveTo(w * 0.70f, h * 0.50f)
                lineTo(w * 0.90f, h * 0.30f)
                lineTo(w * 0.90f, h * 0.70f)
                close()
            }
            drawPath(tail, color, style = line)
            drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.34f, h * 0.43f))
        }

        SafetyGroupPictogramKind.PEANUTS -> {
            drawOval(color, Offset(w * 0.19f, h * 0.16f), Size(w * 0.36f, h * 0.48f), style = line)
            drawOval(color, Offset(w * 0.45f, h * 0.36f), Size(w * 0.36f, h * 0.48f), style = line)
            drawLine(color, Offset(w * 0.40f, h * 0.36f), Offset(w * 0.60f, h * 0.64f), stroke * 0.75f, StrokeCap.Round)
            drawCircle(color, radius = w * 0.03f, center = Offset(w * 0.35f, h * 0.38f))
            drawCircle(color, radius = w * 0.03f, center = Offset(w * 0.65f, h * 0.61f))
        }

        SafetyGroupPictogramKind.SOYBEANS -> {
            val pod = Path().apply {
                moveTo(w * 0.14f, h * 0.54f)
                cubicTo(w * 0.26f, h * 0.18f, w * 0.70f, h * 0.16f, w * 0.86f, h * 0.44f)
                cubicTo(w * 0.74f, h * 0.78f, w * 0.30f, h * 0.84f, w * 0.14f, h * 0.54f)
                close()
            }
            drawPath(pod, color, style = line)
            listOf(0.34f, 0.51f, 0.68f).forEach { x ->
                drawCircle(color, radius = w * 0.075f, center = Offset(w * x, h * 0.51f))
            }
        }

        SafetyGroupPictogramKind.MILK -> {
            val carton = Path().apply {
                moveTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.62f, h * 0.18f)
                lineTo(w * 0.78f, h * 0.34f)
                lineTo(w * 0.78f, h * 0.84f)
                lineTo(w * 0.26f, h * 0.84f)
                lineTo(w * 0.26f, h * 0.30f)
                close()
            }
            drawPath(carton, color, style = line)
            drawLine(color, Offset(w * 0.28f, h * 0.30f), Offset(w * 0.67f, h * 0.30f), stroke, StrokeCap.Round)
            drawLine(color, Offset(w * 0.62f, h * 0.18f), Offset(w * 0.62f, h * 0.30f), stroke, StrokeCap.Round)
        }

        SafetyGroupPictogramKind.NUTS -> {
            drawOval(color, Offset(w * 0.26f, h * 0.32f), Size(w * 0.48f, h * 0.52f), style = line)
            drawArc(color, 200f, 140f, false, Offset(w * 0.21f, h * 0.17f), Size(w * 0.58f, h * 0.38f), style = line)
            drawLine(color, Offset(w * 0.50f, h * 0.18f), Offset(w * 0.56f, h * 0.08f), stroke, StrokeCap.Round)
        }

        SafetyGroupPictogramKind.CELERY -> {
            listOf(0.36f, 0.50f, 0.64f).forEach { x ->
                drawLine(color, Offset(w * x, h * 0.82f), Offset(w * x, h * 0.30f), stroke, StrokeCap.Round)
            }
            drawOval(color, Offset(w * 0.16f, h * 0.14f), Size(w * 0.34f, h * 0.24f), style = line)
            drawOval(color, Offset(w * 0.50f, h * 0.10f), Size(w * 0.34f, h * 0.24f), style = line)
        }

        SafetyGroupPictogramKind.MUSTARD -> {
            drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.34f, h * 0.55f), style = line)
            drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.55f, h * 0.62f), style = line)
            drawCircle(color, radius = w * 0.10f, center = Offset(w * 0.62f, h * 0.40f), style = line)
            val leaf = Path().apply {
                moveTo(w * 0.22f, h * 0.33f)
                cubicTo(w * 0.30f, h * 0.12f, w * 0.55f, h * 0.12f, w * 0.57f, h * 0.28f)
                cubicTo(w * 0.44f, h * 0.38f, w * 0.30f, h * 0.42f, w * 0.22f, h * 0.33f)
                close()
            }
            drawPath(leaf, color, style = line)
        }

        SafetyGroupPictogramKind.SESAME -> {
            val seeds = listOf(
                Offset(w * 0.35f, h * 0.40f),
                Offset(w * 0.58f, h * 0.34f),
                Offset(w * 0.52f, h * 0.62f),
            )
            seeds.forEachIndexed { index, center ->
                drawOval(
                    color = color,
                    topLeft = Offset(center.x - w * 0.09f, center.y - h * 0.15f),
                    size = Size(w * 0.18f, h * 0.30f),
                    style = line,
                )
                if (index == 1) {
                    drawLine(color, Offset(center.x - w * 0.03f, center.y - h * 0.08f), Offset(center.x + w * 0.03f, center.y + h * 0.08f), stroke * 0.65f, StrokeCap.Round)
                }
            }
        }

        SafetyGroupPictogramKind.LUPIN -> {
            drawLine(color, Offset(w * 0.50f, h * 0.88f), Offset(w * 0.50f, h * 0.22f), stroke, StrokeCap.Round)
            listOf(
                Offset(w * 0.39f, h * 0.34f), Offset(w * 0.61f, h * 0.34f),
                Offset(w * 0.36f, h * 0.48f), Offset(w * 0.64f, h * 0.48f),
                Offset(w * 0.40f, h * 0.62f), Offset(w * 0.60f, h * 0.62f),
            ).forEach { center ->
                drawOval(
                    color = color,
                    topLeft = Offset(center.x - w * 0.09f, center.y - h * 0.06f),
                    size = Size(w * 0.18f, h * 0.12f),
                )
            }
            drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.20f))
        }

        SafetyGroupPictogramKind.MOLLUSCS -> {
            val shell = Path().apply {
                moveTo(w * 0.18f, h * 0.72f)
                cubicTo(w * 0.20f, h * 0.30f, w * 0.80f, h * 0.30f, w * 0.82f, h * 0.72f)
                lineTo(w * 0.18f, h * 0.72f)
                close()
            }
            drawPath(shell, color, style = line)
            listOf(0.34f, 0.50f, 0.66f).forEach { x ->
                drawLine(color, Offset(w * 0.50f, h * 0.72f), Offset(w * x, h * 0.38f), stroke * 0.7f, StrokeCap.Round)
            }
        }

        SafetyGroupPictogramKind.SULPHITES,
        SafetyGroupPictogramKind.UNKNOWN,
        -> Unit
    }
}
