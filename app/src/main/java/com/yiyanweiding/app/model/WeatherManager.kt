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
 * Manages weather data fetching from OpenWeatherMap API.
 * Uses OpenWeatherMap OneCall-like Current Weather endpoint.
 * Caches results and refreshes hourly.
 */
object WeatherManager {

    // OpenWeatherMap API key — get yours free at https://openweathermap.org/
    private const val API_KEY = "3076ca88b66b429e70c2a4cac5f173e7"

    // Current weather endpoint (free tier)
    private const val WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=zh_cn"

    // Default city (Beijing). Change to your city (in Chinese or English).
    // free tier limitation: needs a fixed city name
    private const val DEFAULT_CITY = "Beijing"

    private const val PREFS_NAME = "yiyan_weather_cache"
    private const val KEY_CACHE = "weather_json"
    private const val KEY_TIME = "weather_cache_time"
    private const val KEY_CITY = "weather_city"
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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val city = prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY

            val urlStr = String.format(WEATHER_URL, java.net.URLEncoder.encode(city, "UTF-8"), API_KEY)
            val url = URL(urlStr)

            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = JSONObject(response)
            return if (json.optInt("cod") == 200) json else null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getWeatherType(weatherJson: JSONObject?): WeatherType {
        if (weatherJson == null) return WeatherType.UNKNOWN

        // Check if it's night using sys.sunset
        val sys = weatherJson.optJSONObject("sys")
        val sunrise = sys?.optLong("sunrise", 0L) ?: 0L
        val sunset = sys?.optLong("sunset", 0L) ?: 0L
        val now = System.currentTimeMillis() / 1000
        val isNight = sunset > 0 && (now < sunrise || now > sunset)

        // OpenWeatherMap weather ID codes
        val weatherArr = weatherJson.optJSONArray("weather")
        val id = if (weatherArr != null && weatherArr.length() > 0)
            weatherArr.getJSONObject(0).optInt("id", 800) else 800
        val main = if (weatherArr != null && weatherArr.length() > 0)
            weatherArr.getJSONObject(0).optString("main", "") else ""

        val type = when {
            id == 800 -> WeatherType.SUNNY
            id in 801..802 -> WeatherType.CLOUDY
            id in 803..804 -> WeatherType.OVERCAST
            id in 300..321 -> WeatherType.LIGHT_RAIN
            id in 500..501 -> WeatherType.LIGHT_RAIN
            id in 502..504 -> WeatherType.MODERATE_RAIN
            id in 520..531 -> WeatherType.HEAVY_RAIN
            id in 200..232 -> WeatherType.THUNDERSTORM
            id in 600..622 -> WeatherType.SNOW
            id in 700..781 -> WeatherType.FOG
            main.contains("Cloud", ignoreCase = true) -> WeatherType.CLOUDY
            main.contains("Rain", ignoreCase = true) -> WeatherType.RAIN
            main.contains("Drizzle", ignoreCase = true) -> WeatherType.LIGHT_RAIN
            main.contains("Snow", ignoreCase = true) -> WeatherType.SNOW
            main.contains("Thunderstorm", ignoreCase = true) -> WeatherType.THUNDERSTORM
            main.contains("Fog", ignoreCase = true) || main.contains("Mist", ignoreCase = true) -> WeatherType.FOG
            main.contains("Haze", ignoreCase = true) -> WeatherType.FOG
            else -> WeatherType.UNKNOWN
        }

        return if (isNight && type == WeatherType.SUNNY) WeatherType.NIGHT else type
    }

    fun getWeatherColors(weatherJson: JSONObject?): WeatherColors {
        val type = getWeatherType(weatherJson)

        return when (type) {
            WeatherType.NIGHT -> WeatherColors(
                backgroundStart = 0xFF0B0D2E.toInt(),
                backgroundEnd = 0xFF1A1040.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFF7C4DFF.toInt()
            )
            WeatherType.SUNNY -> WeatherColors(
                backgroundStart = 0xFFFF6B35.toInt(),
                backgroundEnd = 0xFFFFA726.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFFFFD54F.toInt()
            )
            WeatherType.CLOUDY -> WeatherColors(
                backgroundStart = 0xFF5C7A9A.toInt(),
                backgroundEnd = 0xFF8EABCA.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFFB0BEC5.toInt()
            )
            WeatherType.OVERCAST -> WeatherColors(
                backgroundStart = 0xFF4A4A4A.toInt(),
                backgroundEnd = 0xFF707070.toInt(),
                cardTint = 0x10FFFFFF.toInt(),
                accentColor = 0xFF9E9E9E.toInt()
            )
            in listOf(WeatherType.LIGHT_RAIN, WeatherType.MODERATE_RAIN, WeatherType.HEAVY_RAIN, WeatherType.RAIN) -> WeatherColors(
                backgroundStart = 0xFF1A237E.toInt(),
                backgroundEnd = 0xFF455A64.toInt(),
                cardTint = 0x20FFFFFF.toInt(),
                accentColor = 0xFF64B5F6.toInt()
            )
            WeatherType.THUNDERSTORM -> WeatherColors(
                backgroundStart = 0xFF1A0033.toInt(),
                backgroundEnd = 0xFF311B92.toInt(),
                cardTint = 0x20FFFFFF.toInt(),
                accentColor = 0xFFE040FB.toInt()
            )
            WeatherType.SNOW -> WeatherColors(
                backgroundStart = 0xFF37474F.toInt(),
                backgroundEnd = 0xFF607D8B.toInt(),
                cardTint = 0x30FFFFFF.toInt(),
                accentColor = 0xFF90CAF9.toInt()
            )
            WeatherType.FOG -> WeatherColors(
                backgroundStart = 0xFF3E4A59.toInt(),
                backgroundEnd = 0xFF6B7B8D.toInt(),
                cardTint = 0x20FFFFFF.toInt(),
                accentColor = 0xFFB0BEC5.toInt()
            )
            WeatherType.WINDY -> WeatherColors(
                backgroundStart = 0xFF2C3E50.toInt(),
                backgroundEnd = 0xFF546E7A.toInt(),
                cardTint = 0x15FFFFFF.toInt(),
                accentColor = 0xFF80DEEA.toInt()
            )
            else -> {
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
        val main = weatherJson.optJSONObject("main")
        val temp = main?.optDouble("temp", 0.0) ?: return "--°"
        return "${temp.toInt()}°"
    }

    fun getWeatherEmoji(weatherJson: JSONObject?): String {
        return getWeatherType(weatherJson).emoji
    }

    fun getWeatherText(weatherJson: JSONObject?): String {
        if (weatherJson == null) return ""
        val weatherArr = weatherJson.optJSONArray("weather")
        if (weatherArr != null && weatherArr.length() > 0) {
            return weatherArr.getJSONObject(0).optString("description", "")
        }
        return ""
    }

    /**
     * Returns the display name of the current city.
     */
    fun getCityName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
    }

    /**
     * Sets a custom city. Next weather fetch will use this city.
     */
    fun setCity(context: Context, city: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CITY, city)
            .putLong(KEY_TIME, 0L) // Invalidate cache
            .apply()
        cachedWeather = null // Force re-fetch
    }
}
