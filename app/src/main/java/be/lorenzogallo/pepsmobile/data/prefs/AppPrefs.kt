package be.lorenzogallo.pepsmobile.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _biometricLock = MutableStateFlow(prefs.getBoolean(KEY_BIOMETRIC_LOCK, true))
    val biometricLock: StateFlow<Boolean> = _biometricLock

    private val _secureScreen = MutableStateFlow(prefs.getBoolean(KEY_SECURE_SCREEN, true))
    val secureScreen: StateFlow<Boolean> = _secureScreen

    private val _dashboardWidgets = MutableStateFlow(loadDashboardWidgets())
    val dashboardWidgets: StateFlow<List<DashboardWidgetConfig>> = _dashboardWidgets

    fun setBiometricLock(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_LOCK, enabled).apply()
        _biometricLock.value = enabled
    }

    fun setSecureScreen(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_SCREEN, enabled).apply()
        _secureScreen.value = enabled
    }

    fun setDashboardWidgets(config: List<DashboardWidgetConfig>) {
        val normalized = normalize(config)
        prefs.edit().putString(KEY_DASHBOARD, normalized.joinToString(",") { "${it.widget.id}:${if (it.enabled) 1 else 0}" }).apply()
        _dashboardWidgets.value = normalized
    }

    fun toggleDashboardWidget(widget: DashboardWidget, enabled: Boolean) =
        setDashboardWidgets(_dashboardWidgets.value.map { if (it.widget == widget) it.copy(enabled = enabled) else it })

    fun moveDashboardWidget(widget: DashboardWidget, delta: Int) {
        val list = _dashboardWidgets.value.toMutableList()
        val index = list.indexOfFirst { it.widget == widget }
        val target = index + delta
        if (index < 0 || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        setDashboardWidgets(list)
    }

    private fun loadDashboardWidgets(): List<DashboardWidgetConfig> {
        val raw = prefs.getString(KEY_DASHBOARD, null) ?: return DashboardWidget.entries.map { DashboardWidgetConfig(it, true) }
        val parsed = raw.split(',').mapNotNull { token ->
            val parts = token.split(':')
            val widget = DashboardWidget.byId(parts.getOrNull(0).orEmpty()) ?: return@mapNotNull null
            DashboardWidgetConfig(widget, parts.getOrNull(1) != "0")
        }
        return normalize(parsed)
    }

    private fun normalize(config: List<DashboardWidgetConfig>): List<DashboardWidgetConfig> {
        val seen = config.distinctBy { it.widget }
        val missing = DashboardWidget.entries.filter { w -> seen.none { it.widget == w } }.map { DashboardWidgetConfig(it, true) }
        return seen + missing
    }

    private companion object {
        const val PREFS_NAME = "peps_prefs"
        const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        const val KEY_SECURE_SCREEN = "secure_screen"
        const val KEY_DASHBOARD = "dashboard_widgets"
    }
}
