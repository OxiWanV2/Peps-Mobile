package be.lorenzogallo.pepsmobile.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.automirrored.outlined.Grading
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.data.model.AgendaOverview
import be.lorenzogallo.pepsmobile.data.model.Lesson
import be.lorenzogallo.pepsmobile.data.model.NewsItem
import be.lorenzogallo.pepsmobile.data.model.PepsPage
import be.lorenzogallo.pepsmobile.data.model.SchoolYear
import be.lorenzogallo.pepsmobile.data.prefs.DashboardWidget
import be.lorenzogallo.pepsmobile.ui.common.ErrorView
import be.lorenzogallo.pepsmobile.ui.common.LoadingView
import be.lorenzogallo.pepsmobile.ui.common.UiState
import be.lorenzogallo.pepsmobile.ui.home.Routes
import be.lorenzogallo.pepsmobile.ui.results.KpiGrid
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val EVAL_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)
private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onCustomize: () -> Unit,
    onOpenAgendaDate: (LocalDate) -> Unit,
    onOpenTab: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onReconnect: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val now by produceState(initialValue = LocalTime.now()) {
        while (true) {
            delay(60_000)
            value = LocalTime.now()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Accueil") },
                actions = {
                    IconButton(onClick = onCustomize) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Personnaliser l'accueil")
                    }
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(
                            badge = {
                                if (state.unreadCount > 0) {
                                    Badge { Text(state.unreadCount.coerceAtMost(99).toString()) }
                                }
                            },
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Réglages")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val loginError = state.needsLogin
            when {
                state.initialLoading -> LoadingView()
                loginError != null -> ErrorView(
                    message = loginError.message,
                    onRetry = viewModel::refresh,
                    needsLogin = true,
                    onReconnect = onReconnect,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.widgets.isEmpty()) {
                        item(key = "no-widgets") {
                            EmptyHint("Aucun widget activé. Utilisez le bouton de personnalisation en haut à droite.")
                        }
                    }
                    state.widgets.forEach { widget ->
                        when (widget) {
                            DashboardWidget.TODAY -> todaySection(state, now, onOpenAgendaDate)
                            DashboardWidget.EVAL_WEEK -> lessonsSection(
                                key = "eval-week",
                                title = "Évaluations cette semaine",
                                icon = Icons.AutoMirrored.Outlined.FactCheck,
                                agenda = state.agenda,
                                pick = { it.weekEvaluations },
                                emptyText = "Aucune évaluation prévue cette semaine.",
                                markPast = true,
                                onOpenAgendaDate = onOpenAgendaDate,
                                onRetry = viewModel::refresh,
                            )
                            DashboardWidget.EVAL_UPCOMING -> lessonsSection(
                                key = "eval-upcoming",
                                title = "Évaluations à venir",
                                icon = Icons.Outlined.Upcoming,
                                subtitle = "${state.agenda.weeksAhead()} prochaines semaines",
                                agenda = state.agenda,
                                pick = { it.upcomingEvaluations },
                                emptyText = "Aucune évaluation annoncée dans les ${state.agenda.weeksAhead()} prochaines semaines.",
                                markPast = false,
                                onOpenAgendaDate = onOpenAgendaDate,
                                onRetry = viewModel::refresh,
                            )
                            DashboardWidget.HOMEWORK -> lessonsSection(
                                key = "homework",
                                title = "Devoirs",
                                icon = Icons.AutoMirrored.Outlined.Assignment,
                                agenda = state.agenda,
                                pick = { it.homework(LocalDate.now()) },
                                emptyText = "Aucun devoir annoncé.",
                                markPast = false,
                                badge = "Devoir",
                                onOpenAgendaDate = onOpenAgendaDate,
                                onRetry = viewModel::refresh,
                            )
                            DashboardWidget.RESULTS -> resultsSection(state.results, onOpen = { onOpenTab(Routes.RESULTS) }, onRetry = viewModel::refresh)
                            DashboardWidget.NEWS -> newsSection(state, viewModel::refresh)
                            DashboardWidget.SHORTCUTS -> shortcutsSection(onOpenTab, onOpenWeb)
                        }
                    }
                }
            }
        }
    }
}

private fun UiState<AgendaOverview>?.weeksAhead(): Int = (this as? UiState.Success)?.data?.weeksAhead ?: 4

private fun LazyListScope.todaySection(state: DashboardState, now: LocalTime, onOpenAgendaDate: (LocalDate) -> Unit) {
    item(key = "today") {
        val today = LocalDate.now()
        val agenda = (state.agenda as? UiState.Success)?.data
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenAgendaDate(today) },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val greeting = state.profile?.firstName?.takeIf { it.isNotBlank() }?.let { "Bonjour $it" } ?: "Bonjour"
                        Text(greeting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = today.format(LONG_DATE).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                        Text(
                            text = "S${SchoolYear.isoWeek(today).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                when (val a = state.agenda) {
                    null, UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    is UiState.Error -> Text(a.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> TodaySummary(agenda = agenda ?: a.data, today = today, now = now)
                }
            }
        }
    }
}

@Composable
private fun TodaySummary(agenda: AgendaOverview, today: LocalDate, now: LocalTime) {
    val todayLessons = agenda.thisWeek.filter { it.date == today }.sortedBy { it.start }
    if (todayLessons.isEmpty()) {
        Text("Pas de cours aujourd'hui.", style = MaterialTheme.typography.bodyMedium)
        val nextDay = agenda.thisWeek.filter { it.date.isAfter(today) }.minByOrNull { it.date }?.date
        if (nextDay != null) {
            val lessons = agenda.thisWeek.filter { it.date == nextDay }.sortedBy { it.start }
            Text(
                text = "Prochain jour de cours : ${nextDay.format(EVAL_DATE)} · ${lessons.size} cours dès ${lessons.first().start}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    val first = todayLessons.first()
    val last = todayLessons.last()
    Text(
        text = "${todayLessons.size} cours · ${first.start} → ${last.end}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
    )
    val current = todayLessons.firstOrNull { time(it.start)?.let { s -> !now.isBefore(s) } == true && time(it.end)?.isAfter(now) == true }
    val next = todayLessons.firstOrNull { time(it.start)?.isAfter(now) == true }
    val evaluations = todayLessons.count { it.hasEvaluation }
    Spacer(Modifier.height(4.dp))
    when {
        current != null -> LessonLine(label = "En cours", lesson = current, detail = "jusqu'à ${current.end}")
        next != null && time(first.start)?.isAfter(now) == true -> Unit
        next == null -> Text("Journée terminée.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (next != null) LessonLine(label = if (current == null && time(first.start)?.isAfter(now) == true) "Premier cours" else "Ensuite", lesson = next, detail = "à ${next.start}")
    if (evaluations > 0) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (evaluations == 1) "1 évaluation aujourd'hui" else "$evaluations évaluations aujourd'hui",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LessonLine(label: String, lesson: Lesson, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color(lesson.color), CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label : ${lesson.title} $detail${lesson.room.takeIf { it.isNotEmpty() }?.let { " · ${it.replace("|", " / ")}" }.orEmpty()}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun time(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

private fun LazyListScope.lessonsSection(
    key: String,
    title: String,
    icon: ImageVector,
    agenda: UiState<AgendaOverview>?,
    pick: (AgendaOverview) -> List<Lesson>,
    emptyText: String,
    markPast: Boolean,
    subtitle: String? = null,
    badge: String? = null,
    onOpenAgendaDate: (LocalDate) -> Unit,
    onRetry: () -> Unit,
) {
    when (agenda) {
        null, UiState.Loading -> item(key = "$key-loading") {
            SectionHeader(title, icon, subtitle = subtitle)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        is UiState.Error -> item(key = "$key-error") {
            SectionHeader(title, icon, subtitle = subtitle)
            InlineError("Journal de classe indisponible : ${agenda.message}", onRetry)
        }
        is UiState.Success -> {
            val lessons = pick(agenda.data)
            val today = LocalDate.now()
            item(key = "$key-header") { SectionHeader(title, icon, count = lessons.size, subtitle = subtitle) }
            if (lessons.isEmpty()) {
                item(key = "$key-empty") { EmptyHint(emptyText) }
            } else {
                items(lessons, key = { "$key-${it.id}" }, contentType = { "lesson" }) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        done = markPast && lesson.date.isBefore(today),
                        badge = badge,
                        onClick = { onOpenAgendaDate(lesson.date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, done: Boolean, badge: String?, onClick: () -> Unit) {
    val color = Color(lesson.color)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (done) 0.65f else 1f)
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (done) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Passée",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    badge?.let {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                        Text(
                            text = lesson.date.format(EVAL_DATE).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${lesson.start} – ${lesson.end}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (lesson.room.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = lesson.room.replace("|", " / "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val detail = listOf(lesson.lesson, lesson.description)
                    .filter { it.isNotBlank() && !it.equals("evaluation", ignoreCase = true) && !it.equals("évaluation", ignoreCase = true) }
                    .joinToString("\n")
                if (detail.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.resultsSection(results: UiState<PepsPage>?, onOpen: () -> Unit, onRetry: () -> Unit) {
    item(key = "results") {
        SectionHeader("Résultats", Icons.AutoMirrored.Outlined.Grading)
        when (results) {
            null, UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is UiState.Error -> InlineError(results.message, onRetry)
            is UiState.Success -> {
                val panel = results.data.panels.firstOrNull { it.kpis.isNotEmpty() }
                if (panel == null) {
                    EmptyHint("Aucune moyenne disponible pour le moment.")
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpen),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            panel.subtitle?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                            }
                            KpiGrid(panel.kpis)
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.shortcutsSection(onOpenTab: (String) -> Unit, onOpenWeb: (String) -> Unit) {
    item(key = "shortcuts") {
        SectionHeader("Raccourcis", Icons.Outlined.Tune)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Shortcut("Mes cours", Icons.AutoMirrored.Outlined.MenuBook) { onOpenTab(Routes.COURSES) }
            Shortcut("Journal", Icons.Outlined.CalendarMonth) { onOpenTab(Routes.AGENDA) }
            Shortcut("Résultats", Icons.AutoMirrored.Outlined.Grading) { onOpenTab(Routes.RESULTS) }
            Shortcut("Site PepSchool", Icons.Outlined.OpenInBrowser) { onOpenWeb("/?page=dashboard") }
        }
    }
}

@Composable
private fun Shortcut(label: String, icon: ImageVector, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

private fun LazyListScope.newsSection(state: DashboardState, onRetry: () -> Unit) {
    item(key = "news-header") {
        SectionHeader("Actualités", Icons.Outlined.Newspaper)
        state.notificationsError?.let { error ->
            Text(
                text = "Notifications indisponibles : $error",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    when (val news = state.news) {
        null, UiState.Loading -> item(key = "news-loading") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        is UiState.Error -> item(key = "news-error") { InlineError(news.message, onRetry) }
        is UiState.Success -> {
            if (news.data.isEmpty()) {
                item(key = "news-empty") { EmptyHint("Aucune actualité pour le moment.") }
            } else {
                items(news.data, contentType = { "news" }) { item -> NewsCard(item) }
            }
        }
    }
}

@Composable
private fun NewsCard(item: NewsItem) {
    var expanded by rememberSaveable(item.title, item.date) { mutableStateOf(false) }
    val accent = newsAccent(item.type)
    val isLong = item.body.length > 280 || item.body.count { it == '\n' } > 5

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                item.badge?.let { badge ->
                    Surface(color = accent.container, contentColor = accent.onContainer, shape = CircleShape) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                item.date?.let { date ->
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.body.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded || !isLong) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isLong) {
                    TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (expanded) "Réduire" else "Lire la suite")
                    }
                }
            }
            if (item.recipients.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.recipients.forEach { recipient ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
                        ) {
                            Text(
                                text = recipient,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, count: Int? = null, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (count != null && count > 0) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun InlineError(message: String, onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry) { Text("Réessayer") }
    }
}

private data class Accent(val container: Color, val onContainer: Color)

@Composable
private fun newsAccent(type: String): Accent {
    val scheme = MaterialTheme.colorScheme
    return when (type.lowercase()) {
        "danger", "urgent", "error", "alert" -> Accent(scheme.errorContainer, scheme.onErrorContainer)
        "warning", "attention" -> Accent(scheme.tertiaryContainer, scheme.onTertiaryContainer)
        "success", "ok" -> Accent(scheme.secondaryContainer, scheme.onSecondaryContainer)
        else -> Accent(scheme.primaryContainer, scheme.onPrimaryContainer)
    }
}
