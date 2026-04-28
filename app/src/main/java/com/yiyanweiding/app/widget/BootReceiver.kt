package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yiyanweiding.app.model.QuoteDatabase
import java.util.Calendar

/**
 * Receives BOOT_COMPLETED and DATE_CHANGED broadcasts to refresh widgets.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED -> {
                // Re-initialize database just in case
                QuoteDatabase.init(context)

                // Update all widgets
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, YiYanWidgetProvider::class.java)
                )
                for (appWidgetId in appWidgetIds) {
                    YiYanWidgetProvider.updateWidget(
                        context, appWidgetManager, appWidgetId, false
                    )
                }

                // Re-schedule daily alarm
                YiYanWidgetProvider.scheduleNextRefresh(context)
            }
        }
    }
}
