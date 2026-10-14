package be.lorenzogallo.pepsmobile

import android.app.Application
import be.lorenzogallo.pepsmobile.di.AppContainer

class PepsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
