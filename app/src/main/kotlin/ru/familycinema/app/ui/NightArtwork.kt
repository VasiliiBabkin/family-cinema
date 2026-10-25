package ru.familycinema.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import ru.familycinema.app.R

/** Original vector illustration; authored for this project, CC0. No remote image assets. */
@Composable
fun NightArtwork(modifier: Modifier = Modifier, decorative: Boolean = false) {
    val description = stringResource(R.string.art_description)
    Canvas(
        modifier.then(
            if (decorative) Modifier.clearAndSetSemantics {}
            else Modifier.semantics { contentDescription = description }
        )
    ) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF243654), Color(0xFF376765), Color(0xFF689982)))
        )
        val stars =
            listOf(
                .10f to .13f,
                .24f to .26f,
                .44f to .12f,
                .57f to .26f,
                .86f to .13f,
                .90f to .34f,
                .37f to .36f,
            )
        stars.forEachIndexed { i, (x, y) ->
            drawCircle(
                Color(0xFFF6DFC0).copy(alpha = .6f),
                if (i % 2 == 0) 3f else 2f,
                Offset(w * x, h * y),
            )
        }
        drawCircle(Color(0xFFFFE8AC), w * .12f, Offset(w * .74f, h * .24f))
        drawCircle(Color(0xFF29445B), w * .108f, Offset(w * .79f, h * .207f))
        fun hill(y: Float, color: Color, bend: Float) {
            val p =
                Path().apply {
                    moveTo(0f, h * y)
                    cubicTo(w * .35f, h * (y - bend), w * .65f, h * (y + bend), w, h * (y - .02f))
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
            drawPath(p, color)
        }
        hill(.65f, Color(0xFF477F75), .30f)
        hill(.77f, Color(0xFF2D625D), .13f)
        listOf(.08f, .18f, .88f, .96f).forEachIndexed { i, x ->
            val top = h * (.45f + i % 2 * .10f)
            val bottom = h * .86f
            val half = w * .055f
            val p =
                Path().apply {
                    moveTo(w * x, top)
                    lineTo(w * x - half, bottom)
                    lineTo(w * x + half, bottom)
                    close()
                }
            drawPath(p, Color(0xFF244D4C))
            drawLine(Color(0xFF244D4C), Offset(w * x, bottom), Offset(w * x, h * .92f), w * .012f)
        }
        // A little shelter at the heart of the meadow.
        val cx = w * .53f
        val cy = h * .69f
        drawRoundRect(
            Color(0xFFC8A780),
            Offset(cx - w * .10f, cy),
            Size(w * .20f, h * .17f),
            CornerRadius(8f),
        )
        drawPath(
            Path().apply {
                moveTo(cx - w * .14f, cy)
                lineTo(cx, cy - h * .15f)
                lineTo(cx + w * .14f, cy)
                close()
            },
            Color(0xFF273F4A),
        )
        drawRoundRect(
            Color(0xFFFFD184),
            Offset(cx - w * .032f, cy + h * .035f),
            Size(w * .064f, h * .085f),
            CornerRadius(5f),
        )
        drawLine(Color(0xFF9A7857), Offset(cx, cy + h * .035f), Offset(cx, cy + h * .12f), 2f)
        hill(.91f, Color(0xFF173E40), .08f)
        // Rabbit, drawn from simple vector shapes rather than a stock character.
        val rx = w * .28f
        val ry = h * .88f
        drawOval(
            Color(0xFFF7E6CF),
            Offset(rx - w * .026f, ry - h * .105f),
            Size(w * .020f, h * .09f),
        )
        drawOval(
            Color(0xFFF7E6CF),
            Offset(rx + w * .004f, ry - h * .12f),
            Size(w * .020f, h * .10f),
        )
        drawOval(Color(0xFFF7E6CF), Offset(rx - w * .04f, ry - h * .035f), Size(w * .08f, h * .07f))
        drawCircle(Color(0xFF263D3D), w * .004f, Offset(rx + w * .014f, ry - h * .008f))
        drawCircle(Color(0xFFF9CD7E), w * .008f, Offset(w * .72f, h * .84f))
    }
}
