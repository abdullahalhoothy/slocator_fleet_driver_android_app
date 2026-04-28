package com.slocator.fleetdriver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.slocator.fleetdriver.data.CompletionStore
import com.slocator.fleetdriver.data.DayResolver
import com.slocator.fleetdriver.data.DriverSchedule
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.ui.screens.LoginScreen
import com.slocator.fleetdriver.ui.screens.RoutesScreen
import com.slocator.fleetdriver.ui.screens.RoutesUiState
import com.slocator.fleetdriver.ui.theme.SLocatorTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen MUST be installed before super.onCreate.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SLocatorApp
        val repo = RoutesRepository(applicationContext)
        val completion = CompletionStore(applicationContext)

        setContent {
            SLocatorTheme {
                AppRoot(
                    app = app,
                    repo = repo,
                    completion = completion,
                    onOpenMaps = { url -> openInMaps(url) }
                )
            }
        }
    }

    private fun openInMaps(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // Prefer the Google Maps app, but fall back gracefully if it isn't installed.
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback: any browser/maps app that can handle the URL.
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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
    repo: RoutesRepository,
    completion: CompletionStore,
    onOpenMaps: (String) -> Unit
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    // We don't bother serializing the schedule into the saved-state bundle —
    // it's already cached on disk and the routes screen re-loads it on entry.
    var schedule by remember { mutableStateOf<DriverSchedule?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var banner by remember { mutableStateOf<String?>(null) }

    val notFoundMsg = stringResource(R.string.login_error_not_found)
    val networkMsg = stringResource(R.string.error_network)

    NavHost(
        navController = nav,
        startDestination = if (app.prefs.lastDriverId != null) "routes" else "login"
    ) {
        composable("login") {
            LoginScreen(
                initialPhone = app.prefs.lastDriverId.orEmpty(),
                isLoading = loading,
                errorText = loginError,
                onSubmit = { phone ->
                    loginError = null
                    loading = true
                    scope.launch {
                        val res = repo.loadAndParse(forceRefresh = false)
                        loading = false
                        res.onSuccess { wb ->
                            val sched = repo.extractSchedule(wb, phone)
                            if (sched == null) {
                                loginError = notFoundMsg
                            } else {
                                schedule = sched
                                app.prefs.lastDriverId = sched.driverId
                                nav.navigate("routes") { popUpTo("login") { inclusive = true } }
                            }
                        }.onFailure {
                            loginError = networkMsg
                        }
                    }
                },
                onToggleLanguage = { app.toggleLanguage() },
                languageToggleLabel = stringResource(
                    id = if ((app.prefs.languageOverride ?: "ar") == "ar")
                        R.string.lang_toggle_to_en
                    else
                        R.string.lang_toggle_to_ar
                )
            )
        }

        composable("routes") {
            // Lazy-load the schedule if we got here via "remember-me" auto-resume.
            LaunchedEffect(Unit) {
                if (schedule == null) {
                    val driverId = app.prefs.lastDriverId
                    if (driverId == null) {
                        nav.navigate("login") { popUpTo("routes") { inclusive = true } }
                        return@LaunchedEffect
                    }
                    refreshing = true
                    val res = repo.loadAndParse(forceRefresh = false)
                    refreshing = false
                    res.onSuccess { wb ->
                        val sched = repo.extractSchedule(wb, driverId)
                        if (sched == null) {
                            // saved id no longer exists in workbook
                            app.prefs.lastDriverId = null
                            nav.navigate("login") { popUpTo("routes") { inclusive = true } }
                        } else {
                            schedule = sched
                        }
                    }.onFailure {
                        banner = networkMsg
                    }
                }
            }

            val today = remember { LocalDate.now() }
            val day = schedule?.let { DayResolver.pickDay(it.days, today) }
            val parts = day?.parts.orEmpty()

            RoutesScreen(
                state = RoutesUiState(
                    driverId = schedule?.driverId.orEmpty(),
                    day = day,
                    parts = parts,
                    isRefreshing = refreshing,
                    errorBanner = banner,
                    isPartDone = { part ->
                        completion.isDone(
                            schedule?.driverId.orEmpty(),
                            day?.date ?: today,
                            part.partNumber
                        )
                    },
                    onTogglePart = { part, value ->
                        completion.setDone(
                            schedule?.driverId.orEmpty(),
                            day?.date ?: today,
                            part.partNumber,
                            value
                        )
                    },
                    onOpenRoute = { part ->
                        onOpenMaps(part.mapsUrl)
                    },
                    onRefresh = {
                        if (refreshing) return@RoutesUiState
                        banner = null
                        refreshing = true
                        scope.launch {
                            val res = repo.loadAndParse(forceRefresh = true)
                            refreshing = false
                            res.onSuccess { wb ->
                                val sched = repo.extractSchedule(
                                    wb,
                                    schedule?.driverId.orEmpty()
                                )
                                if (sched != null) schedule = sched
                            }.onFailure {
                                banner = networkMsg
                            }
                        }
                    },
                    onLogout = {
                        app.prefs.lastDriverId = null
                        schedule = null
                        nav.navigate("login") { popUpTo("routes") { inclusive = true } }
                    }
                )
            )
        }
    }
}
