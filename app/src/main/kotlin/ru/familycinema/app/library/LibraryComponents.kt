package ru.familycinema.app.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import java.io.File
import ru.familycinema.app.*
import ru.familycinema.app.R
import ru.familycinema.app.ui.*
import ru.familycinema.sources.SafeNetwork

@Composable internal fun s(id: Int) = stringResource(id)

@Composable
fun LibraryEntry(model: CinemaViewModel, parent: Boolean) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { model.openLibrary() },
            modifier = Modifier.cinemaFocus().testTag("library_entry"),
        ) {
            Icon(Icons.Rounded.VideoLibrary, null)
            Spacer(Modifier.width(8.dp))
            Text(s(R.string.library_title))
        }
        if (parent) {
            OutlinedButton(
                onClick = { model.openLibrary(Screen.SOURCES) },
                modifier = Modifier.cinemaFocus().testTag("sources_open"),
            ) {
                Text(s(R.string.sources_title))
            }
            OutlinedButton(
                onClick = { model.openLibrary(Screen.POLICY) },
                modifier = Modifier.cinemaFocus().testTag("policy_open"),
            ) {
                Text(s(R.string.policy_title))
            }
        }
    }
}

@Composable
internal fun Cover(w: Work, modifier: Modifier, parent: Boolean) {
    val context = LocalContext.current
    val loader = remember { CinemaImages.get(context) }
    val url = w.poster
    if (url != null && (url.startsWith(context.filesDir.absolutePath) && File(url).exists()))
        AsyncImage(
            model = url,
            imageLoader = loader,
            contentDescription = null,
            modifier = modifier.clip(CinemaTokens.Corners),
            contentScale = ContentScale.Crop,
        )
    else
        Box(modifier.clip(CinemaTokens.Corners)) {
            NightArtwork(Modifier.matchParentSize(), decorative = true)
        }
}

@Composable
internal fun Confirm(title: String, body: String, yes: () -> Unit, no: () -> Unit) {
    AlertDialog(
        onDismissRequest = no,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = yes,
                modifier = Modifier.testTag("library_confirm").cinemaFocus(),
            ) {
                Text(s(R.string.confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = no, modifier = Modifier.cinemaFocus()) { Text(s(R.string.cancel)) }
        },
    )
}

private object CinemaImages {
    @Volatile private var loader: ImageLoader? = null

    fun get(context: android.content.Context): ImageLoader =
        loader
            ?: synchronized(this) {
                loader
                    ?: ImageLoader.Builder(context.applicationContext)
                        .okHttpClient(SafeNetwork.client)
                        .memoryCache {
                            coil.memory.MemoryCache.Builder(context.applicationContext)
                                .maxSizeBytes(24 * 1024 * 1024)
                                .build()
                        }
                        .diskCache {
                            DiskCache.Builder()
                                .directory(File(context.cacheDir, "poster-render-cache"))
                                .maxSizeBytes(16L * 1024 * 1024)
                                .build()
                        }
                        .build()
                        .also { loader = it }
            }
}
