package com.fieldnotes.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fieldnotes.AppContainer
import com.fieldnotes.data.FieldNotesRepository
import com.fieldnotes.data.transfer.ExportCodec
import com.fieldnotes.data.transfer.ImportMode
import com.fieldnotes.data.transfer.ImportPreview
import com.fieldnotes.data.transfer.ParsedImport
import com.fieldnotes.data.transfer.TransferFiles
import com.fieldnotes.domain.Counts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExportRequest(val fileName: String, val mime: String)

data class ImportPreviewUi(
    val fileName: String,
    val size: Long,
    val preview: ImportPreview,
)

data class TransferState(
    val busy: Boolean = false,
    val exportRequest: ExportRequest? = null,
    val importPreview: ImportPreviewUi? = null,
)

class SettingsViewModel(
    private val repository: FieldNotesRepository,
    private val context: Context,
) : ViewModel() {

    val counts: StateFlow<Counts> = repository.observeCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Counts())

    private val _transfer = MutableStateFlow(TransferState())
    val transfer: StateFlow<TransferState> = _transfer.asStateFlow()

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val messages = eventChannel.receiveAsFlow()

    private var pendingExport: ByteArray? = null
    private var pendingImport: ParsedImport? = null

    fun prepareExport(gzip: Boolean) {
        if (_transfer.value.busy) return
        _transfer.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repository.export(gzip) }
                .onSuccess { bytes ->
                    pendingExport = bytes
                    _transfer.update {
                        it.copy(
                            busy = false,
                            exportRequest = ExportRequest(
                                fileName = TransferFiles.suggestedName(gzip),
                                mime = if (gzip) TransferFiles.MIME_GZIP else TransferFiles.MIME_JSON,
                            ),
                        )
                    }
                }
                .onFailure {
                    _transfer.update { it.copy(busy = false) }
                    eventChannel.send(it.message ?: "导出失败")
                }
        }
    }

    fun consumeExportRequest() {
        _transfer.update { it.copy(exportRequest = null) }
    }

    fun writeExport(uri: Uri) {
        val bytes = pendingExport ?: return
        _transfer.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { TransferFiles.write(context, uri, bytes) } }
                .onSuccess {
                    pendingExport = null
                    _transfer.update { it.copy(busy = false) }
                    eventChannel.send("已导出 ${TransferFiles.formatSize(bytes.size.toLong())}")
                }
                .onFailure {
                    _transfer.update { it.copy(busy = false) }
                    eventChannel.send(it.message ?: "导出失败")
                }
        }
    }

    fun readImport(uri: Uri) {
        if (_transfer.value.busy) return
        _transfer.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val name = TransferFiles.displayName(context, uri) ?: "导入文件"
                    val size = TransferFiles.displaySize(context, uri) ?: 0L
                    val bytes = TransferFiles.read(context, uri)
                    val parsed = ExportCodec.decode(bytes)
                    Triple(name, size, parsed)
                }
            }.onSuccess { (name, size, parsed) ->
                pendingImport = parsed
                _transfer.update {
                    it.copy(
                        busy = false,
                        importPreview = ImportPreviewUi(name, size, parsed.preview),
                    )
                }
            }.onFailure {
                _transfer.update { it.copy(busy = false) }
                eventChannel.send(it.message ?: "导入失败")
            }
        }
    }

    fun cancelImport() {
        pendingImport = null
        _transfer.update { it.copy(importPreview = null) }
    }

    fun commitImport(mode: ImportMode) {
        val parsed = pendingImport ?: return
        if (_transfer.value.busy) return
        _transfer.update { it.copy(busy = true) }
        viewModelScope.launch {
            runCatching { repository.importSnapshot(parsed, mode) }
                .onSuccess { result ->
                    pendingImport = null
                    _transfer.update { it.copy(busy = false, importPreview = null) }
                    val parts = mutableListOf<String>()
                    if (result.categories > 0) parts += "${result.categories} 个分类"
                    if (result.items > 0) parts += "${result.items} 件物品"
                    if (result.templates > 0) parts += "${result.templates} 个模板"
                    if (result.plans > 0) parts += "${result.plans} 个计划"
                    var text = if (parts.isEmpty()) "没有新增数据" else "已导入 " + parts.joinToString("、")
                    if (result.skipped > 0) text += "；跳过 ${result.skipped} 条重复"
                    if (result.invalid > 0) text += "；忽略 ${result.invalid} 条无效"
                    eventChannel.send(text)
                }
                .onFailure {
                    _transfer.update { it.copy(busy = false) }
                    eventChannel.send(it.message ?: "导入失败，请重试")
                }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            eventChannel.send("已清空所有数据")
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container.repository, container.applicationContext) }
        }
    }
}
