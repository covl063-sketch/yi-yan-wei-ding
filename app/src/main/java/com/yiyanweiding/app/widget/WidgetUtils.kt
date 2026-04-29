package com.yiyanweiding.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.yiyanweiding.app.R
import com.yiyanweiding.app.model.QuoteDatabase
import com.yiyanweiding.app.model.WeatherManager
import com.yiyanweiding.app.model.ColorUtils
import com.yiyanweiding.app.model.WeatherBackgroundRenderer
import java.util.*

object WidgetUtils {

    private const val PREFS_NAME = "widget_prefs"
    private const val PREFS_CITY = "weather_city"
    private const val DAILY_REFRESH_HOUR = 0 // UTC 0 = BJT 8

    const val ACTION_NEXT_QUOTE = "com.yiyanweiding.app.NEXT_QUOTE"
    const val ACTION_TOGGLE_FAVORITE = "com.yiyanweiding.app.TOGGLE_FAVORITE"
    const val ACTION_COPY_QUOTE = "com.yiyanweiding.app.COPY_QUOTE"
    const val ACTION_REFRESH_WEATHER = "com.yiyanweiding.app.REFRESH_WEATHER"
    const val EXTRA_CITY = "weather_city"

    /**
     * Update a single widget instance with weather-aware background and quote content.
     */
    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        layoutId: Int,
        providerClass: Class<*>
    ) {
        val views = RemoteViews(context.packageName, layoutId)
        val quote = QuoteDatabase.getDailyAQuote(context)

        // Sync user city from widget prefs to WeatherManager
        val city = getCityRaw(context)
        if (city.isNotEmpty() && city != "auto") {
            WeatherManager.setCity(context, city)
        }

        // Get weather data
        val weather = WeatherManager.getWeather(context)
        val weatherType = WeatherManager.getWeatherType(context)

        // Set quote text
        if (quote != null) {
            views.setTextViewText(R.id.widget_quote_text, quote.text)
        } else {
            views.setTextViewText(R.id.widget_quote_text, "一言为定")
        }

        // Weather info display on all sizes
        val weatherDisplay = "${weatherType.emoji} ${weather.temp}"
        views.setTextViewText(R.id.widget_weather_info, weatherDisplay)
        views.setViewVisibility(R.id.widget_weather_info, android.view.View.VISIBLE)

        // Day counter
        views.setTextViewText(R.id.widget_day_counter, "Day ${QuoteDatabase.getDayCount(context)}")

        // Weather background rendering (Bitmap)
        try {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
            val density = context.resources.displayMetrics.density
            val isNight = isNightTime()

            val bitmap = WeatherBackgroundRenderer.render(minWidth, minHeight, weatherType, isNight, density)
            views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
        } catch (_: Exception) {
            // leave transparent
        }

        // Card background color from WeatherManager
        val colors = WeatherManager.getWeatherColors(context)
        views.setInt(R.id.widget_card, "setBackgroundColor", colors.cardTint)

        // --- Click actions ---

        // Root / quote tap -> next quote
        val nextIntent = Intent(context, providerClass).apply {
            action = ACTION_NEXT_QUOTE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val nextPI = PendingIntent.getBroadcast(
            context, appWidgetId, nextIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_quote_text, nextPI)
        views.setOnClickPendingIntent(R.id.widget_root, nextPI)

        // Favorite button (large only)
        val favIntent = Intent(context, providerClass).apply {
            action = ACTION_TOGGLE_FAVORITE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val favPI = PendingIntent.getBroadcast(
            context, appWidgetId + 1000, favIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_favorite_btn, favPI)

        // Copy button (large only)
        val copyIntent = Intent(context, providerClass).apply {
            action = ACTION_COPY_QUOTE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val copyPI = PendingIntent.getBroadcast(
            context, appWidgetId + 2000, copyIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_copy_btn, copyPI)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun isNightTime(): Boolean {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour < 6 || hour >= 18
    }

    /**
     * Schedule daily refresh at configured hour.
     */
    fun scheduleNextRefresh(context: Context, providerClass: Class<*>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, providerClass).apply {
            action = ACTION_NEXT_QUOTE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, DAILY_REFRESH_HOUR)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    /**
     * Update all widget instances of a given provider type.
     */
    fun updateAllWidgets(context: Context, providerClass: Class<*>, layoutId: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val provider = ComponentName(context, providerClass)
        val ids = appWidgetManager.getAppWidgetIds(provider)

        for (appWidgetId in ids) {
            updateWidget(context, appWidgetManager, appWidgetId, layoutId, providerClass)
        }
    }

    // --- City preference (user-facing, from MainActivity) ---

    fun getCity(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_CITY, "") ?: ""
    }

    fun setCity(context: Context, city: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_CITY, city)
            .apply()
    }

    private fun getCityRaw(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_CITY, "auto") ?: "auto"
    }
}
