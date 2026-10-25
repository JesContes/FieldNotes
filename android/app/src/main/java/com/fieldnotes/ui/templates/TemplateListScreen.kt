package com.fieldnotes.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldnotes.FieldNotesApp
import com.fieldnotes.domain.TextRules
import com.fieldnotes.ui.components.BottomNav
import com.fieldnotes.ui.components.ColorRow
import com.fieldnotes.ui.components.ConfirmDialog
import com.fieldnotes.ui.components.EmptyState
import com.fieldnotes.ui.components.Glyph
import com.fieldnotes.ui.components.GlyphIcon
import com.fieldnotes.ui.components.PageHeader
import com.fieldnotes.ui.components.PillChip
import com.fieldnotes.ui.components.SearchField
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.colorFromHex

@Composable
fun TemplateListScreen(
    onOpenTemplate: (Long) -> Unit,
    onTab: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: TemplatesViewModel = viewModel(factory = TemplatesViewModel.factory(app.container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showCreate by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<TemplateCardUi?>(null) }
    var deleteTarget by remember { mutableStateOf<TemplateCardUi?>(null) }
    var menuId by remember { mutableStateOf<Long?>(null) }
    var searchOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNav(active = "templates", onSelect = onTab) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }, containerColor = SkyBlue, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "新建模板", modifier = Modifier.size(28.dp))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().background(PageBackground).padding(padding)) {
            PageHeader(
                title = "模板",
                subtitle = "常带的东西，一次收好",
                trailing = {
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索", tint = InkSecondary)
                    }
                },
            )
            if (searchOpen) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "搜索模板…",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(4.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("全部模板", style = MaterialTheme.typography.titleSmall, color = InkPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    "${state.totalCount} 个模板 · ${state.totalItems} 件物品",
                    fontSize = 11.sp,
                    color = InkSecondary,
                )
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SkyBlue)
                }
                state.templates.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        glyph = Glyph.Checklist,
                        title = if (state.totalCount == 0) "还没有清单模板" else "没有匹配的模板",
                        subtitle = "把常带的东西存成模板，下次一键导入",
                        primaryLabel = "新建模板",
                        onPrimary = { showCreate = true },
                    )
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            menuOpen = menuId == template.id,
                            onOpen = { onOpenTemplate(template.id) },
                            onMenu = { menuId = if (menuId == template.id) null else template.id },
                            onRename = {
                                menuId = null
                                editTarget = template
                            },
                            onDuplicate = {
                                menuId = null
                                viewModel.duplicate(template.id)
                            },
                            onDelete = {
                                menuId = null
                                deleteTarget = template
                            },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        TemplateEditDialog(
            title = "新建模板",
            initialName = "",
            initialDescription = "",
            initialColor = "#4DB6AC",
            onDismiss = { showCreate = false },
            onSave = { name, desc, color ->
                showCreate = false
                viewModel.create(name, desc, color)
            },
        )
    }
    editTarget?.let { target ->
        TemplateEditDialog(
            title = "编辑模板",
            initialName = target.name,
            initialDescription = target.description,
            initialColor = target.colorHex,
            onDismiss = { editTarget = null },
            onSave = { name, desc, color ->
                editTarget = null
                viewModel.update(target.id, name, desc, color)
            },
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除模板",
            message = "确定要删除「${target.name}」吗？模板中的 ${target.itemCount} 条记录会一并删除，物品库与已创建的计划不受影响。",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.delete(target.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun TemplateCard(
    template: TemplateCardUi,
    menuOpen: Boolean,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colorFromHex(template.colorHex)),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Checklist, Color.White, Modifier.size(23.dp), 1.8f)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        template.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (template.description.isNotBlank()) {
                        Text(
                            template.description,
                            fontSize = 12.sp,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text("${template.itemCount} 件", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                Box {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = Color(0xFFB9C9D6))
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = onMenu,
                        modifier = Modifier.background(Color.White),
                    ) {
                        DropdownMenuItem(text = { Text("重命名 / 编辑") }, onClick = onRename)
                        DropdownMenuItem(text = { Text("复制模板") }, onClick = onDuplicate)
                        DropdownMenuItem(text = { Text("删除模板", color = DangerRed) }, onClick = onDelete)
                    }
                }
            }
            if (template.categorySummary.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(template.categorySummary, fontSize = 11.sp, color = InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(template.lastUsedText, fontSize = 11.sp, color = InkSecondary)
        }
    }
}

@Composable
fun TemplateEditDialog(
    title: String,
    initialName: String,
    initialDescription: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var color by remember { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("模板名称") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    singleLine = true,
                    label = { Text("说明（选填）") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("主题色", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                Spacer(Modifier.height(8.dp))
                ColorRow(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description, color) },
                enabled = TextRules.isValidName(name, TextRules.MAX_TEMPLATE_NAME),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}
