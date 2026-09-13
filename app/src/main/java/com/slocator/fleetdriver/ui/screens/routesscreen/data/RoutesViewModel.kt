package com.slocator.fleetdriver.ui.screens.routesscreen.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slocator.fleetdriver.data.CompletionStore
import com.slocator.fleetdriver.data.DayResolver
import com.slocator.fleetdriver.data.DriverSchedule
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RouteTrackingApi
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.data.ScheduledDay
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesAction
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesTab
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

sealed class RoutesEvent {
    object ToggleLanguage : RoutesEvent()
    object StartTrackingService : RoutesEvent()
    object StopTrackingService : RoutesEvent()
}

class RoutesViewModel(
    private val repo: RoutesRepository,
    private val prefs: PreferencesStore,
    private val completion: CompletionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RoutesUiState(isRouteActive = prefs.sessionId != null)
    )
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    private val _events = Channel<RoutesEvent>()
    val events = _events.receiveAsFlow()

    private var currentSchedule: DriverSchedule? = null
    private var currentDayIndex: Int = -1

    init {
        loadData()
        loadTerritories()
    }

    fun handleAction(action: RoutesAction) {
        when (action) {
            is RoutesAction.TogglePart -> togglePart(action.part.partNumber, action.done)
            is RoutesAction.OpenRoute -> { /* Handled by Route component for navigation/intent */ }
            RoutesAction.Refresh -> refresh()
            RoutesAction.Logout -> logout()
            RoutesAction.ToggleLanguage -> {
                viewModelScope.launch {
                    _events.send(RoutesEvent.ToggleLanguage)
                }
            }
            RoutesAction.PreviousDay -> switchDay(-1)
            RoutesAction.NextDay -> switchDay(1)
            RoutesAction.StartRoute -> startRoute()
            RoutesAction.EndRoute -> endRoute()
            is RoutesAction.SelectTab -> selectTab(action.tab)
            is RoutesAction.AddNote -> postNote(action.text)
            RoutesAction.OpenNoteDialog -> {
                _uiState.update { it.copy(showNoteDialog = true, noteText = "") }
            }
            RoutesAction.DismissNoteDialog -> {
                _uiState.update { it.copy(showNoteDialog = false, noteText = "") }
            }
            is RoutesAction.UpdateNoteText -> {
                _uiState.update { it.copy(noteText = action.text) }
            }
        }
    }

    private fun loadData() {
        val driverId = prefs.driverId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorBanner = null) }
            repo.fetchSchedule(driverId)
                .onSuccess { sched ->
                    currentSchedule = sched
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    // Exact date match first; otherwise fall back to the first planned day.
                    val day = DayResolver.pickDay(sched.days, today) ?: sched.days.firstOrNull()
                    currentDayIndex = sched.days.indexOf(day)

                    // Store report URLs from the response
                    _uiState.update { it.copy(reportUrls = sched.reportUrls) }

                    updateUiWithDay(day)
                }
                .onFailure {
                    _uiState.update { it.copy(isRefreshing = false, errorBanner = "network") }
                }
        }
    }

    private fun updateUiWithDay(day: ScheduledDay?) {
        val driverId = prefs.driverId ?: return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Sync completed parts set
        val completed = day?.parts?.filter {
            completion.isDone(driverId, day.date ?: today, it.partNumber)
        }?.map { it.partNumber }?.toSet() ?: emptySet()

        _uiState.update {
            it.copy(
                driverId = driverId,
                driverName = currentSchedule?.driverName.orEmpty(),
                driverPhone = prefs.driverPhone.orEmpty(),
                day = day,
                parts = day?.parts.orEmpty().mapIndexed { index, part -> part.copy(partNumber = index + 1) },
                isRefreshing = false,
                completedParts = completed,
                errorBanner = null,
                hasPreviousDay = currentDayIndex > 0,
                hasNextDay = currentDayIndex < (currentSchedule?.days?.size?.minus(1) ?: -1),
                currentDayIndex = currentDayIndex,
                onPreviousDay = { handleAction(RoutesAction.PreviousDay) },
                onNextDay = { handleAction(RoutesAction.NextDay) },
                onStartRoute = { handleAction(RoutesAction.StartRoute) },
                onEndRoute = { handleAction(RoutesAction.EndRoute) }
            )
        }
    }

    private fun switchDay(delta: Int) {
        val sched = currentSchedule ?: return
        val newIndex = currentDayIndex + delta
        if (newIndex in sched.days.indices) {
            currentDayIndex = newIndex
            updateUiWithDay(sched.days[newIndex])
        }
    }

    private fun refresh() {
        loadData()
        loadTerritories()
    }

    private fun selectTab(tab: RoutesTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == RoutesTab.TERRITORIES && _uiState.value.territoryLocations.isEmpty()) {
            loadTerritories()
        }
    }

    private fun loadTerritories() {
        val driverId = prefs.driverId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isTerritoriesLoading = true) }
            repo.fetchTerritories(driverId)
                .onSuccess { territories ->
                    // Territories are an unordered set of locations: flatten and
                    // de-duplicate customers that appear in more than one territory.
                    val locations = territories
                        .flatMap { it.customers }
                        .distinctBy { it.customerId }
                    _uiState.update {
                        it.copy(isTerritoriesLoading = false, territoryLocations = locations)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isTerritoriesLoading = false) }
                }
        }
    }

    private fun togglePart(partNumber: Int, done: Boolean) {
        val driverId = _uiState.value.driverId
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val date = _uiState.value.day?.date ?: today

        completion.setDone(driverId, date, partNumber, done)

        _uiState.update { state ->
            val newCompleted = if (done) {
                state.completedParts + partNumber
            } else {
                state.completedParts - partNumber
            }
            state.copy(completedParts = newCompleted)
        }
    }

    private fun logout() {
        prefs.driverId = null
        prefs.driverPhone = null
        prefs.sessionId = null
    }

    // ── Route-tracking ──────────────────────────────────────────────

    private fun startRoute() {
        val driverId = prefs.driverId ?: return
        val routeDayId = _uiState.value.day?.routeDayId

        _uiState.update { it.copy(isTrackingLoading = true) }

        viewModelScope.launch {
            RouteTrackingApi.startRoute(driverId, routeDayId)
                .onSuccess { response ->
                    prefs.sessionId = response.sessionId
                    _uiState.update {
                        it.copy(
                            isRouteActive = true,
                            isTrackingLoading = false,
                            errorBanner = null
                        )
                    }
                    _events.send(RoutesEvent.StartTrackingService)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTrackingLoading = false,
                            errorBanner = error.message ?: "network"
                        )
                    }
                }
        }
    }

    private fun endRoute() {
        val driverId = prefs.driverId ?: return

        _uiState.update { it.copy(isTrackingLoading = true) }

        viewModelScope.launch {
            RouteTrackingApi.endRoute(driverId)
                .onSuccess {
                    prefs.sessionId = null
                    _uiState.update {
                        it.copy(
                            isRouteActive = false,
                            isTrackingLoading = false,
                            errorBanner = null
                        )
                    }
                    _events.send(RoutesEvent.StopTrackingService)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isTrackingLoading = false,
                            errorBanner = error.message ?: "network"
                        )
                    }
                }
        }
    }

    // ── Note posting ────────────────────────────────────────────────

    private fun postNote(noteText: String) {
        val driverId = prefs.driverId ?: return

        _uiState.update { it.copy(isNoteSending = true) }

        viewModelScope.launch {
            RouteTrackingApi.postNote(driverId, noteText, prefs.sessionId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            showNoteDialog = false,
                            noteText = "",
                            isNoteSending = false,
                            errorBanner = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isNoteSending = false,
                            errorBanner = error.message ?: "network"
                        )
                    }
                }
        }
    }
}
