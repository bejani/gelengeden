package com.gelengeden.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gelengeden.app.GelengedenApp
import com.gelengeden.app.MainActivity
import com.gelengeden.app.R
import com.gelengeden.app.ui.util.formatMoney
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds and pushes RemoteViews for the balance home-screen widget.
 */
object BalanceWidgetUpdater {

    fun updateAll(
        context: Context,
        income: Double,
        expense: Double,
        balance: Double
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, BalanceWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return

        val views = buildRemoteViews(context, income, expense, balance)
        ids.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        income: Double,
        expense: Double,
        balance: Double
    ) {
        if (appWidgetIds.isEmpty()) return
        val views = buildRemoteViews(context, income, expense, balance)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    suspend fun refreshFromDatabase(context: Context) {
        val app = context.applicationContext as? GelengedenApp ?: return
        val (income, expense, balance) = withContext(Dispatchers.IO) {
            app.repository.getTotalsOnce()
        }
        withContext(Dispatchers.Main) {
            updateAll(app, income, expense, balance)
        }
    }

    suspend fun refreshFromDatabase(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? GelengedenApp ?: return
        val (income, expense, balance) = withContext(Dispatchers.IO) {
            app.repository.getTotalsOnce()
        }
        withContext(Dispatchers.Main) {
            updateWidgets(app, appWidgetManager, appWidgetIds, income, expense, balance)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        income: Double,
        expense: Double,
        balance: Double
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_balance)
        views.setTextViewText(R.id.widget_balance_value, formatMoney(balance))
        views.setTextViewText(R.id.widget_income_value, formatMoney(income))
        views.setTextViewText(R.id.widget_expense_value, formatMoney(expense))

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        return views
    }
}
