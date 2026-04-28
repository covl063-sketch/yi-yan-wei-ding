package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/**
 * AppWidgetProvider for YiYanWeiDing (一言为定).
 * Abstract base — each size subclass provides its own default layout resource.
 */
abstract class YiYanWidgetProvider(
    private val defaultLayoutId: Int
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Prime weather cache in background
        try {
            android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute {
                com.yiyanweiding.app.model.WeatherManager.getWeather(context)
            }
        } catch (_: Exception) {}

        for (appWidgetId in appWidgetIds) {
            WidgetUtils.updateWidget(context, appWidgetManager, appWidgetId, false, defaultLayoutId, this@YiYanWidgetProvider::class.java)
        }
        WidgetUtils.scheduleNextRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetUtils.ACTION_NEXT_QUOTE -> {
                val id = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val mgr = AppWidgetManager.getInstance(context)
                    WidgetUtils.updateWidget(context, mgr, id, true, defaultLayoutId, this.javaClass)
                }
            }
            WidgetUtils.ACTION_TOGGLE_FAVORITE -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val text = intent.getStringExtra(WidgetUtils.EXTRA_QUOTE_TEXT) ?: ""
                val from = intent.getStringExtra(WidgetUtils.EXTRA_QUOTE_FROM) ?: ""
                val cat = intent.getStringExtra(WidgetUtils.EXTRA_QUOTE_CATEGORY) ?: ""
                val quote = com.yiyanweiding.app.model.Quote(text, from, cat)
                com.yiyanweiding.app.model.FavoritesManager.toggleFavorite(quote)
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val mgr = AppWidgetManager.getInstance(context)
                    WidgetUtils.updateWidget(context, mgr, id, false, defaultLayoutId, this.javaClass)
                }
            }
            WidgetUtils.ACTION_COPY_QUOTE -> {
                val text = intent.getStringExtra(WidgetUtils.EXTRA_QUOTE_TEXT) ?: ""
                val clip = android.content.ClipData.newPlainText("quote", text)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(clip)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUtils.scheduleNextRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUtils.cancelScheduledRefresh(context)
    }
}
