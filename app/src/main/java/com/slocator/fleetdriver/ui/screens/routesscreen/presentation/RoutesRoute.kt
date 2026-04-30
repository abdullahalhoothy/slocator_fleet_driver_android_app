package com.slocator.fleetdriver.ui.screens.routesscreen.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.screens.routesscreen.data.RoutesViewModel
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesAction
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RoutesRoute(
    onOpenMaps: (String) -> Unit,
    onLogout: () -> Unit,
    onToggleLanguage: () -> Unit,
    viewModel: RoutesViewModel = koinViewModel(parameters = {
        parametersOf(onToggleLanguage)
    })
) {
    val state by viewModel.uiState.collectAsState()

    val langArLabel = stringResource(R.string.lang_toggle_to_ar)
    val langEnLabel = stringResource(R.string.lang_toggle_to_en)

    // Sync labels on start
    LaunchedEffect(langArLabel, langEnLabel) {
        viewModel.handleAction(RoutesAction.Refresh, langArLabel, langEnLabel)
    }

    val networkErrorMsg = stringResource(R.string.error_network)

    RoutesScreen(
        state = state.copy(
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
            onOpenRoute = { part -> onOpenMaps(part.mapsUrl) },
            onRefresh = { viewModel.handleAction(RoutesAction.Refresh) },
            onLogout = {
                viewModel.handleAction(RoutesAction.Logout)
                onLogout()
            },
            onToggleLanguage = {
                viewModel.handleAction(RoutesAction.ToggleLanguage, langArLabel, langEnLabel)
            }
        )
    )
}
