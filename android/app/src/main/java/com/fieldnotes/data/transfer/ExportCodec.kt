package com.fieldnotes.data.transfer

import com.fieldnotes.domain.TextRules
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.LocalDate
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object ExportCodec {
    const val FORMAT = "field-notes-export"
    const val VERSION = 1
    const val APP_VERSION = "1.0"

    private const val MAX_FILE_BYTES = 32 * 1024 * 1024
    private const val MAX_DECOMPRESSED_BYTES = 128 * 1024 * 1024
    private const val MAX_CATEGORIES = 1_000
    private const val MAX_ITEMS = 10_000
    private const val MAX_TEMPLATES = 5_000
    private const val MAX_PLANS = 5_000
    private const val MAX_ENTRIES = 100_000
    private const val MIN_EPOCH_DAY = -25567L   // 1900-01-01
    private const val MAX_EPOCH_DAY = 84005L    // 2200-01-01 左右，见 isValidEpochDay
    private val COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

    fun encode(snapshot: ExportSnapshot, gzip: Boolean): ByteArray {
        val root = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", nowSeconds())
            put("appVersion", APP_VERSION)
            put("categories", JSONArray().apply {
                snapshot.categories.forEach { category ->
                    put(JSONObject().apply {
                        put("n", category.name)
                        put("c", category.color)
                        put("a", category.createdAt)
                    })
                }
            })
            put("items", JSONArray().apply {
                snapshot.items.forEach { item ->
                    put(JSONObject().apply {
                        put("n", item.name)
                        if (item.categoryIndex != null) {
                            put("k", item.categoryIndex)
                        } else if (!item.categoryName.isNullOrBlank()) {
                            put("k", item.categoryName)
                        }
                        if (item.note.isNotBlank()) put("r", item.note)
                        put("a", item.createdAt)
                        put("u", item.updatedAt)
                    })
                }
            })
            put("templates", JSONArray().apply {
                snapshot.templates.forEach { template ->
                    put(JSONObject().apply {
                        put("n", template.name)
                        if (template.description.isNotBlank()) put("d", template.description)
                        put("c", template.color)
                        put("a", template.createdAt)
                        put("u", template.updatedAt)
                        template.lastUsedAt?.let { put("l", it) }
                        put("it", JSONArray().apply {
                            template.items.forEach { entry ->
                                put(JSONObject().apply {
                                    put("n", entry.name)
                                    if (entry.categoryName.isNotBlank()) put("g", entry.categoryName)
                                })
                            }
                        })
                    })
                }
            })
            put("plans", JSONArray().apply {
                snapshot.plans.forEach { plan ->
                    put(JSONObject().apply {
                        if (plan.title.isNotBlank()) put("t", plan.title)
                        put("dst", plan.destination)
                        put("s", plan.startEpochDay)
                        put("e", plan.endEpochDay)
                        put("c", plan.color)
                        if (plan.note.isNotBlank()) put("m", plan.note)
                        put("a", plan.createdAt)
                        put("u", plan.updatedAt)
                        put("it", JSONArray().apply {
                            plan.items.forEach { entry ->
                                put(JSONObject().apply {
                                    put("n", entry.name)
                                    if (entry.categoryName.isNotBlank()) put("g", entry.categoryName)
                                    if (entry.checked) put("x", 1)
                                })
                            }
                        })
                    })
                }
            })
        }
        val json = root.toString().toByteArray(StandardCharsets.UTF_8)
        if (!gzip) return json
        return ByteArrayOutputStream().use { out ->
            GZIPOutputStream(out).use { it.write(json) }
            out.toByteArray()
        }
    }

    fun isGzip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x1F.toByte() && bytes[1] == 0x8B.toByte()

    fun decode(bytes: ByteArray): ParsedImport {
        if (bytes.isEmpty()) throw ImportException("文件为空")
        if (bytes.size > MAX_FILE_BYTES) throw ImportException("文件过大（上限 32 MB）")
        val raw = if (isGzip(bytes)) {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { readLimited(it, MAX_DECOMPRESSED_BYTES) }
        } else {
            bytes
        }
        val jsonBytes = if (raw.size >= 3 &&
            raw[0] == 0xEF.toByte() && raw[1] == 0xBB.toByte() && raw[2] == 0xBF.toByte()
        ) raw.copyOfRange(3, raw.size) else raw

        val root = try {
            JSONObject(String(jsonBytes, StandardCharsets.UTF_8))
        } catch (e: JSONException) {
            throw ImportException("文件内容损坏，无法解析")
        }

        if (root.optString("format") != FORMAT) {
            throw ImportException("这不是有效的寸笺导出文件")
        }
        val version = root.optInt("version", -1)
        if (version < 1) throw ImportException("文件版本不受支持")
        if (version > VERSION) throw ImportException("文件版本过高，请先升级应用")

        val now = nowSeconds()
        var duplicates = 0
        var invalid = 0

        val categories = mutableListOf<ExportCategory>()
        val categoryKeys = mutableSetOf<String>()
        val categoriesArray = root.optJSONArray("categories") ?: JSONArray()
        if (categoriesArray.length() > MAX_CATEGORIES) {
            throw ImportException("分类数量超出上限（$MAX_CATEGORIES）")
        }
        for (i in 0 until categoriesArray.length()) {
            val obj = categoriesArray.optJSONObject(i) ?: run { invalid++; null } ?: continue
            val name = cleanName(obj.optString("n"), TextRules.MAX_CATEGORY_NAME)
            if (name.isEmpty()) { invalid++; continue }
            val key = TextRules.key(name)
            if (!categoryKeys.add(key)) { duplicates++; continue }
            categories += ExportCategory(
                name = name,
                color = normalizeColor(obj.optString("c"), "#66CCFF"),
                createdAt = timestamp(obj.optLong("a", 0), now),
            )
        }

        val items = mutableListOf<ExportItem>()
        val itemKeys = mutableSetOf<String>()
        val itemsArray = root.optJSONArray("items") ?: JSONArray()
        if (itemsArray.length() > MAX_ITEMS) {
            throw ImportException("物品数量超出上限（$MAX_ITEMS）")
        }
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: run { invalid++; null } ?: continue
            val name = cleanName(obj.optString("n"), TextRules.MAX_ITEM_NAME)
            if (name.isEmpty()) { invalid++; continue }
            val key = TextRules.key(name)
            if (!itemKeys.add(key)) { duplicates++; continue }

            var categoryIndex: Int? = null
            var categoryName: String? = null
            when (val rawCategory = obj.opt("k")) {
                is Number -> {
                    val idx = rawCategory.toInt()
                    if (idx in categories.indices) categoryIndex = idx
                }
                is String -> {
                    val value = cleanName(rawCategory, TextRules.MAX_CATEGORY_NAME)
                    if (value.isNotEmpty()) categoryName = value
                }
            }
            items += ExportItem(
                name = name,
                categoryIndex = categoryIndex,
                categoryName = categoryName,
                note = TextRules.truncate(obj.optString("r"), TextRules.MAX_ITEM_NOTE),
                createdAt = timestamp(obj.optLong("a", 0), now),
                updatedAt = timestamp(obj.optLong("u", 0), now),
            )
        }

        val templates = mutableListOf<ExportTemplate>()
        val templateKeys = mutableSetOf<String>()
        var templateEntries = 0
        val templatesArray = root.optJSONArray("templates") ?: JSONArray()
        if (templatesArray.length() > MAX_TEMPLATES) {
            throw ImportException("模板数量超出上限（$MAX_TEMPLATES）")
        }
        for (i in 0 until templatesArray.length()) {
            val obj = templatesArray.optJSONObject(i) ?: run { invalid++; null } ?: continue
            val name = cleanName(obj.optString("n"), TextRules.MAX_TEMPLATE_NAME)
            if (name.isEmpty()) { invalid++; continue }
            val key = TextRules.key(name)
            if (!templateKeys.add(key)) { duplicates++; continue }

            val entries = mutableListOf<ExportChecklistItem>()
            val entryKeys = mutableSetOf<String>()
            val it = obj.optJSONArray("it") ?: JSONArray()
            if (it.length() > MAX_ENTRIES) {
                throw ImportException("模板条目数量超出上限（$MAX_ENTRIES）")
            }
            for (j in 0 until it.length()) {
                val entryObj = it.optJSONObject(j) ?: run { invalid++; null } ?: continue
                val entryName = cleanName(entryObj.optString("n"), TextRules.MAX_ITEM_NAME)
                if (entryName.isEmpty()) { invalid++; continue }
                val entryKey = TextRules.key(entryName)
                if (!entryKeys.add(entryKey)) { duplicates++; continue }
                entries += ExportChecklistItem(
                    name = entryName,
                    categoryName = cleanName(entryObj.optString("g"), TextRules.MAX_CATEGORY_NAME),
                )
            }
            templateEntries += entries.size
            templates += ExportTemplate(
                name = name,
                description = TextRules.truncate(obj.optString("d"), TextRules.MAX_TEMPLATE_DESC),
                color = normalizeColor(obj.optString("c"), "#4DB6AC"),
                createdAt = timestamp(obj.optLong("a", 0), now),
                updatedAt = timestamp(obj.optLong("u", 0), now),
                lastUsedAt = obj.optLong("l", 0).takeIf { it > 0 },
                items = entries,
            )
        }

        val plans = mutableListOf<ExportPlan>()
        val planKeys = mutableSetOf<String>()
        var planEntries = 0
        val plansArray = root.optJSONArray("plans") ?: JSONArray()
        if (plansArray.length() > MAX_PLANS) {
            throw ImportException("计划数量超出上限（$MAX_PLANS）")
        }
        for (i in 0 until plansArray.length()) {
            val obj = plansArray.optJSONObject(i) ?: run { invalid++; null } ?: continue
            val destination = cleanName(obj.optString("dst"), TextRules.MAX_DESTINATION)
            if (destination.isEmpty()) { invalid++; continue }
            val start = obj.optLong("s", Long.MIN_VALUE)
            val end = obj.optLong("e", Long.MIN_VALUE)
            if (!isValidEpochDay(start) || !isValidEpochDay(end) || end < start) {
                invalid++
                continue
            }
            val title = TextRules.truncate(
                TextRules.normalize(obj.optString("t")),
                TextRules.MAX_PLAN_TITLE,
            )
            val identity = planIdentity(title, destination, start, end)
            if (!planKeys.add(identity)) { duplicates++; continue }

            val entries = mutableListOf<ExportPlanItem>()
            val entryKeys = mutableSetOf<String>()
            val it = obj.optJSONArray("it") ?: JSONArray()
            if (it.length() > MAX_ENTRIES) {
                throw ImportException("计划条目数量超出上限（$MAX_ENTRIES）")
            }
            for (j in 0 until it.length()) {
                val entryObj = it.optJSONObject(j) ?: run { invalid++; null } ?: continue
                val entryName = cleanName(entryObj.optString("n"), TextRules.MAX_ITEM_NAME)
                if (entryName.isEmpty()) { invalid++; continue }
                val entryKey = TextRules.key(entryName)
                if (!entryKeys.add(entryKey)) { duplicates++; continue }
                entries += ExportPlanItem(
                    name = entryName,
                    categoryName = cleanName(entryObj.optString("g"), TextRules.MAX_CATEGORY_NAME),
                    checked = isChecked(entryObj.opt("x")),
                )
            }
            planEntries += entries.size
            plans += ExportPlan(
                title = title,
                destination = destination,
                startEpochDay = start,
                endEpochDay = end,
                color = normalizeColor(obj.optString("c"), "#66CCFF"),
                note = TextRules.truncate(obj.optString("m"), TextRules.MAX_PLAN_NOTE),
                createdAt = timestamp(obj.optLong("a", 0), now),
                updatedAt = timestamp(obj.optLong("u", 0), now),
                items = entries,
            )
        }

        if (categories.isEmpty() && items.isEmpty() && templates.isEmpty() && plans.isEmpty()) {
            throw ImportException("文件不包含任何数据")
        }

        val snapshot = ExportSnapshot(categories, items, templates, plans)
        val preview = ImportPreview(
            categories = categories.size,
            items = items.size,
            templates = templates.size,
            plans = plans.size,
            templateEntries = templateEntries,
            planEntries = planEntries,
            duplicates = duplicates,
            invalid = invalid,
        )
        return ParsedImport(snapshot, preview)
    }

    fun planIdentity(title: String, destination: String, start: Long, end: Long): String {
        val key = TextRules.key(title.ifBlank { destination })
        return "$key|${TextRules.key(destination)}|$start|$end"
    }

    private fun cleanName(raw: String, max: Int): String =
        TextRules.truncate(TextRules.normalize(raw), max)

    private fun normalizeColor(raw: String, fallback: String): String =
        if (COLOR_REGEX.matches(raw)) raw.uppercase(Locale.ROOT) else fallback

    private fun timestamp(value: Long, fallback: Long): Long = if (value > 0) value else fallback

    private fun isChecked(raw: Any?): Boolean = when (raw) {
        is Boolean -> raw
        is Number -> raw.toInt() == 1
        is String -> raw == "1" || raw.equals("true", ignoreCase = true)
        else -> false
    }

    private fun isValidEpochDay(value: Long): Boolean {
        if (value < MIN_EPOCH_DAY || value > MAX_EPOCH_DAY) return false
        return try {
            LocalDate.ofEpochDay(value)
            true
        } catch (e: DateTimeException) {
            false
        }
    }

    private fun readLimited(input: java.io.InputStream, max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > max) throw ImportException("解压后文件过大（上限 128 MB）")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000
}
