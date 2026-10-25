package ru.familycinema.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

object Playback {
    fun create(context: Context, uri: String, position: Long): ExoPlayer {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Плеер должен создаваться на главном потоке"
        }
        return ExoPlayer.Builder(context.applicationContext)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(uri))
                seekTo(position)
                prepare()
                playWhenReady = true
            }
    }
}
