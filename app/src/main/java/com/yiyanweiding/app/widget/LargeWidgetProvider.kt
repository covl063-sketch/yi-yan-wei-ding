package com.yiyanweiding.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.yiyanweiding.app.R

/**
 * Large widget (4x4) — full quote with favorite and copy actions.
 */
class LargeWidgetProvider : YiYanWidgetProvider() {
    override fun getLayoutId(minWidth: Int): Int = R.layout.widget_large
}
