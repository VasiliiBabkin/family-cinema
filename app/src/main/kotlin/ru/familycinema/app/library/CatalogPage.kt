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
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.familycinema.app.*
import ru.familycinema.app.R
import ru.familycinema.app.ui.*

@Composable
internal fun CatalogPage(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    val c = model.library
    val list = c.filtered()
    Column(Modifier.fillMaxSize().padding(horizontal = if (tv) 40.dp else 20.dp)) {
        Text(
            s(
                if (model.parent) R.string.library_parent_heading
                else R.string.library_child_heading
            ),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                c.query,
                { c.query = it.take(200) },
                Modifier.weight(1f).testTag("library_search").cinemaFocus(),
                label = { Text(s(R.string.search_title)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
            )
            if (model.parent)
                Button(
                    onClick = { c.discover(c.query.isNotBlank()) },
                    enabled = !c.busy,
                    modifier = Modifier.testTag("search_online").cinemaFocus(),
                ) {
                    Text(s(R.string.search_sources))
                }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterChip(
                selected = c.onlyFavorites,
                onClick = { c.onlyFavorites = !c.onlyFavorites },
                label = { Text(s(R.string.favorites)) },
                modifier = Modifier.cinemaFocus(),
            )
            FilterChip(
                selected = c.onlyHistory,
                onClick = {
                    c.onlyHistory = !c.onlyHistory
                    c.updateHistoryIndex()
                },
                label = { Text(s(R.string.history)) },
                modifier = Modifier.cinemaFocus(),
            )
            if (model.parent)
                TextButton(
                    onClick = { model.openLibrary(Screen.POLICY) },
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(s(R.string.policy_title))
                }
            if (model.parent)
                TextButton(
                    onClick = { model.openLibrary(Screen.SOURCES) },
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(s(R.string.add_content))
                }
        }
        if (list.isEmpty())
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    Icons.Rounded.AutoStories,
                    null,
                    Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(s(R.string.library_empty), style = MaterialTheme.typography.titleLarge)
                Text(
                    s(
                        if (model.parent) R.string.library_empty_parent
                        else R.string.library_empty_child
                    )
                )
            }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (tv) 3 else if (wide) 3 else 2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(list, key = { it.id }) { w ->
                Card(
                    onClick = { model.openWork(w) },
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag("work_${w.id}")
                            .initialFocus(tv && c.query.isBlank() && list.firstOrNull()?.id == w.id)
                            .cinemaFocus(),
                    shape = CinemaTokens.Corners,
                ) {
                    Cover(
                        w,
                        Modifier.fillMaxWidth()
                            .height(if (tv) 170.dp else if (wide) 160.dp else 130.dp),
                        model.parent,
                    )
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            w.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            listOfNotNull(w.year?.toString(), w.age?.let { "$it+" })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        val pos = w.id in c.inProgressWorkIds
                        if (pos)
                            Text(
                                s(R.string.continue_play),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                    }
                }
            }
        }
    }
}
