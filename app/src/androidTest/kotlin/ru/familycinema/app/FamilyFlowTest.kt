package ru.familycinema.app

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.ui.PlayerView
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.familycinema.parental.FamilyStore

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class FamilyFlowTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun click(text: String) { ui.onNodeWithText(text).performScrollTo().performClick();ui.waitForIdle() }
 private fun pin(value: String) { value.forEach { click(it.toString()) };click("Подтвердить") }
 private fun findPlayer(v: View): PlayerView? {
  if(v is PlayerView) return v
  if(v is ViewGroup) for(i in 0 until v.childCount) {findPlayer(v.getChildAt(i))?.let {return it}}
  return null
 }
 private fun capture(name: String) {
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
   File(ui.activity.filesDir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
  }
 }
 private fun decodedVideo(): Boolean { var result=false;ui.runOnUiThread { result=findPlayer(ui.activity.window.decorView)?.player?.videoSize?.width==960 };return result }
 private fun position(): Long { var p=-1L;ui.runOnUiThread { p=findPlayer(ui.activity.window.decorView)?.player?.currentPosition ?: -1 };return p }
 @Test fun approvalPlaybackResumeAndRevoke() {
  // Запускать на чистых данных: adb shell pm clear ru.familycinema.app
  ui.onNodeWithText("Создайте родительский PIN").assertExists()
  pin("1234");pin("1234")
  ui.waitUntil(60000) { ui.onAllNodesWithText("Родительская").fetchSemanticsNodes().isNotEmpty() }
  capture("parent")
  click("Открыть карточку");click("Одобрить эту версию")
  click("Выйти")
  capture("child-catalog")
  click("Открыть карточку");click("Смотреть")
  ui.waitUntil(60000) { position()>1500 && decodedVideo() }
  capture("player")
  click("К карточке")
  ui.onNodeWithText("Продолжить просмотр").assertExists()
  val saved=FamilyStore(ui.activity).position("quiet-evening")
  assertTrue(saved>1000)
  click("Продолжить просмотр")
  ui.waitUntil(60000) { position()>=saved }
  click("К карточке");click("Родителям")
  pin("9999")
  ui.waitUntil(60000) { ui.onAllNodesWithText("Неверный PIN").fetchSemanticsNodes().isNotEmpty() }
  pin("1234")
  ui.waitUntil(60000) { ui.onAllNodesWithText("Родительская").fetchSemanticsNodes().isNotEmpty() }
  click("Открыть карточку");click("Убрать из детского каталога");click("Выйти")
  ui.onNodeWithText("Здесь появятся ваши истории").assertExists()
  assertFalse(FamilyStore(ui.activity).approved(DemoCatalog.films().first()))
 }
}
