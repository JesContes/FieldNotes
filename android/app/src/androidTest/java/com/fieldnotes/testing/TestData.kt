package com.fieldnotes.testing

import androidx.test.core.app.ApplicationProvider
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.data.PlanDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

object TestData {
    fun app(): TestFieldNotesApp = ApplicationProvider.getApplicationContext()

    fun repo(): FieldNotesRepository = app().container.repository

    fun reset() = runBlocking {
        repo().clearAllData()
        repo().seedIfEmpty()
    }

    fun createItems(vararg names: String): List<Long> = runBlocking {
        names.map { repo().createItem(it, null, "") }
    }

    fun seedPlan(
        destination: String = "东京",
        title: String = "东京五日游",
        items: List<Pair<String, Boolean>> = listOf("护照" to false, "充电器" to false),
    ): Long = runBlocking {
        val ids = items.map { repo().createItem(it.first, null, "") }
        val planId = repo().createPlan(
            PlanDraft(
                title = title,
                destination = destination,
                start = LocalDate.now(),
                end = LocalDate.now().plusDays(2),
                note = "",
                colorHex = "#66CCFF",
            )
        )
        repo().addItemsToPlan(planId, ids)
        val plan = repo().getPlan(planId) ?: error("plan missing")
        items.filter { it.second }.forEach { (name, _) ->
            plan.items.firstOrNull { it.name == name }?.let { repo().toggleItem(it.id, true) }
        }
        planId
    }

    fun seedTemplate(
        name: String = "夏季游",
        items: List<String> = listOf("防晒霜", "驱蚊水"),
    ): Long = runBlocking {
        val ids = items.map { repo().createItem(it, null, "") }
        val templateId = repo().createTemplate(name, "海边 5 天", "#4DB6AC")
        repo().addItemsToTemplate(templateId, ids)
        templateId
    }

    fun categoryId(name: String): Long? = runBlocking {
        repo().observeCategories().first().firstOrNull { it.name == name }?.id
    }
}
