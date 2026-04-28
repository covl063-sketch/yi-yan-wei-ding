package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.yiyanweiding.app.model.QuoteDatabase
import com.yiyanweiding.app.model.WeatherManager

/**
 * Receives BOOT_COMPLETED and DATE_CHANGED broadcasts to refresh widgets.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED -> {
                QuoteDatabase.init(context)

                // Prime weather cache in background
                try {
                    android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute {
                        WeatherManager.getWeather(context)
                    }
                } catch (_: Exception) {}

                val appWidgetManager = AppWidgetManager.getInstance(context)

                // Update all widget instances across all three sizes
                for ((providerClass, layoutId) in listOf(
                    SmallWidgetProvider::class.java to com.yiyanweiding.app.R.layout.widget_small,
                    MediumWidgetProvider::class.java to com.yiyanweiding.app.R.layout.widget_medium,
                    LargeWidgetProvider::class.java to com.yiyanweiding.app.R.layout.widget_large
                )) {
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, providerClass)
                    )
                    for (appWidgetId in ids) {
                        WidgetUtils.updateWidget(context, appWidgetManager, appWidgetId, layoutId, providerClass)
                    }
                }
                
                WidgetUtils.scheduleNextRefresh(context, SmallWidgetProvider::class.java)
            }
        }
    }
}