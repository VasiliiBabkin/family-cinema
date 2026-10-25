package ru.familycinema.player
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
object Playback {
 @androidx.annotation.MainThread
 fun create(context: Context, uri: String, position: Long): ExoPlayer = ExoPlayer.Builder(context).build().apply {
 setMediaItem(MediaItem.fromUri(uri)); seekTo(position); prepare(); playWhenReady=true
 }
}
