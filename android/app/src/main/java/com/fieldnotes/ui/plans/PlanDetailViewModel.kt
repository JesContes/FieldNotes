package com.fieldnotes.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.domain.ChecklistGroup
import com.fieldnotes.domain.Category
import com.fieldnotes.domain.Item
import com.fieldnotes.domain.Plan
import com.fieldnotes.domain.PlanItem
import com.fieldnotes.domain.Template
import com.fieldnotes.domain.buildGroups
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanDetailUiState(
    val loading: Boolean = true,
    val plan: Plan? = null,
    val groups: List<ChecklistGroup> = emptyList(),
    val checked: Int = 0,
    val total: Int = 0,
    val hasUnchecked: Boolean = false,
)

sealed interface PlanDetailEvent {
    data class Message(val text: String) : PlanDetailEvent
    data class Removed(val name: String) : PlanDetailEvent
    data class SavedAsTemplate(val templateId: Long) : PlanDetailEvent
    data object Deleted : PlanDetailEvent
}

class PlanDetailViewModel(
    private val repository: FieldNotesRepository,
    private val planId: Long,
) : ViewModel() {

    private val eventChannel = Channel<PlanDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val pendingRemoved = MutableStateFlow<PlanItem?>(null)

    val allItems: StateFlow<List<Item>> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTemplates: StateFlow<List<Template>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<PlanDetailUiState> = combine(
        repository.observePlan(planId),
        repository.observeCategories(),
    ) { plan, categories ->
        if (plan == null) {
            PlanDetailUiState(loading = false)
        } else {
            PlanDetailUiState(
                loading = false,
                plan = plan,
                groups = buildGroups(plan.items, categories.map { it.name }),
                checked = plan.checkedCount,
                total = plan.totalCount,
                hasUnchecked = plan.hasUnchecked,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PlanDetailUiState(),
    )

    fun toggle(item: PlanItem) {
        viewModelScope.launch {
            repository.toggleItem(item.id, !item.checked)
        }
    }

    fun setAll(checked: Boolean) {
        viewModelScope.launch {
            repository.setAllChecked(planId, checked)
        }
    }

    fun remove(item: PlanItem) {
        pendingRemoved.value = item
        viewModelScope.launch {
            repository.removePlanItem(item.id)
            eventChannel.send(PlanDetailEvent.Removed(item.name))
        }
    }

    fun undoRemove() {
        val item = pendingRemoved.value ?: return
        pendingRemoved.value = null
        viewModelScope.launch {
            val restored = repository.restorePlanItem(planId, item)
            eventChannel.send(
                PlanDetailEvent.Message(if (restored) "已恢复 ${item.name}" else "恢复失败，物品已存在")
            )
        }
    }

    fun clearPendingRemoved() {
        pendingRemoved.value = null
    }

    fun addItems(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = repository.addItemsToPlan(planId, ids)
            val text = when {
                result.added == 0 -> "所选物品均已在清单中"
                result.skipped > 0 -> "已添加 ${result.added} 件，跳过 ${result.skipped} 件已有"
                else -> "已添加 ${result.added} 件"
            }
            eventChannel.send(PlanDetailEvent.Message(text))
        }
    }

    fun importTemplate(templateId: Long) {
        viewModelScope.launch {
            val result = repository.importTemplateToPlan(planId, templateId)
            val text = when {
                result.added == 0 -> "该模板物品均已在清单中"
                result.skipped > 0 -> "导入 ${result.added} 件，跳过 ${result.skipped} 件已有"
                else -> "导入 ${result.added} 件"
            }
            eventChannel.send(PlanDetailEvent.Message(text))
        }
    }

    fun saveAsTemplate(name: String) {
        viewModelScope.launch {
            runCatching { repository.savePlanAsTemplate(planId, name) }
                .onSuccess { eventChannel.send(PlanDetailEvent.SavedAsTemplate(it)) }
                .onFailure { eventChannel.send(PlanDetailEvent.Message(it.message ?: "保存失败")) }
        }
    }

    fun deletePlan() {
        viewModelScope.launch {
            repository.deletePlan(planId)
            eventChannel.send(PlanDetailEvent.Deleted)
        }
    }

    companion object {
        fun factory(container: AppContainer, planId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { PlanDetailViewModel(container.repository, planId) }
        }
    }
}
