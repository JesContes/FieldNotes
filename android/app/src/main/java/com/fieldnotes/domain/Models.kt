package com.fieldnotes.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Category(
    val id: Long,
    val name: String,
    val colorHex: String,
    val sortOrder: Int,
)

data class Item(
    val id: Long,
    val name: String,
    val note: String,
    val categoryId: Long?,
    val categoryName: String,
    val categoryColor: String,
)

data class TemplateItem(
    val id: Long,
    val name: String,
    val categoryName: String,
    val sortOrder: Int,
)

data class Template(
    val id: Long,
    val name: String,
    val description: String,
    val colorHex: String,
    val lastUsedAt: Long?,
    val updatedAt: Long,
    val items: List<TemplateItem>,
)

data class PlanItem(
    val id: Long,
    val name: String,
    val categoryName: String,
    val checked: Boolean,
    val sortOrder: Int,
)

data class Plan(
    val id: Long,
    val title: String,
    val destination: String,
    val start: LocalDate,
    val end: LocalDate,
    val note: String,
    val colorHex: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<PlanItem>,
) {
    val displayTitle: String get() = title.ifBlank { destination }
    val checkedCount: Int get() = items.count { it.checked }
    val totalCount: Int get() = items.size
    val hasUnchecked: Boolean get() = totalCount > 0 && checkedCount < totalCount
    val progress: Float get() = if (totalCount == 0) 0f else checkedCount.toFloat() / totalCount
}

data class ChecklistGroup(
    val categoryName: String,
    val checked: Int,
    val total: Int,
    val items: List<PlanItem>,
)

enum class PlanFilter(val label: String) {
    ALL("全部"),
    ONGOING("进行中"),
    UPCOMING("未开始"),
    PAST("已结束"),
}

data class PlanCardUi(
    val id: Long,
    val destination: String,
    val title: String,
    val dateRange: String,
    val status: PlanStatus,
    val statusText: String,
    val checked: Int,
    val total: Int,
    val progress: Float,
    val hasUnchecked: Boolean,
    val preview: String,
    val colorHex: String,
)

data class ItemGroup(
    val categoryName: String,
    val colorHex: String,
    val items: List<Item>,
)

data class Counts(
    val plans: Int = 0,
    val templates: Int = 0,
    val items: Int = 0,
    val categories: Int = 0,
)

data class AddItemsResult(val added: Int, val skipped: Int)

fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))

fun formatDateShort(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MM.dd"))

fun formatRange(start: LocalDate, end: LocalDate): String {
    val days = (end.toEpochDay() - start.toEpochDay()) + 1
    val range = if (start.year == end.year) {
        "${formatDate(start)} – ${end.format(DateTimeFormatter.ofPattern("MM.dd"))}"
    } else {
        "${formatDate(start)} – ${formatDate(end)}"
    }
    return "$range · ${days} 天"
}

fun formatLastUsed(seconds: Long?): String {
    if (seconds == null || seconds <= 0) return "尚未使用"
    val date = java.time.Instant.ofEpochSecond(seconds)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return "上次使用 ${date.format(DateTimeFormatter.ofPattern("MM-dd"))}"
}
