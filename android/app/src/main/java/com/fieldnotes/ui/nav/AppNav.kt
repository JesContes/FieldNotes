package com.fieldnotes.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.navArgument
import com.fieldnotes.ui.items.CategoriesScreen
import com.fieldnotes.ui.items.ItemsScreen
import com.fieldnotes.ui.plans.PlanDetailScreen
import com.fieldnotes.ui.plans.PlanEditScreen
import com.fieldnotes.ui.plans.PlanListScreen
import com.fieldnotes.ui.settings.SettingsScreen
import com.fieldnotes.ui.templates.TemplateDetailScreen
import com.fieldnotes.ui.templates.TemplateListScreen

@Composable
fun AppNav() {
    val navController = rememberNavController()

    fun switchTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = "plans") {
        composable("plans") {
            PlanListScreen(
                onOpenPlan = { navController.navigate("plan/$it") },
                onCreatePlan = { navController.navigate("planEdit") },
                onTab = ::switchTab,
            )
        }
        composable(
            route = "plan/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType }),
        ) { entry ->
            val planId = entry.arguments?.getLong("planId") ?: return@composable
            PlanDetailScreen(
                planId = planId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("planEdit?planId=$it") },
                onOpenTemplate = { navController.navigate("template/$it") },
            )
        }
        composable(
            route = "planEdit?planId={planId}",
            arguments = listOf(navArgument("planId") {
                type = NavType.LongType
                defaultValue = -1L
            }),
        ) { entry ->
            val planId = entry.arguments?.getLong("planId")?.takeIf { it > 0 }
            PlanEditScreen(
                planId = planId,
                onBack = { navController.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        navController.navigate("plan/$id") {
                            popUpTo("plans")
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable("templates") {
            TemplateListScreen(
                onOpenTemplate = { navController.navigate("template/$it") },
                onTab = ::switchTab,
            )
        }
        composable(
            route = "template/{templateId}",
            arguments = listOf(navArgument("templateId") { type = NavType.LongType }),
        ) { entry ->
            val templateId = entry.arguments?.getLong("templateId") ?: return@composable
            TemplateDetailScreen(
                templateId = templateId,
                onBack = { navController.popBackStack() },
                onOpenTemplate = { navController.navigate("template/$it") },
            )
        }
        composable("items") {
            ItemsScreen(
                onOpenCategories = { navController.navigate("categories") },
                onTab = ::switchTab,
            )
        }
        composable("categories") {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onTab = ::switchTab)
        }
    }
}
