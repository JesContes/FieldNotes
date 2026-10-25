package com.fieldnotes.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldnotes.FieldNotesApp
import com.fieldnotes.domain.PlanFilter
import com.fieldnotes.ui.components.BottomNav
import com.fieldnotes.ui.components.EmptyState
import com.fieldnotes.ui.components.Glyph
import com.fieldnotes.ui.components.PageHeader
import com.fieldnotes.ui.components.PillChip
import com.fieldnotes.ui.components.PlanCardView
import com.fieldnotes.ui.components.SearchField
import com.fieldnotes.ui.theme.InkPrimary
import com.fieldnotes.ui.theme.InkSecondary
import com.fieldnotes.ui.theme.PageBackground
import com.fieldnotes.ui.theme.SkyBlue

@Composable
fun PlanListScreen(
    onOpenPlan: (Long) -> Unit,
    onCreatePlan: () -> Unit,
    onTab: (String) -> Unit,
) {
    val app = LocalContext.current.applicationContext as FieldNotesApp
    val viewModel: PlansViewModel = viewModel(factory = PlansViewModel.factory(app.container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = PageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { BottomNav(active = "plans", onSelect = onTab) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreatePlan,
                containerColor = SkyBlue,
                contentColor = Color.White,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建计划", modifier = Modifier.size(28.dp))
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(PageBackground)
                .padding(padding),
        ) {
            PageHeader(
                title = "寸笺",
                subtitle = "一纸清单，装下整段旅程",
                trailing = {
                    IconButton(onClick = {
                        if (searchOpen) viewModel.setQuery("")
                        searchOpen = !searchOpen
                    }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索", tint = InkSecondary)
                    }
                },
            )

            if (searchOpen) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "搜索目的地或计划名",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(4.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("全部计划", style = MaterialTheme.typography.titleSmall, color = InkPrimary)
                Spacer(Modifier.weight(1f))
                Text(
                    "待收 ${state.pendingTotal} 件 · 共 ${state.totalPlans} 个计划",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary,
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlanFilter.entries.forEach { filter ->
                    PillChip(
                        text = if (state.counts[filter] != null) "${filter.label} ${state.counts[filter]}" else filter.label,
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SkyBlue)
                }
                state.plans.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    if (state.totalPlans == 0) {
                        EmptyState(
                            glyph = Glyph.Suitcase,
                            title = "还没有出行计划",
                            subtitle = "建一份计划，把要带的东西一件件收进来",
                            primaryLabel = "新建计划",
                            onPrimary = onCreatePlan,
                        )
                    } else {
                        EmptyState(
                            glyph = Glyph.Checklist,
                            title = "没有匹配的计划",
                            subtitle = "换个筛选条件，或查看全部计划",
                            primaryLabel = "查看全部",
                            onPrimary = {
                                viewModel.setFilter(PlanFilter.ALL)
                                viewModel.setQuery("")
                            },
                        )
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.plans, key = { it.id }) { card ->
                        PlanCardView(card = card, onClick = { onOpenPlan(card.id) })
                    }
                }
            }
        }
    }
}
