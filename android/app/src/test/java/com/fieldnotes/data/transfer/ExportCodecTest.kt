package com.fieldnotes.data.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportCodecTest {

    private fun snapshot() = ExportSnapshot(
        categories = listOf(
            ExportCategory("日用品", "#66CCFF", 1000),
            ExportCategory("服装", "#4DB6AC", 1000),
        ),
        items = listOf(
            ExportItem("牙刷", 0, null, "软毛", 1000, 1000),
            ExportItem("短袖", 1, null, "", 1000, 1000),
        ),
        templates = listOf(
            ExportTemplate(
                "夏季游", "海边", "#4DB6AC", 1000, 1000, null,
                listOf(ExportChecklistItem("短袖", "服装"), ExportChecklistItem("防晒霜", "日用品")),
            )
        ),
        plans = listOf(
            ExportPlan(
                "东京五日游", "东京", 20752, 20756, "#66CCFF", "看展",
                1000, 1000,
                listOf(
                    ExportPlanItem("护照", "证件", true),
                    ExportPlanItem("充电器", "电子设备", false),
                ),
            )
        ),
    )

    @Test
    fun roundTrip() {
        val bytes = ExportCodec.encode(snapshot(), gzip = false)
        val parsed = ExportCodec.decode(bytes)
        assertEquals(2, parsed.snapshot.categories.size)
        assertEquals(2, parsed.snapshot.items.size)
        assertEquals(1, parsed.snapshot.templates.size)
        assertEquals(1, parsed.snapshot.plans.size)
        assertEquals("牙刷", parsed.snapshot.items[0].name)
        assertEquals(0, parsed.snapshot.items[0].categoryIndex)
        assertEquals(1, parsed.snapshot.plans[0].items.count { it.checked })
        assertEquals(20752, parsed.snapshot.plans[0].startEpochDay)
        assertFalse(ExportCodec.isGzip(bytes))
    }

    @Test
    fun gzipRoundTrip() {
        val bytes = ExportCodec.encode(snapshot(), gzip = true)
        assertTrue(ExportCodec.isGzip(bytes))
        val parsed = ExportCodec.decode(bytes)
        assertEquals("夏季游", parsed.snapshot.templates[0].name)
        assertEquals(2, parsed.snapshot.templates[0].items.size)
    }

    @Test
    fun rejectsWrongFormat() {
        val bytes = "{\"format\":\"other\",\"version\":1}".toByteArray()
        val error = runCatching { ExportCodec.decode(bytes) }.exceptionOrNull()
        assertTrue(error is ImportException)
    }

    @Test
    fun rejectsFutureVersion() {
        val bytes = "{\"format\":\"field-notes-export\",\"version\":2}".toByteArray()
        val error = runCatching { ExportCodec.decode(bytes) }.exceptionOrNull()
        assertTrue(error is ImportException)
    }

    @Test
    fun rejectsOversizedCategoryArray() {
        val categories = (0..1000).joinToString(",") { "{\"n\":\"c$it\"}" }
        val json = """
            {"format":"field-notes-export","version":1,"categories":[$categories],
             "items":[],"templates":[],"plans":[]}
        """.trimIndent().toByteArray()
        val error = runCatching { ExportCodec.decode(json) }.exceptionOrNull()
        assertTrue(error is ImportException)
    }

    @Test
    fun skipsInvalidPlanDateAndDeduplicates() {
        val json = """
            {
              "format":"field-notes-export",
              "version":1,
              "categories":[],
              "items":[{"n":"牙刷","k":0},{"n":"牙刷"}],
              "templates":[],
              "plans":[
                {"dst":"东京","s":999999999,"e":999999999},
                {"dst":"大阪","s":20752,"e":20756,"it":[{"n":"护照","g":"证件","x":1}]}
              ]
            }
        """.trimIndent().toByteArray()
        val parsed = ExportCodec.decode(json)
        assertEquals(1, parsed.snapshot.items.size)
        assertEquals(1, parsed.snapshot.plans.size)
        assertEquals(1, parsed.snapshot.plans[0].items.count { it.checked })
        assertEquals(1, parsed.preview.duplicates)
        assertEquals(1, parsed.preview.invalid)
    }
}
