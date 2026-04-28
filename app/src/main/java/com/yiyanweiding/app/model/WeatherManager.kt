package com.yiyanweiding.app.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * Manages weather data fetching from QWeather (和风天气) API.
 * Caches results and refreshes hourly.
 */
object WeatherManager {

    // TODO: Replace with your own QWeather API key from https://dev.qweather.com
    private const val API_KEY = "f5f689df49a940ea8c7d457b386a6010"

    // Auto city lookup
    private const val CITY_LOOKUP_URL = "https://geoapi.qweather.com/v2/city/lookup?location=%s&key=%s"

    // Weather API
    private const val WEATHER_URL = "https://devapi.qweather.com/v7/weather/now?location=%s&key=%s"

    private const val PREFS_NAME = "yiyan_weather_cache"
    private const val KEY_CACHE = "weather_json"
    private const val KEY_TIME = "weather_cache_time"
    private const val CACHE_TTL_MS = 60 * 60 * 1000L // 1 hour

    // Weather types mapped to color schemes
    enum class WeatherType(val displayName: String, val emoji: String) {
        SUNNY("晴", "☀️"),
        CLOUDY("多云", "⛅"),
        OVERCAST("阴", "☁️"),
        LIGHT_RAIN("小雨", "🌦"),
        MODERATE_RAIN("中雨", "🌧"),
        HEAVY_RAIN("大雨", "🌧"),
        RAIN("雨", "🌧"),
        THUNDERSTORM("雷阵雨", "⛈"),
        SNOW("雪", "❄️"),
        FOG("雾", "🌫"),
        WINDY("风", "💨"),
        NIGHT("夜", "🌙"),
        UNKNOWN("未知", "✨")
    }

    /**
     * Color scheme derived from weather.
     */
    data class WeatherColors(
        val backgroundStart: Int,  // Gradient start
        val backgroundEnd: Int,    // Gradient end
        val cardTint: Int,         // Card overlay tint
        val textColor: Int = 0xFFFFFFFF.toInt(),
        val accentColor: Int,      // Accent for icons
        val isDark: Boolean = true // Whether background is dark
    )

    private var cachedWeather: JSONObject? = null

    fun getWeather(context: Context): JSONObject? {
        // Check cache first
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cacheTime = prefs.getLong(KEY_TIME, 0L)
        val now = System.currentTimeMillis()

        if (now - cacheTime < CACHE_TTL_MS && prefs.contains(KEY_CACHE)) {
            cachedWeather = try {
                JSONObject(prefs.getString(KEY_CACHE, "{}"))
            } catch (e: Exception) { null }
            return cachedWeather
        }

        // Fetch fresh data
        val weather = fetchWeather(context)
        if (weather != null) {
            cachedWeather = weather
            prefs.edit()
                .putString(KEY_CACHE, weather.toString())
                .putLong(KEY_TIME, now)
                .apply()
        }
        return cachedWeather
    }

    private fun fetchWeather(context: Context): JSONObject? {
        try {
            // Step 1: Auto-detect city via IP (QWeather provides this)
            val cityId = getCityId() ?: return null

            // Step 2: Fetch weather
            val url = URL(String.format(WEATHER_URL, cityId, API_KEY))
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = JSONObject(response)
            return if (json.optString("code") == "200") json.getJSONObject("now") else null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getCityId(): String? {
        return try {
            // Try auto IP location via QWeather
            val url = URL("https://geoapi.qweather.com/v2/city/lookup?location=auto_ip&key=$API_KEY")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = JSONObject(response)
            if (json.optString("code") == "200") {
                val location = json.getJSONArray("location")
                if (location.length() > 0) {
                    location.getJSONObject(0).optString("id")
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getWeatherType(weatherJson: JSONObject?): WeatherType {
        if (weatherJson == null) return WeatherType.UNKNOWN
        val icon = weatherJson.optString("icon", "999")
        val text = weatherJson.optString("text", "")

        // QWeather icon code approach
        return when {
            icon in arrayOf("100", "150") -> WeatherType.SUNNY
            icon in arrayOf("101", "102", "103") -> WeatherType.CLOUDY
            icon in arrayOf("104") -> WeatherType.OVERCAST
            icon in arrayOf("300", "301", "302") -> WeatherType.LIGHT_RAIN
            icon in arrayOf("303", "304") -> WeatherType.MODERATE_RAIN
            icon in arrayOf("305", "306", "307") -> WeatherType.HEAVY_RAIN
            icon in arrayOf("308", "309", "310", "311", "312", "313") -> WeatherType.RAIN
            icon in arrayOf("400", "401", "402", "403") -> WeatherType.SNOW
            icon in arrayOf("500", "501", "502", "503", "504", "507", "508") -> WeatherType.FOG
            icon in arrayOf("200", "201", "202", "203", "204", "205", "206",
                "207", "208", "209", "210", "211", "212", "213") -> WeatherType.THUNDERSTORM
            else -> {
                // Fallback to text matching
                when {
                    text.contains("晴") -> WeatherType.SUNNY
                    text.contains("云") -> WeatherType.CLOUDY
                    text.contains("阴") -> WeatherType.OVERCAST
                    text.contains("雨") -> if (text.contains("雷") || text.contains("暴"))
                        WeatherType.THUNDERSTORM else WeatherType.RAIN
                    text.contains("雪") -> WeatherType.SNOW
                    text.contains("雾") || text.contains("霾") -> WeatherType.FOG
                    else -> WeatherType.UNKNOWN
                }
            }
        }.also { type ->
            // Check if it's nighttime (icon 150+)
            if (type == WeatherType.SUNNY && icon == "150") return WeatherType.NIGHT
        }
    }

    fun getWeatherColors(weatherJson: JSONObject?): WeatherColors {
        val type = getWeatherType(weatherJson)
        val isNight = weatherJson?.optString("icon", "999")?.toIntOrNull()?.let { it >= 150 } == true

        return when {
            // Night — dark with deep blue/purple
            isNight || type == WeatherType.NIGHT -> WeatherColors(
                backgroundStart = 0xFF0B0D2E.toInt(),
                backgroundEnd = 0xFF1A1040.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFF7C4DFF.toInt()
            )
            // Sunny — warm golden
            type == WeatherType.SUNNY -> WeatherColors(
                backgroundStart = 0xFFFF6B35.toInt(),
                backgroundEnd = 0xFFFFA726.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFFFFD54F.toInt()
            )
            // Cloudy — soft grey-blue
            type == WeatherType.CLOUDY -> WeatherColors(
                backgroundStart = 0xFF5C7A9A.toInt(),
                backgroundEnd = 0xFF8EABCA.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFFB0BEC5.toInt()
            )
            // Overcast — muted grey
            type == WeatherType.OVERCAST -> WeatherColors(
                backgroundStart = 0xFF4A4A4A.toInt(),
                backgroundEnd = 0xFF707070.toInt(),
                cardTint = 0x10FFFFFF.toInt(),
                accentColor = 0xFF9E9E9E.toInt()
            )
            // Rain — deep blue
            type in listOf(WeatherType.LIGHT_RAIN, WeatherType.MODERATE_RAIN, WeatherType.HEAVY_RAIN, WeatherType.RAIN) -> WeatherColors(
                backgroundStart = 0xFF1A237E.toInt(),
                backgroundEnd = 0xFF455A64.toInt(),
                cardTint = 0x20FFFFFF.toInt(),
                accentColor = 0xFF64B5F6.toInt()
            )
            // Thunderstorm — dark purple
            type == WeatherType.THUNDERSTORM -> WeatherColors(
                backgroundStart = 0xFF1A0033.toInt(),
                backgroundEnd = 0xFF311B92.toInt(),
                cardTint = 0x20FFFFFF.toInt(),
                accentColor = 0xFFE040FB.toInt()
            )
            // Snow — light frost
            type == WeatherType.SNOW -> WeatherColors(
                backgroundStart = 0xFF37474F.toInt(),
                backgroundEnd = 0xFF607D8B.toInt(),
                cardTint = 0x30FFFFFF.toInt(),
                accentColor = 0xFF90CAF9.toInt()
            )
            // Fog — hazy grey
            type == WeatherType.FOG -> WeatherColors(
                backgroundStart = 0xFF3E4A59.toInt(),
                backgroundEnd = 0xFF6B7B8D.toInt(),
                cardTint = 0x20FFFFFF.toInt(),
                accentColor = 0xFFB0BEC5.toInt()
            )
            // Default / Unknown
            else -> {
                // Fallback to day-of-year seed
                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val colors = ColorUtils.getGradientColors(dayOfYear)
                WeatherColors(
                    backgroundStart = colors[0],
                    backgroundEnd = colors[1],
                    cardTint = 0x15FFFFFF.toInt(),
                    accentColor = 0xFFFFFFFF.toInt()
                )
            }
        }
    }

    fun getTemperature(weatherJson: JSONObject?): String {
        if (weatherJson == null) return "--°"
        val temp = weatherJson.optString("temp", "--")
        return "${temp}°"
    }

    fun getWeatherEmoji(weatherJson: JSONObject?): String {
        return getWeatherType(weatherJson).emoji
    }

    fun getWeatherText(weatherJson: JSONObject?): String {
        if (weatherJson == null) return ""
        return weatherJson.optString("text", "")
    }
}
