package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.yiyanweiding.app.model.WeatherManager
import com.yiyanweiding.app.model.QuoteDatabase
import com.yiyanweiding.app.model.Quote

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
                WeatherManager.getWeather(context)
            }
        } catch (_: Exception) {}

        for (appWidgetId in appWidgetIds) {
            WidgetUtils.updateWidget(context, appWidgetManager, appWidgetId, defaultLayoutId, this@YiYanWidgetProvider::class.java)
        }
        WidgetUtils.scheduleNextRefresh(context, this@YiYanWidgetProvider::class.java)
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
                    QuoteDatabase.rollDailyQuote(context)
                    val mgr = AppWidgetManager.getInstance(context)
                    WidgetUtils.updateWidget(context, mgr, id, defaultLayoutId, this.javaClass)
                }
            }
            WidgetUtils.ACTION_TOGGLE_FAVORITE -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val quote = QuoteDatabase.getDailyAQuote(context)
                if (quote != null) {
                    try {
                        com.yiyanweiding.app.model.FavoritesManager.init(context)
                    } catch (_: Exception) {}
                    com.yiyanweiding.app.model.FavoritesManager.toggleFavorite(quote)
                }
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val mgr = AppWidgetManager.getInstance(context)
                    WidgetUtils.updateWidget(context, mgr, id, defaultLayoutId, this.javaClass)
                }
            }
            WidgetUtils.ACTION_COPY_QUOTE -> {
                val quote = QuoteDatabase.getDailyAQuote(context)
                if (quote != null) {
                    val clip = android.content.ClipData.newPlainText("quote", quote.text)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(clip)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUtils.scheduleNextRefresh(context, this@YiYanWidgetProvider::class.java)
        // Prime weather data immediately
        try {
            android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute {
                WeatherManager.refresh(context)
            }
        } catch (_: Exception) {}
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
    }
}