package com.yiyanweiding.app.model

import android.graphics.Color
import java.util.Calendar

/**
 * Generates gradient colors for widget backgrounds.
 * Falls back to day-of-year seeded gradients when weather is unavailable.
 */
object ColorUtils {

    // Pleasant gradient color pairs (fallback)
    private val GRADIENTS = arrayOf(
        intArrayOf(0xFF1A237E.toInt(), 0xFF4A148C.toInt()),
        intArrayOf(0xFF004D40.toInt(), 0xFF00695C.toInt()),
        intArrayOf(0xFFB71C1C.toInt(), 0xFF880E4F.toInt()),
        intArrayOf(0xFF0D47A1.toInt(), 0xFF1565C0.toInt()),
        intArrayOf(0xFF33691E.toInt(), 0xFF558B2F.toInt()),
        intArrayOf(0xFFE65100.toInt(), 0xFFBF360C.toInt()),
        intArrayOf(0xFF37474F.toInt(), 0xFF455A64.toInt()),
        intArrayOf(0xFF311B92.toInt(), 0xFF4527A0.toInt()),
        intArrayOf(0xFF004D40.toInt(), 0xFF1B5E20.toInt()),
        intArrayOf(0xFF4A148C.toInt(), 0xFF6A1B9A.toInt()),
        intArrayOf(0xFF1A237E.toInt(), 0xFF283593.toInt()),
        intArrayOf(0xFFB71C1C.toInt(), 0xFFD32F2F.toInt()),
        intArrayOf(0xFF01579B.toInt(), 0xFF0277BD.toInt()),
        intArrayOf(0xFF3E2723.toInt(), 0xFF4E342E.toInt()),
        intArrayOf(0xFF212121.toInt(), 0xFF424242.toInt()),
        intArrayOf(0xFF827717.toInt(), 0xFF9E9D24.toInt()),
        intArrayOf(0xFF4A148C.toInt(), 0xFF7B1FA2.toInt()),
        intArrayOf(0xFF006064.toInt(), 0xFF00838F.toInt()),
        intArrayOf(0xFFBF360C.toInt(), 0xFFDD2C00.toInt()),
        intArrayOf(0xFF1B5E20.toInt(), 0xFF2E7D32.toInt()),
    )

    fun getGradientColors(dayOfYear: Int): IntArray {
        val seed = dayOfYear % GRADIENTS.size
        return GRADIENTS[seed]
    }

    fun getDominantColor(dayOfYear: Int): Int {
        return getGradientColors(dayOfYear)[0]
    }

    fun getTextColor(): Int = Color.WHITE
    fun getSecondaryTextColor(): Int = Color.argb(170, 255, 255, 255)

    /**
     * Get weather-aware background drawable XML.
     * Since RemoteViews can't use gradient drawable programmatically,
     * we set a solid background color and overlay tint.
     */
    fun getWeatherBackgroundStart(context: android.content.Context): Int {
        val weather = WeatherManager.getWeather(context)
        if (weather != null) {
            return WeatherManager.getWeatherColors(weather).backgroundStart
        }
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return getDominantColor(dayOfYear)
    }

    fun getWeatherAccentColor(context: android.content.Context): Int {
        val weather = WeatherManager.getWeather(context)
        if (weather != null) {
            return WeatherManager.getWeatherColors(weather).accentColor
        }
        return 0xFFFFFFFF.toInt()
    }
}
