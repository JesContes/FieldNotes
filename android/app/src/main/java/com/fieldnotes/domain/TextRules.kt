package com.fieldnotes.domain

import java.util.Locale

/**
 * 名称归一化与长度规则。
 *
 * - 归一化：去首尾空白、连续空白折叠为单个空格；
 * - nameKey：在归一化基础上转小写，用于唯一性判断与去重；
 * - 长度按 Unicode 码点计算，emoji 算 1 个字符。
 */
object TextRules {
    const val MAX_CATEGORY_NAME = 12
    const val MAX_ITEM_NAME = 40
    const val MAX_TEMPLATE_NAME = 30
    const val MAX_PLAN_TITLE = 30
    const val MAX_DESTINATION = 40
    const val MAX_ITEM_NOTE = 100
    const val MAX_TEMPLATE_DESC = 140
    const val MAX_PLAN_NOTE = 200

    fun normalize(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    fun key(raw: String): String = normalize(raw).lowercase(Locale.ROOT)

    fun codePointLength(value: String): Int = value.codePointCount(0, value.length)

    fun truncate(value: String, maxCodePoints: Int): String {
        if (codePointLength(value) <= maxCodePoints) return value
        val end = value.offsetByCodePoints(0, maxCodePoints)
        return value.substring(0, end)
    }

    fun isValidName(value: String, max: Int): Boolean {
        val normalized = normalize(value)
        val length = codePointLength(normalized)
        return length in 1..max
    }
}
