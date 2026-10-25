package com.fieldnotes.testing

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.fieldnotes.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemCategoryTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun seed() {
        TestData.reset()
        TestData.createItems("牙刷")
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("物品").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun renameItemUpdatesLibrary() {
        rule.onNodeWithText("物品").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("牙刷").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("牙刷").performClick()

        rule.onNodeWithTag("item_name").performTextClearance()
        rule.onNodeWithTag("item_name").performTextInput("电动牙刷")
        rule.onNodeWithText("保存修改").performClick()

        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("电动牙刷").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(rule.onAllNodesWithText("电动牙刷").fetchSemanticsNodes().isNotEmpty())
    }
}
