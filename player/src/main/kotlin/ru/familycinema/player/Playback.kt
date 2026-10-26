package ru.familycinema.player

import android.content.Context
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@androidx.media3.common.util.UnstableApi
object Playback {
    fun create(context: Context, uri: String, position: Long): ExoPlayer {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Плеер должен создаваться на главном потоке"
        }
        val upstream =
            androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(
                ru.familycinema.sources.SafeNetwork.mediaClient
            )
        // Remote playlists may reference only HTTPS, never local file/content schemes.
        val dataSource: androidx.media3.datasource.DataSource.Factory =
            if (android.net.Uri.parse(uri).scheme.equals("https", ignoreCase = true)) upstream
            else
                androidx.media3.datasource.DefaultDataSource.Factory(
                    context.applicationContext,
                    upstream,
                )
        return ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSource)
            )
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
