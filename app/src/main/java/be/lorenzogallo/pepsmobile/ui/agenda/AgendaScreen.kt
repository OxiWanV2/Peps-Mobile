package be.lorenzogallo.pepsmobile.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.data.model.Lesson
import be.lorenzogallo.pepsmobile.ui.common.EmptyView
import be.lorenzogallo.pepsmobile.ui.common.ErrorView
import be.lorenzogallo.pepsmobile.ui.common.LoadingView
import be.lorenzogallo.pepsmobile.ui.common.LocalAppContainer
import be.lorenzogallo.pepsmobile.ui.common.UiState
import be.lorenzogallo.pepsmobile.ui.common.appViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_SHORT = DateTimeFormatter.ofPattern("EEE", Locale.FRENCH)
private val DAY_LONG = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val RANGE_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private val RANGE_DAY_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

@Composable
fun AgendaScreen(
    onReconnect: () -> Unit,
    viewModel: AgendaViewModel = appViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requests = LocalAppContainer.current.navigationRequests
    val requestedDate by requests.agendaDate.collectAsStateWithLifecycle()
    LaunchedEffect(requestedDate) {
        requestedDate?.let { date ->
            viewModel.showDate(date)
            requests.clear()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Journal de classe")
                        Text(
                            text = "Année scolaire ${state.schoolYear.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (!state.isCurrentWeek) {
                        IconButton(onClick = viewModel::today) {
                            Icon(Icons.Outlined.Today, contentDescription = "Aujourd'hui")
                        }
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
            WeekHeader(state = state, onPrevious = viewModel::previousWeek, onNext = viewModel::nextWeek)

            val lessons = (state.lessons as? UiState.Success)?.data.orEmpty()
            DaySelector(
                weekStart = state.weekStart,
                selected = state.selectedDate,
                lessons = lessons,
                onSelect = viewModel::selectDay,
            )
            HorizontalDivider()

            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state.lessons) {
                    UiState.Loading -> LoadingView()
                    is UiState.Error -> ErrorView(
                        message = s.message,
                        onRetry = viewModel::refresh,
                        needsLogin = s.needsLogin,
                        onReconnect = onReconnect,
                    )
                    is UiState.Success -> {
                        val dayLessons = remember(s.data, state.selectedDate) {
                            s.data.filter { it.date == state.selectedDate }
                        }
                        if (dayLessons.isEmpty()) {
                            EmptyView("Aucun cours le ${state.selectedDate.format(DAY_LONG)}")
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                item {
                                    Text(
                                        text = state.selectedDate.format(DAY_LONG).replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                                items(dayLessons, key = { it.id }) { lesson -> LessonCard(lesson) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(state: AgendaState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = state.canGoPrevious) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Semaine précédente")
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Semaine ${state.weekNumber.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            val sameYear = state.weekStart.year == state.weekEnd.year
            val rangeText = if (sameYear) {
                "${state.weekStart.format(RANGE_DAY)} – ${state.weekEnd.format(RANGE_DAY_YEAR)}"
            } else {
                "${state.weekStart.format(RANGE_DAY_YEAR)} – ${state.weekEnd.format(RANGE_DAY_YEAR)}"
            }
            Text(
                text = rangeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onNext, enabled = state.canGoNext) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Semaine suivante")
        }
    }
}

@Composable
private fun DaySelector(
    weekStart: LocalDate,
    selected: LocalDate,
    lessons: List<Lesson>,
    onSelect: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    val days = remember(weekStart, lessons) {
        (0L..6L).map { weekStart.plusDays(it) }.filter { day ->
            day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY || lessons.any { it.date == day }
        }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it.toEpochDay() }) { day ->
            val count = lessons.count { it.date == day }
            val hasEvaluation = lessons.any { it.date == day && it.hasEvaluation }
            FilterChip(
                selected = day == selected,
                onClick = { onSelect(day) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 40.dp)) {
                        Text(
                            text = day.format(DAY_SHORT).removeSuffix(".").replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Medium,
                        )
                        Text(
                            text = when {
                                hasEvaluation -> "Éval."
                                count > 0 -> "$count h"
                                else -> "–"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasEvaluation) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                modifier = Modifier.height(72.dp),
            )
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson) {
    val color = Color(lesson.color)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
            )
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(lesson.start, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = lesson.end,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (lesson.room.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = lesson.room.replace("|", " / "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (lesson.lesson.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(lesson.lesson, style = MaterialTheme.typography.bodyMedium)
                }
                if (lesson.description.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = lesson.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (lesson.hasEvaluation || lesson.hasHomework) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (lesson.hasEvaluation) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Évaluation") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.FactCheck, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    labelColor = MaterialTheme.colorScheme.onErrorContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            )
                        }
                        if (lesson.hasHomework) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Devoir") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
