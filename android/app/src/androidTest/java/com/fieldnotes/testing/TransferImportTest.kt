package com.fieldnotes.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fieldnotes.data.transfer.ExportCategory
import com.fieldnotes.data.transfer.ExportChecklistItem
import com.fieldnotes.data.transfer.ExportCodec
import com.fieldnotes.data.transfer.ExportItem
import com.fieldnotes.data.transfer.ExportPlan
import com.fieldnotes.data.transfer.ExportPlanItem
import com.fieldnotes.data.transfer.ExportSnapshot
import com.fieldnotes.data.transfer.ExportTemplate
import com.fieldnotes.data.transfer.ImportMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TransferImportTest {

    private fun snapshot(): ExportSnapshot {
        val start = LocalDate.now().toEpochDay()
        return ExportSnapshot(
            categories = listOf(ExportCategory("测试分类", "#66CCFF", 1000)),
            items = listOf(ExportItem("测试物品", 0, null, "备注", 1000, 1000)),
            templates = listOf(
                ExportTemplate(
                    "测试模板", "说明", "#4DB6AC", 1000, 1000, null,
                    listOf(ExportChecklistItem("测试物品", "测试分类")),
                )
            ),
            plans = listOf(
                ExportPlan(
                    "测试计划", "测试地", start, start + 2, "#66CCFF", "",
                    1000, 1000,
                    listOf(ExportPlanItem("测试物品", "测试分类", true)),
                )
            ),
        )
    }

    @Before
    fun seed() {
        TestData.reset()
    }

    @Test
    fun mergeImportInsertsThenSkipsOnSecondRun() = runBlocking {
        val parsed = ExportCodec.decode(ExportCodec.encode(snapshot(), gzip = false))
        val first = TestData.repo().importSnapshot(parsed, ImportMode.MERGE)
        assertEquals(1, first.categories)
        assertEquals(1, first.items)
        assertEquals(1, first.templates)
        assertEquals(1, first.plans)

        val second = TestData.repo().importSnapshot(parsed, ImportMode.MERGE)
        assertEquals(0, second.categories)
        assertEquals(0, second.items)
        assertEquals(0, second.templates)
        assertEquals(0, second.plans)
        assertTrue(second.skipped >= 3)

        assertEquals(1, TestData.repo().observePlans().first().size)
        assertEquals(1, TestData.repo().observeTemplates().first().size)
    }

    @Test
    fun gzipExportIsReadable() {
        val bytes = ExportCodec.encode(snapshot(), gzip = true)
        assertTrue(ExportCodec.isGzip(bytes))
        val parsed = ExportCodec.decode(bytes)
        assertEquals("测试计划", parsed.snapshot.plans[0].title)
        assertEquals(1, parsed.snapshot.plans[0].items.count { it.checked })
    }

    @Test
    fun replaceImportClearsLocalDataFirst() = runBlocking {
        val parsed = ExportCodec.decode(ExportCodec.encode(snapshot(), gzip = false))
        TestData.repo().importSnapshot(parsed, ImportMode.REPLACE)
        assertEquals(1, TestData.repo().observePlans().first().size)
        assertEquals(1, TestData.repo().observeCategories().first().size)
        assertEquals("测试分类", TestData.repo().observeCategories().first()[0].name)
        assertEquals(1, TestData.repo().observeItems().first().size)
    }
}
