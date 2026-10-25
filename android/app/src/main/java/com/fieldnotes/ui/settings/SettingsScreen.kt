package com.fieldnotes.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldnotes.FieldNotesApp
import com.fieldnotes.data.transfer.ImportMode
import com.fieldnotes.data.transfer.TransferFiles
import com.fieldnotes.ui.components.BottomNav
import com.fieldnotes.ui.components.ConfirmDialog
import com.fieldnotes.ui.components.Glyph
import com.fieldnotes.ui.components.GlyphIcon
import com.fieldnotes.ui.components.PageHeader
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.PrimaryInk
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueLight
import com.fieldnotes.ui.theme.SkyBlueDeep

@Composable
fun SettingsScreen(onTab: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app.container))
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val transfer by viewModel.transfer.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showClear by remember { mutableStateOf(false) }
    var confirmReplace by remember { mutableStateOf(false) }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TransferFiles.MIME_JSON)
    ) { uri -> if (uri != null) viewModel.writeExport(uri) }
    val gzipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TransferFiles.MIME_GZIP)
    ) { uri -> if (uri != null) viewModel.writeExport(uri) }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.readImport(uri) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(transfer.exportRequest) {
        val request = transfer.exportRequest ?: return@LaunchedEffect
        viewModel.consumeExportRequest()
        if (request.mime == TransferFiles.MIME_GZIP) gzipLauncher.launch(request.fileName)
        else jsonLauncher.launch(request.fileName)
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { BottomNav(active = "settings", onSelect = onTab) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(PageBackground)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PageHeader(title = "我的", subtitle = "数据都在本机，换机用导出带走")
            if (transfer.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = SkyBlue)
            } else {
                Spacer(Modifier.height(2.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                    StatCell("${counts.plans}", "出行计划", Modifier.weight(1f))
                    StatCell("${counts.templates}", "清单模板", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                    StatCell("${counts.items}", "物品", Modifier.weight(1f))
                    StatCell("${counts.categories}", "分类", Modifier.weight(1f))
                }
            }

            Text(
                "数据管理",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = InkPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp),
            )
            SettingsRow(
                title = "导出数据（JSON）",
                subtitle = "完整导出分类、物品、模板与计划",
                glyph = Glyph.Download,
                color = SkyBlueDeep,
                onClick = { viewModel.prepareExport(false) },
            )
            SettingsRow(
                title = "导出压缩包（.gz）",
                subtitle = "内容不变，体积更小，适合备份",
                glyph = Glyph.Download,
                color = Color(0xFF4DB6AC),
                onClick = { viewModel.prepareExport(true) },
            )
            SettingsRow(
                title = "导入数据…",
                subtitle = "从备份文件合并或覆盖恢复",
                glyph = Glyph.Upload,
                color = Color(0xFFFFB74D),
                onClick = { openLauncher.launch(arrayOf("*/*")) },
            )
            SettingsRow(
                title = "清空所有数据",
                subtitle = "不可恢复，建议先导出备份",
                glyph = Glyph.Trash,
                color = DangerRed,
                titleColor = DangerRed,
                onClick = { showClear = true },
            )

            Text(
                "关于",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = InkPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp),
            )
            AboutRow("关于寸笺", "版本 1.0")
            AboutRow("隐私说明", "纯本地 · 无网络权限")
            Spacer(Modifier.height(24.dp))
            Text(
                "寸笺 Field Notes · 所有数据仅存于本机",
                fontSize = 11.sp,
                color = InkSecondary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (showClear) {
        ConfirmDialog(
            title = "清空所有数据",
            message = "将删除全部计划、模板、物品与分类，且不可恢复。建议先导出备份。",
            confirmLabel = "清空",
            onDismiss = { showClear = false },
            onConfirm = {
                showClear = false
                viewModel.clearAllData()
            },
        )
    }
    transfer.importPreview?.let { preview ->
        ImportPreviewDialog(
            preview = preview,
            busy = transfer.busy,
            onCancel = viewModel::cancelImport,
            onMerge = { viewModel.commitImport(ImportMode.MERGE) },
            onReplace = { confirmReplace = true },
        )
    }
    if (confirmReplace && transfer.importPreview != null) {
        ConfirmDialog(
            title = "覆盖导入",
            message = "将先清空本机全部计划、模板、物品与分类，再导入该文件。此操作不可恢复，确定继续吗？",
            confirmLabel = "覆盖导入",
            onDismiss = { confirmReplace = false },
            onConfirm = {
                confirmReplace = false
                viewModel.commitImport(ImportMode.REPLACE)
            },
        )
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(label, fontSize = 11.sp, color = InkSecondary)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    glyph: Glyph,
    color: Color,
    onClick: () -> Unit,
    titleColor: Color = InkPrimary,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, color, Modifier.size(21.dp), 1.8f)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = titleColor)
            Text(subtitle, fontSize = 11.sp, color = InkSecondary)
        }
        Text("›", fontSize = 20.sp, color = Color(0xFFC2D1DD))
    }
}

@Composable
private fun AboutRow(title: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(15.dp)).background(Color.White)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, color = InkPrimary)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 11.sp, color = InkSecondary)
        Text("›", fontSize = 20.sp, color = Color(0xFFC2D1DD))
    }
}

@Composable
private fun ImportPreviewDialog(
    preview: ImportPreviewUi,
    busy: Boolean,
    onCancel: () -> Unit,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text("导入数据") },
        text = {
            Column {
                Text(
                    "${preview.fileName} · ${TransferFiles.formatSize(preview.size)}",
                    fontSize = 11.sp,
                    color = InkSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF7FBFE)).padding(12.dp),
                ) {
                    Column {
                        Row {
                            PreviewStat("${preview.preview.categories}", "分类", Modifier.weight(1f))
                            PreviewStat("${preview.preview.items}", "物品", Modifier.weight(1f))
                            PreviewStat("${preview.preview.templates}", "模板", Modifier.weight(1f))
                            PreviewStat("${preview.preview.plans}", "计划", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "模板条目 ${preview.preview.templateEntries} 条 · 计划条目 ${preview.preview.planEntries} 条",
                            fontSize = 10.sp,
                            color = InkSecondary,
                        )
                        if (preview.preview.duplicates > 0 || preview.preview.invalid > 0) {
                            Text(
                                "文件内重复 ${preview.preview.duplicates} 条 · 无效 ${preview.preview.invalid} 条",
                                fontSize = 10.sp,
                                color = InkSecondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("合并：只补充本机没有的数据，重复项跳过。", fontSize = 11.sp, color = InkSecondary)
                Text("覆盖：先清空本机全部数据，再导入此文件。", fontSize = 11.sp, color = DangerRed)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFF3F3)).padding(8.dp),
                ) {
                    Text("覆盖操作不可恢复，建议先导出一份备份。", fontSize = 10.sp, color = Color(0xFFC94A4A))
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onMerge, enabled = !busy) { Text("合并") }
                Button(
                    onClick = onReplace,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
                ) { Text("覆盖") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !busy) { Text("取消") }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
private fun PreviewStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
        Text(label, fontSize = 10.sp, color = InkSecondary)
    }
}
