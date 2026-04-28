package com.yiyanweiding.app.model

import android.graphics.Color
import java.util.Calendar

/**
 * Generates gradient colors for widget backgrounds based on day-of-year seed.
 */
object ColorUtils {

    // Pleasant gradient color pairs
    private val GRADIENTS = arrayOf(
        intArrayOf(0xFF1A237E.toInt(), 0xFF4A148C.toInt()),  // Deep Indigo -> Purple
        intArrayOf(0xFF004D40.toInt(), 0xFF00695C.toInt()),  // Dark Teal -> Teal
        intArrayOf(0xFFB71C1C.toInt(), 0xFF880E4F.toInt()),  // Deep Red -> Pink
        intArrayOf(0xFF0D47A1.toInt(), 0xFF1565C0.toInt()),  // Blue -> Light Blue
        intArrayOf(0xFF33691E.toInt(), 0xFF558B2F.toInt()),  // Dark Green -> Green
        intArrayOf(0xFFE65100.toInt(), 0xFFBF360C.toInt()),  // Deep Orange -> Red Orange
        intArrayOf(0xFF37474F.toInt(), 0xFF455A64.toInt()),  // Blue Grey -> Grey
        intArrayOf(0xFF311B92.toInt(), 0xFF4527A0.toInt()),  // Deep Purple -> Purple
        intArrayOf(0xFF004D40.toInt(), 0xFF1B5E20.toInt()),  // Teal -> Green
        intArrayOf(0xFF4A148C.toInt(), 0xFF6A1B9A.toInt()),  // Purple -> Light Purple
        intArrayOf(0xFF1A237E.toInt(), 0xFF283593.toInt()),  // Indigo -> Dark Blue
        intArrayOf(0xFFB71C1C.toInt(), 0xFFD32F2F.toInt()),  // Red -> Bright Red
        intArrayOf(0xFF01579B.toInt(), 0xFF0277BD.toInt()),  // Dark Blue -> Blue
        intArrayOf(0xFF3E2723.toInt(), 0xFF4E342E.toInt()),  // Brown -> Dark Brown
        intArrayOf(0xFF212121.toInt(), 0xFF424242.toInt()),   // Black -> Dark Grey
        intArrayOf(0xFF827717.toInt(), 0xFF9E9D24.toInt()),  // Olive -> Yellow Green
        intArrayOf(0xFF4A148C.toInt(), 0xFF7B1FA2.toInt()),  // Purple -> Violet
        intArrayOf(0xFF006064.toInt(), 0xFF00838F.toInt()),  // Cyan -> Light Cyan
        intArrayOf(0xFFBF360C.toInt(), 0xFFDD2C00.toInt()),  // Deep Orange -> Red
        intArrayOf(0xFF1B5E20.toInt(), 0xFF2E7D32.toInt()),  // Forest -> Green
    )

    fun getGradientColors(dayOfYear: Int): IntArray {
        val seed = dayOfYear % GRADIENTS.size
        return GRADIENTS[seed]
    }

    fun getGradientColorsForTimestamp(timestamp: Long): IntArray {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return getGradientColors(cal.get(Calendar.DAY_OF_YEAR))
    }

    // For RemoteViews we need to set background color as a single color
    // We'll use the first color of the pair as the dominant color
    fun getDominantColor(dayOfYear: Int): Int {
        return getGradientColors(dayOfYear)[0]
    }

    fun getTextColor(): Int = Color.WHITE
    fun getSecondaryTextColor(): Int = Color.argb(170, 255, 255, 255)
}
