package ru.familycinema.app.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.familycinema.app.*
import ru.familycinema.app.R
import ru.familycinema.app.ui.*

@Composable
fun LibraryUi(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    val c = model.library
    if (!model.parent && model.screen in listOf(Screen.SOURCES, Screen.POLICY)) return
    Column(Modifier.fillMaxSize()) {
        if (c.busy) {
            val transfer by c.repo.transfer.collectAsStateWithLifecycle()
            if (transfer.total > 0)
                LinearProgressIndicator(
                    progress = { (transfer.bytes.toFloat() / transfer.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            else LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (transfer.bytes > 0) {
                        if (transfer.total > 0)
                            stringResource(
                                R.string.transfer_progress,
                                transfer.bytes / 1024 / 1024,
                                transfer.total / 1024 / 1024,
                            )
                        else stringResource(R.string.transfer_unknown, transfer.bytes / 1024 / 1024)
                    } else s(R.string.working),
                    Modifier.weight(1f),
                )
                TextButton(onClick = { c.cancel() }, modifier = Modifier.cinemaFocus()) {
                    Text(s(R.string.cancel))
                }
            }
        }
        if (c.message.isNotBlank())
            Text(
                c.message,
                Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        if (!c.ready)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        else
            when (model.screen) {
                Screen.SOURCES -> SourcesPage(model, tv)
                Screen.POLICY -> PolicyPage(model, tv)
                Screen.WORK -> WorkPage(model, wide, tv)
                else -> CatalogPage(model, wide, tv)
            }
    }
}
