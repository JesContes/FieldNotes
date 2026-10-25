package com.fieldnotes.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.data.prefs.SettingsStore
import com.fieldnotes.domain.PlanCardUi
import com.fieldnotes.domain.PlanFilter
import com.fieldnotes.domain.Plan
import com.fieldnotes.domain.buildPlanCard
import com.fieldnotes.domain.sortPlans
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlansUiState(
    val loading: Boolean = true,
    val filter: PlanFilter = PlanFilter.ALL,
    val query: String = "",
    val plans: List<PlanCardUi> = emptyList(),
    val totalPlans: Int = 0,
    val pendingTotal: Int = 0,
    val counts: Map<PlanFilter, Int> = emptyMap(),
)

class PlansViewModel(
    private val repository: FieldNotesRepository,
    private val settingsStore: SettingsStore,
    private val todayFlow: StateFlow<LocalDate>,
) : ViewModel() {

    private val filter = MutableStateFlow(PlanFilter.ALL)
    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val saved = settingsStore.planFilter.first()
            filter.value = PlanFilter.entries.firstOrNull { it.label == saved } ?: PlanFilter.ALL
        }
    }

    val uiState: StateFlow<PlansUiState> = combine(
        repository.observePlans(),
        todayFlow,
        filter,
        query,
    ) { plans, today, currentFilter, currentQuery ->
        val sorted = sortPlans(plans, today)
        val matching = sorted.filter { plan ->
            matchesFilter(plan, currentFilter, today) &&
                (currentQuery.isBlank() || plan.destination.contains(currentQuery, true) ||
                    plan.title.contains(currentQuery, true))
        }
        PlansUiState(
            loading = false,
            filter = currentFilter,
            query = currentQuery,
            plans = matching.map { buildPlanCard(it, today) },
            totalPlans = plans.size,
            pendingTotal = plans.sumOf { plan -> plan.items.count { !it.checked } },
            counts = PlanFilter.entries.associateWith { f ->
                plans.count { matchesFilter(it, f, today) }
            },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlansUiState(),
    )

    fun setFilter(value: PlanFilter) {
        filter.value = value
        viewModelScope.launch { settingsStore.setPlanFilter(value.label) }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    private fun matchesFilter(plan: Plan, value: PlanFilter, today: LocalDate): Boolean {
        if (value == PlanFilter.ALL) return true
        val status = com.fieldnotes.domain.PlanStatus.of(plan.start, plan.end, today)
        return when (value) {
            PlanFilter.ONGOING -> status == com.fieldnotes.domain.PlanStatus.ONGOING
            PlanFilter.UPCOMING -> status == com.fieldnotes.domain.PlanStatus.UPCOMING
            PlanFilter.PAST -> status == com.fieldnotes.domain.PlanStatus.PAST
            PlanFilter.ALL -> true
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlansViewModel(container.repository, container.settingsStore, container.todayFlow)
            }
        }
    }
}
