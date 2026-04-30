package com.slocator.fleetdriver.ui.screens.routesscreen.doamin

import com.slocator.fleetdriver.data.RoutePart
import com.slocator.fleetdriver.data.ScheduledDay

data class RoutesUiState(
    val driverId: String = "",
    val day: ScheduledDay? = null,
    val parts: List<RoutePart> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorBanner: String? = null,
    val completedParts: Set<Int> = emptySet(),
    val languageToggleLabel: String = "",
    val hasPreviousDay: Boolean = false,
    val hasNextDay: Boolean = false,
    val currentDayIndex: Int = 0,
    val onPreviousDay: () -> Unit = { },
    val onNextDay: () -> Unit = { },
    val isPartDone: (RoutePart) -> Boolean = { false },
    val onTogglePart: (RoutePart, Boolean) -> Unit = { _, _ -> },
    val onOpenRoute: (RoutePart) -> Unit = { _ -> },
    val onRefresh: () -> Unit = { },
    val onLogout: () -> Unit = { },
    val onToggleLanguage: () -> Unit = { }
)