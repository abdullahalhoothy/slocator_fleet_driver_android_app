package com.slocator.fleetdriver.ui.screens.login.domain

data class LoginUiState(
    val initialPhone: String = "",
    val initialManagerPhone: String = "",
    val isLoading: Boolean = false,
    val errorText: String? = null,
    val languageToggleLabel: String = ""
)