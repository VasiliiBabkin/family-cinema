package ru.familycinema.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.ui.PlayerView
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class FamilyFlowTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val clean =
        object : ExternalResource() {
            override fun before() {
                kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    ru.familycinema.app.library.CinemaDatabase.get(
                            InstrumentationRegistry.getInstrumentation().targetContext
                        )
                        .clearAllTables()
                }
                InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getSharedPreferences("family_private", 0)
                    .edit()
                    .clear()
                    .commit()
            }
        }
    @get:Rule val chain: RuleChain = RuleChain.outerRule(clean).around(ui)
    private val device
        get() = InstrumentationRegistry.getArguments().getString("layout", "phone")

    private fun tag(name: String) = ui.onNodeWithTag(name)

    private fun click(name: String) {
        val node = tag(name)
        // PIN and detail actions may be inside a scroll container; native player is not.
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {}
        node.performClick()
        ui.waitForIdle()
    }

    private fun pin(value: String) {
        value.forEach { click("pin_$it") }
        click("pin_confirm")
    }

    private fun awaitScreen(screen: Screen) {
        ui.waitUntil(90000) {
            var result = false
            ui.runOnUiThread { result = ui.activity.cinema.screen == screen }
            result
        }
        ui.waitForIdle()
    }

    private fun capture(name: String) {
        val instrument = InstrumentationRegistry.getInstrumentation()
        ui.waitForIdle()
        instrument.waitForIdleSync()
        android.os.SystemClock.sleep(600)
        runCatching { instrument.uiAutomation.waitForIdle(200, 5000) }
        instrument.uiAutomation.takeScreenshot()?.let { bitmap ->
            if (name == "player-clean") {
                val color = bitmap.getPixel(bitmap.width / 2, bitmap.height / 3)
                assertTrue(
                    "После пересоздания должен отображаться кадр видео, а не чёрный экран",
                    android.graphics.Color.red(color) +
                        android.graphics.Color.green(color) +
                        android.graphics.Color.blue(color) > 30,
                )
            }
            File(ui.activity.filesDir, "$device-$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } ?: error("Не удалось получить screenshot")
    }

    private fun playerView(view: View): PlayerView? {
        if (view is PlayerView) return view
        if (view is ViewGroup)
            for (i in 0 until view.childCount) playerView(view.getChildAt(i))?.let {
                return it
            }
        return null
    }

    private fun decoded(): Boolean {
        var result = false
        ui.runOnUiThread {
            val p = ui.activity.cinema.player
            result = p != null && p.videoSize.width == 960 && p.currentPosition > 1200
        }
        return result
    }

    private fun key(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        ui.waitForIdle()
    }

    @Test
    fun nativeFamilyFlowFullscreenRotationAndResume() {
        awaitScreen(Screen.PIN)
        val actualTv =
            ui.activity.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
                Configuration.UI_MODE_TYPE_TELEVISION
        if (device == "tv") {
            assertTrue("TV screenshot требует настоящего TV-образа", actualTv)
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            ui.runOnUiThread { assertEquals("2", ui.activity.cinema.pin) }
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            ui.runOnUiThread { assertEquals("25", ui.activity.cinema.pin) }
            key(KeyEvent.KEYCODE_DPAD_LEFT)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            ui.runOnUiThread { assertEquals("254", ui.activity.cinema.pin) }
            key(KeyEvent.KEYCODE_DPAD_UP)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            ui.runOnUiThread { assertEquals("2541", ui.activity.cinema.pin) }
            click("pin_clear")
        }
        if (device == "tablet")
            assertTrue(ui.activity.resources.configuration.smallestScreenWidthDp >= 600)
        capture("pin")
        pin("1234")
        pin("1234")
        awaitScreen(Screen.PARENT)
        capture("parent")
        click("action_details")
        awaitScreen(Screen.DETAILS)
        capture("parent-details")
        click("action_approve")
        capture("approval")
        click("confirm_approval")
        click("exit_parent")
        awaitScreen(Screen.CHILD)
        capture("child-catalog")
        click("action_details")
        awaitScreen(Screen.DETAILS)
        capture("child-details")
        val phone =
            ui.activity.resources.configuration.smallestScreenWidthDp < 600 && device != "tv"
        if (phone)
            ui.runOnUiThread {
                ui.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        click("action_play")
        awaitScreen(Screen.PLAYER)
        ui.waitUntil(90000) { decoded() }
        if (phone)
            ui.runOnUiThread {
                assertEquals(
                    Configuration.ORIENTATION_LANDSCAPE,
                    ui.activity.resources.configuration.orientation,
                )
            }
        ui.runOnUiThread {
            val p = ui.activity.cinema.player!!
            File(ui.activity.filesDir, "$device-playback-diagnostic.txt")
                .writeText(
                    "state=${p.playbackState} playing=${p.isPlaying} ready=${p.playWhenReady} suppression=${p.playbackSuppressionReason} position=${p.currentPosition} duration=${p.duration} foreground=${ui.activity.cinema.foreground}\n"
                )
            // Cold software TV emulation can consume the 40 s fixture while the UI settles.
            // Establish active playback explicitly before checking the inactivity timeout.
            p.seekTo(2000)
            p.play()
            ui.activity.cinema.showControls()
        }
        ui.waitUntil(15000) {
            var hidden = false
            ui.runOnUiThread { hidden = !ui.activity.cinema.controlsVisible }
            hidden
        }
        tag("player_controls").assertDoesNotExist()
        capture("player-autohide")
        val instrument = InstrumentationRegistry.getInstrumentation()
        var touchX = 0f
        var touchY = 0f
        ui.runOnUiThread {
            touchX = ui.activity.window.decorView.width * .85f
            touchY = ui.activity.window.decorView.height * .30f
        }
        val touchTime = android.os.SystemClock.uptimeMillis()
        for (action in
            listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
            val event =
                android.view.MotionEvent.obtain(
                    touchTime,
                    android.os.SystemClock.uptimeMillis(),
                    action,
                    touchX,
                    touchY,
                    0,
                )
            instrument.sendPointerSync(event)
            event.recycle()
        }
        ui.waitUntil(15000) {
            var visible = false
            ui.runOnUiThread { visible = ui.activity.cinema.controlsVisible }
            visible
        }
        var original: androidx.media3.exoplayer.ExoPlayer? = null
        ui.runOnUiThread {
            original = ui.activity.cinema.player
            original!!.pause()
            val native = playerView(ui.activity.window.decorView)!!
            assertEquals(
                androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,
                native.resizeMode,
            )
            assertEquals(ui.activity.window.decorView.width, native.width)
            assertEquals(ui.activity.window.decorView.height, native.height)
            ui.activity.cinema.showControls()
        }
        capture("player-controls")
        var beforeSeek = 0L
        ui.runOnUiThread { beforeSeek = ui.activity.cinema.player!!.currentPosition }
        ui.onNodeWithContentDescription(ui.activity.getString(R.string.seek_forward)).performClick()
        ui.waitUntil(15000) {
            var moved = false
            ui.runOnUiThread {
                moved = ui.activity.cinema.player!!.currentPosition >= beforeSeek + 9000
            }
            moved
        }
        ui.onNodeWithContentDescription(ui.activity.getString(R.string.seek_back)).performClick()
        ui.waitUntil(15000) {
            var moved = false
            ui.runOnUiThread {
                moved = ui.activity.cinema.player!!.currentPosition <= beforeSeek + 1000
            }
            moved
        }
        if (phone) {
            ui.runOnUiThread {
                ui.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            ui.waitForIdle()
            ui.runOnUiThread { assertSame(original, ui.activity.cinema.player) }
        }
        // Configuration recreation must retain one player and the exact playback session.
        var oldFrames = 0
        ui.runOnUiThread { oldFrames = ui.activity.cinema.renderedFrames }
        ui.activityRule.scenario.recreate()
        ui.waitUntil(30000) {
            var rendered = false
            ui.runOnUiThread { rendered = ui.activity.cinema.renderedFrames > oldFrames }
            rendered
        }
        ui.waitUntil(90000) { decoded() }
        ui.runOnUiThread {
            assertSame(original, ui.activity.cinema.player)
            ui.activity.cinema.hideControls()
        }
        ui.waitForIdle()
        capture("player-clean")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        ui.waitUntil(10000) {
            var visible = false
            ui.runOnUiThread { visible = ui.activity.cinema.controlsVisible }
            visible
        }
        tag("player_controls").assertIsDisplayed()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        ui.waitUntil(15000) {
            var active = false
            ui.runOnUiThread { active = ui.activity.cinema.playing }
            active
        }
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        ui.waitUntil(15000) {
            var stopped = false
            ui.runOnUiThread { stopped = !ui.activity.cinema.playing }
            stopped
        }
        key(KeyEvent.KEYCODE_BACK)
        awaitScreen(Screen.PLAYER) // First Back hides controls.
        key(KeyEvent.KEYCODE_BACK)
        awaitScreen(Screen.DETAILS)
        var saved = 0L
        ui.runOnUiThread {
            assertNull(ui.activity.cinema.player)
            saved = ui.activity.cinema.store.position("quiet-evening")
            assertTrue(saved > 1000)
            if (phone)
                assertEquals(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                    ui.activity.requestedOrientation,
                )
        }
        click("action_play")
        awaitScreen(Screen.PLAYER)
        ui.waitUntil(90000) {
            var ok = false
            ui.runOnUiThread { ok = (ui.activity.cinema.player?.currentPosition ?: 0) >= saved }
            ok
        }
        // onStop must pause and checkpoint, child playback must not unlock the parent.
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ui.runOnUiThread {
            assertFalse(ui.activity.cinema.player!!.playWhenReady)
            assertFalse(ui.activity.cinema.parent)
        }
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        ui.runOnUiThread {
            assertFalse(ui.activity.cinema.player!!.playWhenReady)
            ui.activity.cinema.hideControls()
        }
        key(KeyEvent.KEYCODE_BACK)
        awaitScreen(Screen.DETAILS)
        click("open_parents")
        awaitScreen(Screen.PIN)
        pin("9999")
        ui.waitUntil(90000) {
            var wrong = false
            ui.runOnUiThread { wrong = ui.activity.cinema.pinError == PinError.WRONG }
            wrong
        }
        pin("1234")
        awaitScreen(Screen.PARENT)
        click("action_details")
        click("action_approve")
        click("confirm_approval")
        click("exit_parent")
        awaitScreen(Screen.CHILD)
        capture("empty")
        ui.runOnUiThread {
            assertFalse(ui.activity.cinema.approved)
            assertFalse(ui.activity.cinema.store.approved(ui.activity.cinema.film))
        }
    }

    @Test
    fun pinLockoutAndParentBackgroundProtection() {
        pin("1234")
        pin("1234")
        awaitScreen(Screen.PARENT)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        awaitScreen(Screen.CHILD)
        click("open_parents")
        awaitScreen(Screen.PIN)
        repeat(5) {
            pin("0000")
            ui.waitUntil(90000) {
                var busy = true
                ui.runOnUiThread { busy = ui.activity.cinema.pinBusy }
                !busy
            }
        }
        ui.runOnUiThread {
            assertTrue(ui.activity.cinema.store.lockedUntil > System.currentTimeMillis())
            assertFalse(ui.activity.cinema.parent)
        }
        pin("1234")
        ui.runOnUiThread {
            assertEquals(PinError.LOCKED, ui.activity.cinema.pinError)
            assertFalse(ui.activity.cinema.parent)
        }
        capture("pin-locked")
    }
}
