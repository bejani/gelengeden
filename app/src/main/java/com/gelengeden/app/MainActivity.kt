package com.gelengeden.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.gelengeden.app.data.AppearanceManager
import com.gelengeden.app.ui.navigation.GelengedenNavGraph
import com.gelengeden.app.ui.screens.LoginScreen
import com.gelengeden.app.ui.theme.GelengedenTheme
import com.gelengeden.app.ui.util.LocaleHelper
import com.gelengeden.app.ui.viewmodel.AuthPhase
import com.gelengeden.app.ui.viewmodel.AuthViewModel
import com.gelengeden.app.ui.viewmodel.TransactionViewModel

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GelengedenApp

        setContent {
            val themeMode by app.appearanceManager.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                AppearanceManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppearanceManager.ThemeMode.LIGHT -> false
                AppearanceManager.ThemeMode.DARK -> true
            }
            GelengedenTheme(darkTheme = darkTheme) {
                // Force RTL for Persian UI even if system layout direction differs
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val authViewModel: AuthViewModel = viewModel(
                            factory = AuthViewModel.factory(app.authManager)
                        )
                        val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                        when (authState.phase) {
                            AuthPhase.LOADING -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            AuthPhase.SETUP, AuthPhase.LOGIN -> {
                                LoginScreen(authViewModel = authViewModel)
                            }

                            AuthPhase.AUTHENTICATED -> {
                                val navController = rememberNavController()
                                val viewModel: TransactionViewModel = viewModel(
                                    factory = TransactionViewModel.factory(app.repository)
                                )
                                GelengedenNavGraph(
                                    navController = navController,
                                    viewModel = viewModel,
                                    authViewModel = authViewModel,
                                    appearanceManager = app.appearanceManager,
                                    autoBackupManager = app.autoBackupManager
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
