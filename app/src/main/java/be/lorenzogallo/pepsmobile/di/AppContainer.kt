package be.lorenzogallo.pepsmobile.di

import android.content.Context
import be.lorenzogallo.pepsmobile.data.network.PepsApi
import be.lorenzogallo.pepsmobile.data.network.SessionCookieJar
import be.lorenzogallo.pepsmobile.data.network.SessionManager
import be.lorenzogallo.pepsmobile.data.prefs.AppPrefs
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import be.lorenzogallo.pepsmobile.data.security.SecureStore
import be.lorenzogallo.pepsmobile.ui.common.NavigationRequests
import be.lorenzogallo.pepsmobile.widget.PepsWidgetProvider

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val prefs: AppPrefs = AppPrefs(appContext)
    val sessionScope: SessionScope = SessionScope()
    val navigationRequests: NavigationRequests = NavigationRequests()
    val secureStore: SecureStore = SecureStore(appContext)
    val cookieJar: SessionCookieJar = SessionCookieJar(PepsApi.HOST, secureStore)
    val api: PepsApi = PepsApi(cookieJar)
    val session: SessionManager = SessionManager(api, cookieJar, secureStore)
    val repository: PepsRepository = PepsRepository(api, session, secureStore, cookieJar)

    fun refreshHomeWidget() = PepsWidgetProvider.requestUpdate(appContext)
}
