package com.yiyanweiding.app.widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent

/**
 * Service to update widgets in background.
 */
class WidgetUpdateService : IntentService("WidgetUpdateService") {

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return
        val appWidgetManager = AppWidgetManager.getInstance(this)

        for ((providerClass, layoutId) in listOf(
            SmallWidgetProvider::class.java to com.yiyanweiding.app.R.layout.widget_small,
            MediumWidgetProvider::class.java to com.yiyanweiding.app.R.layout.widget_medium,
            LargeWidgetProvider::class.java to com.yiyanweiding.app.R.layout.widget_large
        )) {
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(this, providerClass))
            for (widgetId in ids) {
                WidgetUtils.updateWidget(this, appWidgetManager, widgetId, false, layoutId)
            }
        }
    }
}
