package ru.familycinema.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.familycinema.parental.FamilyStore

/** Run with the 0.1.0 APK installed, then install 0.2.0 without clearing data. */
class UpgradeSeedTest {
    @Test
    fun seedLegacyApproval() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("family_private", 0).edit().clear().commit()
        val store = FamilyStore(context)
        store.setPin("2468")
        store.approve(DemoCatalog.films().first(), true)
        store.savePosition("quiet-evening", 12345L)
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
        ui.runOnUiThread {
            assertEquals(Screen.CHILD, ui.activity.cinema.screen)
            assertFalse(ui.activity.cinema.parent)
            assertTrue(ui.activity.cinema.approved)
            assertEquals(12345L, ui.activity.cinema.position)
        }
    }
}
