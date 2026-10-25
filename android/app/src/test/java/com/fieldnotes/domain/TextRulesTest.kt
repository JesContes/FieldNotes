package com.fieldnotes.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TextRulesTest {
    @Test
    fun normalizeTrimsAndCollapsesWhitespace() {
        assertEquals("牙刷", TextRules.normalize("  牙刷  "))
        assertEquals("a b", TextRules.normalize("a   b"))
    }

    @Test
    fun keyIgnoresCase() {
        assertEquals("toothbrush", TextRules.key("  ToothBrush "))
        assertEquals("短袖", TextRules.key("短袖"))
    }

    @Test
    fun codePointLengthCountsEmojiAsOne() {
        assertEquals(2, TextRules.codePointLength("😀a"))
    }

    @Test
    fun truncateKeepsCodePoints() {
        assertEquals("😀a", TextRules.truncate("😀ab", 2))
    }

    @Test
    fun nameValidation() {
        assertTrue(TextRules.isValidName("牙刷", TextRules.MAX_ITEM_NAME))
        assertFalse(TextRules.isValidName("   ", TextRules.MAX_ITEM_NAME))
        assertFalse(TextRules.isValidName("a".repeat(41), TextRules.MAX_ITEM_NAME))
    }
}

class PlanStatusTest {
    private val today = LocalDate.of(2026, 10, 26)

    @Test
    fun upcoming() {
        val status = PlanStatus.of(today.plusDays(1), today.plusDays(3), today)
        assertEquals(PlanStatus.UPCOMING, status)
        assertEquals("还有 1 天出发", status.label(today.plusDays(1), today.plusDays(3), today))
    }

    @Test
    fun ongoingOnStartDay() {
        val status = PlanStatus.of(today, today.plusDays(3), today)
        assertEquals(PlanStatus.ONGOING, status)
        assertEquals("进行中 · 第 1 天", status.label(today, today.plusDays(3), today))
    }

    @Test
    fun ongoingOnEndDay() {
        val status = PlanStatus.of(today.minusDays(2), today, today)
        assertEquals(PlanStatus.ONGOING, status)
        assertEquals("进行中 · 第 3 天", status.label(today.minusDays(2), today, today))
    }

    @Test
    fun past() {
        val status = PlanStatus.of(today.minusDays(5), today.minusDays(1), today)
        assertEquals(PlanStatus.PAST, status)
        assertEquals("已结束 1 天", status.label(today.minusDays(5), today.minusDays(1), today))
    }

    @Test
    fun redDotRule() {
        val plan = Plan(
            id = 1,
            title = "东京",
            destination = "东京",
            start = today,
            end = today.plusDays(3),
            note = "",
            colorHex = "#66CCFF",
            createdAt = 0,
            updatedAt = 0,
            items = listOf(
                PlanItem(1, "护照", "证件", true, 0),
                PlanItem(2, "充电器", "电子设备", false, 1),
            ),
        )
        assertTrue(plan.hasUnchecked)
        assertEquals(1, plan.checkedCount)
        assertEquals(2, plan.totalCount)
        assertEquals(0.5f, plan.progress, 0.001f)
    }
}
