package com.yiyanweiding.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.yiyanweiding.app.R
import com.yiyanweiding.app.model.ColorUtils
import com.yiyanweiding.app.model.FavoritesManager
import com.yiyanweiding.app.model.QuoteDatabase
import java.util.Calendar

object WidgetUtils {

    const val ACTION_NEXT_QUOTE = "com.yiyanweiding.action.NEXT_QUOTE"
    const val ACTION_TOGGLE_FAVORITE = "com.yiyanweiding.action.TOGGLE_FAVORITE"
    const val ACTION_COPY_QUOTE = "com.yiyanweiding.action.COPY_QUOTE"
    const val EXTRA_QUOTE_TEXT = "extra_quote_text"
    const val EXTRA_QUOTE_FROM = "extra_quote_from"
    const val EXTRA_QUOTE_CATEGORY = "extra_quote_category"
    const val PREFS_NAME = "yiyan_widget_state"
    const val KEY_CURRENT_INDEX = "current_index_"

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        advanceToNext: Boolean,
        layoutId: Int,
        providerClass: Class<*>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var currentIndex = prefs.getInt(KEY_CURRENT_INDEX + appWidgetId, -1)

        if (currentIndex < 0 || advanceToNext) {
            if (currentIndex < 0) {
                val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                currentIndex = today % QuoteDatabase.size()
            } else {
                currentIndex = (currentIndex + 1) % QuoteDatabase.size()
            }
            prefs.edit().putInt(KEY_CURRENT_INDEX + appWidgetId, currentIndex).apply()
        }

        val quote = QuoteDatabase.getQuoteByIndex(currentIndex)
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val dayNumber = dayOfYear

        val views = RemoteViews(context.packageName, layoutId)

        views.setTextViewText(R.id.widget_quote_text, quote.text)
        views.setTextViewText(R.id.widget_day_counter, "Day $dayNumber")

        val bgColor = ColorUtils.getDominantColor(dayOfYear)
        views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

        // Next quote click — route to THIS provider
        val nextIntent = Intent(context, providerClass).apply {
            action = ACTION_NEXT_QUOTE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_quote_text, nextPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_root, nextPendingIntent)

        if (layoutId == R.layout.widget_large) {
            val isFav = FavoritesManager.isFavorite(quote.text)
            views.setImageViewResource(
                R.id.widget_favorite_btn,
                if (isFav) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )

            val favIntent = Intent(context, providerClass).apply {
                action = ACTION_TOGGLE_FAVORITE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_QUOTE_TEXT, quote.text)
                putExtra(EXTRA_QUOTE_FROM, quote.from)
                putExtra(EXTRA_QUOTE_CATEGORY, quote.category)
            }
            val favPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 1000, favIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_favorite_btn, favPendingIntent)

            val copyIntent = Intent(context, providerClass).apply {
                action = ACTION_COPY_QUOTE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_QUOTE_TEXT, quote.text)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId + 2000, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_copy_btn, copyPendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            SmallWidgetProvider::class.java to R.layout.widget_small,
            MediumWidgetProvider::class.java to R.layout.widget_medium,
            LargeWidgetProvider::class.java to R.layout.widget_large
        )
        for ((providerClass, layoutId) in providers) {
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
            for (id in ids) {
                updateWidget(context, appWidgetManager, id, false, layoutId, providerClass)
            }
        }
    }

    fun scheduleNextRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = Intent.ACTION_DATE_CHANGED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 5)
            set(Calendar.MILLISECOND, 0)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC, calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, pendingIntent
        )
    }

    fun cancelScheduledRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BootReceiver::class.java).apply {
            action = Intent.ACTION_DATE_CHANGED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
