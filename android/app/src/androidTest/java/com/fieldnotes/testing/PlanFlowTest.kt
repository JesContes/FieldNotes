package com.fieldnotes.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fieldnotes.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlanFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seed() {
        TestData.reset()
        TestData.seedPlan(items = listOf("护照" to false, "充电器" to false))
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("东京").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun checkingUpdatesProgressAndRedDotState() {
        rule.onNodeWithText("东京").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "有 2 件物品未勾选")
        )

        rule.onNodeWithText("东京").performClick()
        assertTrue(
            rule.onAllNodesWithText("已收 0 / 2 件").fetchSemanticsNodes().isNotEmpty()
        )

        rule.onNodeWithText("护照").performClick()
        rule.onNodeWithText("护照").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已收")
        )
        assertTrue(
            rule.onAllNodesWithText("已收 1 / 2 件").fetchSemanticsNodes().isNotEmpty()
        )
        assertTrue(
            rule.onAllNodesWithText("未收 1 件").fetchSemanticsNodes().isNotEmpty()
        )

        rule.onNodeWithText("全部勾选").performClick()
        assertTrue(
            rule.onAllNodesWithText("已收 2 / 2 件").fetchSemanticsNodes().isNotEmpty()
        )

        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("东京").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("东京").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "已全部打包")
        )
    }
}
