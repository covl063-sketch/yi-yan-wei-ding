package com.yiyanweiding.app.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar

/**
 * Loads and manages the quote database from assets/quotes.json.
 */
object QuoteDatabase {

    private var quotes: List<Quote> = emptyList()
    private var initialized = false
    private var currentDailyIndex = -1

    private const val PREFS_NAME = "yiyan_quote_db"
    private const val KEY_DAY = "daily_quote_day"
    private const val KEY_INDEX = "daily_quote_index"

    fun init(context: Context) {
        if (initialized) return
        try {
            val reader = BufferedReader(
                InputStreamReader(context.assets.open("quotes.json"), "UTF-8")
            )
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            val type = object : TypeToken<List<Quote>>() {}.type
            quotes = Gson().fromJson(sb.toString(), type)
            initialized = true
        } catch (e: Exception) {
            e.printStackTrace()
            quotes = listOf(
                Quote("路漫漫其修远兮，吾将上下而求索。", "屈原《离骚》", "poetry"),
                Quote("知之者不如好之者，好之者不如乐之者。", "孔子《论语》", "philosophy"),
                Quote("生活就像一盒巧克力，你永远不知道下一颗是什么味道。", "《阿甘正传》", "modern")
            )
        }
    }

    fun getQuoteByIndex(index: Int): Quote {
        if (quotes.isEmpty()) {
            return Quote("千里之行，始于足下。", "老子《道德经》", "philosophy")
        }
        return quotes[index % quotes.size]
    }

    fun getTodayQuote(dayOfYear: Int): Quote {
        if (quotes.isEmpty()) return getQuoteByIndex(0)
        return quotes[dayOfYear % quotes.size]
    }

    /**
     * Get the daily quote for today, consistent across app and widgets.
     */
    fun getDailyAQuote(context: Context): Quote? {
        if (quotes.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val savedDay = prefs.getInt(KEY_DAY, -1)
        val savedIndex = prefs.getInt(KEY_INDEX, -1)

        currentDailyIndex = if (savedDay == today && savedIndex >= 0) {
            savedIndex
        } else {
            val index = today % quotes.size
            prefs.edit().putInt(KEY_DAY, today).putInt(KEY_INDEX, index).apply()
            index
        }
        return quotes[currentDailyIndex]
    }

    /**
     * Roll to the next quote for widget tap-to-refresh.
     */
    fun rollDailyQuote(context: Context) {
        if (quotes.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentDailyIndex = (currentDailyIndex + 1) % quotes.size
        prefs.edit()
            .putInt(KEY_DAY, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
            .putInt(KEY_INDEX, currentDailyIndex)
            .apply()
    }

    /**
     * Get the number of consecutive days using this app (for Day N counter).
     */
    fun getDayCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val savedDay = prefs.getInt(KEY_DAY, -1)
        return if (savedDay == today) {
            // Count consecutive days (simplified: just return day of year)
            today
        } else {
            today
        }
    }

    fun size(): Int = quotes.size

    fun allQuotes(): List<Quote> = quotes
}
