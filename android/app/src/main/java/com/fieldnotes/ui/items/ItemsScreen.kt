package com.fieldnotes.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldnotes.FieldNotesApp
import com.fieldnotes.domain.Category
import com.fieldnotes.domain.Item
import com.fieldnotes.domain.TextRules
import com.fieldnotes.ui.components.ColorRow
import com.fieldnotes.ui.components.ConfirmDialog
import com.fieldnotes.ui.components.DetailTopBar
import com.fieldnotes.ui.components.DropdownField
import com.fieldnotes.ui.components.EmptyState
import com.fieldnotes.ui.components.Glyph
import com.fieldnotes.ui.components.GlyphIcon
import com.fieldnotes.ui.components.PageHeader
import com.fieldnotes.ui.components.PillChip
import com.fieldnotes.ui.components.SearchField
import com.fieldnotes.ui.components.SheetHeader
import com.fieldnotes.ui.components.SmallDivider
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueDeep
import com.fieldnotes.ui.theme.SkyBlueLight
import com.fieldnotes.ui.theme.WarningBg
import com.fieldnotes.ui.theme.colorFromHex
import com.fieldnotes.ui.theme.InkTertiary

@Composable
fun ItemsScreen(
    onOpenCategories: () -> Unit,
    onTab: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.factory(app.container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editItem by remember { mutableStateOf<Item?>(null) }
    var showNew by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { com.fieldnotes.ui.components.BottomNav(active = "items", onSelect = onTab) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }, containerColor = SkyBlue, contentColor = Color.White) {
                Icon(Icons.Filled.Add, contentDescription = "新建物品", modifier = Modifier.size(28.dp))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().background(PageBackground).padding(padding)) {
            PageHeader(
                title = "物品",
                subtitle = "所有可能带出门的东西",
                trailing = {
                    TextButton(onClick = onOpenCategories) {
                        Text("分类管理", fontSize = 12.sp, color = SkyBlueDeep, fontWeight = FontWeight.Bold)
                    }
                },
            )
            SearchField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = "搜索物品…",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillChip("全部", state.selectedCategoryId == null, { viewModel.setCategory(null) })
                state.categories.forEach { category ->
                    PillChip(category.name, state.selectedCategoryId == category.id, { viewModel.setCategory(category.id) })
                }
                if (state.uncategorizedCount > 0) {
                    PillChip("未分类", state.selectedCategoryId == -1L, { viewModel.setCategory(-1L) })
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SkyBlue)
                }
                state.groups.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        glyph = Glyph.Box,
                        title = if (state.totalItems == 0) "物品库还是空的" else "没有匹配的物品",
                        subtitle = "把牙刷、短袖、充电器这些都收进来，建清单时直接挑",
                        primaryLabel = "新建物品",
                        onPrimary = { showNew = true },
                    )
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header-${group.categoryName}") {
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)) {
                                Text(group.categoryName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                                Spacer(Modifier.weight(1f))
                                Text("${group.items.size} 件", fontSize = 11.sp, color = InkSecondary)
                            }
                        }
                        item(key = "group-${group.categoryName}") {
                            Card(
                                shape = RoundedCornerShape(15.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            ) {
                                Column {
                                    group.items.forEachIndexed { index, item ->
                                        Row(
                                            Modifier.fillMaxWidth().height(60.dp)
                                                .clickable { editItem = item }
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                Modifier.size(10.dp).clip(CircleShape)
                                                    .background(colorFromHex(group.colorHex)),
                                            )
                                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                                Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkPrimary)
                                                if (item.note.isNotBlank()) {
                                                    Text(
                                                        item.note,
                                                        fontSize = 11.sp,
                                                        color = InkSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            Text("›", fontSize = 20.sp, color = Color(0xFFC2D1DD))
                                        }
                                        if (index != group.items.lastIndex) SmallDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNew) {
        ItemEditSheet(
            item = null,
            categories = state.categories,
            onDismiss = { showNew = false },
            onSave = { name, categoryId, note ->
                showNew = false
                viewModel.createItem(name, categoryId, note)
            },
            onDelete = null,
        )
    }
    editItem?.let { item ->
        ItemEditSheet(
            item = item,
            categories = state.categories,
            onDismiss = { editItem = null },
            onSave = { name, categoryId, note ->
                editItem = null
                viewModel.updateItem(item.id, name, categoryId, note)
            },
            onDelete = {
                editItem = null
                viewModel.deleteItem(item.id)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditSheet(
    item: Item?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, Long?, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(item?.id) { mutableStateOf(item?.name ?: "") }
    var note by remember(item?.id) { mutableStateOf(item?.note ?: "") }
    var categoryId by remember(item?.id) { mutableStateOf(item?.categoryId) }
    var showDelete by remember { mutableStateOf(false) }
    val selectedCategoryName = categories.firstOrNull { it.id == categoryId }?.name ?: "未分类"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(Modifier.fillMaxHeight(0.72f).imePadding().padding(horizontal = 16.dp)) {
            SheetHeader(title = if (item == null) "新建物品" else "编辑物品", onClose = onDismiss)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("物品名称") },
                placeholder = { Text("例如：牙刷", color = InkSecondary) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("item_name"),
            )
            Spacer(Modifier.height(10.dp))
            Text("分类", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkTertiary,
                modifier = Modifier.padding(bottom = 4.dp))
            DropdownField(
                value = selectedCategoryName,
                options = categories.map { it.id to it.name } + (null to "未分类"),
                placeholder = "选择分类（可空）",
                onSelect = { categoryId = it },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（选填）") },
                placeholder = { Text("软毛、旅行装、100g 以内…", color = InkSecondary) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(name, categoryId, note) },
                enabled = TextRules.isValidName(name, TextRules.MAX_ITEM_NAME),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
            ) {
                Text(if (item == null) "添加物品" else "保存修改", fontWeight = FontWeight.Bold)
            }
            if (onDelete != null) {
                TextButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("删除物品", color = DangerRed, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (showDelete && item != null) {
        ConfirmDialog(
            title = "删除物品",
            message = "确定要删除「${item.name}」吗？已创建的计划与模板中的同名条目会保留。",
            onDismiss = { showDelete = false },
            onConfirm = {
                showDelete = false
                onDelete?.invoke()
            },
        )
    }
}

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.factory(app.container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCreate by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<CategoryRowUi?>(null) }
    var deleteTarget by remember { mutableStateOf<CategoryRowUi?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            DetailTopBar(
                title = "物品分类",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "新建分类", tint = SkyBlueDeep)
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(15.dp))
                    .background(WarningBg).padding(14.dp),
            ) {
                Column {
                    Text("删除分类后，其中的物品会归入「未分类」，不会丢失。", fontSize = 12.sp, color = Color(0xFF9A6A1F))
                    Spacer(Modifier.height(4.dp))
                    Text("首次启动预置 6 个分类，可随时改名或换色。", fontSize = 11.sp, color = Color(0xFFB98B45))
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column {
                            state.rows.forEachIndexed { index, row ->
                                Row(
                                    Modifier.fillMaxWidth().height(64.dp)
                                        .clickable { editTarget = row }
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(18.dp).clip(CircleShape).background(colorFromHex(row.colorHex)))
                                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                        Text(row.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                                        Text("${row.itemCount} 件物品", fontSize = 11.sp, color = InkSecondary)
                                    }
                                    Text("›", fontSize = 20.sp, color = Color(0xFFC2D1DD))
                                }
                                if (index != state.rows.lastIndex) SmallDivider()
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFE8EEF3)))
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("未分类", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                                Text("${state.uncategorizedCount} 件物品 · 删除分类后自动归入", fontSize = 11.sp, color = InkSecondary)
                            }
                            Text("系统保留", fontSize = 11.sp, color = InkSecondary)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(SkyBlueLight).padding(14.dp),
                    ) {
                        Column {
                            Text("分类怎么用", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBlueDeep)
                            Text("用于物品筛选与清单分组，不计入清单件数。", fontSize = 11.sp, color = InkTertiary)
                            Text("改名不改写历史计划；删除后物品归入「未分类」。", fontSize = 11.sp, color = InkTertiary)
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { showCreate = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("新建分类", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCreate) {
        CategoryEditDialog(
            title = "新建分类",
            initialName = "",
            initialColor = "#66CCFF",
            onDismiss = { showCreate = false },
            onSave = { name, color ->
                showCreate = false
                viewModel.create(name, color)
            },
            onDelete = null,
        )
    }
    editTarget?.let { target ->
        CategoryEditDialog(
            title = "编辑分类",
            initialName = target.name,
            initialColor = target.colorHex,
            onDismiss = { editTarget = null },
            onSave = { name, color ->
                editTarget = null
                viewModel.update(target.id, name, color)
            },
            onDelete = {
                editTarget = null
                deleteTarget = target
            },
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除分类",
            message = "该分类下有 ${target.itemCount} 件物品。删除后这些物品会归入「未分类」，不会丢失。",
            onDismiss = { deleteTarget = null },
            onConfirm = {
                viewModel.delete(target.id)
                deleteTarget = null
            },
        )
    }
}

@Composable
private fun CategoryEditDialog(
    title: String,
    initialName: String,
    initialColor: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initialName) }
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
                    label = { Text("分类名称") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Text("颜色", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkSecondary)
                Spacer(Modifier.height(8.dp))
                ColorRow(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, color) },
                enabled = TextRules.isValidName(name, TextRules.MAX_CATEGORY_NAME),
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除", color = DangerRed) }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}
