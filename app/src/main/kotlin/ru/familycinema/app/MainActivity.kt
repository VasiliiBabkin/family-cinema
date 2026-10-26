package ru.familycinema.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    val cinema: CinemaViewModel by viewModels()

    private val filePicker =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { uri ->
            cinema.acceptPickedFile(uri)
        }

    fun chooseVideo(title: String, age: Int?, series: Boolean, season: Int, number: Int) {
        if (!cinema.parent) return
        cinema.prepareImport(title, age, series, season, number)
        try {
            filePicker.launch(arrayOf("video/*"))
        } catch (_: android.content.ActivityNotFoundException) {
            cinema.acceptPickedFile(null)
            cinema.library.pickerUnavailable()
        }
    }

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
