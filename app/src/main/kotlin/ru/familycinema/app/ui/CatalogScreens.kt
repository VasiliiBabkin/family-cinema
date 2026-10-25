package ru.familycinema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.familycinema.app.*
import ru.familycinema.app.R

@Composable
fun FilmPoster(modifier: Modifier = Modifier, title: Boolean = true) {
    Box(modifier.clip(CinemaTokens.Corners)) {
        NightArtwork(Modifier.matchParentSize(), decorative = true)
        if (title)
            Column(
                Modifier.align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE102D30)))
                    )
                    .padding(22.dp)
            ) {
                Text(
                    stringResource(R.string.demo_title),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    stringResource(R.string.demo_notice),
                    color = CinemaTokens.Gold,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
    }
}

@Composable
fun ChildCatalog(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    if (model.library.works.any { it.id != "demo" }) {
        ru.familycinema.app.library.LibraryUi(model, wide, tv)
        return
    }
    if (tv) {
        TvChildCatalog(model)
        return
    }
    val pad = if (wide) 28.dp else 20.dp
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(pad),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.child_greeting),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.child_title),
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontSize = if (wide) 34.sp else 28.sp,
                    lineHeight = if (wide) 39.sp else 33.sp,
                ),
        )
        if (!model.approved) {
            if (wide)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilmPoster(
                        Modifier.weight(1f).height(if (tv) 300.dp else 270.dp),
                        title = false,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        EmptyCopy()
                    }
                }
            else {
                FilmPoster(Modifier.fillMaxWidth().height(250.dp), title = false)
                EmptyCopy()
            }
        } else {
            Text(
                stringResource(
                    if (model.position > 1000) R.string.continue_heading else R.string.allowed
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (wide)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilmPoster(
                        Modifier.weight(1.25f)
                            .height(if (tv) 310.dp else 300.dp)
                            .cinemaFocus()
                            .clickable(onClick = model::details)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChildFilmActions(model, tv, wide)
                    }
                }
            else {
                FilmPoster(
                    Modifier.fillMaxWidth()
                        .height(if (model.position > 1000) 200.dp else 230.dp)
                        .cinemaFocus()
                        .clickable(onClick = model::details)
                )
                ChildFilmActions(model, tv, wide)
            }
        }
        if (tv)
            Text(
                stringResource(R.string.tv_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
private fun EmptyCopy() {
    Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge)
    Text(
        stringResource(R.string.empty_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChildFilmActions(model: CinemaViewModel, tv: Boolean, wide: Boolean) {
    if (wide) Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.titleLarge)
    Text(
        stringResource(R.string.film_meta, model.film.age),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (model.position > 1000)
        Text(
            stringResource(
                R.string.resume_hint,
                "%d:%02d".format(model.position / 60000, (model.position / 1000) % 60),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    if (model.position > 1000)
        LinearProgressIndicator(
            progress = { (model.position / 40000f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
        )
    CinemaAction(
        stringResource(if (model.position > 1000) R.string.continue_play else R.string.watch),
        Icons.Rounded.PlayArrow,
        Modifier.initialFocus(tv).testTag("action_play"),
        onClick = model::play,
    )
    TextButton(
        onClick = model::details,
        modifier = Modifier.testTag("action_details").cinemaFocus(),
    ) {
        Text(stringResource(R.string.open_film))
        Icon(Icons.Rounded.ChevronRight, null)
    }
}

@Composable
fun ParentCatalog(model: CinemaViewModel, tv: Boolean) {
    if (tv) {
        TvParentCatalog(model)
        return
    }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (tv) 32.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (tv) 16.dp else 20.dp),
    ) {
        Text(
            stringResource(R.string.parent_kicker),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(stringResource(R.string.parent_title), style = MaterialTheme.typography.headlineLarge)
        Text(
            stringResource(R.string.parent_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .2f))
        Text(stringResource(R.string.library), style = MaterialTheme.typography.titleLarge)
        Card(
            onClick = model::details,
            modifier =
                Modifier.fillMaxWidth().testTag("action_details").initialFocus(tv).cinemaFocus(),
            shape = CinemaTokens.Corners,
            colors =
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                FilmPoster(
                    Modifier.width(if (tv) 240.dp else 112.dp).height(if (tv) 156.dp else 142.dp),
                    title = false,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.demo_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.film_meta, model.film.age),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ApprovalBadge(model.approved)
                    Text(
                        stringResource(R.string.open_film),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (tv) Icon(Icons.Rounded.ChevronRight, null)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.review_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        ru.familycinema.app.library.LibraryEntry(model, true)
        AutoApprovalSetting(model)
        Text(
            stringResource(R.string.security_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ApprovalBadge(approved: Boolean) {
    Surface(
        color = if (approved) Color(0xFF244F43) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (approved) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
                null,
                Modifier.size(16.dp),
                tint = if (approved) Color(0xFFC8F0D5) else MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(if (approved) R.string.approved else R.string.unapproved),
                style = MaterialTheme.typography.labelSmall,
                color = if (approved) Color(0xFFE3FBEA) else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
fun FilmDetails(model: CinemaViewModel, wide: Boolean, tv: Boolean) {
    var confirmation by remember { mutableStateOf(false) }
    var technical by remember { mutableStateOf(false) }
    val body: @Composable ColumnScope.() -> Unit = {
        if (!tv)
            Text(
                stringResource(R.string.demo_title),
                style = MaterialTheme.typography.headlineLarge,
            )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                    stringResource(R.string.film_age, model.film.age),
                    stringResource(R.string.duration),
                    stringResource(R.string.offline),
                )
                .forEach { label ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            label,
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
        }
        Text(
            stringResource(if (tv) R.string.demo_notice else R.string.demo_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CinemaAction(
            stringResource(
                if (model.parent) R.string.preview
                else if (model.position > 1000) R.string.continue_play else R.string.watch
            ),
            Icons.Rounded.PlayArrow,
            Modifier.fillMaxWidth().testTag("action_play").initialFocus(tv),
            secondary = model.parent,
            onClick = model::play,
        )
        if (model.parent) {
            ApprovalBadge(model.approved)
            CinemaAction(
                stringResource(if (model.approved) R.string.revoke else R.string.approve),
                if (model.approved) Icons.Rounded.RemoveCircleOutline else Icons.Rounded.Check,
                Modifier.fillMaxWidth().testTag("action_approve"),
            ) {
                confirmation = true
            }
            if (!tv)
                Text(
                    stringResource(R.string.review_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            TextButton(onClick = { technical = !technical }, modifier = Modifier.cinemaFocus()) {
                Icon(Icons.Rounded.Info, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.version_details))
                Icon(if (technical) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (technical) {
                if (tv) {
                    Text(stringResource(R.string.demo_description))
                    Text(stringResource(R.string.review_hint))
                }
                Text(
                    stringResource(R.string.version_label, model.film.version.id),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(R.string.demo_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (wide)
        Row(
            Modifier.fillMaxSize()
                .padding(
                    horizontal = if (tv) 40.dp else 28.dp,
                    vertical = if (tv) 24.dp else 28.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = if (tv) Alignment.CenterVertically else Alignment.Top,
        ) {
            FilmPoster(Modifier.weight(1f).height(if (tv) 350.dp else 400.dp))
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (tv) 10.dp else 14.dp),
                content = body,
            )
        }
    else
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FilmPoster(Modifier.fillMaxWidth().height(240.dp), title = false)
            body()
        }
    if (confirmation && model.parent)
        AlertDialog(
            onDismissRequest = { confirmation = false },
            icon = { Icon(Icons.Rounded.VerifiedUser, null) },
            title = {
                Text(
                    stringResource(
                        if (model.approved) R.string.revoke_title else R.string.approval_title
                    )
                )
            },
            text = {
                Text(
                    stringResource(
                        if (model.approved) R.string.revoke_body else R.string.approval_body
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.setApproval(!model.approved)
                        confirmation = false
                    },
                    modifier = Modifier.testTag("confirm_approval").cinemaFocus(),
                ) {
                    Text(
                        stringResource(
                            if (model.approved) R.string.revoke else R.string.approve_confirm
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = false }, modifier = Modifier.cinemaFocus()) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
}

/** Only rendered inside the authenticated parent screen. */
@Composable
fun AutoApprovalSetting(model: CinemaViewModel) {
    if (!model.parent) return
    var confirmEnable by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.auto_approval_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.auto_approval_description),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = model.autoApprove,
                    onCheckedChange = { enabled ->
                        if (enabled) confirmEnable = true else model.setAutoApprove(false)
                    },
                    modifier = Modifier.testTag("auto_approval_switch"),
                )
            }
            Text(
                stringResource(R.string.auto_approval_scope),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (confirmEnable) {
        AlertDialog(
            onDismissRequest = { confirmEnable = false },
            title = { Text(stringResource(R.string.auto_approval_confirm_title)) },
            text = { Text(stringResource(R.string.auto_approval_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.setAutoApprove(true)
                        confirmEnable = false
                    }
                ) {
                    Text(stringResource(R.string.auto_approval_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnable = false }) {
                    Text(stringResource(R.string.back))
                }
            },
        )
    }
}
