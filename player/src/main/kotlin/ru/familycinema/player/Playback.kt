package ru.familycinema.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

object Playback {
    // Создание и управление плеером выполняются в UI-эффекте, а не в recomposition.
    fun create(context: Context, uri: String, position: Long): ExoPlayer {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Плеер должен создаваться на главном потоке"
        }
        return ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            seekTo(position)
            prepare()
            playWhenReady = true
        }
    }
}
