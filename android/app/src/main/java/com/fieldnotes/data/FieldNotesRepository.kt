package com.fieldnotes.data

import androidx.room.withTransaction
import com.fieldnotes.data.local.CategoryEntity
import com.fieldnotes.data.local.FieldNotesDatabase
import com.fieldnotes.data.local.ItemEntity
import com.fieldnotes.data.local.ItemWithCategory
import com.fieldnotes.data.local.PlanEntity
import com.fieldnotes.data.local.PlanItemEntity
import com.fieldnotes.data.local.PlanWithItems
import com.fieldnotes.data.local.TemplateEntity
import com.fieldnotes.data.local.TemplateItemEntity
import com.fieldnotes.data.local.TemplateWithItems
import com.fieldnotes.data.transfer.ExportCategory
import com.fieldnotes.data.transfer.ExportChecklistItem
import com.fieldnotes.data.transfer.ExportCodec
import com.fieldnotes.data.transfer.ExportItem
import com.fieldnotes.data.transfer.ExportPlan
import com.fieldnotes.data.transfer.ExportPlanItem
import com.fieldnotes.data.transfer.ExportSnapshot
import com.fieldnotes.data.transfer.ExportTemplate
import com.fieldnotes.data.transfer.ImportMode
import com.fieldnotes.data.transfer.ImportResult
import com.fieldnotes.data.transfer.ParsedImport
import com.fieldnotes.domain.AddItemsResult
import com.fieldnotes.domain.Category
import com.fieldnotes.domain.Counts
import com.fieldnotes.domain.Item
import com.fieldnotes.domain.Plan
import com.fieldnotes.domain.PlanItem
import com.fieldnotes.domain.Template
import com.fieldnotes.domain.TemplateItem
import com.fieldnotes.domain.TextRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

data class PlanDraft(
    val title: String,
    val destination: String,
    val start: LocalDate,
    val end: LocalDate,
    val note: String,
    val colorHex: String,
)

class FieldNotesRepository(private val db: FieldNotesDatabase) {

    private val categoryDao = db.categoryDao()
    private val itemDao = db.itemDao()
    private val templateDao = db.templateDao()
    private val planDao = db.planDao()

    // ---------------- observe ----------------

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeItems(): Flow<List<Item>> =
        itemDao.observeAllWithCategory().map { list -> list.map { it.toDomain() } }

    fun observeTemplates(): Flow<List<Template>> =
        templateDao.observeAllWithItems().map { list -> list.map { it.toDomain() } }

    fun observePlans(): Flow<List<Plan>> =
        planDao.observeAllWithItems().map { list -> list.map { it.toDomain() } }

    fun observePlan(id: Long): Flow<Plan?> =
        planDao.observeByIdWithItems(id).map { it?.toDomain() }

    fun observeTemplate(id: Long): Flow<Template?> =
        templateDao.observeByIdWithItems(id).map { it?.toDomain() }

    fun observeCounts(): Flow<Counts> = combine(
        observePlans(), observeTemplates(), observeItems(), observeCategories(),
    ) { plans, templates, items, categories ->
        Counts(
            plans = plans.size,
            templates = templates.size,
            items = items.size,
            categories = categories.size,
        )
    }

    // ---------------- seed ----------------

    suspend fun seedIfEmpty() = db.withTransaction {
        if (categoryDao.count() > 0) return@withTransaction
        val presets = listOf(
            "日用品" to "#66CCFF",
            "服装" to "#4DB6AC",
            "电子设备" to "#81C784",
            "证件" to "#FFD54F",
            "药品" to "#FFB74D",
            "其他" to "#F06292",
        )
        presets.forEachIndexed { index, (name, color) ->
            categoryDao.insert(
                CategoryEntity(
                    name = name,
                    nameKey = TextRules.key(name),
                    colorHex = color,
                    sortOrder = index,
                    createdAt = nowSeconds(),
                )
            )
        }
    }

    // ---------------- plans ----------------

    suspend fun getPlan(id: Long): Plan? = planDao.getByIdWithItems(id)?.toDomain()

    suspend fun createPlan(draft: PlanDraft): Long = db.withTransaction {
        val now = nowSeconds()
        planDao.insert(
            PlanEntity(
                title = TextRules.truncate(TextRules.normalize(draft.title), TextRules.MAX_PLAN_TITLE),
                destination = TextRules.truncate(TextRules.normalize(draft.destination), TextRules.MAX_DESTINATION),
                startEpochDay = draft.start.toEpochDay(),
                endEpochDay = draft.end.toEpochDay(),
                note = TextRules.truncate(draft.note.trim(), TextRules.MAX_PLAN_NOTE),
                colorHex = draft.colorHex,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun updatePlan(id: Long, draft: PlanDraft): Int = db.withTransaction {
        val existing = planDao.getById(id) ?: return@withTransaction 0
        planDao.update(
            existing.copy(
                title = TextRules.truncate(TextRules.normalize(draft.title), TextRules.MAX_PLAN_TITLE),
                destination = TextRules.truncate(TextRules.normalize(draft.destination), TextRules.MAX_DESTINATION),
                startEpochDay = draft.start.toEpochDay(),
                endEpochDay = draft.end.toEpochDay(),
                note = TextRules.truncate(draft.note.trim(), TextRules.MAX_PLAN_NOTE),
                colorHex = draft.colorHex,
                updatedAt = nowSeconds(),
            )
        )
    }

    suspend fun deletePlan(id: Long): Int = planDao.deleteById(id)

    suspend fun toggleItem(itemId: Long, checked: Boolean): Int =
        planDao.updateChecked(itemId, checked)

    suspend fun setAllChecked(planId: Long, checked: Boolean): Int =
        planDao.updateAllChecked(planId, checked)

    suspend fun addItemsToPlan(planId: Long, itemIds: List<Long>): AddItemsResult =
        db.withTransaction {
            if (planDao.getById(planId) == null) return@withTransaction AddItemsResult(0, 0)
            val items = itemDao.getByIdsWithCategory(itemIds)
            val existing = planDao.getItems(planId).map { it.nameKey }.toSet()
            var sort = planDao.maxSortOrder(planId)
            val rows = mutableListOf<PlanItemEntity>()
            var skipped = 0
            items.forEach { wrapped ->
                val item = wrapped.item
                if (item.nameKey in existing) {
                    skipped++
                    return@forEach
                }
                sort += 1
                rows += PlanItemEntity(
                    planId = planId,
                    name = item.name,
                    nameKey = item.nameKey,
                    categoryName = wrapped.category?.name ?: "",
                    checked = false,
                    sortOrder = sort,
                    sourceItemId = item.id,
                )
            }
            if (rows.isNotEmpty()) planDao.insertItems(rows)
            AddItemsResult(rows.size, skipped)
        }

    suspend fun importTemplateToPlan(planId: Long, templateId: Long): AddItemsResult =
        db.withTransaction {
            val template = templateDao.getByIdWithItems(templateId)
                ?: return@withTransaction AddItemsResult(0, 0)
            val existing = planDao.getItems(planId).map { it.nameKey }.toSet()
            var sort = planDao.maxSortOrder(planId)
            val rows = mutableListOf<PlanItemEntity>()
            var skipped = 0
            template.items.sortedBy { it.sortOrder }.forEach { entry ->
                if (entry.nameKey in existing) {
                    skipped++
                } else {
                    sort += 1
                    rows += PlanItemEntity(
                        planId = planId,
                        name = entry.name,
                        nameKey = entry.nameKey,
                        categoryName = entry.categoryName,
                        checked = false,
                        sortOrder = sort,
                        sourceItemId = entry.sourceItemId,
                    )
                }
            }
            if (rows.isNotEmpty()) planDao.insertItems(rows)
            templateDao.touchLastUsed(templateId, nowSeconds())
            AddItemsResult(rows.size, skipped)
        }

    suspend fun removePlanItem(itemId: Long): Int = planDao.deleteItem(itemId)

    suspend fun restorePlanItem(planId: Long, item: PlanItem): Boolean = db.withTransaction {
        val existing = planDao.getItems(planId).any { it.nameKey == TextRules.key(item.name) }
        if (existing) return@withTransaction false
        planDao.insertItems(
            listOf(
                PlanItemEntity(
                    planId = planId,
                    name = item.name,
                    nameKey = TextRules.key(item.name),
                    categoryName = item.categoryName,
                    checked = item.checked,
                    sortOrder = item.sortOrder,
                )
            )
        )
        true
    }

    suspend fun savePlanAsTemplate(planId: Long, name: String): Long = db.withTransaction {
        val plan = planDao.getByIdWithItems(planId) ?: error("计划不存在")
        val normalized = TextRules.normalize(name)
        require(TextRules.isValidName(normalized, TextRules.MAX_TEMPLATE_NAME)) {
            "模板名需为 1–${TextRules.MAX_TEMPLATE_NAME} 字"
        }
        val key = TextRules.key(normalized)
        require(templateDao.findByNameKey(key) == null) { "已存在同名模板" }
        val now = nowSeconds()
        val templateId = templateDao.insert(
            TemplateEntity(
                name = normalized,
                nameKey = key,
                description = "",
                colorHex = plan.plan.colorHex,
                createdAt = now,
                updatedAt = now,
            )
        )
        val rows = plan.items.sortedBy { it.sortOrder }.mapIndexed { index, item ->
            TemplateItemEntity(
                templateId = templateId,
                name = item.name,
                nameKey = item.nameKey,
                categoryName = item.categoryName,
                sortOrder = index,
                sourceItemId = item.sourceItemId,
            )
        }
        if (rows.isNotEmpty()) templateDao.insertItems(rows)
        templateId
    }

    // ---------------- templates ----------------

    suspend fun getTemplate(id: Long): Template? =
        templateDao.getByIdWithItems(id)?.toDomain()

    suspend fun createTemplate(name: String, description: String, colorHex: String): Long =
        db.withTransaction {
            val normalized = TextRules.normalize(name)
            require(TextRules.isValidName(normalized, TextRules.MAX_TEMPLATE_NAME)) {
                "模板名需为 1–${TextRules.MAX_TEMPLATE_NAME} 字"
            }
            val key = TextRules.key(normalized)
            require(templateDao.findByNameKey(key) == null) { "已存在同名模板" }
            val now = nowSeconds()
            templateDao.insert(
                TemplateEntity(
                    name = normalized,
                    nameKey = key,
                    description = TextRules.truncate(description.trim(), TextRules.MAX_TEMPLATE_DESC),
                    colorHex = colorHex,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }

    suspend fun updateTemplateMeta(id: Long, name: String, description: String, colorHex: String): Int =
        db.withTransaction {
            val existing = templateDao.getById(id) ?: return@withTransaction 0
            val normalized = TextRules.normalize(name)
            require(TextRules.isValidName(normalized, TextRules.MAX_TEMPLATE_NAME)) {
                "模板名需为 1–${TextRules.MAX_TEMPLATE_NAME} 字"
            }
            val key = TextRules.key(normalized)
            val duplicate = templateDao.findByNameKey(key)
            require(duplicate == null || duplicate.id == id) { "已存在同名模板" }
            templateDao.update(
                existing.copy(
                    name = normalized,
                    nameKey = key,
                    description = TextRules.truncate(description.trim(), TextRules.MAX_TEMPLATE_DESC),
                    colorHex = colorHex,
                    updatedAt = nowSeconds(),
                )
            )
        }

    suspend fun deleteTemplate(id: Long): Int = templateDao.deleteById(id)

    suspend fun duplicateTemplate(id: Long): Long = db.withTransaction {
        val source = templateDao.getByIdWithItems(id) ?: error("模板不存在")
        val baseName = "${source.template.name} 副本"
        var name = baseName
        var index = 2
        while (templateDao.findByNameKey(TextRules.key(name)) != null) {
            name = "$baseName $index"
            index++
        }
        val now = nowSeconds()
        val newId = templateDao.insert(
            source.template.copy(
                id = 0,
                name = name,
                nameKey = TextRules.key(name),
                createdAt = now,
                updatedAt = now,
                lastUsedAt = null,
            )
        )
        val rows = source.items.sortedBy { it.sortOrder }.mapIndexed { itemIndex, item ->
            item.copy(id = 0, templateId = newId, sortOrder = itemIndex)
        }
        if (rows.isNotEmpty()) templateDao.insertItems(rows)
        newId
    }

    suspend fun addItemsToTemplate(templateId: Long, itemIds: List<Long>): AddItemsResult =
        db.withTransaction {
            val items = itemDao.getByIdsWithCategory(itemIds)
            val existing = templateDao.getItems(templateId).map { it.nameKey }.toSet()
            var sort = templateDao.maxSortOrder(templateId)
            val rows = mutableListOf<TemplateItemEntity>()
            var skipped = 0
            items.forEach { wrapped ->
                val item = wrapped.item
                if (item.nameKey in existing) {
                    skipped++
                    return@forEach
                }
                sort += 1
                rows += TemplateItemEntity(
                    templateId = templateId,
                    name = item.name,
                    nameKey = item.nameKey,
                    categoryName = wrapped.category?.name ?: "",
                    sortOrder = sort,
                    sourceItemId = item.id,
                )
            }
            if (rows.isNotEmpty()) {
                templateDao.insertItems(rows)
                templateDao.touchLastUsed(templateId, nowSeconds())
            }
            AddItemsResult(rows.size, skipped)
        }

    suspend fun importTemplateIntoTemplate(targetId: Long, sourceId: Long): AddItemsResult =
        db.withTransaction {
            if (targetId == sourceId) return@withTransaction AddItemsResult(0, 0)
            val source = templateDao.getByIdWithItems(sourceId) ?: return@withTransaction AddItemsResult(0, 0)
            val existing = templateDao.getItems(targetId).map { it.nameKey }.toSet()
            var sort = templateDao.maxSortOrder(targetId)
            val rows = mutableListOf<TemplateItemEntity>()
            var skipped = 0
            source.items.sortedBy { it.sortOrder }.forEach { entry ->
                if (entry.nameKey in existing) {
                    skipped++
                } else {
                    sort += 1
                    rows += TemplateItemEntity(
                        templateId = targetId,
                        name = entry.name,
                        nameKey = entry.nameKey,
                        categoryName = entry.categoryName,
                        sortOrder = sort,
                        sourceItemId = entry.sourceItemId,
                    )
                }
            }
            if (rows.isNotEmpty()) {
                templateDao.insertItems(rows)
                templateDao.touchLastUsed(targetId, nowSeconds())
            }
            AddItemsResult(rows.size, skipped)
        }

    suspend fun importPlanIntoTemplate(targetId: Long, planId: Long): AddItemsResult =
        db.withTransaction {
            val source = planDao.getByIdWithItems(planId) ?: return@withTransaction AddItemsResult(0, 0)
            val existing = templateDao.getItems(targetId).map { it.nameKey }.toSet()
            var sort = templateDao.maxSortOrder(targetId)
            val rows = mutableListOf<TemplateItemEntity>()
            var skipped = 0
            source.items.sortedBy { it.sortOrder }.forEach { entry ->
                if (entry.nameKey in existing) {
                    skipped++
                } else {
                    sort += 1
                    rows += TemplateItemEntity(
                        templateId = targetId,
                        name = entry.name,
                        nameKey = entry.nameKey,
                        categoryName = entry.categoryName,
                        sortOrder = sort,
                        sourceItemId = entry.sourceItemId,
                    )
                }
            }
            if (rows.isNotEmpty()) {
                templateDao.insertItems(rows)
                templateDao.touchLastUsed(targetId, nowSeconds())
            }
            AddItemsResult(rows.size, skipped)
        }

    suspend fun removeTemplateItem(itemId: Long): Int = templateDao.deleteItem(itemId)

    suspend fun restoreTemplateItem(templateId: Long, item: TemplateItem): Boolean = db.withTransaction {
        val existing = templateDao.getItems(templateId).any { it.nameKey == TextRules.key(item.name) }
        if (existing) return@withTransaction false
        templateDao.insertItems(
            listOf(
                TemplateItemEntity(
                    templateId = templateId,
                    name = item.name,
                    nameKey = TextRules.key(item.name),
                    categoryName = item.categoryName,
                    sortOrder = item.sortOrder,
                )
            )
        )
        true
    }

    // ---------------- items ----------------

    suspend fun createItem(name: String, categoryId: Long?, note: String): Long = db.withTransaction {
        val normalized = TextRules.normalize(name)
        require(TextRules.isValidName(normalized, TextRules.MAX_ITEM_NAME)) {
            "物品名需为 1–${TextRules.MAX_ITEM_NAME} 字"
        }
        val key = TextRules.key(normalized)
        require(itemDao.findByNameKey(key) == null) { "已存在同名物品" }
        val now = nowSeconds()
        itemDao.insert(
            ItemEntity(
                name = normalized,
                nameKey = key,
                categoryId = categoryId,
                note = TextRules.truncate(note.trim(), TextRules.MAX_ITEM_NOTE),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun updateItem(id: Long, name: String, categoryId: Long?, note: String): Int =
        db.withTransaction {
            val existing = itemDao.getByIdWithCategory(id)?.item ?: return@withTransaction 0
            val normalized = TextRules.normalize(name)
            require(TextRules.isValidName(normalized, TextRules.MAX_ITEM_NAME)) {
                "物品名需为 1–${TextRules.MAX_ITEM_NAME} 字"
            }
            val key = TextRules.key(normalized)
            val duplicate = itemDao.findByNameKey(key)
            require(duplicate == null || duplicate.id == id) { "已存在同名物品" }
            itemDao.update(
                existing.copy(
                    name = normalized,
                    nameKey = key,
                    categoryId = categoryId,
                    note = TextRules.truncate(note.trim(), TextRules.MAX_ITEM_NOTE),
                    updatedAt = nowSeconds(),
                )
            )
        }

    suspend fun deleteItem(id: Long): Int = itemDao.deleteById(id)

    // ---------------- categories ----------------

    suspend fun createCategory(name: String, colorHex: String): Long = db.withTransaction {
        val normalized = TextRules.normalize(name)
        require(TextRules.isValidName(normalized, TextRules.MAX_CATEGORY_NAME)) {
            "分类名需为 1–${TextRules.MAX_CATEGORY_NAME} 字"
        }
        val key = TextRules.key(normalized)
        require(categoryDao.findByNameKey(key) == null) { "已存在同名分类" }
        val sort = (categoryDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
        categoryDao.insert(
            CategoryEntity(
                name = normalized,
                nameKey = key,
                colorHex = colorHex,
                sortOrder = sort,
                createdAt = nowSeconds(),
            )
        )
    }

    suspend fun updateCategory(id: Long, name: String, colorHex: String): Int = db.withTransaction {
        val existing = categoryDao.getById(id) ?: return@withTransaction 0
        val normalized = TextRules.normalize(name)
        require(TextRules.isValidName(normalized, TextRules.MAX_CATEGORY_NAME)) {
            "分类名需为 1–${TextRules.MAX_CATEGORY_NAME} 字"
        }
        val key = TextRules.key(normalized)
        val duplicate = categoryDao.findByNameKey(key)
        require(duplicate == null || duplicate.id == id) { "已存在同名分类" }
        categoryDao.update(existing.copy(name = normalized, nameKey = key, colorHex = colorHex))
    }

    suspend fun deleteCategory(id: Long): Int = db.withTransaction {
        itemDao.clearCategory(id)
        categoryDao.deleteById(id)
    }

    suspend fun clearAllData() = db.withTransaction {
        planDao.deleteAll()
        templateDao.deleteAll()
        itemDao.deleteAll()
        categoryDao.deleteAll()
    }

    // ---------------- export / import ----------------

    suspend fun export(gzip: Boolean): ByteArray {
        val snapshot = db.withTransaction {
            val categories = categoryDao.getAll()
            val categoryIndex = categories.withIndex().associate { it.value.id to it.index }
            val items = itemDao.getAllWithCategory()
            val templates = templateDao.getAllWithItems()
            val plans = planDao.getAllWithItems()

            ExportSnapshot(
                categories = categories.map {
                    ExportCategory(it.name, it.colorHex, it.createdAt)
                },
                items = items.map { wrapped ->
                    ExportItem(
                        name = wrapped.item.name,
                        categoryIndex = wrapped.item.categoryId?.let { categoryIndex[it] },
                        categoryName = wrapped.category?.name,
                        note = wrapped.item.note,
                        createdAt = wrapped.item.createdAt,
                        updatedAt = wrapped.item.updatedAt,
                    )
                },
                templates = templates.map { wrapped ->
                    ExportTemplate(
                        name = wrapped.template.name,
                        description = wrapped.template.description,
                        color = wrapped.template.colorHex,
                        createdAt = wrapped.template.createdAt,
                        updatedAt = wrapped.template.updatedAt,
                        lastUsedAt = wrapped.template.lastUsedAt,
                        items = wrapped.items.sortedBy { it.sortOrder }.map {
                            ExportChecklistItem(it.name, it.categoryName)
                        },
                    )
                },
                plans = plans.map { wrapped ->
                    ExportPlan(
                        title = wrapped.plan.title,
                        destination = wrapped.plan.destination,
                        startEpochDay = wrapped.plan.startEpochDay,
                        endEpochDay = wrapped.plan.endEpochDay,
                        color = wrapped.plan.colorHex,
                        note = wrapped.plan.note,
                        createdAt = wrapped.plan.createdAt,
                        updatedAt = wrapped.plan.updatedAt,
                        items = wrapped.items.sortedBy { it.sortOrder }.map {
                            ExportPlanItem(it.name, it.categoryName, it.checked)
                        },
                    )
                },
            )
        }
        return ExportCodec.encode(snapshot, gzip)
    }

    suspend fun importSnapshot(parsed: ParsedImport, mode: ImportMode): ImportResult =
        db.withTransaction {
            if (mode == ImportMode.REPLACE) {
                planDao.deleteAll()
                templateDao.deleteAll()
                itemDao.deleteAll()
                categoryDao.deleteAll()
            }

            val snapshot = parsed.snapshot
            val now = nowSeconds()
            var insertedCategories = 0
            var insertedItems = 0
            var insertedTemplates = 0
            var insertedPlans = 0
            var skipped = 0

            val categoryByKey = categoryDao.getAll().associateBy { it.nameKey }.toMutableMap()
            val categoryIdByIndex = mutableMapOf<Int, Long>()
            var sort = (categoryDao.getAll().maxOfOrNull { it.sortOrder } ?: -1) + 1
            snapshot.categories.forEachIndexed { index, category ->
                val key = TextRules.key(category.name)
                val existing = categoryByKey[key]
                val id = if (existing != null) {
                    existing.id
                } else {
                    val newId = categoryDao.insert(
                        CategoryEntity(
                            name = category.name,
                            nameKey = key,
                            colorHex = category.color,
                            sortOrder = sort++,
                            createdAt = category.createdAt,
                        )
                    )
                    insertedCategories++
                    categoryByKey[key] = CategoryEntity(
                        id = newId,
                        name = category.name,
                        nameKey = key,
                        colorHex = category.color,
                        sortOrder = sort - 1,
                        createdAt = category.createdAt,
                    )
                    newId
                }
                categoryIdByIndex[index] = id
            }

            val itemKeys = itemDao.getAllWithCategory().map { it.item.nameKey }.toMutableSet()
            snapshot.items.forEach { item ->
                val key = TextRules.key(item.name)
                if (!itemKeys.add(key)) {
                    skipped++
                } else {
                    val categoryId = item.categoryIndex?.let { categoryIdByIndex[it] }
                        ?: item.categoryName?.let { categoryByKey[TextRules.key(it)]?.id }
                    itemDao.insert(
                        ItemEntity(
                            name = item.name,
                            nameKey = key,
                            categoryId = categoryId,
                            note = item.note,
                            createdAt = item.createdAt,
                            updatedAt = item.updatedAt,
                        )
                    )
                    insertedItems++
                }
            }

            val templateKeys = templateDao.getAllWithItems().map { it.template.nameKey }.toMutableSet()
            snapshot.templates.forEach { template ->
                val key = TextRules.key(template.name)
                if (!templateKeys.add(key)) {
                    skipped++
                } else {
                    val templateId = templateDao.insert(
                        TemplateEntity(
                            name = template.name,
                            nameKey = key,
                            description = template.description,
                            colorHex = template.color,
                            createdAt = template.createdAt,
                            updatedAt = template.updatedAt,
                            lastUsedAt = template.lastUsedAt,
                        )
                    )
                    val rows = template.items.mapIndexed { index, entry ->
                        TemplateItemEntity(
                            templateId = templateId,
                            name = entry.name,
                            nameKey = TextRules.key(entry.name),
                            categoryName = entry.categoryName,
                            sortOrder = index,
                        )
                    }
                    if (rows.isNotEmpty()) templateDao.insertItems(rows)
                    insertedTemplates++
                }
            }

            val planKeys = planDao.getAllWithItems().map {
                ExportCodec.planIdentity(
                    it.plan.title,
                    it.plan.destination,
                    it.plan.startEpochDay,
                    it.plan.endEpochDay,
                )
            }.toMutableSet()
            snapshot.plans.forEach { plan ->
                val identity = ExportCodec.planIdentity(
                    plan.title, plan.destination, plan.startEpochDay, plan.endEpochDay,
                )
                if (!planKeys.add(identity)) {
                    skipped++
                } else {
                    val planId = planDao.insert(
                        PlanEntity(
                            title = plan.title,
                            destination = plan.destination,
                            startEpochDay = plan.startEpochDay,
                            endEpochDay = plan.endEpochDay,
                            note = plan.note,
                            colorHex = plan.color,
                            createdAt = plan.createdAt,
                            updatedAt = plan.updatedAt,
                        )
                    )
                    val rows = plan.items.mapIndexed { index, entry ->
                        PlanItemEntity(
                            planId = planId,
                            name = entry.name,
                            nameKey = TextRules.key(entry.name),
                            categoryName = entry.categoryName,
                            checked = entry.checked,
                            sortOrder = index,
                        )
                    }
                    if (rows.isNotEmpty()) planDao.insertItems(rows)
                    insertedPlans++
                }
            }

            ImportResult(
                categories = insertedCategories,
                items = insertedItems,
                templates = insertedTemplates,
                plans = insertedPlans,
                skipped = skipped,
                invalid = parsed.preview.invalid,
            )
        }

    // ---------------- mapping ----------------

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        colorHex = colorHex,
        sortOrder = sortOrder,
    )

    private fun ItemWithCategory.toDomain() = Item(
        id = item.id,
        name = item.name,
        note = item.note,
        categoryId = item.categoryId,
        categoryName = category?.name ?: "",
        categoryColor = category?.colorHex ?: "#C9D6E0",
    )

    private fun TemplateWithItems.toDomain(): Template = Template(
        id = template.id,
        name = template.name,
        description = template.description,
        colorHex = template.colorHex,
        lastUsedAt = template.lastUsedAt,
        updatedAt = template.updatedAt,
        items = items.sortedBy { it.sortOrder }.map {
            TemplateItem(
                id = it.id,
                name = it.name,
                categoryName = it.categoryName,
                sortOrder = it.sortOrder,
            )
        },
    )

    private fun PlanWithItems.toDomain(): Plan = Plan(
        id = plan.id,
        title = plan.title,
        destination = plan.destination,
        start = LocalDate.ofEpochDay(plan.startEpochDay),
        end = LocalDate.ofEpochDay(plan.endEpochDay),
        note = plan.note,
        colorHex = plan.colorHex,
        createdAt = plan.createdAt,
        updatedAt = plan.updatedAt,
        items = items.sortedBy { it.sortOrder }.map {
            PlanItem(
                id = it.id,
                name = it.name,
                categoryName = it.categoryName,
                checked = it.checked,
                sortOrder = it.sortOrder,
            )
        },
    )

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000
}
