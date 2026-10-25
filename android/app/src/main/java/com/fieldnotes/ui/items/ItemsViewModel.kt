package com.fieldnotes.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.domain.Category
import com.fieldnotes.domain.Item
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemGroupUi(
    val categoryName: String,
    val colorHex: String,
    val items: List<Item>,
)

data class ItemsUiState(
    val loading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val groups: List<ItemGroupUi> = emptyList(),
    val query: String = "",
    val selectedCategoryId: Long? = null,
    val totalItems: Int = 0,
    val uncategorizedCount: Int = 0,
)

class ItemsViewModel(
    private val repository: FieldNotesRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val messages = eventChannel.receiveAsFlow()

    val uiState: StateFlow<ItemsUiState> = combine(
        repository.observeItems(),
        repository.observeCategories(),
        query,
        selectedCategoryId,
    ) { items, categories, currentQuery, currentCategory ->
        val categoryOrder = categories.mapIndexed { index, category -> category.id to index }.toMap()
        val filtered = items.filter { item ->
            (currentCategory == null ||
                (currentCategory == -1L && item.categoryId == null) ||
                item.categoryId == currentCategory) &&
                (currentQuery.isBlank() || item.name.contains(currentQuery, true) ||
                    item.note.contains(currentQuery, true))
        }
        val groups = filtered
            .groupBy { it.categoryId }
            .map { (categoryId, rows) ->
                val category = categories.firstOrNull { it.id == categoryId }
                ItemGroupUi(
                    categoryName = category?.name ?: "未分类",
                    colorHex = category?.colorHex ?: "#C9D6E0",
                    items = rows.sortedWith(compareBy { it.name }),
                )
            }
            .sortedWith(
                compareBy<ItemGroupUi> { group ->
                    if (group.categoryName == "未分类") Int.MAX_VALUE - 1
                    else categories.firstOrNull { it.name == group.categoryName }
                        ?.let { categoryOrder[it.id] } ?: Int.MAX_VALUE - 2
                }.thenBy { it.categoryName }
            )
        ItemsUiState(
            loading = false,
            categories = categories,
            groups = groups,
            query = currentQuery,
            selectedCategoryId = currentCategory,
            totalItems = items.size,
            uncategorizedCount = items.count { it.categoryId == null },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemsUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setCategory(id: Long?) {
        selectedCategoryId.value = id
    }

    fun createItem(name: String, categoryId: Long?, note: String) {
        viewModelScope.launch {
            runCatching { repository.createItem(name, categoryId, note) }
                .onSuccess { eventChannel.send("已添加物品") }
                .onFailure { eventChannel.send(it.message ?: "添加失败") }
        }
    }

    fun updateItem(id: Long, name: String, categoryId: Long?, note: String) {
        viewModelScope.launch {
            runCatching { repository.updateItem(id, name, categoryId, note) }
                .onSuccess { eventChannel.send("已保存") }
                .onFailure { eventChannel.send(it.message ?: "保存失败") }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItem(id)
            eventChannel.send("已删除物品")
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ItemsViewModel(container.repository) }
        }
    }
}
