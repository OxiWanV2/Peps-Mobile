package be.lorenzogallo.pepsmobile.ui.courses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.data.model.Course
import be.lorenzogallo.pepsmobile.data.model.CourseTab
import be.lorenzogallo.pepsmobile.ui.common.EmptyView
import be.lorenzogallo.pepsmobile.ui.common.ErrorView
import be.lorenzogallo.pepsmobile.ui.common.LoadingView
import be.lorenzogallo.pepsmobile.ui.common.UiState
import be.lorenzogallo.pepsmobile.ui.common.appViewModel

@Composable
fun CoursesScreen(
    onOpenCourse: (Course, CourseTab) -> Unit,
    onReconnect: () -> Unit,
    viewModel: CoursesViewModel = appViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Mes cours") }) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(
                    message = s.message,
                    onRetry = viewModel::refresh,
                    needsLogin = s.needsLogin,
                    onReconnect = onReconnect,
                )
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptyView("Aucun cours trouvé")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(s.data, key = { it.id }) { course ->
                                CourseCard(course = course, onOpenCourse = onOpenCourse)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onOpenCourse: (Course, CourseTab) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCourse(course, CourseTab.EVALUATIONS) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(course.color))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = course.teacherLabel.ifEmpty { "Enseignant" },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(course.textColor),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            CourseLink(
                label = "Évaluations",
                icon = { Icon(Icons.AutoMirrored.Outlined.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = { onOpenCourse(course, CourseTab.EVALUATIONS) },
            )
            CourseLink(
                label = "Statistiques",
                icon = { Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                onClick = { onOpenCourse(course, CourseTab.STATISTICS) },
            )
        }
    }
}

@Composable
private fun CourseLink(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
