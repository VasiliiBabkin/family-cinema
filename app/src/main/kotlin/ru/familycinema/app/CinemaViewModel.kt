package ru.familycinema.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    LIBRARY,
    SOURCES,
    POLICY,
    WORK,
}

enum class PinError {
    NONE,
    SHORT,
    WRONG,
    MISMATCH,
    LOCKED,
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class CinemaViewModel(application: Application) : AndroidViewModel(application) {
    val store = FamilyStore(application)
    var film by mutableStateOf(DemoCatalog.films().first())
        private set

    var screen by mutableStateOf(if (store.hasPin) Screen.CHILD else Screen.PIN)
        private set

    var parent by mutableStateOf(false)
        private set

    val autoApprove
        get() = library.profile.autoApprove

    fun setAutoApprove(value: Boolean) {
        if (parent) library.setPolicy(library.profile.ageLimit, value)
    }

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
    private var libraryPlayback = false
    private var previewOnly = false
    val library =
        ru.familycinema.app.library.LibraryController(application, viewModelScope, { parent }) {
            selected,
            allowed,
            saved ->
            if (parent || allowed) {
                film = selected
                approved = allowed
                position = saved
                previewOnly = parent
                libraryPlayback = true
                play()
            }
        }

    fun openLibrary(target: Screen = Screen.LIBRARY) {
        if (target != Screen.LIBRARY && !parent) return
        screen = target
        library.reload()
        library.updateHistoryIndex()
    }

    fun openWork(work: ru.familycinema.app.library.Work) {
        library.select(work)
        screen = Screen.WORK
    }

    private data class PendingImport(
        val title: String,
        val age: Int?,
        val series: Boolean,
        val season: Int,
        val number: Int,
        val started: Long,
    )

    private var pendingImport: PendingImport? = null
    private var pickedUri: android.net.Uri? = null

    fun prepareImport(title: String, age: Int?, series: Boolean, season: Int, number: Int) {
        if (parent)
            pendingImport =
                PendingImport(
                    title,
                    age,
                    series,
                    season,
                    number,
                    android.os.SystemClock.elapsedRealtime(),
                )
    }

    fun acceptPickedFile(uri: android.net.Uri?) {
        if (uri == null || pendingImport == null) {
            pendingImport = null
            return
        }
        pickedUri = uri
        if (parent) finishImport() else openParents()
    }

    private fun finishImport() {
        val request = pendingImport ?: return
        val uri = pickedUri ?: return
        pendingImport = null
        pickedUri = null
        if (!parent) return
        if (android.os.SystemClock.elapsedRealtime() - request.started > 300000) {
            screen = Screen.SOURCES
            library.pickerExpired()
            return
        }
        library.import(
            uri,
            request.title,
            request.age,
            request.series,
            request.season,
            request.number,
        )
        screen = Screen.SOURCES
    }

    private fun resetDemo() {
        libraryPlayback = false
        previewOnly = false
        film = DemoCatalog.films().first()
        approved = store.approved(film)
        position = store.position(film.id)
    }

    fun openParents() {
        clearPin()
        screen = Screen.PIN
    }

    fun childMode() {
        closePlayer()
        library.lock()
        resetDemo()
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
                screen =
                    if (libraryPlayback) Screen.WORK
                    else if (parent || approved) Screen.DETAILS else Screen.CHILD
            }
            Screen.DETAILS -> screen = if (parent) Screen.PARENT else Screen.CHILD
            Screen.PIN -> {
                pendingImport = null
                pickedUri = null
                clearPin()
                screen = Screen.CHILD
            }
            Screen.PARENT -> childMode()
            Screen.WORK -> screen = Screen.LIBRARY
            Screen.SOURCES,
            Screen.POLICY -> screen = Screen.PARENT
            Screen.LIBRARY -> {
                resetDemo()
                screen = if (parent) Screen.PARENT else Screen.CHILD
            }
            Screen.CHILD -> Unit
        }
    }

    fun setApproval(value: Boolean) {
        if (!parent) return
        store.approve(film, value)
        approved = store.approved(film)
        viewModelScope.launch {
            library.repo.syncDemo(value)
            library.refresh()
        }
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
                    library.reload()
                    finishImport()
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
                    runCatching {
                            if (parent && film.version.sha256.isBlank()) {
                                ru.familycinema.sources.SafeNetwork.validate(film.version.uri)
                                true
                            } else library.repo.verifyFilm(film)
                        }
                        .getOrDefault(false)
                if (!verified) {
                    verificationFailed = true
                    return@launch
                }
                if (screen != Screen.PLAYER || (!parent && !approved)) return@launch
                player =
                    Playback.create(
                            getApplication(),
                            film.version.uri,
                            if (libraryPlayback) position else store.position(film.id),
                        )
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
                                            if (!previewOnly) store.savePosition(film.id, 0)
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
        if (!previewOnly) {
            store.savePosition(film.id, position)
            library.checkpoint(film.id, position)
        }
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
        library.lock(clearSelection = parent)
        checkpoint()
        player?.pause()
        controlsTimer?.cancel()
        scrubbing = false
        clearPin()
        if (parent) {
            parent = false
            closePlayer()
            resetDemo()
            screen = Screen.CHILD
        }
    }

    override fun onCleared() {
        closePlayer()
    }
}
