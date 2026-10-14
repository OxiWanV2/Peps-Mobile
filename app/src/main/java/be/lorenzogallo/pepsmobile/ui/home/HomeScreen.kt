package be.lorenzogallo.pepsmobile.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import be.lorenzogallo.pepsmobile.data.model.CourseTab
import be.lorenzogallo.pepsmobile.ui.agenda.AgendaScreen
import be.lorenzogallo.pepsmobile.ui.common.LocalAppContainer
import be.lorenzogallo.pepsmobile.ui.common.appViewModel
import be.lorenzogallo.pepsmobile.ui.course.CourseScreen
import be.lorenzogallo.pepsmobile.ui.courses.CoursesScreen
import be.lorenzogallo.pepsmobile.ui.dashboard.DashboardScreen
import be.lorenzogallo.pepsmobile.ui.dashboard.DashboardViewModel
import be.lorenzogallo.pepsmobile.ui.login.AuthViewModel
import be.lorenzogallo.pepsmobile.ui.notifications.NotificationsScreen
import be.lorenzogallo.pepsmobile.ui.results.ResultsScreen
import be.lorenzogallo.pepsmobile.ui.settings.DashboardSettingsScreen
import be.lorenzogallo.pepsmobile.ui.settings.SettingsScreen
import be.lorenzogallo.pepsmobile.ui.web.WebScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DASHBOARD, "Accueil", Icons.Outlined.Dashboard),
    Tab(Routes.COURSES, "Cours", Icons.AutoMirrored.Outlined.MenuBook),
    Tab(Routes.AGENDA, "Journal", Icons.Outlined.CalendarMonth),
    Tab(Routes.RESULTS, "Résultats", Icons.AutoMirrored.Outlined.Grading),
)

@Composable
fun HomeScreen(authViewModel: AuthViewModel) {
    val container = LocalAppContainer.current
    CompositionLocalProvider(LocalViewModelStoreOwner provides container.sessionScope) {
        HomeContent(authViewModel)
    }
}

@Composable
private fun HomeContent(authViewModel: AuthViewModel) {
    val container = LocalAppContainer.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val dashboardViewModel: DashboardViewModel = appViewModel()

    val openWeb: (String) -> Unit = { path -> Routes.web(path)?.let { navController.navigate(it) } }
    val openCourse: (Int, CourseTab, String) -> Unit = { id, tab, name -> navController.navigate(Routes.course(id, tab, name)) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom),
        bottomBar = {
            if (currentRoute in Routes.mainTabs) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onCustomize = { navController.navigate(Routes.DASHBOARD_SETTINGS) },
                    onOpenAgendaDate = { date ->
                        container.navigationRequests.agendaDate.value = date
                        navController.navigateToTab(Routes.AGENDA)
                    },
                    onOpenTab = { route -> navController.navigateToTab(route) },
                    onOpenWeb = openWeb,
                    onReconnect = authViewModel::logout,
                )
            }
            composable(Routes.COURSES) {
                CoursesScreen(
                    onOpenCourse = { course, tab -> openCourse(course.id, tab, course.name) },
                    onReconnect = authViewModel::logout,
                )
            }
            composable(Routes.AGENDA) {
                AgendaScreen(onReconnect = authViewModel::logout)
            }
            composable(Routes.RESULTS) {
                ResultsScreen(
                    onOpenWeb = openWeb,
                    onOpenCourse = { id, name -> openCourse(id, CourseTab.EVALUATIONS, name) },
                    onReconnect = authViewModel::logout,
                )
            }
            composable(
                route = Routes.COURSE,
                arguments = listOf(
                    navArgument(Routes.COURSE_ID_ARG) { type = NavType.IntType },
                    navArgument(Routes.COURSE_TAB_ARG) { type = NavType.StringType; defaultValue = CourseTab.EVALUATIONS.pageCours },
                    navArgument(Routes.COURSE_NAME_ARG) { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val courseId = entry.arguments?.getInt(Routes.COURSE_ID_ARG) ?: 0
                val tabArg = entry.arguments?.getString(Routes.COURSE_TAB_ARG)
                val tab = CourseTab.entries.firstOrNull { it.pageCours == tabArg } ?: CourseTab.EVALUATIONS
                CourseScreen(
                    courseId = courseId,
                    initialTab = tab,
                    courseName = entry.arguments?.getString(Routes.COURSE_NAME_ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenWeb = openWeb,
                    onReconnect = authViewModel::logout,
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    viewModel = dashboardViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenLink = openWeb,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenDashboardSettings = { navController.navigate(Routes.DASHBOARD_SETTINGS) },
                )
            }
            composable(Routes.DASHBOARD_SETTINGS) {
                DashboardSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.WEB,
                arguments = listOf(navArgument(Routes.WEB_ARG) { type = NavType.StringType }),
            ) { entry ->
                val url = entry.arguments?.getString(Routes.WEB_ARG).orEmpty()
                WebScreen(url = url, onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
