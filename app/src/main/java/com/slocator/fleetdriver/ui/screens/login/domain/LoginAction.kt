package com.slocator.fleetdriver.ui.screens.login.domain

sealed class LoginAction {
    data class Submit(val phone: String) : LoginAction()
    object ToggleLanguage : LoginAction()
    object RefreshLabels : LoginAction()
}