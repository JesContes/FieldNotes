package com.fieldnotes.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.domain.Template
import com.fieldnotes.domain.formatLastUsed
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplateCardUi(
    val id: Long,
    val name: String,
    val description: String,
    val colorHex: String,
    val itemCount: Int,
    val categorySummary: String,
    val lastUsedText: String,
)

data class TemplatesUiState(
    val loading: Boolean = true,
    val query: String = "",
    val templates: List<TemplateCardUi> = emptyList(),
    val totalCount: Int = 0,
    val totalItems: Int = 0,
)

class TemplatesViewModel(
    private val repository: FieldNotesRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val messages = eventChannel.receiveAsFlow()

    val uiState: StateFlow<TemplatesUiState> = combine(
        repository.observeTemplates(),
        query,
    ) { templates, currentQuery ->
        val matching = if (currentQuery.isBlank()) templates else templates.filter {
            it.name.contains(currentQuery, true) || it.description.contains(currentQuery, true)
        }
        TemplatesUiState(
            loading = false,
            query = currentQuery,
            templates = matching.map { it.toCard() },
            totalCount = templates.size,
            totalItems = templates.sumOf { it.items.size },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemplatesUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun create(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            runCatching { repository.createTemplate(name, description, colorHex) }
                .onSuccess { eventChannel.send("已创建模板「${name.trim()}」") }
                .onFailure { eventChannel.send(it.message ?: "创建失败") }
        }
    }

    fun update(id: Long, name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            runCatching { repository.updateTemplateMeta(id, name, description, colorHex) }
                .onSuccess { eventChannel.send("已保存") }
                .onFailure { eventChannel.send(it.message ?: "保存失败") }
        }
    }

    fun duplicate(id: Long) {
        viewModelScope.launch {
            runCatching { repository.duplicateTemplate(id) }
                .onSuccess { eventChannel.send("已复制模板") }
                .onFailure { eventChannel.send(it.message ?: "复制失败") }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            eventChannel.send("已删除模板")
        }
    }

    private fun Template.toCard(): TemplateCardUi {
        val summary = items.groupingBy { it.categoryName.ifBlank { "未分类" } }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(3)
            .joinToString(" · ") { "${it.key} ${it.value}" }
        return TemplateCardUi(
            id = id,
            name = name,
            description = description,
            colorHex = colorHex,
            itemCount = items.size,
            categorySummary = summary,
            lastUsedText = formatLastUsed(lastUsedAt),
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { TemplatesViewModel(container.repository) }
        }
    }
}
