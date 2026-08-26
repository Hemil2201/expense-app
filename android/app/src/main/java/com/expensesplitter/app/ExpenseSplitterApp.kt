package com.expensesplitter.app

import android.app.Application
import com.expensesplitter.app.di.AppContainer

class ExpenseSplitterApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
