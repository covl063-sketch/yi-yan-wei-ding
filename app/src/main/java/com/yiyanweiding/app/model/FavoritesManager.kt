package com.yiyanweiding.app.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages favorite quotes stored in SharedPreferences as JSON array.
 */
object FavoritesManager {

    private const val PREFS_NAME = "yiyan_favorites"
    private const val KEY_FAVORITES = "favorites_list"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getFavorites(): List<Quote> {
        val json = prefs.getString(KEY_FAVORITES, "[]") ?: "[]"
        val type = object : TypeToken<List<Quote>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isFavorite(quoteText: String): Boolean {
        return getFavorites().any { it.text == quoteText }
    }

    fun toggleFavorite(quote: Quote): Boolean {
        val favorites = getFavorites().toMutableList()
        val existing = favorites.find { it.text == quote.text }
        return if (existing != null) {
            favorites.remove(existing)
            saveFavorites(favorites)
            false // removed
        } else {
            favorites.add(0, quote)
            saveFavorites(favorites)
            true // added
        }
    }

    fun addFavorite(quote: Quote) {
        val favorites = getFavorites().toMutableList()
        if (favorites.none { it.text == quote.text }) {
            favorites.add(0, quote)
            saveFavorites(favorites)
        }
    }

    fun removeFavorite(quoteText: String) {
        val favorites = getFavorites().toMutableList()
        favorites.removeAll { it.text == quoteText }
        saveFavorites(favorites)
    }

    private fun saveFavorites(favorites: List<Quote>) {
        prefs.edit().putString(KEY_FAVORITES, gson.toJson(favorites)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_FAVORITES).apply()
    }
}
