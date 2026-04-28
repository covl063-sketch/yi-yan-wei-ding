package com.yiyanweiding.app.model

/**
 * Data model for a single quote.
 */
data class Quote(
    val text: String,
    val from: String,
    val category: String  // "poetry", "philosophy", "modern"
)
