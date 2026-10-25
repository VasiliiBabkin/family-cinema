package ru.familycinema.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.familycinema.parental.FamilyStore

/** Run with the 0.2.0 APK installed, then install 0.2.0 without clearing data. */
class UpgradeSeedTest {
    @Test
    fun seedLegacyApproval() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("family_private", 0).edit().clear().commit()
        val store = FamilyStore(context)
        store.setPin("2468")
        store.approve(DemoCatalog.films().first(), true)
        store.savePosition("quiet-evening", 12345L)
        // This fixture has no Activity.onStop to flush the old version's apply().
        assertTrue(
            context
                .getSharedPreferences("family_private", 0)
                .edit()
                .putBoolean("upgrade_fixture_ready", true)
                .putBoolean("auto_approve", true)
                .commit()
        )
        assertEquals(12345L, store.position("quiet-evening"))
        assertTrue(store.hasPin)
        assertTrue(store.approved(DemoCatalog.films().first()))
    }
}

/** No cleanup rule: verifies the real data left by the older signed application. */
class UpgradeVerifyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test
    fun retainsPinApprovalAndPosition() {
        val store = FamilyStore(InstrumentationRegistry.getInstrumentation().targetContext)
        assertTrue(store.verify("2468"))
        assertTrue(store.approved(DemoCatalog.films().first()))
        assertEquals(12345L, store.position("quiet-evening"))
        ui.waitUntil(90000) {
            var ready = false
            ui.runOnUiThread { ready = ui.activity.cinema.library.ready }
            ready
        }
        kotlinx.coroutines.runBlocking {
            val repo = ui.activity.cinema.library.repo
            assertFalse(repo.profile().autoApprove)
            assertTrue(repo.allowed(repo.dao.version("demo")!!))
            assertEquals(12345L, repo.dao.progress("demo")!!.position)
        }
        ui.runOnUiThread {
            assertEquals(Screen.CHILD, ui.activity.cinema.screen)
            assertFalse(ui.activity.cinema.parent)
            assertTrue(ui.activity.cinema.approved)
            assertEquals(12345L, ui.activity.cinema.position)
        }
    }
}
