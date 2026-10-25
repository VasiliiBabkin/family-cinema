package ru.familycinema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familycinema.app.*
import ru.familycinema.app.R

@Composable
fun PinScreen(model: CinemaViewModel, tv: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(16.dp)) {
        val wide = maxWidth > 650.dp
        val intro: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 8.dp)) {
                Icon(
                    Icons.Rounded.Lock,
                    null,
                    Modifier.size(if (wide) 58.dp else 40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(if (wide) 15.dp else 10.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(
                        if (model.confirmation != null) R.string.pin_repeat
                        else if (model.store.hasPin) R.string.pin_login else R.string.pin_create
                    ),
                    style =
                        if (wide) MaterialTheme.typography.headlineLarge
                        else MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(
                        if (model.confirmation != null) R.string.pin_repeat_body
                        else if (model.store.hasPin) R.string.pin_login_body
                        else R.string.pin_create_body
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (wide)
                    NightArtwork(
                        Modifier.width(280.dp)
                            .height(if (tv) 100.dp else 180.dp)
                            .clip(CinemaTokens.Corners),
                        true,
                    )
            }
        }
        val keypad: @Composable () -> Unit = {
            Column(
                Modifier.widthIn(max = 380.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (wide && !tv) 8.dp else 4.dp),
            ) {
                val count = stringResource(R.string.pin_count, model.pin.length)
                Row(
                    Modifier.height(if (wide && !tv) 42.dp else 28.dp).clearAndSetSemantics {
                        contentDescription = count
                    },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(8) { i ->
                        Box(
                            Modifier.size(if (i < model.pin.length) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < model.pin.length) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = .45f)
                                )
                        )
                    }
                }
                val error =
                    when (model.pinError) {
                        PinError.SHORT -> R.string.pin_short
                        PinError.WRONG -> R.string.pin_wrong
                        PinError.MISMATCH -> R.string.pin_mismatch
                        PinError.LOCKED -> R.string.pin_locked
                        else -> null
                    }
                if (error != null)
                    Text(
                        stringResource(error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("clear", "0", "erase"),
                    )
                    .forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { key ->
                                FilledTonalButton(
                                    onClick = {
                                        when (key) {
                                            "erase" -> model.erasePin()
                                            "clear" -> model.clearPin()
                                            else -> model.digit(key)
                                        }
                                    },
                                    enabled = !model.pinBusy,
                                    modifier =
                                        Modifier.testTag("pin_$key")
                                            .initialFocus(tv && key == "1")
                                            .size(if (tv) 64.dp else if (wide) 66.dp else 60.dp)
                                            .cinemaFocus(),
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    when (key) {
                                        "erase" ->
                                            Icon(
                                                Icons.AutoMirrored.Rounded.Backspace,
                                                stringResource(R.string.pin_delete),
                                            )
                                        "clear" ->
                                            Icon(
                                                Icons.Rounded.Close,
                                                stringResource(R.string.pin_clear),
                                            )
                                        else -> Text(key, fontSize = if (tv) 27.sp else 24.sp)
                                    }
                                }
                            }
                        }
                    }
                CinemaAction(
                    stringResource(if (model.pinBusy) R.string.checking else R.string.confirm),
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    Modifier.fillMaxWidth().testTag("pin_confirm"),
                    enabled = !model.pinBusy,
                ) {
                    model.confirmPin()
                }
                Text(
                    stringResource(R.string.pin_keep),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (wide)
            Row(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) { intro() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    keypad()
                }
            }
        else
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                intro()
                keypad()
            }
    }
}
