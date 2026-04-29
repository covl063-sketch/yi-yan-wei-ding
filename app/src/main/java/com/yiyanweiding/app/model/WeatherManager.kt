package com.yiyanweiding.app.model

import android.content.Context
import android.content.SharedPreferences
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

object WeatherManager {

    // wttr.in — free, no API key
    private const val WTTR_URL = "https://wttr.in/%s?format=%%C+%%t&lang=zh"

    // OpenWeatherMap fallback
    private const val OWM_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=zh_cn"
    private const val OWM_KEY = "32fdc4a23695a52733a0afea37a444ef"

    private const val DEFAULT_CITY = "Beijing"
    private const val PREFS_NAME = "yiyan_weather_cache"
    private const val KEY_TIME = "weather_cache_time"
    private const val KEY_CITY = "weather_city"
    private const val KEY_COND = "weather_cond"
    private const val KEY_TEMP = "weather_temp"
    private const val CACHE_TTL_MS = 60 * 60 * 1000L
    private const val TIMEOUT = 4000

    data class SimpleWeather(val condition: String, val temp: String, val raw: String)

    enum class WeatherType(val displayName: String, val emoji: String) {
        SUNNY("晴", "\u2600\uFE0F"),
        CLOUDY("多云", "\u26C5"),
        OVERCAST("阴", "\u2601\uFE0F"),
        LIGHT_RAIN("小雨", "\uD83C\uDF26"),
        MODERATE_RAIN("中雨", "\uD83C\uDF27"),
        HEAVY_RAIN("大雨", "\uD83C\uDF27"),
        RAIN("雨", "\uD83C\uDF27"),
        THUNDERSTORM("雷阵雨", "\u26C8"),
        SNOW("雪", "\u2744\uFE0F"),
        FOG("雾", "\uD83C\uDF2B"),
        WINDY("风", "\uD83D\uDCA8"),
        NIGHT("夜", "\uD83C\uDF19"),
        UNKNOWN("未知", "\u2728")
    }

    data class WeatherColors(
        val backgroundStart: Int, val backgroundEnd: Int,
        val cardTint: Int, val accentColor: Int,
        val textColor: Int = 0xFFFFFFFF.toInt(), val isDark: Boolean = true
    )

    fun refresh(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_TIME, 0L).apply()
        val result = fetchWeatherInternal(context)
        if (result != null) {
            cacheSimple(context, result)
        } else {
            // Retry once after a short delay (network may not be ready)
            try {
                Thread.sleep(2000)
                val retry = fetchWeatherInternal(context)
                if (retry != null) cacheSimple(context, retry)
            } catch (_: Exception) {}
        }
    }

    fun getWeather(context: Context): SimpleWeather {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cacheTime = prefs.getLong(KEY_TIME, 0L)
        if (System.currentTimeMillis() - cacheTime < CACHE_TTL_MS) {
            readCachedSimple(prefs)?.let { return it }
        }
        val result = fetchWeatherInternal(context)
        if (result != null) { cacheSimple(context, result); return result }
        return readCachedSimple(prefs) ?: SimpleWeather("未知", "--\u00B0", "")
    }

    fun getWeatherType(context: Context): WeatherType {
        return classifyCondition(getWeather(context).condition)
    }

    fun getWeatherColors(context: Context): WeatherColors {
        return colorsForType(getWeatherType(context))
    }

    fun setCity(context: Context, city: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CITY, city).putLong(KEY_TIME, 0L).apply()
    }

    fun getCityName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
    }

    // --- private ---

    private fun fetchWeatherInternal(context: Context): SimpleWeather? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val city = prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY

        // Try wttr.in first
        try {
            val urlStr = WTTR_URL.format(URLEncoder.encode(city, "UTF-8"))
            val resp = httpGet(urlStr)
            if (resp != null && resp.isNotBlank()) {
                val parts = resp.trim().split("\\s+".toRegex(), limit = 2)
                val cond = if (parts.isNotEmpty()) parts[0] else "未知"
                val temp = if (parts.size > 1) parts[1] else "--\u00B0"
                prefs.edit().putLong(KEY_TIME, System.currentTimeMillis()).apply()
                return SimpleWeather(classifyConditionDisplay(cond), temp, resp.trim())
            }
        } catch (_: Exception) {}

        // Fallback: OWM
        try {
            val urlStr = OWM_URL.format(URLEncoder.encode(city, "UTF-8"), OWM_KEY)
            val resp = httpGet(urlStr)
            if (resp != null) {
                val json = org.json.JSONObject(resp)
                if (json.optInt("cod") == 200 || json.has("main")) {
                    val arr = json.optJSONArray("weather")
                    val desc = if (arr != null && arr.length() > 0)
                        arr.getJSONObject(0).optString("description", "未知") else "未知"
                    val main = json.optJSONObject("main")
                    val tempC = main?.optDouble("temp", 0.0) ?: 0.0
                    val simple = SimpleWeather(classifyConditionDisplay(desc), "${tempC.toInt()}\u00B0C", desc)
                    prefs.edit().putLong(KEY_TIME, System.currentTimeMillis()).apply()
                    return simple
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun cacheSimple(ctx: Context, sw: SimpleWeather) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_COND, sw.condition).putString(KEY_TEMP, sw.temp)
            .putLong(KEY_TIME, System.currentTimeMillis()).apply()
    }

    private fun readCachedSimple(prefs: SharedPreferences): SimpleWeather? {
        val cond = prefs.getString(KEY_COND, null) ?: return null
        val temp = prefs.getString(KEY_TEMP, null) ?: return null
        return SimpleWeather(cond, temp, "$cond $temp")
    }

    private fun httpGet(urlStr: String): String? {
        try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT
            conn.readTimeout = TIMEOUT
            conn.requestMethod = "GET"
            val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
            val result = reader.readText()
            reader.close()
            conn.disconnect()
            return result
        } catch (_: Exception) { return null }
    }

    private fun classifyCondition(cond: String): WeatherType {
        val c = cond.lowercase().trim()
        return when {
            c.contains("sunny") || c.contains("clear") || c.contains("晴") -> WeatherType.SUNNY
            c.contains("partly cloudy") || c.contains("多云") -> WeatherType.CLOUDY
            c.contains("cloudy") || c.contains("overcast") || c.contains("阴") -> WeatherType.OVERCAST
            c.contains("thund") || c.contains("storm") || c.contains("雷阵雨") || c.contains("雷") -> WeatherType.THUNDERSTORM
            c.contains("heavy rain") || c.contains("torrential") || c.contains("大雨") || c.contains("暴雨") -> WeatherType.HEAVY_RAIN
            c.contains("moderate rain") || c.contains("中雨") -> WeatherType.MODERATE_RAIN
            c.contains("light rain") || c.contains("drizzle") || c.contains("shower") || c.contains("小雨") || c.contains("阵雨") -> WeatherType.LIGHT_RAIN
            c.contains("rain") || c.contains("雨") -> WeatherType.RAIN
            c.contains("snow") || c.contains("sleet") || c.contains("blizzard") || c.contains("ice") || c.contains("雪") || c.contains("冰") -> WeatherType.SNOW
            c.contains("fog") || c.contains("mist") || c.contains("haze") || c.contains("雾") || c.contains("霾") -> WeatherType.FOG
            c.contains("wind") || c.contains("风") -> WeatherType.WINDY
            else -> WeatherType.UNKNOWN
        }
    }

    private fun classifyConditionDisplay(cond: String): String {
        val type = classifyCondition(cond)
        return if (type == WeatherType.UNKNOWN) cond.split(" ").firstOrNull()?.take(2) ?: "未知" else type.displayName
    }

    private fun colorsForType(type: WeatherType): WeatherColors {
        return when (type) {
            WeatherType.NIGHT -> WeatherColors(0xFF0B0D2E.toInt(), 0xFF1A1040.toInt(), 0x15FFFFFF.toInt(), 0xFF7C4DFF.toInt())
            WeatherType.SUNNY -> WeatherColors(0xFFFF6B35.toInt(), 0xFFFFA726.toInt(), 0x15FFFFFF.toInt(), 0xFFFFD54F.toInt())
            WeatherType.CLOUDY -> WeatherColors(0xFF5C7A9A.toInt(), 0xFF8EABCA.toInt(), 0x15FFFFFF.toInt(), 0xFFB0BEC5.toInt())
            WeatherType.OVERCAST -> WeatherColors(0xFF4A4A4A.toInt(), 0xFF707070.toInt(), 0x10FFFFFF.toInt(), 0xFF9E9E9E.toInt())
            in listOf(WeatherType.LIGHT_RAIN, WeatherType.MODERATE_RAIN, WeatherType.HEAVY_RAIN, WeatherType.RAIN) ->
                WeatherColors(0xFF1A237E.toInt(), 0xFF455A64.toInt(), 0x20FFFFFF.toInt(), 0xFF64B5F6.toInt())
            WeatherType.THUNDERSTORM -> WeatherColors(0xFF1A0033.toInt(), 0xFF311B92.toInt(), 0x20FFFFFF.toInt(), 0xFFE040FB.toInt())
            WeatherType.SNOW -> WeatherColors(0xFF37474F.toInt(), 0xFF607D8B.toInt(), 0x30FFFFFF.toInt(), 0xFF90CAF9.toInt())
            WeatherType.FOG -> WeatherColors(0xFF3E4A59.toInt(), 0xFF6B7B8D.toInt(), 0x20FFFFFF.toInt(), 0xFFB0BEC5.toInt())
            WeatherType.WINDY -> WeatherColors(0xFF2C3E50.toInt(), 0xFF546E7A.toInt(), 0x15FFFFFF.toInt(), 0xFF80DEEA.toInt())
            else -> {
                val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val c = ColorUtils.getGradientColors(day)
                WeatherColors(c[0], c[1], 0x15FFFFFF.toInt(), 0xFFFFFFFF.toInt())
            }
        }
    }
}
