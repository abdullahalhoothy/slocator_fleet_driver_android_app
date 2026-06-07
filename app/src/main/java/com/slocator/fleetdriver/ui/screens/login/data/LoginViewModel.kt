package com.slocator.fleetdriver.ui.screens.login.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.ui.screens.login.domain.LoginAction
import com.slocator.fleetdriver.ui.screens.login.domain.LoginUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class LoginEvent {
    data class Success(val phone: String) : LoginEvent()
    object ToggleLanguage : LoginEvent()
}

class LoginViewModel(
    private val repo: RoutesRepository,
    private val prefs: PreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            initialPhone = prefs.lastDriverId.orEmpty(),
            initialManagerPhone = prefs.lastManagerPhone.orEmpty(),
            languageToggleLabel = "" // Will be updated by Route
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun handleAction(action: LoginAction) {
        when (action) {
            is LoginAction.Submit -> {
                if (action.phone.isNotBlank() && action.managerPhone.isNotBlank()) {
                    submit(action.phone, action.managerPhone)
                }
            }
            LoginAction.ToggleLanguage -> {
                viewModelScope.launch {
                    _events.send(LoginEvent.ToggleLanguage)
                }
            }
            LoginAction.RefreshLabels -> { }
        }
    }

    private fun submit(phone: String, managerPhone: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorText = null) }
        viewModelScope.launch {
            val res = repo.fetchSchedule(phone, managerPhone)
            res.onSuccess { _ ->
                prefs.lastDriverId = phone
                prefs.lastManagerPhone = managerPhone
                _events.send(LoginEvent.Success(phone))
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, errorText = "network") }
            }
        }
    }
}
