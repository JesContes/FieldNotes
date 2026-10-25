package com.fieldnotes.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRowUi(
    val id: Long,
    val name: String,
    val colorHex: String,
    val itemCount: Int,
)

data class CategoriesUiState(
    val loading: Boolean = true,
    val rows: List<CategoryRowUi> = emptyList(),
    val uncategorizedCount: Int = 0,
)

class CategoriesViewModel(
    private val repository: FieldNotesRepository,
) : ViewModel() {

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val messages = eventChannel.receiveAsFlow()

    val uiState: StateFlow<CategoriesUiState> = combine(
        repository.observeCategories(),
        repository.observeItems(),
    ) { categories, items ->
        CategoriesUiState(
            loading = false,
            rows = categories.map { category ->
                CategoryRowUi(
                    id = category.id,
                    name = category.name,
                    colorHex = category.colorHex,
                    itemCount = items.count { it.categoryId == category.id },
                )
            },
            uncategorizedCount = items.count { it.categoryId == null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun create(name: String, colorHex: String) {
        viewModelScope.launch {
            runCatching { repository.createCategory(name, colorHex) }
                .onSuccess { eventChannel.send("已创建分类") }
                .onFailure { eventChannel.send(it.message ?: "创建失败") }
        }
    }

    fun update(id: Long, name: String, colorHex: String) {
        viewModelScope.launch {
            runCatching { repository.updateCategory(id, name, colorHex) }
                .onSuccess { eventChannel.send("已保存") }
                .onFailure { eventChannel.send(it.message ?: "保存失败") }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            eventChannel.send("已删除分类，物品已归入未分类")
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { CategoriesViewModel(container.repository) }
        }
    }
}
