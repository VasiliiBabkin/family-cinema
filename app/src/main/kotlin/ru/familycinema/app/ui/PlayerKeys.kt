package ru.familycinema.app.ui

import android.view.KeyEvent

/** Routes remote and media keys to the same state as touch controls. */
class PlayerKeys {
    private var consumeUp = false

    fun handle(
        event: KeyEvent,
        visible: Boolean,
        playing: Boolean,
        show: () -> Unit,
        toggle: () -> Unit,
        seek: (Long) -> Unit,
    ): Boolean {
        val reveal =
            event.keyCode in
                listOf(
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_UP,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_RIGHT,
                )
        if (reveal && event.action == KeyEvent.ACTION_UP && consumeUp) {
            consumeUp = false
            return true
        }
        if (reveal && event.action == KeyEvent.ACTION_DOWN && !visible) {
            show()
            consumeUp = true
            return true
        }
        if (
            event.keyCode in
                listOf(
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY,
                    KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                    KeyEvent.KEYCODE_MEDIA_REWIND,
                )
        ) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                show()
                when (event.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> seek(10000)
                    KeyEvent.KEYCODE_MEDIA_REWIND -> seek(-10000)
                    KeyEvent.KEYCODE_MEDIA_PLAY -> if (!playing) toggle()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> if (playing) toggle()
                    else -> toggle()
                }
            }
            return true
        }
        return false
    }
}
