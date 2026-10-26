package ru.familycinema.app

import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.height
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import ru.familycinema.app.library.*

class TvLayoutTest {
    private val ui = createAndroidComposeRule<MainActivity>()
    private val clean =
        object : ExternalResource() {
            override fun before() {
                val ctx = InstrumentationRegistry.getInstrumentation().targetContext
                ctx.getSharedPreferences("family_private", 0).edit().clear().commit()
                runBlocking(Dispatchers.IO) { CinemaDatabase.get(ctx).clearAllTables() }
            }
        }
    @get:Rule val chain: RuleChain = RuleChain.outerRule(clean).around(ui)

    private fun screen(expected: Screen) {
        ui.waitUntil(90000) {
            var ok = false
            ui.runOnUiThread { ok = ui.activity.cinema.screen == expected }
            ok
        }
        ui.waitForIdle()
    }

    private fun pin() {
        "1234".forEach { ui.onNodeWithTag("pin_$it").performClick() }
        ui.onNodeWithTag("pin_confirm").performClick()
    }

    private fun capture(name: String) {
        ui.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        ui.waitUntil(15000) {
            i.uiAutomation.rootInActiveWindow?.packageName?.toString() == "ru.familycinema.app"
        }
        assertEquals("ru.familycinema.app", i.uiAutomation.rootInActiveWindow?.packageName?.toString())
        val bitmap = i.uiAutomation.takeScreenshot() ?: error("No screenshot")
        File(ui.activity.filesDir, "v03-tv-$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun fullyVisible(id: String) {
        val node = ui.onNodeWithTag("work_$id")
        val whole = node.getUnclippedBoundsInRoot()
        val clipped = node.getBoundsInRoot()
        assertEquals(
            "Полная высота TV-карточки должна помещаться в видимой области",
            whole.height.value,
            clipped.height.value,
            1f,
        )
        ui.onNodeWithText(ui.activity.getString(R.string.continue_play)).assertIsDisplayed()
    }

    @Test
    fun longTitleAndResumeCardFitTvViewport() {
        assertEquals(
            Configuration.UI_MODE_TYPE_TELEVISION,
            ui.activity.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK,
        )
        screen(Screen.PIN)
        pin()
        pin()
        screen(Screen.PARENT)
        val model = ui.activity.cinema
        val repo = model.library.repo
        val title = "Тёплый семейный вечер — личное видео с очень длинным русским названием"
        ui.runOnUiThread {
            model.prepareImport(title, 6, false, 1, 1)
            model.acceptPickedFile(Uri.parse("content://ru.familycinema.app.test.media/clip"))
        }
        screen(Screen.SOURCES)
        ui.waitUntil(90000) {
            var done = false
            ui.runOnUiThread { done = model.library.ready && !model.library.busy }
            done
        }
        val work = runBlocking { repo.dao.works().first { it.title == title } }
        val version = runBlocking { repo.dao.versions(work.id).single() }
        runBlocking {
            repo.decide(version, true)
            repo.dao.progress(Progress(versionId = version.id, position = 12345))
        }
        ui.runOnUiThread {
            model.openLibrary()
            model.library.query = "Теплый"
        }
        screen(Screen.LIBRARY)
        ui.waitUntil(90000) {
            var done = false
            ui.runOnUiThread {
                done = !model.library.busy && work.id in model.library.inProgressWorkIds
            }
            done
        }
        ui.waitForIdle()
        fullyVisible(work.id)
        capture("library")
        ui.runOnUiThread { model.childMode() }
        screen(Screen.CHILD)
        ui.waitUntil(90000) {
            var done = false
            ui.runOnUiThread {
                done =
                    model.library.works.any { it.id == work.id } &&
                        work.id in model.library.inProgressWorkIds
            }
            done
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)
        ui.onNodeWithTag("work_${work.id}").performSemanticsAction(SemanticsActions.RequestFocus)
        ui.waitForIdle()
        fullyVisible(work.id)
        capture("child")
        capture("card-fit")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
        screen(Screen.WORK)
    }
}
