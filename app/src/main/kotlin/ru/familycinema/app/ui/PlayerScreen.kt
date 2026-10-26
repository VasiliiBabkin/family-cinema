package ru.familycinema.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.familycinema.app.*
import ru.familycinema.app.R

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(model: CinemaViewModel, tv: Boolean) {
    val activity = LocalContext.current as Activity
    val configuration = LocalConfiguration.current
    val phone = !tv && configuration.smallestScreenWidthDp < 600
    val lifecycle = LocalLifecycleOwner.current
    val player = model.player
    val playing = model.playing
    DisposableEffect(activity) {
        val oldCallback = activity.window.callback
        val keys = PlayerKeys()
        val keyCallback =
            object : android.view.Window.Callback by oldCallback {
                override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
                    if (
                        keys.handle(
                            event,
                            model.controlsVisible,
                            model.playing,
                            model::showControls,
                            model::togglePlayback,
                            model::seekBy,
                        )
                    )
                        return true
                    return oldCallback.dispatchKeyEvent(event)
                }
            }
        activity.window.callback = keyCallback
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        val insets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        val snapshot =
            model.presentation
                ?: WindowPresentation(
                        activity.requestedOrientation,
                        insets?.isVisible(WindowInsetsCompat.Type.statusBars()) != false,
                        insets?.isVisible(WindowInsetsCompat.Type.navigationBars()) != false,
                        controller.systemBarsBehavior,
                    )
                    .also { model.presentation = it }
        if (phone) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            if (activity.window.callback === keyCallback) activity.window.callback = oldCallback
            if (!activity.isChangingConfigurations) {
                controller.systemBarsBehavior = snapshot.behavior
                if (snapshot.statusVisible) controller.show(WindowInsetsCompat.Type.statusBars())
                else controller.hide(WindowInsetsCompat.Type.statusBars())
                if (snapshot.navigationVisible)
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                else controller.hide(WindowInsetsCompat.Type.navigationBars())
                if (phone) activity.requestedOrientation = snapshot.orientation
                model.presentation = null
            }
        }
    }
    LaunchedEffect(player, lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                while (true) {
                    delay(1000)
                    model.checkpoint()
                }
            }
        }
    }
    BackHandler {
        if (
            model.player != null &&
                model.controlsVisible &&
                !model.verificationFailed &&
                !model.playbackFailed
        )
            model.hideControls()
        else model.back()
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black).pointerInput(model) {
            detectTapGestures { model.toggleControls() }
        }
    ) {
        if (model.verificationFailed || model.playbackFailed) {
            Column(
                Modifier.align(Alignment.Center).widthIn(max = 520.dp).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Rounded.ErrorOutline,
                    null,
                    Modifier.size(48.dp),
                    tint = CinemaTokens.Gold,
                )
                Text(
                    stringResource(
                        if (model.verificationFailed) R.string.video_invalid
                        else R.string.video_error
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    stringResource(
                        if (model.verificationFailed) R.string.video_invalid_body
                        else R.string.video_error_body
                    ),
                    color = Color(0xFFCBD1D9),
                )
                CinemaAction(
                    stringResource(R.string.exit_video),
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = model::back,
                )
            }
        } else if (player == null) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CircularProgressIndicator(color = CinemaTokens.Gold)
                Text(stringResource(R.string.video_check), color = Color.White)
            }
        } else {
            AndroidView(
                factory = { context ->
                    ru.familycinema.player.CinemaVideoView(context).apply { bind(player) }
                },
                update = {
                    it.bind(player)
                    it.keepScreenOn = playing
                },
                onRelease = { it.unbind() },
                modifier = Modifier.fillMaxSize(),
            )
            PlayerControls(model, playing, tv)
        }
    }
}
