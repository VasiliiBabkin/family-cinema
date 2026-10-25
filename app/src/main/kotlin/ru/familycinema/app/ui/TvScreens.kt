package ru.familycinema.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.familycinema.app.CinemaViewModel
import ru.familycinema.app.R

/** Distance-first TV composition: one story, stable focus and two meaningful actions. */
@Composable
fun TvChildCatalog(model: CinemaViewModel) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
        val imageHeight = (maxHeight * .58f).coerceAtMost(260.dp)
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(.85f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    stringResource(R.string.child_greeting),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(R.string.child_title),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    stringResource(
                        if (model.approved && model.position > 1000) R.string.continue_heading
                        else if (model.approved) R.string.allowed else R.string.empty_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.tv_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FilmPoster(Modifier.fillMaxWidth().height(imageHeight), title = model.approved)
                if (model.approved) {
                    Text(
                        stringResource(R.string.film_meta, model.film.age),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (model.position > 1000)
                        LinearProgressIndicator(
                            progress = { (model.position / 40000f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CinemaAction(
                            stringResource(
                                if (model.position > 1000) R.string.continue_short
                                else R.string.watch
                            ),
                            Icons.Rounded.PlayArrow,
                            Modifier.initialFocus(true).testTag("action_play"),
                            onClick = model::play,
                        )
                        TextButton(
                            onClick = model::details,
                            modifier = Modifier.testTag("action_details").cinemaFocus(),
                        ) {
                            Text(stringResource(R.string.details_short))
                            Icon(Icons.Rounded.ChevronRight, null)
                        }
                    }
                } else
                    Text(
                        stringResource(R.string.empty_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
        }
    }
}

@Composable
fun TvParentCatalog(model: CinemaViewModel) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(.9f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(
                stringResource(R.string.parent_kicker),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.parent_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                stringResource(R.string.review_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FilmPoster(Modifier.fillMaxWidth().height(210.dp))
            ApprovalBadge(model.approved)
            ru.familycinema.app.library.LibraryEntry(model, true)
            AutoApprovalSetting(model)
            CinemaAction(
                stringResource(R.string.open_film),
                Icons.Rounded.ChevronRight,
                Modifier.initialFocus(true).testTag("action_details"),
                onClick = model::details,
            )
        }
    }
}
