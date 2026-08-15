package com.gelengeden.app

import android.app.Application
import android.content.Context
import com.gelengeden.app.data.AppDatabase
import com.gelengeden.app.data.AuthManager
import com.gelengeden.app.data.TransactionRepository
import com.gelengeden.app.ui.util.LocaleHelper
import com.gelengeden.app.widget.BalanceWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class GelengedenApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { TransactionRepository(database) }
    val authManager by lazy { AuthManager(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        observeTotalsForWidget()
    }

    /**
     * Keeps any placed home-screen widgets in sync whenever totals change
     * (add / edit / delete / restore).
     */
    private fun observeTotalsForWidget() {
        applicationScope.launch {
            combine(
                repository.getTotalIncome(),
                repository.getTotalExpense()
            ) { income, expense ->
                Triple(income, expense, income - expense)
            }
                .distinctUntilChanged()
                .collect { (income, expense, balance) ->
                    launch(Dispatchers.Main) {
                        BalanceWidgetUpdater.updateAll(
                            this@GelengedenApp,
                            income,
                            expense,
                            balance
                        )
                    }
                }
        }
    }
}
