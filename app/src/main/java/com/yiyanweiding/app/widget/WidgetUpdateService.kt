package com.yiyanweiding.app.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.Intent

/**
 * Service to update widgets in background.
 */
class WidgetUpdateService : IntentService("WidgetUpdateService") {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(this, YiYanWidgetProvider::class.java)
        )
        for (appWidgetId in appWidgetIds) {
            YiYanWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, false)
        }
    }
}
