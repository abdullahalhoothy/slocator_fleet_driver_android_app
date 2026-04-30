package com.slocator.fleetdriver.ui.screens.routesscreen.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slocator.fleetdriver.data.CompletionStore
import com.slocator.fleetdriver.data.DayResolver
import com.slocator.fleetdriver.data.DriverSchedule
import com.slocator.fleetdriver.data.PreferencesStore
import com.slocator.fleetdriver.data.RoutesRepository
import com.slocator.fleetdriver.data.ScheduledDay
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesAction
import com.slocator.fleetdriver.ui.screens.routesscreen.doamin.RoutesUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class RoutesViewModel(
    private val repo: RoutesRepository,
    private val prefs: PreferencesStore,
    private val completion: CompletionStore,
    private val onToggleLanguage: () -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    private var currentSchedule: DriverSchedule? = null
    private var currentDayIndex: Int = -1

    init {
        loadData()
    }

    fun handleAction(action: RoutesAction, langArLabel: String = "", langEnLabel: String = "") {
        if (langArLabel.isNotEmpty() || langEnLabel.isNotEmpty()) {
            updateLanguageLabel(langArLabel, langEnLabel)
        }

        when (action) {
            is RoutesAction.TogglePart -> togglePart(action.part.partNumber, action.done)
            is RoutesAction.OpenRoute -> { /* Handled by Route component for navigation/intent */ }
            RoutesAction.Refresh -> refresh()
            RoutesAction.Logout -> logout()
            RoutesAction.ToggleLanguage -> {
                onToggleLanguage()
                updateLanguageLabel(langArLabel, langEnLabel)
            }
            RoutesAction.PreviousDay -> switchDay(-1)
            RoutesAction.NextDay -> switchDay(1)
        }
    }

    private fun updateLanguageLabel(ar: String, en: String) {
        _uiState.update {
            it.copy(
                languageToggleLabel = if ((prefs.languageOverride ?: "ar") == "ar") en else ar
            )
        }
    }

    private fun loadData(forceRefresh: Boolean = false) {
        val driverId = prefs.lastDriverId ?: return
        val managerPhone = prefs.lastManagerPhone ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorBanner = null) }
            val res = repo.fetchSchedule(driverId, managerPhone)
            res.onSuccess { sched ->
                currentSchedule = sched
                val today = Clock.System.todayIn(TimeZone.Companion.currentSystemDefault())
                val day = DayResolver.pickDay(sched.days, today)
                currentDayIndex = sched.days.indexOf(day)

                updateUiWithDay(day)
            }.onFailure {
                _uiState.update { it.copy(isRefreshing = false, errorBanner = "network") }
            }
        }
    }

    private fun updateUiWithDay(day: ScheduledDay?) {
        val driverId = prefs.lastDriverId ?: return
        val today = Clock.System.todayIn(TimeZone.Companion.currentSystemDefault())
        
        // Sync completed parts set
        val completed = day?.parts?.filter {
            completion.isDone(driverId, day.date ?: today, it.partNumber)
        }?.map { it.partNumber }?.toSet() ?: emptySet()

        _uiState.update {
            it.copy(
                driverId = driverId,
                day = day,
                parts = day?.parts.orEmpty().mapIndexed { index, part -> part.copy(partNumber = index + 1) },
                isRefreshing = false,
                completedParts = completed,
                errorBanner = null,
                hasPreviousDay = currentDayIndex > 0,
                hasNextDay = currentDayIndex < (currentSchedule?.days?.size?.minus(1) ?: -1),
                currentDayIndex = currentDayIndex,
                onPreviousDay = { handleAction(RoutesAction.PreviousDay) },
                onNextDay = { handleAction(RoutesAction.NextDay) }
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
        loadData(forceRefresh = true)
    }

    private fun togglePart(partNumber: Int, done: Boolean) {
        val driverId = _uiState.value.driverId
        val today = Clock.System.todayIn(TimeZone.Companion.currentSystemDefault())
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
        prefs.lastDriverId = null
    }
}