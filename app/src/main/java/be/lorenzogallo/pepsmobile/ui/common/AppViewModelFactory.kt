package be.lorenzogallo.pepsmobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import be.lorenzogallo.pepsmobile.di.AppContainer
import be.lorenzogallo.pepsmobile.ui.agenda.AgendaViewModel
import be.lorenzogallo.pepsmobile.ui.courses.CoursesViewModel
import be.lorenzogallo.pepsmobile.ui.dashboard.DashboardViewModel
import be.lorenzogallo.pepsmobile.ui.login.AuthViewModel
import be.lorenzogallo.pepsmobile.ui.results.ResultsViewModel
import be.lorenzogallo.pepsmobile.ui.settings.ProfileViewModel

val LocalAppContainer = compositionLocalOf<AppContainer> { error("AppContainer non fourni") }

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.repository, container.sessionScope, container::refreshHomeWidget)
        modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.repository, container.prefs, container::refreshHomeWidget)
        modelClass.isAssignableFrom(CoursesViewModel::class.java) -> CoursesViewModel(container.repository)
        modelClass.isAssignableFrom(AgendaViewModel::class.java) -> AgendaViewModel(container.repository)
        modelClass.isAssignableFrom(ResultsViewModel::class.java) -> ResultsViewModel(container.repository)
        modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(container.repository)
        else -> throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    } as T
}

@Composable
inline fun <reified VM : ViewModel> appViewModel(): VM {
    val container = LocalAppContainer.current
    return viewModel(factory = AppViewModelFactory(container))
}
