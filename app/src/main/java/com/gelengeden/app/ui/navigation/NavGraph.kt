package com.gelengeden.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gelengeden.app.ui.screens.AboutScreen
import com.gelengeden.app.ui.screens.AddEditTransactionScreen
import com.gelengeden.app.ui.screens.BackupRestoreScreen
import com.gelengeden.app.ui.screens.BankSmsSettingsScreen
import com.gelengeden.app.ui.screens.HomeScreen
import com.gelengeden.app.ui.screens.ManageCategoriesScreen
import com.gelengeden.app.ui.screens.ReportsScreen
import com.gelengeden.app.ui.screens.SettingsScreen
import com.gelengeden.app.ui.viewmodel.AuthViewModel
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val CATEGORIES = "categories"
    const val REPORTS = "reports"
    const val BACKUP = "backup"
    const val ABOUT = "about"
    const val SETTINGS = "settings"
    const val BANK_SMS_SETTINGS = "bank_sms_settings"

    fun edit(id: Long) = "edit/$id"
}

@Composable
fun GelengedenNavGraph(
    navController: NavHostController,
    viewModel: TransactionViewModel,
    authViewModel: AuthViewModel
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
                onAboutClick = { navController.navigate(Routes.ABOUT) }
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

        composable(Routes.CATEGORIES) {
            ManageCategoriesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REPORTS) {
            ReportsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BACKUP) {
            BackupRestoreScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                authViewModel = authViewModel,
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
