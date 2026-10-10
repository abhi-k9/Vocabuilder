package io.github.abhik9.vocabuilder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VocabuilderApplication : Application() {
    @Inject lateinit var appStartup: AppStartup

    override fun onCreate() {
        super.onCreate()
        appStartup.start()
    }
}
