package ru.familycinema.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.familycinema.app.CinemaViewModel
import ru.familycinema.app.R

@Composable
fun PlayerControls(model: CinemaViewModel, playing: Boolean, tv: Boolean) {
    val focus = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val position = model.position
    val duration = model.duration
    var seeking by remember { mutableStateOf(false) }
    var preview by remember { mutableFloatStateOf(0f) }
    DisposableEffect(model) { onDispose { model.holdControls(false) } }
    LaunchedEffect(model.controlsVisible, windowFocused) {
        if (model.controlsVisible && windowFocused) {
            withFrameNanos {}
            runCatching { focus.requestFocus() }
        }
    }
    AnimatedVisibility(
        visible = model.controlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize().testTag("player_controls"),
    ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
            Row(
                Modifier.align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xD9000000), Color.Transparent))
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(onClick = model::back, modifier = Modifier.cinemaFocus().size(52.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.exit_video),
                        tint = Color.White,
                    )
                }
                Text(
                    model.film.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000)))
                    )
                    .padding(horizontal = if (tv) 40.dp else 24.dp, vertical = 12.dp)
            ) {
                val description = stringResource(R.string.seek_position)
                Slider(
                    value =
                        if (seeking) preview
                        else position.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = {
                        seeking = true
                        preview = it
                        model.holdControls(true)
                    },
                    onValueChangeFinished = {
                        model.seekTo(preview.toLong())
                        seeking = false
                        model.holdControls(false)
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier =
                        Modifier.fillMaxWidth().semantics { contentDescription = description },
                    colors =
                        SliderDefaults.colors(
                            thumbColor = CinemaTokens.Gold,
                            activeTrackColor = CinemaTokens.Gold,
                            inactiveTrackColor = Color(0xFF68727C),
                        ),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        formatTime(if (seeking) preview.toLong() else position),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (tv) 28.dp else 16.dp),
                    ) {
                        IconButton(
                            onClick = { model.seekBy(-10000) },
                            modifier = Modifier.cinemaFocus().size(56.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Replay10,
                                stringResource(R.string.seek_back),
                                Modifier.size(32.dp),
                                tint = Color.White,
                            )
                        }
                        FilledIconButton(
                            onClick = model::togglePlayback,
                            modifier =
                                Modifier.focusRequester(focus)
                                    .cinemaFocus()
                                    .size(if (tv) 72.dp else 60.dp),
                            shape = CircleShape,
                            colors =
                                IconButtonDefaults.filledIconButtonColors(
                                    containerColor = CinemaTokens.Gold,
                                    contentColor = CinemaTokens.Night,
                                ),
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                stringResource(if (playing) R.string.pause else R.string.play),
                                Modifier.size(32.dp),
                            )
                        }
                        IconButton(
                            onClick = { model.seekBy(10000) },
                            modifier = Modifier.cinemaFocus().size(56.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Forward10,
                                stringResource(R.string.seek_forward),
                                Modifier.size(32.dp),
                                tint = Color.White,
                            )
                        }
                    }
                    Text(
                        formatTime(duration),
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long) =
    "%d:%02d"
        .format(milliseconds.coerceAtLeast(0) / 60000, (milliseconds.coerceAtLeast(0) / 1000) % 60)
