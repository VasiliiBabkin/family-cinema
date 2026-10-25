package ru.familycinema.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    val cinema: CinemaViewModel by viewModels()

    override fun attachBaseContext(base: Context) {
        val configuration =
            Configuration(base.resources.configuration).apply { setLocale(java.util.Locale("ru")) }
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { CinemaApp(cinema) }
    }

    override fun onStart() {
        super.onStart()
        cinema.foreground = true
    }

    override fun onStop() {
        cinema.onBackground(isChangingConfigurations)
        super.onStop()
    }
}
