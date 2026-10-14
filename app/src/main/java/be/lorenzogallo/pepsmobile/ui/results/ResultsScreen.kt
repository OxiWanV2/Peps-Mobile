package be.lorenzogallo.pepsmobile.ui.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.ui.common.ErrorView
import be.lorenzogallo.pepsmobile.ui.common.LoadingView
import be.lorenzogallo.pepsmobile.ui.common.UiState
import be.lorenzogallo.pepsmobile.ui.common.appViewModel

@Composable
fun ResultsScreen(
    onOpenWeb: (String) -> Unit,
    onOpenCourse: (courseId: Int, name: String) -> Unit,
    onReconnect: () -> Unit,
    viewModel: ResultsViewModel = appViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Résultats") },
                actions = {
                    IconButton(onClick = { onOpenWeb(state.currentPath) }) {
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
            if (state.hubTabs.size > 1) {
                val selectedIndex = state.hubTabs.indexOfFirst { it.path == state.hubPath }.coerceAtLeast(0)
                PrimaryTabRow(selectedTabIndex = selectedIndex) {
                    state.hubTabs.forEach { tab ->
                        Tab(
                            selected = tab.path == state.hubPath,
                            onClick = { viewModel.selectHubTab(tab) },
                            text = { Text(tab.label) },
                        )
                    }
                }
            }
            (state.page as? UiState.Success)?.data?.let { page ->
                PeriodChips(
                    periods = page.periods,
                    selectedPath = state.periodPath,
                    onSelect = { viewModel.selectPeriod(it.path) },
                )
            }
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state.page) {
                    UiState.Loading -> LoadingView()
                    is UiState.Error -> ErrorView(
                        message = s.message,
                        onRetry = viewModel::refresh,
                        needsLogin = s.needsLogin,
                        onReconnect = onReconnect,
                    )
                    is UiState.Success -> PepsPageContent(
                        page = s.data,
                        onOpenWeb = { onOpenWeb(state.currentPath) },
                        onOpenCourse = onOpenCourse,
                    )
                }
            }
        }
    }
}
