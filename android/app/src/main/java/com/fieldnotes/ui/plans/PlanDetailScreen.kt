package com.fieldnotes.ui.plans

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldnotes.FieldNotesApp
import com.fieldnotes.domain.Plan
import com.fieldnotes.domain.PlanItem
import com.fieldnotes.domain.TextRules
import com.fieldnotes.domain.formatDate
import com.fieldnotes.domain.label
import com.fieldnotes.ui.components.ChecklistRow
import com.fieldnotes.ui.components.ConfirmDialog
import com.fieldnotes.ui.components.DetailTopBar
import com.fieldnotes.ui.components.Glyph
import com.fieldnotes.ui.components.GlyphIcon
import com.fieldnotes.ui.components.MoreButton
import com.fieldnotes.ui.components.ProgressLine
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.InkTertiary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueDeep
import com.fieldnotes.ui.theme.colorFromHex

@Composable
fun PlanDetailScreen(
    planId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenTemplate: (Long) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: PlanDetailViewModel = viewModel(factory = PlanDetailViewModel.factory(app.container, planId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allTemplates by viewModel.allTemplates.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }
    var showItemPicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlanDetailEvent.Message -> snackbar.showSnackbar(event.text)
                is PlanDetailEvent.Removed -> {
                    val result = snackbar.showSnackbar(
                        message = "已移除 ${event.name}",
                        actionLabel = "撤销",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoRemove()
                    else viewModel.clearPendingRemoved()
                }
                is PlanDetailEvent.SavedAsTemplate -> {
                    snackbar.showSnackbar("已存为模板")
                    onOpenTemplate(event.templateId)
                }
                PlanDetailEvent.Deleted -> {
                    onBack()
                }
            }
        }
    }

    val plan = state.plan

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            DetailTopBar(
                title = plan?.displayTitle ?: "计划详情",
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
                                text = { Text("存为模板") },
                                onClick = {
                                    menuOpen = false
                                    templateName = (plan?.displayTitle ?: "") + " 模板"
                                    showSaveTemplate = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("全部勾选") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.setAll(true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("全部取消") },
                                onClick = {
                                    menuOpen = false
                                    viewModel.setAll(false)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("编辑计划") },
                                onClick = {
                                    menuOpen = false
                                    onEdit(planId)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除计划", color = DangerRed) },
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
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { showTemplatePicker = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("导入模板", color = SkyBlueDeep, fontWeight = FontWeight.Bold)
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
            plan == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("计划已不存在", color = InkPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onBack) { Text("返回", color = SkyBlueDeep) }
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HeroCard(plan) }
                item { ProgressCard(plan, onCheckAll = { viewModel.setAll(true) }) }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(4.dp, 20.dp).clip(RoundedCornerShape(2.dp)).background(SkyBlue))
                        Text(
                            "出行清单",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        Text("按分类分组", fontSize = 11.sp, color = InkSecondary)
                    }
                }
                if (state.groups.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("清单还是空的", fontWeight = FontWeight.Bold, color = InkPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text("添加物品，或从模板一次性导入", fontSize = 12.sp, color = InkSecondary)
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
                                        Text(
                                            group.categoryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = InkPrimary,
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            "${group.checked}/${group.total}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = InkSecondary,
                                        )
                                    }
                                    group.items.forEachIndexed { index, item ->
                                        ChecklistRow(
                                            name = item.name,
                                            checked = item.checked,
                                            onToggle = { viewModel.toggle(item) },
                                            onRemove = { viewModel.remove(item) },
                                        )
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

    if (showItemPicker && plan != null) {
        ItemPickerSheet(
            items = allItems,
            categories = allCategories,
            existingKeys = plan.items.map { TextRules.key(it.name) }.toSet(),
            onDismiss = { showItemPicker = false },
            onConfirm = { viewModel.addItems(it) },
        )
    }
    if (showTemplatePicker) {
        TemplatePickerSheet(
            templates = allTemplates,
            existingKeys = plan?.items?.map { TextRules.key(it.name) }?.toSet() ?: emptySet(),
            onDismiss = { showTemplatePicker = false },
            onConfirm = { viewModel.importTemplate(it) },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "删除计划",
            message = "确定要删除「${plan?.displayTitle ?: ""}」吗？计划中的 ${plan?.totalCount ?: 0} 条清单记录会一并删除，模板与物品库不受影响。",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                viewModel.deletePlan()
            },
        )
    }
    if (showSaveTemplate) {
        AlertDialog(
            onDismissRequest = { showSaveTemplate = false },
            title = { Text("存为模板") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    singleLine = true,
                    label = { Text("模板名称") },
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveTemplate = false
                        viewModel.saveAsTemplate(templateName)
                    },
                    enabled = templateName.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplate = false }) { Text("取消") }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
        )
    }
}

@Composable
private fun HeroCard(plan: Plan) {
    val color = colorFromHex(plan.colorHex)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(color)
            .padding(20.dp),
    ) {
        GlyphIcon(
            Glyph.Suitcase,
            Color.White.copy(alpha = 0.28f),
            Modifier.size(112.dp).align(Alignment.CenterEnd),
            2.2f,
        )
        Column {
            Text(
                plan.destination,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (plan.title.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    plan.title,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                formatDate(plan.start) + " – " + formatDate(plan.end),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                val status = com.fieldnotes.domain.PlanStatus.of(plan.start, plan.end, java.time.LocalDate.now())
                Text(
                    status.label(plan.start, plan.end, java.time.LocalDate.now()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ProgressCard(plan: Plan, onCheckAll: () -> Unit) {
    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "已收 ${plan.checkedCount} / ${plan.totalCount} 件",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = InkPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (plan.hasUnchecked) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFECEC))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "未收 ${plan.totalCount - plan.checkedCount} 件",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                }
                TextButton(onClick = onCheckAll) { Text("全部勾选", fontSize = 11.sp, color = SkyBlueDeep) }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressLine(plan.progress, Modifier.weight(1f), height = 6)
                Spacer(Modifier.size(10.dp))
                Text(
                    "${(plan.progress * 100).roundToInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkSecondary,
                )
            }
        }
    }
}
