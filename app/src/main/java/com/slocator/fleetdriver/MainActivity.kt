package com.slocator.fleetdriver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.core.os.LocaleListCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slocator.fleetdriver.ui.screens.login.presentation.LoginRoute
import com.slocator.fleetdriver.ui.screens.routesscreen.presentation.RoutesRoute
import com.slocator.fleetdriver.ui.theme.SLocatorTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as SLocatorApp
        
        // Ensure the locale is applied as early as possible.
        val targetTag = app.prefs.languageOverride ?: "ar"
        val current = AppCompatDelegate.getApplicationLocales()
        if (current.isEmpty || current.get(0)?.language != targetTag) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetTag))
        }

        // Splash screen MUST be installed before super.onCreate.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SLocatorTheme {
                AppRoot(
                    app = app,
                    onOpenMaps = { url -> openInMaps(url) }
                )
            }
        }
    }

    private fun openInMaps(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            // Prefer the Google Maps app, but fall back gracefully if it isn't installed.
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback: any browser/maps app that can handle the URL.
            val fallback = Intent(Intent.ACTION_VIEW, url.toUri())
            if (fallback.resolveActivity(packageManager) != null) {
                startActivity(fallback)
            } else {
                Toast.makeText(this, R.string.error_no_maps, Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
private fun AppRoot(
    app: SLocatorApp,
    onOpenMaps: (String) -> Unit
) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = if (app.prefs.lastDriverId != null) "routes" else "login"
    ) {
        composable("login") {
            LoginRoute(
                onLoginSuccess = {
                    nav.navigate("routes") { popUpTo("login") { inclusive = true } }
                },
                onToggleLanguage = { app.toggleLanguage() }
            )
        }

        composable("routes") {
            RoutesRoute(
                onOpenMaps = onOpenMaps,
                onLogout = {
                    nav.navigate("login") { popUpTo("routes") { inclusive = true } }
                },
                onToggleLanguage = { app.toggleLanguage() }
            )
        }
    }
}
