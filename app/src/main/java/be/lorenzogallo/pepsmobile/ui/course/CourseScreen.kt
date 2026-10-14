package be.lorenzogallo.pepsmobile.ui.course

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import be.lorenzogallo.pepsmobile.data.model.CourseTab
import be.lorenzogallo.pepsmobile.ui.common.ErrorView
import be.lorenzogallo.pepsmobile.ui.common.LoadingView
import be.lorenzogallo.pepsmobile.ui.common.LocalAppContainer
import be.lorenzogallo.pepsmobile.ui.common.UiState
import be.lorenzogallo.pepsmobile.ui.results.PepsPageContent
import be.lorenzogallo.pepsmobile.ui.results.PeriodChips

@Composable
fun CourseScreen(
    courseId: Int,
    initialTab: CourseTab,
    courseName: String,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onReconnect: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: CourseViewModel = viewModel(
        key = "course-$courseId",
        factory = viewModelFactory {
            initializer { CourseViewModel(container.repository, courseId, initialTab) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabState = state.current
    val webPath = tabState.periodPath ?: "/?page=cours&coursID=$courseId&pageCours=${state.tab.pageCours}"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.title ?: courseName.ifEmpty { "Cours" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenWeb(webPath) }) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = "Ouvrir la version web")
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualiser")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = CourseTab.entries.indexOf(state.tab)) {
                CourseTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    CourseTab.EVALUATIONS -> Icons.AutoMirrored.Outlined.FactCheck
                                    CourseTab.STATISTICS -> Icons.Outlined.BarChart
                                },
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
            (tabState.page as? UiState.Success)?.data?.let { page ->
                PeriodChips(
                    periods = page.periods,
                    selectedPath = tabState.periodPath,
                    onSelect = { viewModel.selectPeriod(it.path) },
                )
            }
            PullToRefreshBox(
                isRefreshing = tabState.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = tabState.page) {
                    UiState.Loading -> LoadingView()
                    is UiState.Error -> ErrorView(
                        message = s.message,
                        onRetry = viewModel::refresh,
                        needsLogin = s.needsLogin,
                        onReconnect = onReconnect,
                    )
                    is UiState.Success -> PepsPageContent(page = s.data, onOpenWeb = { onOpenWeb(webPath) })
                }
            }
        }
    }
}
