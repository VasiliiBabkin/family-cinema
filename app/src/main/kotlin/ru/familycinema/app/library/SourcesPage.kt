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
import androidx.compose.ui.unit.dp
import ru.familycinema.app.*
import ru.familycinema.app.R
import ru.familycinema.app.ui.*

@Composable
internal fun SourcesPage(model: CinemaViewModel, tv: Boolean) {
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
            Text(s(R.string.sources_title), style = MaterialTheme.typography.headlineLarge)
            Text(s(R.string.sources_intro))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { localDialog = true },
                    modifier = Modifier.testTag("import_local").cinemaFocus(),
                ) {
                    Icon(Icons.Rounded.FolderOpen, null)
                    Spacer(Modifier.width(8.dp))
                    Text(s(R.string.import_file))
                }
                OutlinedButton(
                    onClick = { directDialog = true },
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(s(R.string.add_https))
                }
            }
        }
        item {
            HorizontalDivider()
            Text(s(R.string.add_addon), style = MaterialTheme.typography.titleLarge)
        }
        item {
            OutlinedTextField(
                endpoint,
                { endpoint = it.take(8192) },
                Modifier.fillMaxWidth().testTag("source_endpoint").cinemaFocus(),
                label = { Text(s(R.string.manifest_url)) },
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
                Text(s(R.string.source_rights), Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = { c.addSource(endpoint, rights) },
                enabled = rights && endpoint.isNotBlank() && !c.busy,
                modifier = Modifier.testTag("source_connect").cinemaFocus(),
            ) {
                Text(s(R.string.connect_source))
            }
        }
        items(c.sources.filter { it.id != "direct" }, key = { it.id }) { source ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(source.title, style = MaterialTheme.typography.titleLarge)
                    Text(source.capabilities, style = MaterialTheme.typography.bodySmall)
                    if (source.id != "local")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s(R.string.source_enabled), Modifier.weight(1f))
                            Switch(
                                source.enabled,
                                { c.sourcePolicy(source, enabled = it) },
                                modifier = Modifier.cinemaFocus(),
                            )
                        }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s(R.string.source_trusted), Modifier.weight(1f))
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
                Text(s(R.string.refresh_catalogs))
            }
        }
    }
    if (trust != null)
        Confirm(
            s(R.string.trust_title),
            s(R.string.trust_body),
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
            title = { Text(s(if (localDialog) R.string.import_file else R.string.add_https)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        title,
                        { title = it.take(300) },
                        label = { Text(s(R.string.work_title)) },
                        modifier = Modifier.testTag("import_title").cinemaFocus(),
                    )
                    OutlinedTextField(
                        age,
                        { age = it.take(2) },
                        label = { Text(s(R.string.age_hint)) },
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
                            label = { Text(s(R.string.https_video)) },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.cinemaFocus(),
                        )
                    else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(series, { series = it }, modifier = Modifier.cinemaFocus())
                            Text(s(R.string.series))
                        }
                        if (series)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    season,
                                    { season = it.take(3) },
                                    Modifier.weight(1f),
                                    label = { Text(s(R.string.season)) },
                                )
                                OutlinedTextField(
                                    number,
                                    { number = it.take(4) },
                                    Modifier.weight(1f),
                                    label = { Text(s(R.string.episode)) },
                                )
                            }
                    }
                    Text(s(R.string.copy_notice))
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
                    Text(s(R.string.add_content))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        localDialog = false
                        directDialog = false
                    }
                ) {
                    Text(s(R.string.cancel))
                }
            },
        )
}
