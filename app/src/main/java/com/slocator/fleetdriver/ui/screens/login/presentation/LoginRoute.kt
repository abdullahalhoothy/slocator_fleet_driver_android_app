package com.slocator.fleetdriver.ui.screens.login.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.slocator.fleetdriver.R
import com.slocator.fleetdriver.ui.screens.login.domain.LoginAction
import com.slocator.fleetdriver.ui.screens.LoginMessages
import com.slocator.fleetdriver.ui.screens.LoginViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun LoginRoute(
    onLoginSuccess: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(parameters = {
        parametersOf(onLoginSuccess, onToggleLanguage)
    })
) {
    val state by viewModel.uiState.collectAsState()

    val messages = LoginMessages(
        notFoundMsg = stringResource(R.string.login_error_not_found),
        networkMsg = stringResource(R.string.error_network),
        langArLabel = stringResource(R.string.lang_toggle_to_ar),
        langEnLabel = stringResource(R.string.lang_toggle_to_en)
    )

    // Initial label setup
    LaunchedEffect(messages) {
        viewModel.handleAction(LoginAction.RefreshLabels, messages)
    }

    LoginScreen(
        initialPhone = state.initialPhone,
        initialManagerPhone = state.initialManagerPhone,
        isLoading = state.isLoading,
        errorText = state.errorText,
        onSubmit = { phone, managerPhone -> viewModel.handleAction(LoginAction.Submit(phone, managerPhone), messages) },
        onToggleLanguage = { viewModel.handleAction(LoginAction.ToggleLanguage, messages) },
        languageToggleLabel = state.languageToggleLabel
    )
}
