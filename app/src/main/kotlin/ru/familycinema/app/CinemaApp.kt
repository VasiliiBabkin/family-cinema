package ru.familycinema.app

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.familycinema.app.ui.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CinemaApp(model: CinemaViewModel) {
    val configuration = LocalConfiguration.current
    val tv =
        configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val wide = tv || configuration.screenWidthDp >= 600
    val dark = model.parent || model.screen == Screen.PIN || model.screen == Screen.PLAYER
    val activity = LocalContext.current as Activity
    val view = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(model.parent, model.library.busy) {
        val previous = view.keepScreenOn
        view.keepScreenOn = model.parent && model.library.busy
        onDispose { view.keepScreenOn = previous }
    }
    DisposableEffect(dark) {
        val controller =
            androidx.core.view.WindowInsetsControllerCompat(
                activity.window,
                activity.window.decorView,
            )
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        onDispose {}
    }
    CinemaTheme(dark, tv) {
        if (model.screen == Screen.PLAYER) {
            PlayerScreen(model, tv)
        } else {
            BackHandler(model.screen != Screen.CHILD) { model.back() }
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(
                                    if (model.screen == Screen.PIN) R.string.app_name
                                    else if (model.parent) R.string.parent_kicker
                                    else R.string.app_name
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        },
                        navigationIcon = {
                            if (
                                model.screen in
                                    listOf(
                                        Screen.DETAILS,
                                        Screen.LIBRARY,
                                        Screen.WORK,
                                        Screen.SOURCES,
                                        Screen.POLICY,
                                    ) || model.screen == Screen.PIN && model.store.hasPin
                            )
                                IconButton(
                                    onClick = model::back,
                                    modifier = Modifier.cinemaFocus(),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.ArrowBack,
                                        stringResource(R.string.back),
                                    )
                                }
                        },
                        actions = {
                            if (model.screen != Screen.PIN) {
                                IconButton(
                                    onClick = { model.openLibrary() },
                                    modifier = Modifier.testTag("library_open").cinemaFocus(),
                                ) {
                                    Icon(
                                        Icons.Rounded.VideoLibrary,
                                        stringResource(R.string.library_title),
                                    )
                                }
                                if (model.parent)
                                    IconButton(
                                        onClick = model::childMode,
                                        modifier = Modifier.testTag("exit_parent").cinemaFocus(),
                                    ) {
                                        Icon(
                                            Icons.Rounded.ChildCare,
                                            stringResource(R.string.exit_parent),
                                        )
                                    }
                                else
                                    IconButton(
                                        onClick = model::openParents,
                                        modifier = Modifier.testTag("open_parents").cinemaFocus(),
                                    ) {
                                        Icon(Icons.Rounded.Lock, stringResource(R.string.parents))
                                    }
                            }
                        },
                        colors =
                            TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (wide && model.screen != Screen.PIN && !tv)
                        NavigationRail(containerColor = MaterialTheme.colorScheme.background) {
                            Spacer(Modifier.height(32.dp))
                            NavigationRailItem(
                                selected = !model.parent,
                                onClick = model::childMode,
                                icon = { Icon(Icons.Rounded.Home, null) },
                                label = { Text(stringResource(R.string.children)) },
                                modifier = Modifier.cinemaFocus(),
                            )
                            Spacer(Modifier.height(16.dp))
                            NavigationRailItem(
                                selected = model.parent,
                                onClick = {
                                    if (model.parent) model.back() else model.openParents()
                                },
                                icon = { Icon(Icons.Rounded.Lock, null) },
                                label = { Text(stringResource(R.string.parents)) },
                                modifier = Modifier.cinemaFocus(),
                            )
                        }
                    AnimatedContent(
                        targetState = model.screen,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        transitionSpec = {
                            (fadeIn(tween(motionDuration())) +
                                slideInHorizontally(tween(motionDuration())) {
                                    it / 16
                                }) togetherWith fadeOut(tween(motionDuration() / 2))
                        },
                        label = "screen",
                    ) { screen ->
                        when (screen) {
                            Screen.CHILD -> ChildCatalog(model, wide, tv)
                            Screen.PARENT -> ParentCatalog(model, tv)
                            Screen.PIN -> PinScreen(model, tv)
                            Screen.DETAILS -> FilmDetails(model, wide, tv)
                            Screen.LIBRARY,
                            Screen.SOURCES,
                            Screen.POLICY,
                            Screen.WORK -> ru.familycinema.app.library.LibraryUi(model, wide, tv)
                            Screen.PLAYER -> Unit
                        }
                    }
                }
            }
        }
    }
}
