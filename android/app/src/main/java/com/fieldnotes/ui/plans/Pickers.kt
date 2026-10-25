package com.fieldnotes.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fieldnotes.domain.Category
import com.fieldnotes.domain.Item
import com.fieldnotes.domain.Template
import com.fieldnotes.domain.TextRules
import com.fieldnotes.ui.components.CheckBadge
import com.fieldnotes.ui.components.PillChip
import com.fieldnotes.ui.components.SearchField
import com.fieldnotes.ui.components.SheetHeader
import com.fieldnotes.ui.components.SmallDivider
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.InkTertiary
import com.fieldnotes.ui.theme.PlaceholderGray
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueLight
import com.fieldnotes.ui.theme.SkyBlueDeep
import com.fieldnotes.ui.theme.colorFromHex

data class SourceOption(
    val id: Long,
    val title: String,
    val subtitle: String,
    val itemCount: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemPickerSheet(
    items: List<Item>,
    categories: List<Category>,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    val categoryOrder = categories.mapIndexed { index, category -> category.name to index }.toMap()
    val categoryNames = categories.map { it.name }.filter { name ->
        items.any { it.categoryName == name }
    }.toMutableList()
    if (items.any { it.categoryName.isBlank() }) categoryNames += "未分类"
    val sortedItems = items.sortedWith(
        compareBy<Item> { categoryOrder[it.categoryName] ?: Int.MAX_VALUE - 1 }
            .thenBy { it.name }
    )
    val filtered = sortedItems.filter { item ->
        (selectedCategory == null || item.categoryName.ifBlank { "未分类" } == selectedCategory) &&
            (query.isBlank() || item.name.contains(query, true) || item.note.contains(query, true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFD8E3EC))) },
    ) {
        Column(Modifier.fillMaxHeight(0.88f).imePadding()) {
            SheetHeader(
                title = "添加物品",
                subtitle = "可多选；已在清单中的物品会自动跳过",
                onClose = onDismiss,
            )
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "搜索物品…",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PillChip("全部", selectedCategory == null, { selectedCategory = null })
                categoryNames.forEach { category ->
                    PillChip(category, selectedCategory == category, { selectedCategory = category })
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(filtered, key = { it.id }) { item ->
                    val existing = TextRules.key(item.name) in existingKeys
                    val isSelected = item.id in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(if (existing) Modifier.alpha(0.45f) else Modifier)
                            .clickable(enabled = !existing) {
                                selected = if (isSelected) selected - item.id else selected + item.id
                            }
                            .background(if (isSelected) Color(0xFFF2FAFF) else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = InkPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.categoryName.ifBlank { "未分类" },
                                fontSize = 11.sp,
                                color = InkSecondary,
                            )
                        }
                        if (existing) {
                            Text("已在清单", fontSize = 11.sp, color = PlaceholderGray)
                        } else {
                            CheckBadge(isSelected)
                        }
                    }
                    SmallDivider()
                }
            }
            Button(
                onClick = {
                    onConfirm(selected.toList())
                    onDismiss()
                },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
            ) {
                Text("添加 ${selected.size} 件", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePickerSheet(
    templates: List<Template>,
    existingKeys: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedId by remember { mutableStateOf<Long?>(templates.firstOrNull()?.id) }
    val selected = templates.firstOrNull { it.id == selectedId }
    val duplicateCount = selected?.items?.count { TextRules.key(it.name) in existingKeys } ?: 0
    val addCount = (selected?.items?.size ?: 0) - duplicateCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(Modifier.fillMaxHeight(0.84f).imePadding()) {
            SheetHeader(
                title = "导入模板",
                subtitle = "选一份模板，一次性把物品加入本次清单",
                onClose = onDismiss,
            )
            if (templates.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("还没有模板，先到「模板」页创建一个", color = InkSecondary)
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    items(templates, key = { it.id }) { template ->
                        val isSelected = template.id == selectedId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (isSelected) Color(0xFFF1FAFF) else Color.White)
                                .clickable { selectedId = template.id }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(colorFromHex(template.colorHex)),
                                contentAlignment = Alignment.Center,
                            ) {
                                com.fieldnotes.ui.components.GlyphIcon(
                                    com.fieldnotes.ui.components.Glyph.Checklist,
                                    Color.White,
                                    Modifier.size(22.dp),
                                    1.8f,
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(template.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                                Text(
                                    "${template.items.size} 件 · " +
                                        template.items.groupingBy { it.categoryName.ifBlank { "未分类" } }
                                            .eachCount().entries.take(3)
                                            .joinToString(" · ") { "${it.key} ${it.value}" },
                                    fontSize = 11.sp,
                                    color = InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (isSelected) {
                                Box(
                                    Modifier.size(22.dp).clip(CircleShape).background(SkyBlueDeep),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                selected?.let { template ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFF7FBFE))
                            .padding(12.dp),
                    ) {
                        Row {
                            Text("模板内容（${template.items.size} 件）", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                            Spacer(Modifier.weight(1f))
                            Text(
                                if (duplicateCount > 0) "将跳过 $duplicateCount 件已有" else "全部可导入",
                                fontSize = 11.sp,
                                color = InkSecondary,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            template.items.take(9).joinToString(" · ") { it.name } +
                                if (template.items.size > 9) " …" else "",
                            fontSize = 11.sp,
                            color = InkTertiary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Button(
                onClick = {
                    selectedId?.let(onConfirm)
                    onDismiss()
                },
                enabled = selected != null && addCount > 0,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
            ) {
                Text(
                    if (addCount > 0) "导入 $addCount 件" else "没有可导入的物品",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcePickerSheet(
    title: String,
    subtitle: String,
    options: List<SourceOption>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedId by remember { mutableStateOf<Long?>(options.firstOrNull()?.id) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(Modifier.fillMaxHeight(0.76f).imePadding()) {
            SheetHeader(title = title, subtitle = subtitle, onClose = onDismiss)
            if (options.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("暂无可选来源", color = InkSecondary)
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    items(options, key = { it.id }) { option ->
                        val isSelected = option.id == selectedId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(15.dp))
                                .background(if (isSelected) Color(0xFFF1FAFF) else Color.White)
                                .clickable { selectedId = option.id }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(option.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = InkPrimary)
                                Text(option.subtitle, fontSize = 11.sp, color = InkSecondary)
                            }
                            Text("${option.itemCount} 件", fontSize = 11.sp, color = InkSecondary)
                            Spacer(Modifier.width(10.dp))
                            CheckBadge(isSelected)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Button(
                onClick = {
                    selectedId?.let(onConfirm)
                    onDismiss()
                },
                enabled = selectedId != null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
            ) {
                Text("导入", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
