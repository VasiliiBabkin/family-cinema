package ru.familycinema.app

import android.content.res.Configuration
import android.net.Uri
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.*
import ru.familycinema.app.library.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class FlowV03Test {
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
    private val layout
        get() = InstrumentationRegistry.getArguments().getString("layout", "phone")

    private fun click(tag: String) {
        if (tag == "library_confirm")
            ui.waitUntil(15000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        if (
            tag.startsWith("version_") || ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        ) {
            ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasTestTag(tag))
        }
        val node = ui.onNodeWithTag(tag)
        try {
            node.performScrollTo()
        } catch (_: AssertionError) {}
        node.performClick()
        ui.waitForIdle()
    }

    private fun pin(s: String) {
        s.forEach { click("pin_$it") }
        click("pin_confirm")
    }

    private fun screen(s: Screen) {
        ui.waitUntil(90000) {
            var ok = false
            ui.runOnUiThread { ok = ui.activity.cinema.screen == s }
            ok
        }
        ui.waitForIdle()
    }

    private fun idle() {
        ui.waitUntil(90000) {
            var ready = false
            var role = false
            var ids = emptySet<String>()
            ui.runOnUiThread {
                val c = ui.activity.cinema.library
                ready = c.ready && !c.busy
                role = ui.activity.cinema.parent
                ids = c.works.map { it.id }.toSet()
            }
            ready &&
                ids ==
                    runBlocking {
                        ui.activity.cinema.library.repo.visible(role).map { it.id }.toSet()
                    }
        }
        ui.waitForIdle()
    }

    private fun capture(name: String) {
        ui.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        i.waitForIdleSync()
        android.os.SystemClock.sleep(500)
        runCatching { i.uiAutomation.waitForIdle(200, 5000) }
        val root = i.uiAutomation.rootInActiveWindow
        assertTrue(
            "Скриншот должен принадлежать приложению, а не системному ANR",
            root?.packageName?.toString() == "ru.familycinema.app" ||
                root?.packageName?.toString()?.contains("inputmethod") == true,
        )
        val bitmap = i.uiAutomation.takeScreenshot() ?: error("No screenshot")
        File(ui.activity.filesDir, "v03-$layout-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun key(code: Int) {
        ui.waitUntil(30000) {
            var focused = false
            ui.runOnUiThread { focused = ui.activity.hasWindowFocus() }
            focused
        }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        ui.waitForIdle()
    }

    @Test
    fun nativeLibraryImportApprovalSearchPlaybackAndPolicy() {
        screen(Screen.PIN)
        if (layout == "tv") {
            assertEquals(
                Configuration.UI_MODE_TYPE_TELEVISION,
                ui.activity.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK,
            )
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            ui.runOnUiThread { assertEquals("2", ui.activity.cinema.pin) }
            click("pin_clear")
        }
        capture("pin")
        pin("1234")
        pin("1234")
        screen(Screen.PARENT)
        idle()
        capture("parent")
        click("library_open")
        screen(Screen.LIBRARY)
        idle()
        ui.onNodeWithTag("library_search").performTextInput("tikhiy")
        capture("search")
        ui.onNodeWithTag("work_demo").assertExists()
        // Real content:// import, with the same re-authentication path used after system SAF.
        ui.runOnUiThread {
            ui.activity.cinema.prepareImport(
                "Тёплый семейный вечер — личное видео с очень длинным русским названием",
                6,
                false,
                1,
                1,
            )
            ui.activity.cinema.onBackground(false)
            ui.activity.cinema.foreground = true
            ui.activity.cinema.acceptPickedFile(
                Uri.parse("content://ru.familycinema.app.test.media/clip")
            )
        }
        screen(Screen.PIN)
        pin("1234")
        screen(Screen.SOURCES)
        idle()
        capture("sources")
        val repo = ui.activity.cinema.library.repo
        val imported = runBlocking {
            repo.dao.works().first {
                it.title == "Тёплый семейный вечер — личное видео с очень длинным русским названием"
            }
        }
        val version = runBlocking { repo.dao.versions(imported.id).single() }
        assertTrue(version.verified)
        assertEquals(GeneratedDemo.sha256, version.sha256)
        ui.runOnUiThread { ui.activity.cinema.openLibrary() }
        screen(Screen.LIBRARY)
        idle()
        ui.onNodeWithTag("library_search").performTextReplacement("Теплый")
        ui.onNodeWithTag("library_search").performImeAction()
        capture("library")
        if (layout == "tv") {
            ui.onNodeWithTag("work_${imported.id}")
                .performSemanticsAction(SemanticsActions.RequestFocus)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
        } else click("work_${imported.id}")
        screen(Screen.WORK)
        idle()
        capture("details")
        click("version_approve")
        click("library_confirm")
        idle()
        assertTrue(runBlocking { repo.allowed(version) })
        click("exit_parent")
        screen(Screen.CHILD)
        idle()
        capture("child")
        ui.runOnUiThread { ui.activity.cinema.library.setPolicy(21, true) }
        assertFalse(runBlocking { repo.profile().autoApprove })
        ui.onNodeWithTag("source_endpoint").assertDoesNotExist()
        click("work_${imported.id}")
        screen(Screen.WORK)
        idle()
        click("version_play")
        screen(Screen.PLAYER)
        ui.waitUntil(90000) {
            var ok = false
            ui.runOnUiThread {
                val p = ui.activity.cinema.player
                ok = p != null && p.videoSize.width == 960 && p.currentPosition > 1500
            }
            ok
        }
        ui.runOnUiThread {
            ui.activity.cinema.player!!.pause()
            ui.activity.cinema.showControls()
        }
        capture("player")
        ui.runOnUiThread { ui.activity.cinema.hideControls() }
        key(KeyEvent.KEYCODE_BACK)
        screen(Screen.WORK)
        ui.waitUntil(30000) {
            runBlocking { (repo.dao.progress(version.id)?.position ?: 0) > 1000 }
        }
        click("version_play")
        screen(Screen.PLAYER)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        ui.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        ui.runOnUiThread {
            assertFalse(ui.activity.cinema.player!!.playWhenReady)
            ui.activity.cinema.hideControls()
            assertNotNull(ui.activity.cinema.library.selected)
        }
        key(KeyEvent.KEYCODE_BACK)
        screen(Screen.WORK)
        click("open_parents")
        screen(Screen.PIN)
        pin("1234")
        screen(Screen.PARENT)
        ui.runOnUiThread { ui.activity.cinema.openLibrary(Screen.POLICY) }
        screen(Screen.POLICY)
        idle()
        capture("policy")
        click("policy_auto")
        click("library_confirm")
        idle()
        assertTrue(runBlocking { repo.profile().autoApprove })
        ui.runOnUiThread { ui.activity.cinema.openLibrary(Screen.SOURCES) }
        screen(Screen.SOURCES)
        idle()
        ui.onNodeWithTag("source_endpoint").performTextInput("https://127.0.0.1/manifest.json")
        click("source_rights")
        click("source_connect")
        idle()
        capture("source-error")
        assertTrue(runBlocking { repo.dao.sources().none { it.id != "local" } })
        ui.runOnUiThread { ui.activity.cinema.openWork(imported) }
        screen(Screen.WORK)
        idle()
        click("version_approve")
        click("library_confirm")
        idle()
        assertFalse(runBlocking { repo.allowed(version) })
        ui.runOnUiThread { ui.activity.cinema.openLibrary(Screen.SOURCES) }
        screen(Screen.SOURCES)
        idle()
        click("source_trust_local")
        click("library_confirm")
        idle()
        ui.runOnUiThread { ui.activity.cinema.library.sweepAuto() }
        idle()
        assertFalse(runBlocking { repo.allowed(version) })
        ui.runOnUiThread {
            ui.activity.cinema.prepareImport("Новый семейный вечер", 6, false, 1, 1)
            ui.activity.cinema.acceptPickedFile(
                Uri.parse("content://ru.familycinema.app.test.media/second")
            )
        }
        screen(Screen.SOURCES)
        idle()
        val automatic = runBlocking {
            val w = repo.dao.works().first { it.title == "Новый семейный вечер" }
            repo.dao.versions(w.id).single()
        }
        assertTrue(runBlocking { repo.allowed(automatic) })
        assertEquals("automatic", runBlocking { repo.dao.decision(automatic.id)!!.origin })
        ui.runOnUiThread { ui.activity.cinema.openLibrary(Screen.POLICY) }
        screen(Screen.POLICY)
        idle()
        capture("audit")
        click("policy_auto")
        idle()
        assertFalse(runBlocking { repo.profile().autoApprove })
        assertTrue(runBlocking { repo.allowed(automatic) })
        ui.runOnUiThread {
            ui.activity.cinema.prepareImport("Без новых авторазрешений", 6, false, 1, 1)
            ui.activity.cinema.acceptPickedFile(
                Uri.parse("content://ru.familycinema.app.test.media/second")
            )
        }
        screen(Screen.SOURCES)
        idle()
        val disabled = runBlocking {
            val w = repo.dao.works().first { it.title == "Без новых авторазрешений" }
            repo.dao.versions(w.id).single()
        }
        assertFalse(runBlocking { repo.allowed(disabled) })
        click("exit_parent")
        screen(Screen.CHILD)
        idle()
        capture("auto-child")
        assertEquals(1, runBlocking { repo.visible(false).size })

        assertFalse(runBlocking { repo.visible(false).any { it.id == imported.id } })
    }

    @Test
    fun seriesShowsOnlyApprovedEpisodes() {
        screen(Screen.PIN)
        pin("1234")
        pin("1234")
        screen(Screen.PARENT)
        idle()
        val model = ui.activity.cinema
        val repo = model.library.repo
        ui.runOnUiThread {
            model.prepareImport("Домашний сериал", 6, true, 1, 1)
            model.acceptPickedFile(Uri.parse("content://ru.familycinema.app.test.media/clip"))
        }
        screen(Screen.SOURCES)
        idle()
        val work = runBlocking { repo.dao.works().first { it.type == "series" } }
        val episode = runBlocking { repo.dao.episodes(work.id).single() }
        ui.runOnUiThread { model.openWork(work) }
        screen(Screen.WORK)
        idle()
        ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText("1. Серия 1"))
        ui.onNodeWithText("1. Серия 1").performClick()
        idle()
        click("version_approve")
        click("library_confirm")
        idle()
        ui.runOnUiThread {
            model.prepareImport("Домашний сериал", 6, true, 2, 1)
            model.acceptPickedFile(Uri.parse("content://ru.familycinema.app.test.media/second"))
        }
        screen(Screen.SOURCES)
        idle()
        assertEquals(2, runBlocking { repo.dao.episodes(work.id).size })
        click("exit_parent")
        screen(Screen.CHILD)
        idle()
        click("work_${work.id}")
        screen(Screen.WORK)
        idle()
        ui.runOnUiThread { assertEquals(listOf(episode.id), model.library.episodes.map { it.id }) }
        ui.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText("1. Серия 1"))
        ui.onNodeWithText("1. Серия 1").performClick()
        capture("series")
        ui.onNodeWithText("Сезон 2").assertDoesNotExist()
        click("version_play")
        screen(Screen.PLAYER)
        ui.waitUntil(90000) {
            var decoded = false
            ui.runOnUiThread { decoded = model.player?.videoSize?.width == 960 }
            decoded
        }
        ui.runOnUiThread {
            model.player!!.pause()
            model.showControls()
        }
        capture("series-player")
    }
}
