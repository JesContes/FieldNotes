package com.fieldnotes.data.transfer

/** 与数据库无关的导出 DTO，便于纯 JVM 单元测试。 */
data class ExportCategory(
    val name: String,
    val color: String,
    val createdAt: Long,
)

data class ExportItem(
    val name: String,
    val categoryIndex: Int?,
    val categoryName: String?,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ExportChecklistItem(
    val name: String,
    val categoryName: String,
)

data class ExportPlanItem(
    val name: String,
    val categoryName: String,
    val checked: Boolean,
)

data class ExportTemplate(
    val name: String,
    val description: String,
    val color: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?,
    val items: List<ExportChecklistItem>,
)

data class ExportPlan(
    val title: String,
    val destination: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val color: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<ExportPlanItem>,
)

data class ExportSnapshot(
    val categories: List<ExportCategory>,
    val items: List<ExportItem>,
    val templates: List<ExportTemplate>,
    val plans: List<ExportPlan>,
)

data class ImportPreview(
    val categories: Int,
    val items: Int,
    val templates: Int,
    val plans: Int,
    val templateEntries: Int,
    val planEntries: Int,
    val duplicates: Int,
    val invalid: Int,
)

data class ParsedImport(
    val snapshot: ExportSnapshot,
    val preview: ImportPreview,
)

data class ImportResult(
    val categories: Int = 0,
    val items: Int = 0,
    val templates: Int = 0,
    val plans: Int = 0,
    val skipped: Int = 0,
    val invalid: Int = 0,
)

enum class ImportMode { MERGE, REPLACE }

class ImportException(message: String) : Exception(message)
