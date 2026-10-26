package ru.familycinema.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CinemaTokens {
    val Night = Color(0xFF101A25)
    val Gold = Color(0xFFF4CF8E)
    val Cream = Color(0xFFFFF6E9)
    val Forest = Color(0xFF27584D)
    val Corners = RoundedCornerShape(28.dp)
    const val TransitionMs = 220
}

@Composable
fun CinemaTheme(dark: Boolean, tv: Boolean = false, content: @Composable () -> Unit) {
    val colors =
        if (dark)
            darkColorScheme(
                primary = CinemaTokens.Gold,
                secondaryContainer = Color(0xFF293B48),
                onSecondaryContainer = Color(0xFFF0E5CF),
                onPrimary = Color(0xFF30220C),
                background = CinemaTokens.Night,
                surface = CinemaTokens.Night,
                surfaceVariant = Color(0xFF202D3B),
                onSurface = Color(0xFFF4F2EE),
                onSurfaceVariant = Color(0xFFBFC8D3),
                outline = Color(0xFF8A9AAE),
            )
        else
            lightColorScheme(
                primary = CinemaTokens.Forest,
                secondaryContainer = Color(0xFFE1EBDD),
                onSecondaryContainer = CinemaTokens.Forest,
                onPrimary = Color.White,
                background = CinemaTokens.Cream,
                surface = CinemaTokens.Cream,
                surfaceVariant = Color(0xFFF3E7D2),
                onSurface = Color(0xFF252E2B),
                onSurfaceVariant = Color(0xFF566158),
                outline = Color(0xFF758178),
            )
    MaterialTheme(
        colorScheme = colors,
        typography =
            Typography(
                headlineLarge =
                    TextStyle(
                        fontFamily = if (dark) FontFamily.SansSerif else FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (tv) 40.sp else 34.sp,
                        lineHeight = if (tv) 46.sp else 39.sp,
                    ),
                headlineMedium =
                    TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                    ),
                titleLarge =
                    TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (tv) 28.sp else 23.sp,
                        lineHeight = if (tv) 34.sp else 29.sp,
                    ),
                bodyLarge =
                    TextStyle(
                        fontSize = if (tv) 20.sp else 16.sp,
                        lineHeight = if (tv) 28.sp else 24.sp,
                    ),
                bodyMedium =
                    TextStyle(
                        fontSize = if (tv) 18.sp else 14.sp,
                        lineHeight = if (tv) 26.sp else 21.sp,
                    ),
                labelLarge =
                    TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (tv) 18.sp else 14.sp,
                        lineHeight = if (tv) 24.sp else 20.sp,
                    ),
            ),
        content = content,
    )
}

@Composable
fun Modifier.cinemaFocus(): Modifier {
    var focused by remember { mutableStateOf(false) }
    val color = if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent
    return this.onFocusChanged { focused = it.isFocused }
        .border(if (focused) 3.dp else 0.dp, color, RoundedCornerShape(20.dp))
        .padding(4.dp)
}

@Composable
fun CinemaAction(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val body: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text)
    }
    if (secondary)
        OutlinedButton(
            onClick,
            modifier.cinemaFocus().heightIn(min = 56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            content = body,
        )
    else
        Button(
            onClick,
            modifier.cinemaFocus().heightIn(min = 56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            content = body,
        )
}

fun motionDuration() = if (ValueAnimator.areAnimatorsEnabled()) CinemaTokens.TransitionMs else 0

@Composable
fun Modifier.initialFocus(enabled: Boolean): Modifier {
    val requester = remember { androidx.compose.ui.focus.FocusRequester() }
    var requested by remember(enabled) { mutableStateOf(false) }
    // A lazy item may be detached before a delayed frame callback runs. Request only
    // from placement, while the actual focus target is attached to this layout.
    return this.focusRequester(requester).onGloballyPositioned {
        if (enabled && !requested) {
            requested = true
            requester.requestFocus()
        }
    }
}
