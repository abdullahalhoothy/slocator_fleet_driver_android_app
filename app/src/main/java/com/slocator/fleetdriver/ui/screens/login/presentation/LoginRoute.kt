package com.slocator.fleetdriver.ui.screens.login.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.screens.login.data.LoginEvent
import com.slocator.fleetdriver.ui.screens.login.domain.LoginAction
import com.slocator.fleetdriver.ui.screens.login.data.LoginViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoute(
    onLoginSuccess: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    viewModel: LoginViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.Success -> onLoginSuccess(event.phone)
                LoginEvent.ToggleLanguage -> onToggleLanguage()
            }
        }
    }

    val networkMsg = stringResource(R.string.error_network)
    val langToggleLabel = stringResource(R.string.lang_toggle_label)

    LoginScreen(
        initialPhone = state.initialPhone,
        initialManagerPhone = state.initialManagerPhone,
        isLoading = state.isLoading,
        errorText = if (state.errorText == "network") networkMsg else state.errorText,
        onSubmit = { phone, managerPhone -> viewModel.handleAction(LoginAction.Submit(phone, managerPhone)) },
        onToggleLanguage = { viewModel.handleAction(LoginAction.ToggleLanguage) },
        languageToggleLabel = langToggleLabel
    )
}
