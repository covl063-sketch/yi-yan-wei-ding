package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.yiyanweiding.app.R

/**
 * Small widget (2x2) — single quote, no extra actions.
 */
class SmallWidgetProvider : YiYanWidgetProvider() {
    override fun getLayoutId(minWidth: Int): Int = R.layout.widget_small
}
