package com.slocator.fleetdriver.ui.screens.routesscreen.doamin

import com.slocator.fleetdriver.data.CustomerOut
import com.slocator.fleetdriver.data.ReportUrls
import com.slocator.fleetdriver.data.RoutePart
import com.slocator.fleetdriver.data.ScheduledDay

/** Which list the screen is showing. */
enum class RoutesTab { ROUTES, TERRITORIES }

data class RoutesUiState(
    val driverId: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val day: ScheduledDay? = null,
    val parts: List<RoutePart> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
    val completedParts: Set<Int> = emptySet(),
    val languageToggleLabel: String = "",
    val hasPreviousDay: Boolean = false,
    val hasNextDay: Boolean = false,
    val currentDayIndex: Int = 0,
    // Tabs
    val selectedTab: RoutesTab = RoutesTab.ROUTES,
    // Territories tab — an unordered list of customer locations
    val territoryLocations: List<CustomerOut> = emptyList(),
    val isTerritoriesLoading: Boolean = false,
    // Route-tracking state
    val isRouteActive: Boolean = false,
    val isTrackingLoading: Boolean = false,
    // Note dialog state
    val showNoteDialog: Boolean = false,
    val noteText: String = "",
    val isNoteSending: Boolean = false,
    // Report URLs from the server response
    val reportUrls: ReportUrls = ReportUrls(null, null, null),
    // Callbacks
    val onPreviousDay: () -> Unit = { },
    val onNextDay: () -> Unit = { },
    val isPartDone: (RoutePart) -> Boolean = { false },
    val onTogglePart: (RoutePart, Boolean) -> Unit = { _, _ -> },
    val onOpenRoute: (RoutePart) -> Unit = { _ -> },
    val onRefresh: () -> Unit = { },
    val onLogout: () -> Unit = { },
    val onToggleLanguage: () -> Unit = { },
    val onStartRoute: () -> Unit = { },
    val onEndRoute: () -> Unit = { },
    val onAddNote: () -> Unit = { },
    val onDismissNote: () -> Unit = { },
    val onNoteTextChange: (String) -> Unit = { },
    val onSubmitNote: () -> Unit = { },
    val onOpenReport: (url: String, title: String) -> Unit = { _, _ -> },
    val onSelectTab: (RoutesTab) -> Unit = { },
    val onOpenLocation: (CustomerOut) -> Unit = { }
)