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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import java.io.File
import ru.familycinema.app.*
import ru.familycinema.app.R
import ru.familycinema.app.ui.*
import ru.familycinema.sources.SafeNetwork

@Composable private fun S(id: Int) = stringResource(id)

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
            Text(S(R.string.library_title))
        }
        if (parent) {
            OutlinedButton(
                onClick = { model.openLibrary(Screen.SOURCES) },
                modifier = Modifier.cinemaFocus().testTag("sources_open"),
            ) {
                Text(S(R.string.sources_title))
            }
            OutlinedButton(
                onClick = { model.openLibrary(Screen.POLICY) },
                modifier = Modifier.cinemaFocus().testTag("policy_open"),
            ) {
                Text(S(R.string.policy_title))
            }
        }
    }
}

@Composable
fun LibraryUi(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    val c = model.library
    if (!model.parent && model.screen in listOf(Screen.SOURCES, Screen.POLICY)) return
    Column(Modifier.fillMaxSize()) {
        if (c.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
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

@Composable
private fun CatalogPage(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    val c = model.library
    val list = c.filtered()
    Column(Modifier.fillMaxSize().padding(horizontal = if (tv) 40.dp else 20.dp)) {
        Text(
            S(
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
                label = { Text(S(R.string.search_title)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
            )
            if (model.parent)
                Button(
                    onClick = { c.discover(c.query.isNotBlank()) },
                    enabled = !c.busy,
                    modifier = Modifier.testTag("search_online").cinemaFocus(),
                ) {
                    Text(S(R.string.search_sources))
                }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterChip(
                selected = c.onlyFavorites,
                onClick = { c.onlyFavorites = !c.onlyFavorites },
                label = { Text(S(R.string.favorites)) },
                modifier = Modifier.cinemaFocus(),
            )
            FilterChip(
                selected = c.onlyHistory,
                onClick = {
                    c.onlyHistory = !c.onlyHistory
                    c.updateHistoryIndex()
                },
                label = { Text(S(R.string.history)) },
                modifier = Modifier.cinemaFocus(),
            )
            if (model.parent)
                TextButton(
                    onClick = { model.openLibrary(Screen.SOURCES) },
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(S(R.string.add_content))
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
                Text(S(R.string.library_empty), style = MaterialTheme.typography.titleLarge)
                Text(
                    S(
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
                    modifier = Modifier.fillMaxWidth().testTag("work_${w.id}").cinemaFocus(),
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
                        val pos = w.id in c.historyWorkIds && c.history.any { it.position > 1000 }
                        if (pos)
                            Text(
                                S(R.string.continue_play),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun Cover(w: Work, modifier: Modifier, parent: Boolean) {
    val context = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(context)
            .okHttpClient(SafeNetwork.client)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "posters"))
                    .maxSizeBytes(32L * 1024 * 1024)
                    .build()
            }
            .build()
    }
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
            Text(
                w.title,
                Modifier.align(Alignment.BottomStart)
                    .background(androidx.compose.ui.graphics.Color(0xBB102D30))
                    .fillMaxWidth()
                    .padding(12.dp),
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        }
}

@Composable
private fun SourcesPage(model: CinemaViewModel, tv: Boolean) {
    val c = model.library
    var endpoint by remember { mutableStateOf("") }
    var rights by remember { mutableStateOf(false) }
    var localDialog by remember { mutableStateOf(false) }
    var directDialog by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var series by remember { mutableStateOf(false) }
    var season by remember { mutableStateOf("1") }
    var number by remember { mutableStateOf("1") }
    var direct by remember { mutableStateOf("") }
    var trust by remember { mutableStateOf<Source?>(null) }
    val activity = LocalContext.current as MainActivity
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = if (tv) 40.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text(S(R.string.sources_title), style = MaterialTheme.typography.headlineLarge)
            Text(S(R.string.sources_intro))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { localDialog = true },
                    modifier = Modifier.testTag("import_local").cinemaFocus(),
                ) {
                    Icon(Icons.Rounded.FolderOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text(S(R.string.import_file))
                }
                OutlinedButton(
                    onClick = { directDialog = true },
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(S(R.string.add_https))
                }
            }
        }
        item {
            HorizontalDivider()
            Text(S(R.string.add_addon), style = MaterialTheme.typography.titleLarge)
        }
        item {
            OutlinedTextField(
                endpoint,
                { endpoint = it.take(8192) },
                Modifier.fillMaxWidth().testTag("source_endpoint").cinemaFocus(),
                label = { Text(S(R.string.manifest_url)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    rights,
                    { rights = it },
                    modifier = Modifier.testTag("source_rights").cinemaFocus(),
                )
                Text(S(R.string.source_rights), Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = { c.addSource(endpoint, rights) },
                enabled = rights && endpoint.isNotBlank() && !c.busy,
                modifier = Modifier.testTag("source_connect").cinemaFocus(),
            ) {
                Text(S(R.string.connect_source))
            }
        }
        items(c.sources.filter { it.id != "direct" }, key = { it.id }) { source ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(source.title, style = MaterialTheme.typography.titleLarge)
                    Text(source.capabilities, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(S(R.string.source_enabled), Modifier.weight(1f))
                        Switch(
                            source.enabled,
                            { c.sourcePolicy(source, enabled = it) },
                            modifier = Modifier.cinemaFocus(),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(S(R.string.source_trusted), Modifier.weight(1f))
                        Switch(
                            source.trusted,
                            { if (it) trust = source else c.sourcePolicy(source, trust = false) },
                            modifier = Modifier.testTag("source_trust_${source.id}").cinemaFocus(),
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { c.discover(false) },
                enabled = !c.busy,
                modifier = Modifier.cinemaFocus(),
            ) {
                Text(S(R.string.refresh_catalogs))
            }
        }
    }
    if (trust != null)
        Confirm(
            S(R.string.trust_title),
            S(R.string.trust_body),
            {
                c.sourcePolicy(trust!!, trust = true)
                trust = null
            },
            { trust = null },
        )
    if (localDialog || directDialog)
        AlertDialog(
            onDismissRequest = {
                localDialog = false
                directDialog = false
            },
            title = { Text(S(if (localDialog) R.string.import_file else R.string.add_https)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        title,
                        { title = it.take(300) },
                        label = { Text(S(R.string.work_title)) },
                        modifier = Modifier.testTag("import_title").cinemaFocus(),
                    )
                    OutlinedTextField(
                        age,
                        { age = it.take(2) },
                        label = { Text(S(R.string.age_hint)) },
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                        modifier = Modifier.testTag("import_age").cinemaFocus(),
                    )
                    if (directDialog)
                        OutlinedTextField(
                            direct,
                            { direct = it.take(8192) },
                            label = { Text(S(R.string.https_video)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.cinemaFocus(),
                        )
                    else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(series, { series = it }, modifier = Modifier.cinemaFocus())
                            Text(S(R.string.series))
                        }
                        if (series)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    season,
                                    { season = it.take(3) },
                                    Modifier.weight(1f),
                                    label = { Text(S(R.string.season)) },
                                )
                                OutlinedTextField(
                                    number,
                                    { number = it.take(4) },
                                    Modifier.weight(1f),
                                    label = { Text(S(R.string.episode)) },
                                )
                            }
                    }
                    Text(S(R.string.copy_notice))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (localDialog) {
                            activity.chooseVideo(
                                title,
                                age.toIntOrNull(),
                                series,
                                season.toIntOrNull() ?: 1,
                                number.toIntOrNull() ?: 1,
                            )
                            localDialog = false
                        } else {
                            c.direct(direct, title, age.toIntOrNull())
                            directDialog = false
                        }
                    },
                    enabled = title.isNotBlank() && (age.isBlank() || age.toIntOrNull() in 0..21),
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(S(R.string.add_content))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        localDialog = false
                        directDialog = false
                    }
                ) {
                    Text(S(R.string.cancel))
                }
            },
        )
}

@Composable
private fun WorkPage(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    val c = model.library
    val w = c.selected ?: return
    var age by remember(w.id, w.age) { mutableStateOf(w.age?.toString() ?: "") }
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
                            Text(S(R.string.favorites))
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
                    Text(S(R.string.favorites))
                }
            }
        }
        if (model.parent)
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        age,
                        { age = it.take(2) },
                        Modifier.width(180.dp).testTag("work_age").cinemaFocus(),
                        label = { Text(S(R.string.age_hint)) },
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
                        Text(S(R.string.save))
                    }
                }
                Text(S(R.string.age_policy_note), style = MaterialTheme.typography.bodySmall)
                if (w.sourceId != "local" && w.sourceId != "direct")
                    TextButton(onClick = { c.details() }, modifier = Modifier.cinemaFocus()) {
                        Text(S(R.string.load_details))
                    }
            }
        if (w.type == "series") {
            item { Text(S(R.string.seasons_episodes), style = MaterialTheme.typography.titleLarge) }
            c.episodes
                .groupBy { it.season }
                .forEach { (season, episodes) ->
                    item {
                        Text(
                            "${S(R.string.season)} $season",
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
            Text(S(R.string.available_versions), style = MaterialTheme.typography.titleLarge)
            if (model.parent && w.sourceId != "local" && w.sourceId != "direct")
                Button(
                    onClick = { c.findVersions() },
                    enabled = !c.busy,
                    modifier = Modifier.testTag("versions_find").cinemaFocus(),
                ) {
                    Text(S(R.string.find_versions))
                }
        }
        if (shown.isEmpty()) item { Text(S(R.string.versions_empty)) }
        items(shown, key = { it.id }) { v ->
            val d = c.decisions[v.id]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        v.title,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        S(
                            if (v.verified) R.string.verified_copy
                            else if (v.kind == "torrent") R.string.torrent_unavailable
                            else R.string.needs_verification
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (model.parent && d != null)
                        Text(
                            S(if (d.allowed) R.string.approved else R.string.unapproved) +
                                " · " +
                                d.origin,
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
                                Text(S(R.string.download_verify))
                            }
                        if (v.verified) {
                            Button(
                                onClick = { c.launch(v) },
                                enabled = !c.busy,
                                modifier = Modifier.testTag("version_play").cinemaFocus(),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Text(S(if (model.parent) R.string.preview else R.string.watch))
                            }
                            if (model.parent)
                                OutlinedButton(
                                    onClick = { approval = v to (d?.allowed != true) },
                                    modifier = Modifier.testTag("version_approve").cinemaFocus(),
                                ) {
                                    Text(
                                        S(
                                            if (d?.allowed == true) R.string.revoke
                                            else R.string.approve
                                        )
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
    approval?.let { (v, allow) ->
        Confirm(
            S(if (allow) R.string.approval_title else R.string.revoke_title),
            S(if (allow) R.string.approval_body else R.string.revoke_body),
            {
                c.decide(v, allow)
                approval = null
            },
            { approval = null },
        )
    }
}

@Composable
private fun WorkCopy(w: Work) {
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
private fun PolicyPage(model: CinemaViewModel, tv: Boolean) {
    val c = model.library
    var age by remember(c.profile.ageLimit) { mutableStateOf(c.profile.ageLimit.toString()) }
    var confirm by remember { mutableStateOf(false) }
    var clean by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = if (tv) 40.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        item {
            Text(S(R.string.policy_title), style = MaterialTheme.typography.headlineLarge)
            Text(S(R.string.policy_intro))
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    age,
                    { age = it.take(2) },
                    Modifier.width(180.dp).testTag("policy_age"),
                    label = { Text(S(R.string.age_limit)) },
                )
                Button(
                    onClick = { c.setPolicy(age.toInt(), c.profile.autoApprove) },
                    enabled = age.toIntOrNull() in 0..21,
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(S(R.string.save))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        S(R.string.auto_approval_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(S(R.string.auto_policy_note))
                }
                Switch(
                    c.profile.autoApprove,
                    { if (it) confirm = true else c.setPolicy(c.profile.ageLimit, false) },
                    modifier = Modifier.testTag("policy_auto").cinemaFocus(),
                )
            }
        }
        item {
            OutlinedButton(
                onClick = { model.openLibrary(Screen.SOURCES) },
                modifier = Modifier.cinemaFocus(),
            ) {
                Text(S(R.string.trusted_sources))
            }
        }
        item {
            OutlinedButton(
                onClick = { c.sweepAuto() },
                enabled = !c.busy,
                modifier = Modifier.testTag("policy_sweep").cinemaFocus(),
            ) {
                Text(S(R.string.apply_auto))
            }
        }
        item {
            HorizontalDivider()
            Text(S(R.string.disk_budget), style = MaterialTheme.typography.titleLarge)
            Text("${c.repo.used()/1024/1024} / ${c.repo.budget/1024/1024} МБ")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1, 2, 4).forEach { gb ->
                    OutlinedButton(
                        onClick = { c.setBudget(gb) },
                        modifier = Modifier.cinemaFocus(),
                    ) {
                        Text("$gb ГБ")
                    }
                }
            }
            TextButton(onClick = { clean = true }, modifier = Modifier.cinemaFocus()) {
                Text(S(R.string.cleanup_cache))
            }
        }
        item { Text(S(R.string.audit_title), style = MaterialTheme.typography.titleLarge) }
        items(c.audit, key = { it.id }) { a ->
            Column(Modifier.padding(vertical = 8.dp)) {
                Text(a.event, style = MaterialTheme.typography.titleMedium)
                Text(a.detail, style = MaterialTheme.typography.bodySmall)
                Text(
                    java.text.DateFormat.getDateTimeInstance().format(java.util.Date(a.at)),
                    style = MaterialTheme.typography.labelSmall,
                )
                HorizontalDivider()
            }
        }
    }
    if (confirm)
        Confirm(
            S(R.string.auto_approval_title),
            S(R.string.auto_policy_note),
            {
                c.setPolicy(c.profile.ageLimit, true)
                confirm = false
            },
            { confirm = false },
        )
    if (clean)
        Confirm(
            S(R.string.cleanup_cache),
            S(R.string.cleanup_note),
            {
                c.cleanup()
                clean = false
            },
            { clean = false },
        )
}

@Composable
private fun Confirm(title: String, body: String, yes: () -> Unit, no: () -> Unit) {
    AlertDialog(
        onDismissRequest = no,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = yes,
                modifier = Modifier.testTag("library_confirm").cinemaFocus(),
            ) {
                Text(S(R.string.confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = no, modifier = Modifier.cinemaFocus()) { Text(S(R.string.cancel)) }
        },
    )
}
