package com.fieldnotes.ui.plans

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import com.fieldnotes.domain.TextRules
import com.fieldnotes.domain.formatDate
import com.fieldnotes.domain.formatRange
import com.fieldnotes.ui.components.ColorRow
import com.fieldnotes.ui.components.DetailTopBar
import com.fieldnotes.ui.theme.DangerRed
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.SkyBlue
import com.fieldnotes.ui.theme.SkyBlueDeep
import com.fieldnotes.ui.theme.SkyBlueLight
import com.fieldnotes.ui.theme.InkTertiary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun PlanEditScreen(
    planId: Long?,
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: PlanEditViewModel = viewModel(factory = PlanEditViewModel.factory(app.container, planId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PlanEditEvent.Saved -> onSaved(event.planId, event.isNew)
                is PlanEditEvent.Message -> snackbar.showSnackbar(event.text)
                PlanEditEvent.LoadFailed -> {
                    snackbar.showSnackbar("计划已不存在")
                    onBack()
                }
            }
        }
    }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            DetailTopBar(
                title = if (planId == null) "新建计划" else "编辑计划",
                onBack = onBack,
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = Color.White),
                ) {
                    Text(
                        if (planId == null) "创建计划" else "保存修改",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SkyBlue)
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FieldLabel("目的地", "必填")
            OutlinedTextField(
                value = state.destination,
                onValueChange = viewModel::setDestination,
                singleLine = true,
                placeholder = { Text("例如：东京", color = InkSecondary) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )

            Spacer(Modifier.height(8.dp))
            FieldLabel("计划名称", "选填")
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                singleLine = true,
                placeholder = { Text("例如：东京五日游", color = InkSecondary) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
            )

            Spacer(Modifier.height(8.dp))
            FieldLabel("出行时间", if (state.end >= state.start) "共 ${state.end.toEpochDay() - state.start.toEpochDay() + 1} 天" else "")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { showDatePicker = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = SkyBlueDeep, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        if (state.start.year == state.end.year)
                            "${state.start}  →  ${state.end}"
                        else
                            "${state.start}  →  ${state.end}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary,
                    )
                    Text(formatRange(state.start, state.end), fontSize = 11.sp, color = InkSecondary)
                }
                Text("修改", fontSize = 12.sp, color = SkyBlueDeep, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            FieldLabel("备注", "选填 · 200 字内")
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                minLines = 3,
                maxLines = 4,
                placeholder = { Text("行程安排、住宿、其他提醒…", color = InkSecondary) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors(),
                supportingText = {
                    Text(
                        "${TextRules.codePointLength(state.note)}/${TextRules.MAX_PLAN_NOTE}",
                        color = InkSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                },
            )

            Spacer(Modifier.height(8.dp))
            FieldLabel("主题色", "")
            ColorRow(selected = state.colorHex, onSelect = viewModel::setColor)

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(SkyBlueLight)
                    .padding(14.dp),
            ) {
                Column {
                    Text("创建后可以", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyBlueDeep)
                    Spacer(Modifier.height(4.dp))
                    Text("· 直接添加物品，或从模板一键导入", fontSize = 11.sp, color = InkTertiary)
                    Text("· 逐项勾选打包，未勾选会在计划卡片显示红点", fontSize = 11.sp, color = InkTertiary)
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = DangerRed, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        DateRangeDialog(
            initialStart = state.start,
            initialEnd = state.end,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.setDates(start, end)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun FieldLabel(title: String, hint: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InkTertiary)
        Spacer(Modifier.weight(1f))
        if (hint.isNotBlank()) Text(hint, fontSize = 11.sp, color = InkSecondary)
    }
}

@Composable
private fun fieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = SkyBlue,
    unfocusedBorderColor = Color(0xFFE5EDF3),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStart.toUtcMillis(),
        initialSelectedEndDateMillis = initialEnd.toUtcMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(start.toUtcDate(), end.toUtcDate())
                    }
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = Color.White),
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.height(470.dp),
            showModeToggle = false,
            title = {
                Text(
                    "出行时间",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary,
                )
            },
            headline = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis
                Text(
                    text = if (start != null && end != null) {
                        "${formatDate(start.toUtcDate())}  –  ${formatDate(end.toUtcDate())}"
                    } else {
                        "选择出发与返程日期"
                    },
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary,
                )
            },
        )
    }
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
