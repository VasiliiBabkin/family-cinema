package ru.familycinema.player

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/** Video-only view. Compose owns controls; the surface binding owns its callbacks. */
@androidx.media3.common.util.UnstableApi
class CinemaVideoView(context: Context) : PlayerView(context) {
    private var surfaceBinding: Pair<SurfaceHolder, SurfaceHolder.Callback>? = null

    init {
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        isFocusable = false
        isFocusableInTouchMode = false
        setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING)
    }

    fun bind(session: ExoPlayer) {
        if (player === session) return
        unbind()
        player = session
        val surface = videoSurfaceView as? SurfaceView ?: return
        val callback =
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) = Unit

                override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    // A paused retained decoder needs one frame for the replacement surface.
                    if (player === session && !session.playWhenReady)
                        session.seekTo(session.currentPosition)
                }
            }
        surface.holder.addCallback(callback)
        surfaceBinding = surface.holder to callback
        if (surface.holder.surface.isValid && !session.playWhenReady)
            session.seekTo(session.currentPosition)
    }

    fun unbind() {
        surfaceBinding?.let { (holder, callback) -> holder.removeCallback(callback) }
        surfaceBinding = null
        player = null
        keepScreenOn = false
    }
}
