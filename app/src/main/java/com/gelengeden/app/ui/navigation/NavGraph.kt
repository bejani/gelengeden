package com.gelengeden.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gelengeden.app.data.AppearanceManager
import com.gelengeden.app.data.AutoBackupManager
import com.gelengeden.app.ui.screens.AboutScreen
import com.gelengeden.app.ui.screens.AddEditTransactionScreen
import com.gelengeden.app.ui.screens.BackupRestoreScreen
import com.gelengeden.app.ui.screens.BankSmsSettingsScreen
import com.gelengeden.app.ui.screens.CATEGORY_DETAIL_ALL_TIME_END
import com.gelengeden.app.ui.screens.CATEGORY_DETAIL_ALL_TIME_START
import com.gelengeden.app.ui.screens.CategoryDetailScreen
import com.gelengeden.app.ui.screens.HomeScreen
import com.gelengeden.app.ui.screens.ManageCategoriesScreen
import com.gelengeden.app.ui.screens.ReportsScreen
import com.gelengeden.app.ui.screens.QuickAddTemplatesScreen
import com.gelengeden.app.ui.screens.SettingsScreen
import com.gelengeden.app.data.TransactionType
import com.gelengeden.app.ui.viewmodel.AuthViewModel
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val QUICK_ADD = "quick_add/{templateId}"
    const val QUICK_ADD_TEMPLATES = "quick_add_templates"
    const val CATEGORIES = "categories"
    const val REPORTS = "reports"
    const val BACKUP = "backup"
    const val ABOUT = "about"
    const val SETTINGS = "settings"
    const val BANK_SMS_SETTINGS = "bank_sms_settings"
    const val CATEGORY_DETAIL = "category_detail/{category}/{type}/{start}/{end}"

    fun edit(id: Long) = "edit/$id"
    fun quickAdd(templateId: Long) = "quick_add/$templateId"

    fun categoryDetail(
        category: String,
        type: TransactionType,
        startMillis: Long,
        endMillis: Long
    ) = "category_detail/${Uri.encode(category)}/${type.name}/$startMillis/$endMillis"
}

@Composable
fun GelengedenNavGraph(
    navController: NavHostController,
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel,
    appearanceManager: AppearanceManager,
    autoBackupManager: AutoBackupManager
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate(Routes.ADD) },
                onTransactionClick = { id -> navController.navigate(Routes.edit(id)) },
                onManageCategoriesClick = { navController.navigate(Routes.CATEGORIES) },
                onReportsClick = { navController.navigate(Routes.REPORTS) },
                onBackupClick = { navController.navigate(Routes.BACKUP) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onAboutClick = { navController.navigate(Routes.ABOUT) },
                onQuickAddTemplatesClick = { navController.navigate(Routes.QUICK_ADD_TEMPLATES) },
                onQuickAddClick = { id -> navController.navigate(Routes.quickAdd(id)) }
            )
        }

        composable(Routes.ADD) {
            AddEditTransactionScreen(
                viewModel = viewModel,
                transactionId = null,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id")
            AddEditTransactionScreen(
                viewModel = viewModel,
                transactionId = id,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.QUICK_ADD,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { entry ->
            AddEditTransactionScreen(
                viewModel = viewModel,
                quickAddTemplateId = entry.arguments?.getLong("templateId"),
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.QUICK_ADD_TEMPLATES) {
            QuickAddTemplatesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onUseTemplate = { id -> navController.navigate(Routes.quickAdd(id)) }
            )
        }

        composable(Routes.CATEGORIES) {
            ManageCategoriesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCategoryClick = { category ->
                    navController.navigate(
                        Routes.categoryDetail(
                            category = category.name,
                            type = category.type,
                            startMillis = CATEGORY_DETAIL_ALL_TIME_START,
                            endMillis = CATEGORY_DETAIL_ALL_TIME_END
                        )
                    )
                }
            )
        }

        composable(Routes.REPORTS) {
            ReportsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCategoryClick = { category, startMillis, endMillis ->
                    navController.navigate(
                        Routes.categoryDetail(
                            category = category.name,
                            type = category.type,
                            startMillis = startMillis,
                            endMillis = endMillis
                        )
                    )
                }
            )
        }

        composable(
            route = Routes.CATEGORY_DETAIL,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
                navArgument("start") { type = NavType.LongType },
                navArgument("end") { type = NavType.LongType }
            )
        ) { entry ->
            val category = Uri.decode(entry.arguments?.getString("category").orEmpty())
            val type = runCatching {
                TransactionType.valueOf(entry.arguments?.getString("type").orEmpty())
            }.getOrDefault(TransactionType.EXPENSE)
            val start = entry.arguments?.getLong("start") ?: CATEGORY_DETAIL_ALL_TIME_START
            val end = entry.arguments?.getLong("end") ?: CATEGORY_DETAIL_ALL_TIME_END
            CategoryDetailScreen(
                viewModel = viewModel,
                categoryName = category,
                type = type,
                startMillis = start,
                endMillis = end,
                onBack = { navController.popBackStack() },
                onTransactionClick = { id -> navController.navigate(Routes.edit(id)) }
            )
        }

        composable(Routes.BACKUP) {
            BackupRestoreScreen(
                viewModel = viewModel,
                autoBackupManager = autoBackupManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                authViewModel = authViewModel,
                appearanceManager = appearanceManager,
                onBack = { navController.popBackStack() },
                onBankSmsClick = { navController.navigate(Routes.BANK_SMS_SETTINGS) },
                onLoggedOut = {
                    // Auth gate in MainActivity will show LoginScreen;
                    // clear nested back stack so we don't restore mid-flow after re-login.
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.BANK_SMS_SETTINGS) {
            BankSmsSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
