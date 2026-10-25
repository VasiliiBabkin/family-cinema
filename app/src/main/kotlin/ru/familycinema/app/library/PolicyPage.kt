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
import ru.familycinema.app.*
import ru.familycinema.app.R
import ru.familycinema.app.ui.*

@Composable
internal fun PolicyPage(model: CinemaViewModel, tv: Boolean) {
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
            Text(s(R.string.policy_title), style = MaterialTheme.typography.headlineLarge)
            Text(s(R.string.policy_intro))
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
                    label = { Text(s(R.string.age_limit)) },
                )
                Button(
                    onClick = { c.setPolicy(age.toInt(), c.profile.autoApprove) },
                    enabled = age.toIntOrNull() in 0..21,
                    modifier = Modifier.cinemaFocus(),
                ) {
                    Text(s(R.string.save))
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s(R.string.auto_approval_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(s(R.string.auto_policy_note))
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
                Text(s(R.string.trusted_sources))
            }
        }
        item {
            OutlinedButton(
                onClick = { c.sweepAuto() },
                enabled = !c.busy,
                modifier = Modifier.testTag("policy_sweep").cinemaFocus(),
            ) {
                Text(s(R.string.apply_auto))
            }
        }
        item {
            HorizontalDivider()
            Text(s(R.string.disk_budget), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.disk_usage,
                    c.repo.used() / 1024 / 1024,
                    c.repo.budget / 1024 / 1024,
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1, 2, 4).forEach { gb ->
                    OutlinedButton(
                        onClick = { c.setBudget(gb) },
                        modifier = Modifier.cinemaFocus(),
                    ) {
                        Text(stringResource(R.string.disk_gb, gb))
                    }
                }
            }
            TextButton(onClick = { clean = true }, modifier = Modifier.cinemaFocus()) {
                Text(s(R.string.cleanup_cache))
            }
        }
        item { Text(s(R.string.audit_title), style = MaterialTheme.typography.titleLarge) }
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
            s(R.string.auto_approval_title),
            s(R.string.auto_policy_note),
            {
                c.setPolicy(c.profile.ageLimit, true)
                confirm = false
            },
            { confirm = false },
        )
    if (clean)
        Confirm(
            s(R.string.cleanup_cache),
            s(R.string.cleanup_note),
            {
                c.cleanup()
                clean = false
            },
            { clean = false },
        )
}
