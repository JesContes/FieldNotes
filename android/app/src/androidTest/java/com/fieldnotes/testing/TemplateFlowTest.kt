package com.fieldnotes.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fieldnotes.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TemplateFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seed() {
        TestData.reset()
        TestData.seedTemplate(items = listOf("防晒霜", "驱蚊水"))
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("模板").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun removeTemplateItemThenUndo() {
        rule.onNodeWithText("模板").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("夏季游").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("夏季游").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("防晒霜").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onAllNodesWithContentDescription("移除")[0].performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("防晒霜").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(rule.onAllNodesWithText("防晒霜").fetchSemanticsNodes().isEmpty())

        rule.onNodeWithText("撤销").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("防晒霜").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(rule.onAllNodesWithText("防晒霜").fetchSemanticsNodes().isNotEmpty())
    }
}
