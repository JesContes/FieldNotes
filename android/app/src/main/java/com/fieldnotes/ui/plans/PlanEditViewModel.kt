package com.fieldnotes.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.data.PlanDraft
import com.fieldnotes.domain.Plan
import com.fieldnotes.domain.TextRules
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.colorFromHex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PlanFormState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val destination: String = "",
    val title: String = "",
    val start: LocalDate = LocalDate.now(),
    val end: LocalDate = LocalDate.now().plusDays(2),
    val note: String = "",
    val colorHex: String = "#66CCFF",
    val error: String? = null,
    val saving: Boolean = false,
)

sealed interface PlanEditEvent {
    data class Saved(val planId: Long, val isNew: Boolean) : PlanEditEvent
    data class Message(val text: String) : PlanEditEvent
    data object LoadFailed : PlanEditEvent
}

class PlanEditViewModel(
    private val repository: FieldNotesRepository,
    private val planId: Long?,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanFormState(isNew = planId == null))
    val state: StateFlow<PlanFormState> = _state.asStateFlow()

    private val eventChannel = Channel<PlanEditEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        if (planId == null) {
            _state.value = _state.value.copy(loading = false)
        } else {
            viewModelScope.launch {
                val plan = repository.getPlan(planId)
                if (plan == null) {
                    eventChannel.send(PlanEditEvent.LoadFailed)
                } else {
                    _state.value = plan.toForm()
                }
            }
        }
    }

    fun setDestination(value: String) {
        _state.value = _state.value.copy(destination = value, error = null)
    }

    fun setTitle(value: String) {
        _state.value = _state.value.copy(title = value, error = null)
    }

    fun setDates(start: LocalDate, end: LocalDate) {
        _state.value = _state.value.copy(
            start = start,
            end = if (end < start) start else end,
            error = null,
        )
    }

    fun setNote(value: String) {
        _state.value = _state.value.copy(note = value, error = null)
    }

    fun setColor(hex: String) {
        _state.value = _state.value.copy(colorHex = hex)
    }

    fun save() {
        val form = _state.value
        if (form.saving) return
        val destination = TextRules.normalize(form.destination)
        val title = TextRules.normalize(form.title)
        val note = form.note.trim()
        val error = when {
            !TextRules.isValidName(destination, TextRules.MAX_DESTINATION) ->
                "目的地需为 1–${TextRules.MAX_DESTINATION} 字"
            TextRules.codePointLength(title) > TextRules.MAX_PLAN_TITLE ->
                "计划名称不能超过 ${TextRules.MAX_PLAN_TITLE} 字"
            form.end < form.start -> "返程日不能早于出发日"
            TextRules.codePointLength(note) > TextRules.MAX_PLAN_NOTE ->
                "备注不能超过 ${TextRules.MAX_PLAN_NOTE} 字"
            else -> null
        }
        if (error != null) {
            _state.value = form.copy(error = error)
            eventChannel.trySend(PlanEditEvent.Message(error))
            return
        }
        _state.value = form.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching {
                val draft = PlanDraft(
                    title = title,
                    destination = destination,
                    start = form.start,
                    end = form.end,
                    note = note,
                    colorHex = form.colorHex,
                )
                if (planId == null) {
                    val id = repository.createPlan(draft)
                    eventChannel.send(PlanEditEvent.Saved(id, isNew = true))
                } else {
                    val rows = repository.updatePlan(planId, draft)
                    if (rows == 0) {
                        _state.value = _state.value.copy(saving = false, error = "计划已不存在")
                        eventChannel.send(PlanEditEvent.Message("计划已不存在"))
                    } else {
                        eventChannel.send(PlanEditEvent.Saved(planId, isNew = false))
                    }
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    saving = false,
                    error = it.message ?: "保存失败，请重试",
                )
                eventChannel.send(PlanEditEvent.Message(it.message ?: "保存失败，请重试"))
            }
        }
    }

    private fun Plan.toForm(): PlanFormState = PlanFormState(
        loading = false,
        isNew = false,
        destination = destination,
        title = title,
        start = start,
        end = end,
        note = note,
        colorHex = colorHex,
        error = null,
    )

    companion object {
        fun factory(container: AppContainer, planId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer { PlanEditViewModel(container.repository, planId) }
        }
    }
}
