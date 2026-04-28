package com.yiyanweiding.app

import android.app.Application
import com.yiyanweiding.app.model.FavoritesManager
import com.yiyanweiding.app.model.QuoteDatabase

class YiYanWeiDingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        QuoteDatabase.init(this)
        FavoritesManager.init(this)
    }
}
