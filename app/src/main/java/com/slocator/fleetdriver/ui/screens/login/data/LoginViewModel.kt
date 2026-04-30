package com.slocator.fleetdriver.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.ui.screens.login.domain.LoginAction
import com.slocator.fleetdriver.ui.screens.login.domain.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repo: RoutesRepository,
    private val prefs: PreferencesStore,
    private val onLoginSuccess: (String) -> Unit,
    private val onToggleLanguage: () -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            initialPhone = prefs.lastDriverId.orEmpty(),
            initialManagerPhone = prefs.lastManagerPhone.orEmpty(),
            languageToggleLabel = "" // Will be updated by Route
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun handleAction(action: LoginAction, messages: LoginMessages) {
        updateLabels(messages.langArLabel, messages.langEnLabel)
        when (action) {
            is LoginAction.Submit -> {
                if (action.phone.isNotBlank() && action.managerPhone.isNotBlank()) {
                    submit(action.phone, action.managerPhone, messages.notFoundMsg, messages.networkMsg)
                }
            }
            LoginAction.ToggleLanguage -> toggleLanguage(messages.langArLabel, messages.langEnLabel)
            LoginAction.RefreshLabels -> { /* Just trigger updateLabels above */ }
        }
    }

    private fun updateLabels(ar: String, en: String) {
        _uiState.update { 
            it.copy(
                languageToggleLabel = if ((prefs.languageOverride ?: "ar") == "ar") en else ar
            )
        }
    }

    private fun submit(phone: String, managerPhone: String, notFound: String, network: String) {
        _uiState.update { it.copy(isLoading = true, errorText = null) }
        viewModelScope.launch {
            val res = repo.fetchSchedule(phone, managerPhone)
            _uiState.update { it.copy(isLoading = false) }
            res.onSuccess { sched ->
                prefs.lastDriverId = phone
                prefs.lastManagerPhone = managerPhone
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onLoginSuccess(phone)
                }
            }.onFailure {
                _uiState.update { it.copy(errorText = network) }
            }
        }
    }

    private fun toggleLanguage(ar: String, en: String) {
        onToggleLanguage()
        updateLabels(ar, en)
    }
}

data class LoginMessages(
    val notFoundMsg: String,
    val networkMsg: String,
    val langArLabel: String,
    val langEnLabel: String
)
