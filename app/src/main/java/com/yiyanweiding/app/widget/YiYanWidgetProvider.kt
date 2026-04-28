package com.yiyanweiding.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import com.yiyanweiding.app.R
import com.yiyanweiding.app.model.ColorUtils
import com.yiyanweiding.app.model.FavoritesManager
import com.yiyanweiding.app.model.QuoteDatabase
import java.util.Calendar

/**
 * AppWidgetProvider for YiYanWeiDing (一言为定).
 * Handles three widget sizes: small (2x2), medium (4x2), large (4x4).
 */
class YiYanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, false)
        }
        scheduleNextRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_NEXT_QUOTE -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, appWidgetId, true)
                }
            }
            ACTION_TOGGLE_FAVORITE -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                val quoteText = intent.getStringExtra(EXTRA_QUOTE_TEXT) ?: ""
                val quoteFrom = intent.getStringExtra(EXTRA_QUOTE_FROM) ?: ""
                val category = intent.getStringExtra(EXTRA_QUOTE_CATEGORY) ?: ""
                // Toggle favorite
                val quote = com.yiyanweiding.app.model.Quote(quoteText, quoteFrom, category)
                FavoritesManager.toggleFavorite(quote)
                // Update widget to show new favorite state
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, appWidgetId, false)
                }
            }
            ACTION_COPY_QUOTE -> {
                val quoteText = intent.getStringExtra(EXTRA_QUOTE_TEXT) ?: ""
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("quote", quoteText)
                clipboard.setPrimaryClip(clip)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelScheduledRefresh(context)
    }

    companion object {
        const val ACTION_NEXT_QUOTE = "com.yiyanweiding.action.NEXT_QUOTE"
        const val ACTION_TOGGLE_FAVORITE = "com.yiyanweiding.action.TOGGLE_FAVORITE"
        const val ACTION_COPY_QUOTE = "com.yiyanweiding.action.COPY_QUOTE"
        const val EXTRA_QUOTE_TEXT = "extra_quote_text"
        const val EXTRA_QUOTE_FROM = "extra_quote_from"
        const val EXTRA_QUOTE_CATEGORY = "extra_quote_category"
        const val PREFS_NAME = "yiyan_widget_state"
        const val KEY_CURRENT_INDEX = "current_index_"

        /**
         * Update a single widget instance.
         */
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            advanceToNext: Boolean
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var currentIndex = prefs.getInt(KEY_CURRENT_INDEX + appWidgetId, -1)

            if (currentIndex < 0 || advanceToNext) {
                // Initialize with today's quote, or advance
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
            val dayNumber = dayOfYear // Simple day counter "Day X"

            // Determine widget size from options
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val layoutId = when {
                minWidth >= 250 -> R.layout.widget_large   // 4x4
                minWidth >= 180 -> R.layout.widget_medium  // 4x2
                else -> R.layout.widget_small              // 2x2
            }

            val views = RemoteViews(context.packageName, layoutId)

            // Set quote text
            views.setTextViewText(R.id.widget_quote_text, quote.text)

            // Set day counter
            views.setTextViewText(R.id.widget_day_counter, "Day $dayNumber")

            // Set background color (gradient first color)
            val bgColor = ColorUtils.getDominantColor(dayOfYear)
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

            // Set click listeners
            val nextIntent = Intent(context, YiYanWidgetProvider::class.java).apply {
                action = ACTION_NEXT_QUOTE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_quote_text, nextPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_root, nextPendingIntent)

            // For large widget: set favorite button and copy button
            if (layoutId == R.layout.widget_large) {
                val isFav = FavoritesManager.isFavorite(quote.text)
                views.setImageViewResource(
                    R.id.widget_favorite_btn,
                    if (isFav) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_border
                )

                val favIntent = Intent(context, YiYanWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_FAVORITE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_QUOTE_TEXT, quote.text)
                    putExtra(EXTRA_QUOTE_FROM, quote.from)
                    putExtra(EXTRA_QUOTE_CATEGORY, quote.category)
                }
                val favPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 1000,
                    favIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_favorite_btn, favPendingIntent)

                val copyIntent = Intent(context, YiYanWidgetProvider::class.java).apply {
                    action = ACTION_COPY_QUOTE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_QUOTE_TEXT, quote.text)
                }
                val copyPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId + 2000,
                    copyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_copy_btn, copyPendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Schedule a daily refresh using AlarmManager.
         */
        fun scheduleNextRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BootReceiver::class.java).apply {
                action = Intent.ACTION_DATE_CHANGED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set alarm for midnight + a small delay
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        fun cancelScheduledRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BootReceiver::class.java).apply {
                action = Intent.ACTION_DATE_CHANGED
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
