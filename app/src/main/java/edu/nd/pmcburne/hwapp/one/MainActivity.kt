// File: app/src/main/java/edu/nd/pmcburne/hwapp/one/MainActivity.kt
package edu.nd.pmcburne.hwapp.one

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.nd.pmcburne.hwapp.one.navigation.AppNavGraph
import edu.nd.pmcburne.hwapp.one.ui.theme.HWStarterRepoTheme

private const val APP_SETTINGS_PREFS = "memo_app_settings"
private const val KEY_DARK_MODE = "dark_mode"

class MainActivity : ComponentActivity() {
    private var darkModeEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(APP_SETTINGS_PREFS, Context.MODE_PRIVATE)
        darkModeEnabled = prefs.getBoolean(KEY_DARK_MODE, false)

        setContent {
            HWStarterRepoTheme(darkTheme = darkModeEnabled) {
                AppNavGraph(
                    onThemeChanged = { enabled ->
                        darkModeEnabled = enabled
                        prefs.edit()
                            .putBoolean(KEY_DARK_MODE, enabled)
                            .apply()
                    }
                )
            }
        }
    }
}