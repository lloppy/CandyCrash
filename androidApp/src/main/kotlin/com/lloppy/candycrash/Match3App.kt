package com.lloppy.candycrash

import android.app.Application
import com.lloppy.candycrash.screens.levels.di.initKoin

class Match3App : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}