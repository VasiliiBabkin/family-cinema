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
internal fun WorkPage(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    val c = model.library
    val w = c.selected ?: return
    var age by remember(w.id, w.age) { mutableStateOf(w.age?.toString() ?: "") }
    var edit by remember { mutableStateOf(false) }
    var approval by remember { mutableStateOf<Pair<Version, Boolean>?>(null) }
    val shown = c.versions.filter { w.type != "series" || it.episodeId == c.episode?.id }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = if (tv) 40.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            if (wide)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(w, Modifier.weight(1f).height(if (tv) 240.dp else 300.dp), model.parent)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WorkCopy(w)
                        TextButton(onClick = { c.favorite(w) }, modifier = Modifier.cinemaFocus()) {
                            Icon(
                                if (w.id in c.favorites) Icons.Rounded.Favorite
                                else Icons.Rounded.FavoriteBorder,
                                null,
                            )
                            Text(s(R.string.favorites))
                        }
                    }
                }
            else {
                Cover(w, Modifier.fillMaxWidth().height(220.dp), model.parent)
                Spacer(Modifier.height(16.dp))
                WorkCopy(w)
                TextButton(onClick = { c.favorite(w) }, modifier = Modifier.cinemaFocus()) {
                    Icon(
                        if (w.id in c.favorites) Icons.Rounded.Favorite
                        else Icons.Rounded.FavoriteBorder,
                        null,
                    )
                    Text(s(R.string.favorites))
                }
            }
        }
        if (model.parent)
            item {
                TextButton(onClick = { edit = true }, modifier = Modifier.cinemaFocus()) {
                    Text(s(R.string.edit_metadata))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        age,
                        { age = it.take(2) },
                        Modifier.width(180.dp).testTag("work_age").cinemaFocus(),
                        label = { Text(s(R.string.age_hint)) },
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                    )
                    OutlinedButton(
                        onClick = { c.editAge(w, age.toIntOrNull()) },
                        enabled = age.isBlank() || age.toIntOrNull() in 0..21,
                        modifier = Modifier.cinemaFocus(),
                    ) {
                        Text(s(R.string.save))
                    }
                }
                Text(s(R.string.age_policy_note), style = MaterialTheme.typography.bodySmall)
                if (w.sourceId != "local" && w.sourceId != "direct")
                    TextButton(onClick = { c.details() }, modifier = Modifier.cinemaFocus()) {
                        Text(s(R.string.load_details))
                    }
            }
        if (w.type == "series") {
            item { Text(s(R.string.seasons_episodes), style = MaterialTheme.typography.titleLarge) }
            c.episodes
                .groupBy { it.season }
                .forEach { (season, episodes) ->
                    item {
                        Text(
                            "${s(R.string.season)} $season",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            episodes.forEach { e ->
                                FilterChip(
                                    selected = c.episode?.id == e.id,
                                    onClick = { c.selectEpisode(e) },
                                    label = { Text("${e.number}. ${e.title}", maxLines = 1) },
                                    modifier = Modifier.cinemaFocus(),
                                )
                            }
                        }
                    }
                }
        }
        item {
            Text(s(R.string.available_versions), style = MaterialTheme.typography.titleLarge)
            if (model.parent && w.sourceId != "local" && w.sourceId != "direct")
                Button(
                    onClick = { c.findVersions() },
                    enabled = !c.busy,
                    modifier = Modifier.testTag("versions_find").cinemaFocus(),
                ) {
                    Text(s(R.string.find_versions))
                }
        }
        if (shown.isEmpty()) item { Text(s(R.string.versions_empty)) }
        items(shown, key = { it.id }) { v ->
            val d = c.decisions[v.id]
            val hasGrant = v.id in c.exactGrants
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (model.parent) v.title else s(R.string.approved),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        s(
                            if (v.verified) R.string.verified_copy
                            else if (v.kind == "torrent") R.string.torrent_unavailable
                            else R.string.needs_verification
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (model.parent)
                        c.sources
                            .find { it.id == v.sourceId }
                            ?.let { source ->
                                Text(source.title, style = MaterialTheme.typography.bodySmall)
                            }
                    if (model.parent && d != null)
                        Text(
                            s(if (hasGrant) R.string.approved else R.string.unapproved) +
                                " · " +
                                s(
                                    when (d.origin) {
                                        "automatic" -> R.string.approval_origin_automatic
                                        "migration" -> R.string.approval_origin_migration
                                        else -> R.string.approval_origin_manual
                                    }
                                ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!v.verified && model.parent && v.kind != "torrent")
                            Button(
                                onClick = { c.verify(v) },
                                enabled = !c.busy,
                                modifier = Modifier.testTag("version_verify").cinemaFocus(),
                            ) {
                                Text(s(R.string.download_verify))
                            }
                        if (!v.verified && model.parent && v.kind == "https") {
                            OutlinedButton(
                                onClick = { c.preview(v) },
                                enabled = !c.busy,
                                modifier = Modifier.testTag("version_preview").cinemaFocus(),
                            ) {
                                Text(s(R.string.preview))
                            }
                        }
                        if (v.verified) {
                            Button(
                                onClick = { c.launch(v) },
                                enabled = !c.busy,
                                modifier = Modifier.testTag("version_play").cinemaFocus(),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Text(
                                    s(
                                        if (model.parent) R.string.preview
                                        else if (
                                            c.history.any {
                                                it.versionId == v.id && it.position > 1000
                                            }
                                        )
                                            R.string.continue_play
                                        else R.string.watch
                                    )
                                )
                            }
                            if (model.parent)
                                OutlinedButton(
                                    onClick = { approval = v to (!hasGrant) },
                                    modifier = Modifier.testTag("version_approve").cinemaFocus(),
                                ) {
                                    Text(s(if (hasGrant) R.string.revoke else R.string.approve))
                                }
                        }
                    }
                }
            }
        }
    }
    if (edit)
        MetadataEditor(
            w,
            {
                c.editMetadata(it)
                edit = false
            },
            { edit = false },
        )
    approval?.let { (v, allow) ->
        Confirm(
            s(if (allow) R.string.approval_title else R.string.revoke_title),
            s(if (allow) R.string.approval_body else R.string.revoke_body),
            {
                c.decide(v, allow)
                approval = null
            },
            { approval = null },
        )
    }
}

@Composable
internal fun WorkCopy(w: Work) {
    Text(w.title, style = MaterialTheme.typography.headlineLarge)
    if (w.original.isNotBlank()) Text(w.original, style = MaterialTheme.typography.titleMedium)
    Text(
        listOfNotNull(
                w.year?.toString(),
                w.age?.let { "$it+" },
                w.genres.takeIf { it.isNotBlank() },
            )
            .joinToString(" · "),
        color = MaterialTheme.colorScheme.primary,
    )
    Text(w.description.take(2000), style = MaterialTheme.typography.bodyLarge)
    Text(
        w.attribution,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun MetadataEditor(work: Work, save: (Work) -> Unit, cancel: () -> Unit) {
    var title by remember { mutableStateOf(work.title) }
    var original by remember { mutableStateOf(work.original) }
    var aliases by remember { mutableStateOf(work.aliases) }
    var description by remember { mutableStateOf(work.description) }
    var year by remember { mutableStateOf(work.year?.toString() ?: "") }
    var genres by remember { mutableStateOf(work.genres) }
    var age by remember { mutableStateOf(work.age?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(s(R.string.edit_metadata)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    title,
                    { title = it.take(300) },
                    label = { Text(s(R.string.work_title)) },
                )
                OutlinedTextField(
                    original,
                    { original = it.take(300) },
                    label = { Text(s(R.string.original_title)) },
                )
                OutlinedTextField(
                    aliases,
                    { aliases = it.take(1000) },
                    label = { Text(s(R.string.aliases)) },
                )
                OutlinedTextField(
                    description,
                    { description = it.take(8000) },
                    label = { Text(s(R.string.description)) },
                )
                OutlinedTextField(
                    year,
                    { year = it.take(4) },
                    label = { Text(s(R.string.release_year)) },
                )
                OutlinedTextField(
                    genres,
                    { genres = it.take(1000) },
                    label = { Text(s(R.string.genres)) },
                )
                OutlinedTextField(age, { age = it.take(2) }, label = { Text(s(R.string.age_hint)) })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    save(
                        work.copy(
                            title = title,
                            original = original,
                            aliases = aliases,
                            description = description,
                            year = year.toIntOrNull(),
                            genres = genres,
                            age = age.toIntOrNull(),
                        )
                    )
                },
                enabled =
                    title.isNotBlank() &&
                        (age.isBlank() || age.toIntOrNull() in 0..21) &&
                        (year.isBlank() || year.toIntOrNull() in 1888..2100),
            ) {
                Text(s(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = cancel) { Text(s(R.string.cancel)) } },
    )
}
