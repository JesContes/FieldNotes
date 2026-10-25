package com.fieldnotes.domain

import java.time.LocalDate

enum class PlanStatus {
    UPCOMING,
    ONGOING,
    PAST;

    companion object {
        fun of(start: LocalDate, end: LocalDate, today: LocalDate): PlanStatus = when {
            today < start -> UPCOMING
            today > end -> PAST
            else -> ONGOING
        }
    }
}

fun PlanStatus.label(start: LocalDate, end: LocalDate, today: LocalDate): String = when (this) {
    PlanStatus.UPCOMING -> {
        val days = start.toEpochDay() - today.toEpochDay()
        "还有 $days 天出发"
    }
    PlanStatus.ONGOING -> {
        val day = today.toEpochDay() - start.toEpochDay() + 1
        "进行中 · 第 $day 天"
    }
    PlanStatus.PAST -> {
        val days = today.toEpochDay() - end.toEpochDay()
        "已结束 $days 天"
    }
}

fun sortPlans(plans: List<Plan>, today: LocalDate): List<Plan> {
    fun group(plan: Plan): Int = when (PlanStatus.of(plan.start, plan.end, today)) {
        PlanStatus.ONGOING -> 0
        PlanStatus.UPCOMING -> 1
        PlanStatus.PAST -> 2
    }

    return plans.sortedWith(
        compareBy<Plan> { group(it) }
            .thenComparator { a, b ->
                val sa = PlanStatus.of(a.start, a.end, today)
                val sb = PlanStatus.of(b.start, b.end, today)
                when {
                    sa == PlanStatus.PAST && sb == PlanStatus.PAST ->
                        b.end.compareTo(a.end)
                    else -> a.start.compareTo(b.start)
                }
            }
            .thenByDescending { it.updatedAt }
    )
}

fun buildPlanCard(plan: Plan, today: LocalDate): PlanCardUi {
    val status = PlanStatus.of(plan.start, plan.end, today)
    val previewItems = plan.items.filter { !it.checked }
    val preview = when {
        plan.totalCount == 0 -> "还没有物品"
        previewItems.isEmpty() -> "已全部打包"
        else -> previewItems.take(4).joinToString(" · ") { it.name } +
            if (previewItems.size > 4) "  +${previewItems.size - 4}" else ""
    }
    return PlanCardUi(
        id = plan.id,
        destination = plan.destination,
        title = plan.title,
        dateRange = formatRange(plan.start, plan.end),
        status = status,
        statusText = status.label(plan.start, plan.end, today),
        checked = plan.checkedCount,
        total = plan.totalCount,
        progress = plan.progress,
        hasUnchecked = plan.hasUnchecked,
        preview = preview,
        colorHex = plan.colorHex,
    )
}

fun buildGroups(items: List<PlanItem>, categoryOrder: List<String>): List<ChecklistGroup> {
    val orderMap = categoryOrder.withIndex().associate { it.value to it.index }
    return items
        .groupBy { it.categoryName.ifBlank { "未分类" } }
        .map { (name, rows) ->
            ChecklistGroup(
                categoryName = name,
                checked = rows.count { it.checked },
                total = rows.size,
                items = rows.sortedBy { it.sortOrder },
            )
        }
        .sortedWith(
            compareBy<ChecklistGroup> {
                if (it.categoryName == "未分类") Int.MAX_VALUE - 1
                else orderMap[it.categoryName] ?: Int.MAX_VALUE - 2
            }.thenBy { it.categoryName }
        )
}
