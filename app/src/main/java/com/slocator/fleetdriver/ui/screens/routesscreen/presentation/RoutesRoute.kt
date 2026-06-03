package com.slocator.fleetdriver.ui.screens.routesscreen.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
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
    viewModel: RoutesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                RoutesEvent.ToggleLanguage -> onToggleLanguage()
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
            }
        )
    )
}
