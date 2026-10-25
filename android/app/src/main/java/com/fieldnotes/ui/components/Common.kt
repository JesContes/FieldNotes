package com.fieldnotes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.fieldnotes.domain.PlanCardUi
import com.fieldnotes.domain.PlanStatus
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.DividerGray
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.InkTertiary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.PlaceholderGray
import com.fieldnotes.ui.theme.PrimaryInk
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueDeep
import com.fieldnotes.ui.theme.SkyBlueLight
import com.fieldnotes.ui.theme.SuccessBg
import com.fieldnotes.ui.theme.SuccessGreen
import com.fieldnotes.ui.theme.colorFromHex
import com.fieldnotes.ui.theme.Palette

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = InkPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
        trailing?.invoke()
    }
}

@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Text("‹", fontSize = 32.sp, color = InkPrimary, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        actions?.invoke()
    }
}

@Composable
fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(SkyBlue))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = InkPrimary,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
        }
    }
}

@Composable
fun StatusPill(status: PlanStatus, text: String) {
    val bg = when (status) {
        PlanStatus.ONGOING -> SuccessBg
        PlanStatus.UPCOMING -> SkyBlueLight
        PlanStatus.PAST -> Color(0xFFF0F3F6)
    }
    val fg = when (status) {
        PlanStatus.ONGOING -> SuccessGreen
        PlanStatus.UPCOMING -> PrimaryInk
        PlanStatus.PAST -> InkSecondary
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
fun RedDot(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).clip(CircleShape).background(DangerRed))
}

@Composable
fun ProgressLine(progress: Float, modifier: Modifier = Modifier, height: Int = 4) {
    Box(
        modifier
            .height(height.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE6EEF4)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height.dp)
                .clip(RoundedCornerShape(50))
                .background(SkyBlue),
        )
    }
}

@Composable
fun PlanCardView(card: PlanCardUi, onClick: () -> Unit) {
    val planColor = colorFromHex(card.colorHex)
    val redDotState = when {
        card.total == 0 -> "清单为空"
        card.hasUnchecked -> "有 ${card.total - card.checked} 件物品未勾选"
        else -> "已全部打包"
    }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = redDotState },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(planColor),
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(Glyph.Suitcase, Color.White, Modifier.size(25.dp), 1.9f)
                }
                Column(
                    Modifier.weight(1f).padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            card.destination,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (card.hasUnchecked) {
                            Spacer(Modifier.width(8.dp))
                            RedDot()
                        }
                    }
                    if (card.title.isNotBlank()) {
                        Text(
                            card.title,
                            fontSize = 12.sp,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                StatusPill(card.status, card.statusText)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    card.dateRange,
                    fontSize = 12.sp,
                    color = InkTertiary,
                    modifier = Modifier.weight(1f).padding(start = 5.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${card.checked}/${card.total} 已收",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkSecondary,
                )
            }

            Spacer(Modifier.height(10.dp))
            ProgressLine(card.progress, Modifier.fillMaxWidth())

            Spacer(Modifier.height(9.dp))
            Text(
                card.preview,
                fontSize = 11.sp,
                color = PlaceholderGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EmptyState(
    glyph: Glyph,
    title: String,
    subtitle: String,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(SkyBlueLight),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(glyph, SkyBlueDeep, Modifier.size(40.dp), 1.7f)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        if (primaryLabel != null && onPrimary != null) {
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = onPrimary, shape = RoundedCornerShape(16.dp)) {
                Text(primaryLabel, color = PrimaryInk)
            }
        }
    }
}

@Composable
fun CheckBadge(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(SkyBlue)
                else Modifier.border(1.5.dp, Color(0xFFC9D6E0), CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun ChecklistRow(
    name: String,
    checked: Boolean,
    onToggle: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { stateDescription = if (checked) "已收" else "未收" }
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckBadge(checked)
        Text(
            text = name,
            fontSize = 15.sp,
            fontWeight = if (checked) FontWeight.Normal else FontWeight.Medium,
            color = if (checked) PlaceholderGray else InkPrimary,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp),
        )
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "移除",
                    tint = Color(0xFFC2D1DD),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "删除",
    destructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = InkPrimary) },
        text = { Text(message, color = InkSecondary, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) DangerRed else PrimaryInk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSecondary) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
    )
}

@Composable
fun ColorRow(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Palette.forEach { color ->
            val hex = "#%06X".format(color.toArgb() and 0xFFFFFF)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (hex.equals(selected, ignoreCase = true)) 2.dp else 0.dp,
                        color = if (hex.equals(selected, ignoreCase = true)) InkPrimary else Color.Transparent,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center,
            ) {
                if (hex.equals(selected, ignoreCase = true)) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SheetHeader(title: String, subtitle: String? = null, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = InkPrimary)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "关闭", tint = InkSecondary)
        }
    }
}

@Composable
fun BottomNav(
    active: String,
    onSelect: (String) -> Unit,
) {
    val items = listOf(
        Triple("plans", "出行", Glyph.Suitcase),
        Triple("templates", "模板", Glyph.Checklist),
        Triple("items", "物品", Glyph.Box),
        Triple("settings", "我的", Glyph.Person),
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        items.forEach { (route, label, glyph) ->
            val selected = route == active
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(route) },
                icon = {
                    GlyphIcon(
                        glyph,
                        if (selected) PrimaryInk else Color(0xFFA9B9C6),
                        Modifier.size(25.dp),
                        1.8f,
                    )
                },
                label = {
                    Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryInk,
                    selectedTextColor = PrimaryInk,
                    unselectedIconColor = Color(0xFFA9B9C6),
                    unselectedTextColor = Color(0xFFA9B9C6),
                    indicatorColor = SkyBlueLight,
                ),
            )
        }
    }
}

@Composable
fun DropdownField(
    value: String,
    options: List<Pair<Long?, String>>,
    placeholder: String,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                value.ifBlank { placeholder },
                color = if (value.isBlank()) PlaceholderGray else InkPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = InkSecondary)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.88f).background(Color.White),
        ) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name, color = InkPrimary) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    },
                )
            }
        }
    }
}

@Composable
fun AddFab(onClick: () -> Unit, contentDescription: String = "新建") {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(SkyBlue)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun MoreButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Filled.MoreVert, contentDescription = "更多", tint = InkSecondary)
    }
}

@Composable
fun AppScreenBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().background(PageBackground)) {
        content()
    }
}

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) SkyBlue else Color.White)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else Color(0xFFD8E3EC),
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else InkTertiary,
        )
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "搜索",
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(placeholder, fontSize = 13.sp, color = PlaceholderGray) },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "清除",
                        tint = InkSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = SkyBlue,
            unfocusedBorderColor = DividerGray,
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

@Composable
fun SmallDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DividerGray))
}
