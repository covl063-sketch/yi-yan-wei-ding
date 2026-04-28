package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yiyanweiding.app.model.QuoteDatabase

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
                for (providerClass in listOf(
                    SmallWidgetProvider::class.java,
                    MediumWidgetProvider::class.java,
                    LargeWidgetProvider::class.java
                )) {
                    val ids = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(context, providerClass)
                    )
                    val layoutId = when (providerClass) {
                        SmallWidgetProvider::class.java -> com.yiyanweiding.app.R.layout.widget_small
                        MediumWidgetProvider::class.java -> com.yiyanweiding.app.R.layout.widget_medium
                        LargeWidgetProvider::class.java -> com.yiyanweiding.app.R.layout.widget_large
                        else -> com.yiyanweiding.app.R.layout.widget_small
                    }
                    for (appWidgetId in ids) {
                        WidgetUtils.updateWidget(context, appWidgetManager, appWidgetId, false, layoutId, providerClass)
                    }
                }

                WidgetUtils.scheduleNextRefresh(context)
            }
        }
    }
}
