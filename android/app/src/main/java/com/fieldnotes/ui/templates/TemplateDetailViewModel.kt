package com.fieldnotes.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.domain.Item
import com.fieldnotes.domain.Category
import com.fieldnotes.domain.Plan
import com.fieldnotes.domain.Template
import com.fieldnotes.domain.TemplateItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateGroup(
    val categoryName: String,
    val items: List<TemplateItem>,
)

data class TemplateDetailUiState(
    val loading: Boolean = true,
    val template: Template? = null,
    val groups: List<TemplateGroup> = emptyList(),
)

sealed interface TemplateDetailEvent {
    data class Message(val text: String) : TemplateDetailEvent
    data class Removed(val name: String) : TemplateDetailEvent
    data class Duplicated(val id: Long) : TemplateDetailEvent
    data object Deleted : TemplateDetailEvent
}

class TemplateDetailViewModel(
    private val repository: FieldNotesRepository,
    private val templateId: Long,
) : ViewModel() {

    private val eventChannel = Channel<TemplateDetailEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val pendingRemoved = MutableStateFlow<TemplateItem?>(null)

    val allItems: StateFlow<List<Item>> = repository.observeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allCategories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTemplates: StateFlow<List<Template>> = repository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPlans: StateFlow<List<Plan>> = repository.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<TemplateDetailUiState> = combine(
        repository.observeTemplate(templateId),
        repository.observeCategories(),
    ) { template, categories ->
        if (template == null) {
            TemplateDetailUiState(loading = false)
        } else {
            val order = categories.mapIndexed { index, category -> category.name to index }.toMap()
            val groups = template.items
                .groupBy { it.categoryName.ifBlank { "未分类" } }
                .map { (name, items) ->
                    TemplateGroup(name, items.sortedBy { it.sortOrder })
                }
                .sortedWith(
                    compareBy<TemplateGroup> {
                        if (it.categoryName == "未分类") Int.MAX_VALUE - 1
                        else order[it.categoryName] ?: Int.MAX_VALUE - 2
                    }.thenBy { it.categoryName }
                )
            TemplateDetailUiState(
                loading = false,
                template = template,
                groups = groups,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TemplateDetailUiState(),
    )

    fun addItems(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = repository.addItemsToTemplate(templateId, ids)
            eventChannel.send(
                TemplateDetailEvent.Message(
                    when {
                        result.added == 0 -> "所选物品均已在模板中"
                        result.skipped > 0 -> "已添加 ${result.added} 件，跳过 ${result.skipped} 件已有"
                        else -> "已添加 ${result.added} 件"
                    }
                )
            )
        }
    }

    fun remove(item: TemplateItem) {
        pendingRemoved.value = item
        viewModelScope.launch {
            repository.removeTemplateItem(item.id)
            eventChannel.send(TemplateDetailEvent.Removed(item.name))
        }
    }

    fun undoRemove() {
        val item = pendingRemoved.value ?: return
        pendingRemoved.value = null
        viewModelScope.launch {
            val restored = repository.restoreTemplateItem(templateId, item)
            eventChannel.send(
                TemplateDetailEvent.Message(if (restored) "已恢复 ${item.name}" else "恢复失败，物品已存在")
            )
        }
    }

    fun clearPendingRemoved() {
        pendingRemoved.value = null
    }

    fun importFromTemplate(sourceId: Long) {
        viewModelScope.launch {
            val result = repository.importTemplateIntoTemplate(templateId, sourceId)
            eventChannel.send(
                TemplateDetailEvent.Message(
                    when {
                        result.added == 0 -> "来源模板的物品均已在当前模板中"
                        result.skipped > 0 -> "导入 ${result.added} 件，跳过 ${result.skipped} 件已有"
                        else -> "导入 ${result.added} 件"
                    }
                )
            )
        }
    }

    fun importFromPlan(planId: Long) {
        viewModelScope.launch {
            val result = repository.importPlanIntoTemplate(templateId, planId)
            eventChannel.send(
                TemplateDetailEvent.Message(
                    when {
                        result.added == 0 -> "该计划的物品均已在当前模板中"
                        result.skipped > 0 -> "导入 ${result.added} 件，跳过 ${result.skipped} 件已有"
                        else -> "导入 ${result.added} 件"
                    }
                )
            )
        }
    }

    fun rename(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            runCatching { repository.updateTemplateMeta(templateId, name, description, colorHex) }
                .onSuccess { eventChannel.send(TemplateDetailEvent.Message("已保存")) }
                .onFailure { eventChannel.send(TemplateDetailEvent.Message(it.message ?: "保存失败")) }
        }
    }

    fun duplicate() {
        viewModelScope.launch {
            runCatching { repository.duplicateTemplate(templateId) }
                .onSuccess { eventChannel.send(TemplateDetailEvent.Duplicated(it)) }
                .onFailure { eventChannel.send(TemplateDetailEvent.Message(it.message ?: "复制失败")) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
            eventChannel.send(TemplateDetailEvent.Deleted)
        }
    }

    companion object {
        fun factory(container: AppContainer, templateId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { TemplateDetailViewModel(container.repository, templateId) }
        }
    }
}
