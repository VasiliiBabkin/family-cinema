package ru.familycinema.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.familycinema.domain.Film
import ru.familycinema.parental.FamilyStore
import ru.familycinema.player.Playback

data class WindowPresentation(
    val orientation: Int,
    val statusVisible: Boolean,
    val navigationVisible: Boolean,
    val behavior: Int,
)

enum class Screen {
    CHILD,
    PARENT,
    PIN,
    DETAILS,
    PLAYER,
}

enum class PinError {
    NONE,
    SHORT,
    WRONG,
    MISMATCH,
    LOCKED,
}

class CinemaViewModel(application: Application) : AndroidViewModel(application) {
    val store = FamilyStore(application)
    val film: Film = DemoCatalog.films().first()
    var screen by mutableStateOf(if (store.hasPin) Screen.CHILD else Screen.PIN)
        private set

    var parent by mutableStateOf(false)
        private set

    var approved by mutableStateOf(store.approved(film))
        private set

    var position by mutableLongStateOf(store.position(film.id))
        private set

    var pin by mutableStateOf("")
        private set

    var confirmation by mutableStateOf<String?>(null)
        private set

    var pinError by mutableStateOf(PinError.NONE)
        private set

    var pinBusy by mutableStateOf(false)
        private set

    var presentation: WindowPresentation? = null
    var renderedFrames by mutableIntStateOf(0)
        private set

    var playing by mutableStateOf(false)
        private set

    var duration by mutableLongStateOf(40000L)
        private set

    var controlsVisible by mutableStateOf(true)
        private set

    private var controlsTimer: kotlinx.coroutines.Job? = null
    private var scrubbing = false

    private fun scheduleAutoHide() {
        controlsTimer?.cancel()
        controlsTimer = null
        if (playing && controlsVisible && foreground && !scrubbing) {
            controlsTimer =
                viewModelScope.launch {
                    delay(3500)
                    if (playing && foreground && !scrubbing) controlsVisible = false
                }
        }
    }

    fun showControls() {
        controlsVisible = true
        scheduleAutoHide()
    }

    fun hideControls() {
        controlsTimer?.cancel()
        controlsVisible = false
    }

    fun holdControls(held: Boolean) {
        scrubbing = held
        showControls()
    }

    fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    fun togglePlayback() {
        player?.let {
            if (it.isPlaying) it.pause()
            else {
                if (it.playbackState == Player.STATE_ENDED) it.seekTo(0)
                it.play()
            }
        }
        showControls()
    }

    fun seekTo(value: Long) {
        player?.let {
            it.seekTo(value.coerceIn(0L, duration - 1))
            checkpoint()
        }
        showControls()
    }

    fun seekBy(delta: Long) {
        player?.let {
            it.seekTo(
                (it.currentPosition + delta).coerceIn(
                    0L,
                    (it.duration.takeIf { d -> d > 0 } ?: 40000L) - 1,
                )
            )
            checkpoint()
        }
        showControls()
    }

    var foreground = false
    var player by mutableStateOf<ExoPlayer?>(null)
        private set

    var verificationFailed by mutableStateOf(false)
        private set

    var playbackFailed by mutableStateOf(false)
        private set

    private var opening: kotlinx.coroutines.Job? = null

    fun openParents() {
        clearPin()
        screen = Screen.PIN
    }

    fun childMode() {
        closePlayer()
        parent = false
        clearPin()
        screen = Screen.CHILD
    }

    fun details() {
        if (parent || approved) screen = Screen.DETAILS
    }

    fun back() {
        when (screen) {
            Screen.PLAYER -> {
                closePlayer()
                screen = if (parent || approved) Screen.DETAILS else Screen.CHILD
            }
            Screen.DETAILS -> screen = if (parent) Screen.PARENT else Screen.CHILD
            Screen.PIN -> {
                clearPin()
                screen = Screen.CHILD
            }
            Screen.PARENT -> childMode()
            Screen.CHILD -> Unit
        }
    }

    fun setApproval(value: Boolean) {
        if (!parent) return
        store.approve(film, value)
        approved = store.approved(film)
    }

    fun digit(value: String) {
        if (!pinBusy && pin.length < 8) {
            pin += value
            pinError = PinError.NONE
        }
    }

    fun erasePin() {
        if (!pinBusy) pin = pin.dropLast(1)
    }

    fun clearPin() {
        pin = ""
        confirmation = null
        pinError = PinError.NONE
    }

    fun confirmPin() {
        if (pinBusy) return
        if (System.currentTimeMillis() < store.lockedUntil) {
            pinError = PinError.LOCKED
            return
        }
        if (pin.length < 4) {
            pinError = PinError.SHORT
            return
        }
        if (!store.hasPin && confirmation == null) {
            confirmation = pin
            pin = ""
            return
        }
        if (!store.hasPin && confirmation != pin) {
            clearPin()
            pinError = PinError.MISMATCH
            return
        }
        val entered = pin
        pinBusy = true
        viewModelScope.launch {
            val valid =
                withContext(Dispatchers.Default) {
                    if (!store.hasPin) {
                        store.setPin(entered)
                        true
                    } else store.verify(entered)
                }
            pinBusy = false
            pin = ""
            if (valid) {
                store.failedAttempts = 0
                store.lockedUntil = 0
                confirmation = null
                if (foreground && screen == Screen.PIN) {
                    parent = true
                    screen = Screen.PARENT
                }
            } else {
                store.failedAttempts++
                if (store.failedAttempts >= 5) {
                    store.lockedUntil = System.currentTimeMillis() + 60000
                    store.failedAttempts = 0
                }
                pinError = PinError.WRONG
            }
        }
    }

    fun play() {
        if ((!parent && !approved) || screen == Screen.PLAYER) return
        showControls()
        renderedFrames = 0
        screen = Screen.PLAYER
        verificationFailed = false
        playbackFailed = false
        opening?.cancel()
        opening =
            viewModelScope.launch {
                val verified =
                    withContext(Dispatchers.IO) {
                        runCatching {
                                val digest = MessageDigest.getInstance("SHA-256")
                                getApplication<Application>()
                                    .assets
                                    .open("quiet-evening.mp4")
                                    .use { input ->
                                        val bytes = ByteArray(8192)
                                        while (true) {
                                            val count = input.read(bytes)
                                            if (count < 0) break
                                            digest.update(bytes, 0, count)
                                        }
                                    }
                                digest.digest().joinToString("") { "%02x".format(it) } ==
                                    film.version.sha256
                            }
                            .getOrDefault(false)
                    }
                if (!verified) {
                    verificationFailed = true
                    return@launch
                }
                if (screen != Screen.PLAYER || (!parent && !approved)) return@launch
                player =
                    Playback.create(getApplication(), film.version.uri, store.position(film.id))
                        .apply {
                            playWhenReady = foreground
                            addListener(
                                object : Player.Listener {
                                    override fun onPlayerError(
                                        error: androidx.media3.common.PlaybackException
                                    ) {
                                        playbackFailed = true
                                    }

                                    override fun onRenderedFirstFrame() {
                                        renderedFrames++
                                    }

                                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                                        this@CinemaViewModel.playing = isPlaying
                                        scheduleAutoHide()
                                    }

                                    override fun onTimelineChanged(
                                        timeline: androidx.media3.common.Timeline,
                                        reason: Int,
                                    ) {
                                        this@CinemaViewModel.duration =
                                            this@CinemaViewModel.player?.duration?.takeIf { it > 0 }
                                                ?: 40000L
                                    }

                                    override fun onPlaybackStateChanged(playbackState: Int) {
                                        this@CinemaViewModel.duration =
                                            this@CinemaViewModel.player?.duration?.takeIf { it > 0 }
                                                ?: 40000L
                                        if (playbackState == Player.STATE_ENDED) {
                                            position = 0
                                            store.savePosition(film.id, 0)
                                            showControls()
                                        }
                                    }
                                }
                            )
                        }
                playing = player?.isPlaying == true
                duration = player?.duration?.takeIf { it > 0 } ?: 40000L
                scheduleAutoHide()
            }
    }

    fun checkpoint() {
        player?.let {
            if (playing != it.isPlaying) {
                playing = it.isPlaying
                scheduleAutoHide()
            }
            duration = it.duration.takeIf { d -> d > 0 } ?: 40000L
            position =
                if (it.playbackState == Player.STATE_ENDED) 0
                else it.currentPosition.coerceAtLeast(0)
        }
        store.savePosition(film.id, position)
    }

    private fun closePlayer() {
        opening?.cancel()
        opening = null
        checkpoint()
        player?.release()
        player = null
        playing = false
        controlsTimer?.cancel()
        controlsTimer = null
        scrubbing = false
    }

    fun onBackground(changingConfiguration: Boolean) {
        if (changingConfiguration) return
        foreground = false
        checkpoint()
        player?.pause()
        controlsTimer?.cancel()
        scrubbing = false
        clearPin()
        if (parent) {
            parent = false
            closePlayer()
            screen = Screen.CHILD
        }
    }

    override fun onCleared() {
        closePlayer()
    }
}
