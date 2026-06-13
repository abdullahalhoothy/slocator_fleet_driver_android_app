package com.slocator.fleetdriver.ui.screens.routesscreen.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.slocator.fleetdriver.LocationTrackingService
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.screens.routesscreen.data.RoutesEvent
import com.slocator.fleetdriver.ui.screens.routesscreen.data.RoutesViewModel
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesAction
import org.koin.androidx.compose.koinViewModel

@Composable
fun RoutesRoute(
    onOpenMaps: (String) -> Unit,
    onLogout: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenReport: (url: String, title: String) -> Unit = { _, _ -> },
    viewModel: RoutesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Tracks whether a Start Route request is waiting for permissions.
    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    // ── Step 2: POST_NOTIFICATIONS launcher (API 33+) ──────────────
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingStartAfterPermission) {
            pendingStartAfterPermission = false
            checkGpsAndStartRoute(viewModel, context)
        } else if (pendingStartAfterPermission) {
            pendingStartAfterPermission = false
            Toast.makeText(
                context,
                R.string.tracking_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ── Step 1: Location permission launcher ───────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            // Location granted — now check / request notification permission (API 33+).
            if (pendingStartAfterPermission) {
                requestNotificationIfNeeded(context, notificationPermissionLauncher) { proceed ->
                    if (proceed) {
                        pendingStartAfterPermission = false
                        checkGpsAndStartRoute(viewModel, context)
                    } else {
                        pendingStartAfterPermission = false
                        Toast.makeText(
                            context,
                            R.string.tracking_permission_denied,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } else {
            pendingStartAfterPermission = false
            Toast.makeText(
                context,
                R.string.tracking_permission_denied,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ── Events from ViewModel ───────────────────────────────────────
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                RoutesEvent.ToggleLanguage -> onToggleLanguage()
                is RoutesEvent.StartTrackingService -> {
                    // Permission + GPS already confirmed before the API call,
                    // so start the service directly.
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_START
                        putExtra(LocationTrackingService.EXTRA_DRIVER_PHONE, event.driverPhone)
                    }
                    ContextCompat.startForegroundService(context, intent)
                }
                RoutesEvent.StopTrackingService -> {
                    val intent = Intent(context, LocationTrackingService::class.java).apply {
                        action = LocationTrackingService.ACTION_STOP
                    }
                    context.startService(intent)
                }
            }
        }
    }

    val langToggleLabel = stringResource(R.string.lang_toggle_label)
    val networkErrorMsg = stringResource(R.string.error_network)

    RoutesScreen(
        state = state.copy(
            languageToggleLabel = langToggleLabel,
            errorBanner = if (state.errorBanner == "network") networkErrorMsg else state.errorBanner,
            isPartDone = { part -> state.completedParts.contains(part.partNumber) },
            onTogglePart = { part, done ->
                viewModel.handleAction(
                    RoutesAction.TogglePart(
                        part,
                        done
                    )
                )
            },
            onOpenRoute = { part ->
                // Mark the part as done when the user opens the map
                viewModel.handleAction(RoutesAction.TogglePart(part, true))
                onOpenMaps(part.mapsUrl)
            },
            onRefresh = { viewModel.handleAction(RoutesAction.Refresh) },
            onLogout = {
                viewModel.handleAction(RoutesAction.Logout)
                onLogout()
            },
            onToggleLanguage = {
                viewModel.handleAction(RoutesAction.ToggleLanguage)
            },
            onStartRoute = {
                // Two-step permission guard: location → notification → GPS → API.
                if (!hasLocationPermission(context)) {
                    pendingStartAfterPermission = true
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else if (!hasNotificationPermission(context)) {
                    pendingStartAfterPermission = true
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    checkGpsAndStartRoute(viewModel, context)
                }
            },
            onOpenReport = { url, title -> onOpenReport(url, title) }
        )
    )
}

/** True when either FINE or COARSE location is granted. */
private fun hasLocationPermission(context: android.content.Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

/** True when POST_NOTIFICATIONS is granted (or pre-API-33 where it isn't needed). */
private fun hasNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * If running on API 33+ and POST_NOTIFICATIONS isn't granted yet,
 * launches the system permission dialog. Otherwise invokes [onResult](true) immediately.
 */
private fun requestNotificationIfNeeded(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    onResult: (Boolean) -> Unit
) {
    if (hasNotificationPermission(context)) {
        onResult(true)
    } else {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** Checks whether GPS is enabled. If not, opens Location settings. */
private fun checkGpsAndStartRoute(viewModel: RoutesViewModel, context: android.content.Context) {
    val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
    val isGpsOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)

    if (!isGpsOn) {
        Toast.makeText(
            context,
            R.string.tracking_gps_off,
            Toast.LENGTH_LONG
        ).show()
        // Open location settings so the user can turn on GPS.
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } else {
        // Both permission + GPS are good — proceed.
        viewModel.handleAction(RoutesAction.StartRoute)
    }
}
