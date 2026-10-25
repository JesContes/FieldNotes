package com.fieldnotes.ui.templates

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import com.fieldnotes.domain.TemplateItem
import com.fieldnotes.domain.TextRules
import com.fieldnotes.domain.formatLastUsed
import com.fieldnotes.domain.formatRange
import com.fieldnotes.ui.components.ConfirmDialog
import com.fieldnotes.ui.components.DetailTopBar
import com.fieldnotes.ui.components.Glyph
import com.fieldnotes.ui.components.GlyphIcon
import com.fieldnotes.ui.components.MoreButton
import com.fieldnotes.ui.plans.ItemPickerSheet
import com.fieldnotes.ui.plans.SourceOption
import com.fieldnotes.ui.plans.SourcePickerSheet
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueDeep
import com.fieldnotes.ui.theme.colorFromHex

@Composable
fun TemplateDetailScreen(
    templateId: Long,
    onBack: () -> Unit,
    onOpenTemplate: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: TemplateDetailViewModel =
        viewModel(factory = TemplateDetailViewModel.factory(app.container, templateId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlans.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var showItemPicker by remember { mutableStateOf(false) }
    var showPlanSource by remember { mutableStateOf(false) }
    var showTemplateSource by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplateDetailEvent.Message -> snackbar.showSnackbar(event.text)
                is TemplateDetailEvent.Removed -> {
                    val result = snackbar.showSnackbar(
                        "已移除 ${event.name}",
                        actionLabel = "撤销",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
                    else viewModel.clearPendingRemoved()
                }
                is TemplateDetailEvent.Duplicated -> {
                    snackbar.showSnackbar("已复制模板")
                    onOpenTemplate(event.id)
                }
                TemplateDetailEvent.Deleted -> onBack()
            }
        }
    }

    val template = state.template

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            DetailTopBar(
                title = template?.name ?: "模板详情",
                onBack = onBack,
                actions = {
                    Box {
                        MoreButton { menuOpen = true }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(Color.White),
                        ) {
                            DropdownMenuItem(
                                text = { Text("重命名 / 编辑") },
                                onClick = {
                                    menuOpen = false
                                    showRename = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("从模板导入") },
                                onClick = {
                                    menuOpen = false
                                    showTemplateSource = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("复制模板") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.duplicate()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除模板", color = DangerRed) },
                                onClick = {
                                    menuOpen = false
                                    showDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showPlanSource = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("从计划导入", color = SkyBlueDeep, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showItemPicker = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
                ) {
                    Text("添加物品", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyBlue)
            }
            template == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("模板已不存在", color = InkPrimary)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack) { Text("返回", color = SkyBlueDeep) }
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    HeaderCard(
                        name = template.name,
                        description = template.description,
                        colorHex = template.colorHex,
                        itemCount = template.items.size,
                        categoryCount = template.items.map { it.categoryName.ifBlank { "未分类" } }.distinct().size,
                        lastUsed = formatLastUsed(template.lastUsedAt),
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(SkyBlue))
                        Text(
                            "模板物品",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        Text("移除只影响本模板", fontSize = 11.sp, color = InkSecondary)
                    }
                }
                if (state.groups.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("模板还是空的", fontWeight = FontWeight.Bold, color = InkPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text("添加物品，或从历史计划导入", fontSize = 12.sp, color = InkSecondary)
                        }
                    }
                } else {
                    state.groups.forEach { group ->
                        item(key = "group-${group.categoryName}") {
                            Card(
                                shape = RoundedCornerShape(15.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(group.categoryName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = InkPrimary)
                                        Spacer(Modifier.weight(1f))
                                        Text("${group.items.size} 件", fontSize = 11.sp, color = InkSecondary)
                                    }
                                    group.items.forEachIndexed { index, item ->
                                        TemplateItemRow(item = item, onRemove = { viewModel.remove(item) })
                                        if (index != group.items.lastIndex) {
                                            Box(
                                                Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                                    .height(1.dp).background(Color(0xFFEDF3F8)),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showItemPicker) {
        ItemPickerSheet(
            items = allItems,
            categories = allCategories,
            existingKeys = template?.items?.map { TextRules.key(it.name) }?.toSet() ?: emptySet(),
            onDismiss = { showItemPicker = false },
            onConfirm = { viewModel.addItems(it) },
        )
    }
    if (showPlanSource) {
        SourcePickerSheet(
            title = "从计划导入",
            subtitle = "复制历史计划中的全部物品（不复制勾选态）",
            options = allPlans.map {
                SourceOption(
                    id = it.id,
                    title = it.displayTitle,
                    subtitle = formatRange(it.start, it.end),
                    itemCount = it.totalCount,
                )
            },
            onDismiss = { showPlanSource = false },
            onConfirm = { viewModel.importFromPlan(it) },
        )
    }
    if (showTemplateSource) {
        SourcePickerSheet(
            title = "从模板导入",
            subtitle = "把另一个模板的物品复制过来，重复的会跳过",
            options = allTemplates.filter { it.id != templateId }.map {
                SourceOption(
                    id = it.id,
                    title = it.name,
                    subtitle = it.description.ifBlank { formatLastUsed(it.lastUsedAt) },
                    itemCount = it.items.size,
                )
            },
            onDismiss = { showTemplateSource = false },
            onConfirm = { viewModel.importFromTemplate(it) },
        )
    }
    if (showRename && template != null) {
        TemplateEditDialog(
            title = "编辑模板",
            initialName = template.name,
            initialDescription = template.description,
            initialColor = template.colorHex,
            onDismiss = { showRename = false },
            onSave = { name, desc, color ->
                showRename = false
                viewModel.rename(name, desc, color)
            },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "删除模板",
            message = "确定要删除「${template?.name ?: ""}」吗？模板中的 ${template?.items?.size ?: 0} 条记录会一并删除，物品库与计划不受影响。",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.delete()
            },
        )
    }
}

@Composable
private fun HeaderCard(
    name: String,
    description: String,
    colorHex: String,
    itemCount: Int,
    categoryCount: Int,
    lastUsed: String,
) {
    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(colorFromHex(colorHex)),
                contentAlignment = Alignment.Center,
            ) {
                GlyphIcon(Glyph.Checklist, Color.White, Modifier.size(24.dp), 1.8f)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                if (description.isNotBlank()) {
                    Text(description, fontSize = 12.sp, color = InkSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                Text("$itemCount 件物品 · $categoryCount 个分类", fontSize = 11.sp, color = InkSecondary)
            }
            Text(lastUsed, fontSize = 10.sp, color = InkSecondary)
        }
    }
}

@Composable
private fun TemplateItemRow(item: TemplateItem, onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(SkyBlue))
        Text(
            item.name,
            fontSize = 14.sp,
            color = InkPrimary,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "移除", tint = Color(0xFFC2D1DD), modifier = Modifier.size(16.dp))
        }
    }
}
