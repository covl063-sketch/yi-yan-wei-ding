package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.yiyanweiding.app.R

/**
 * Medium widget (4x2) — quote with context line.
 */
class MediumWidgetProvider : YiYanWidgetProvider() {
    override fun getLayoutId(minWidth: Int): Int = R.layout.widget_medium
}
