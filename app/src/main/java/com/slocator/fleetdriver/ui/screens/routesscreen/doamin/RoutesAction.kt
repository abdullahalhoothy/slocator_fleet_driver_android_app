package com.slocator.fleetdriver.ui.screens.routesscreen.doamin

import com.slocator.fleetdriver.data.RoutePart

sealed class RoutesAction {
    data class TogglePart(val part: RoutePart, val done: Boolean) : RoutesAction()
    data class OpenRoute(val part: RoutePart) : RoutesAction()
    object Refresh : RoutesAction()
    object Logout : RoutesAction()
    object ToggleLanguage : RoutesAction()
    object PreviousDay : RoutesAction()
    object NextDay : RoutesAction()
    object StartRoute : RoutesAction()
    object EndRoute : RoutesAction()
}