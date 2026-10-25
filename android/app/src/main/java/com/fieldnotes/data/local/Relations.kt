package com.fieldnotes.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ItemWithCategory(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id")
    val category: CategoryEntity?,
)

data class TemplateWithItems(
    @Embedded val template: TemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "template_id")
    val items: List<TemplateItemEntity>,
)

data class PlanWithItems(
    @Embedded val plan: PlanEntity,
    @Relation(parentColumn = "id", entityColumn = "plan_id")
    val items: List<PlanItemEntity>,
)
