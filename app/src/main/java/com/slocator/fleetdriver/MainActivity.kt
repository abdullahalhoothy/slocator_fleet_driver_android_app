package com.slocator.fleetdriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slocator.fleetdriver.ui.screens.login.presentation.LoginRoute
import com.slocator.fleetdriver.ui.screens.routesscreen.presentation.RoutesRoute
import com.slocator.fleetdriver.ui.theme.SLocatorTheme
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("slocator_app", MODE_PRIVATE)
        val targetTag = prefs.getString("language_override", "ar") ?: "ar"
        
        val locale = Locale.Builder().setLanguage(targetTag).build()
        Locale.setDefault(locale)
        
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as SLocatorApp
        

        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val targetTag = app.prefs.languageOverride ?: "ar"
            val layoutDirection = if (targetTag == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                SLocatorTheme {
                    AppRoot(
                        app = app,
                        onOpenMaps = { url -> openInMaps(url) }
                    )
                }
            }
        }
    }

    private fun convertApiUrlToWebUrl(apiUrl: String): String {
        try {
            val uri = apiUrl.toUri()
            if (uri.getQueryParameter("api") == "1") {
                val origin = uri.getQueryParameter("origin") ?: ""
                val dest = uri.getQueryParameter("destination") ?: ""
                val wpsStr = uri.getQueryParameter("waypoints") ?: ""

                val wps = wpsStr.split("|").filter { it.isNotBlank() }

                val builder = StringBuilder("https://www.google.com/maps/dir/")
                

                builder.append("/")


                if (origin.isNotBlank()) {
                    builder.append(origin).append("/")
                }

                for (wp in wps) {
                    builder.append(wp).append("/")
                }

                if (dest.isNotBlank()) {
                    builder.append(dest).append("/")
                }

                return builder.toString()
            }
        } catch (e: Exception) {
            Log.e("URL_CONVERT", "Failed to convert URL: ${e.message}")
        }
        return apiUrl
    }

    private fun openInMaps(url: String) {
        val webUrl = convertApiUrlToWebUrl(url)

        val intent = Intent(Intent.ACTION_VIEW, webUrl.toUri()).apply {
            // Prefer the Google Maps app, but fall back gracefully if it isn't installed.
            setPackage("com.google.android.apps.maps")
        }
        Log.d("urlO", url)
        Log.d("url", webUrl)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else { // Fallback: any browser/maps app that can handle the URL.
            val fallback = Intent(Intent.ACTION_VIEW, webUrl.toUri())
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
                    nav.navigate("routes") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onToggleLanguage = { app.toggleLanguage() }
            )
        }

        composable("routes") {
            RoutesRoute(
                onOpenMaps = onOpenMaps,
                onLogout = {
                    nav.navigate("login") {
                        popUpTo("routes") { inclusive = true }
                    }
                },
                onToggleLanguage = { app.toggleLanguage() }
            )
        }
    }
}
