package be.lorenzogallo.pepsmobile.ui.notifications

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.data.model.PepsNotification
import be.lorenzogallo.pepsmobile.ui.common.EmptyView
import be.lorenzogallo.pepsmobile.ui.dashboard.DashboardViewModel
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun NotificationsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::markAllRead, enabled = state.unreadCount > 0) {
                        Icon(Icons.Filled.DoneAll, contentDescription = "Tout marquer comme lu")
                    }
                    IconButton(onClick = { confirmDeleteAll = true }, enabled = state.notifications.isNotEmpty()) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = "Tout supprimer")
                    }
                },
            )
        },
    ) { padding ->
        if (state.notifications.isEmpty()) {
            EmptyView("Aucune notification", modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = { viewModel.markRead(notification.id) },
                        onDelete = { viewModel.delete(notification.id) },
                        onOpenLink = { link ->
                            viewModel.markRead(notification.id)
                            onOpenLink(link)
                        },
                    )
                }
            }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Supprimer toutes les notifications ?") },
            text = { Text("Cette action est définitive, également sur PepSchool.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    viewModel.deleteAll()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Annuler") } },
        )
    }
}

@Composable
private fun NotificationCard(
    notification: PepsNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    var expanded by rememberSaveable(notification.id) { mutableStateOf(false) }
    val colors = if (notification.read) CardDefaults.cardColors()
    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)

    Card(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable {
                expanded = !expanded
                onClick()
            },
    ) {
        Row(modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(
                        color = if (notification.read) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.read) FontWeight.Normal else FontWeight.Bold,
                )
                if (notification.text.isNotEmpty()) {
                    Text(
                        text = notification.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                notification.link?.let { link ->
                    TextButton(onClick = { onOpenLink(link) }, contentPadding = PaddingValues(0.dp)) {
                        Text("Voir")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatRelativeDate(notification.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Close, contentDescription = "Supprimer", modifier = Modifier.size(18.dp))
            }
        }
    }
}

private val DATE_PATTERNS = listOf(
    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
)
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)

fun formatRelativeDate(raw: String): String {
    val parsed = DATE_PATTERNS.firstNotNullOfOrNull { pattern ->
        try {
            LocalDateTime.parse(raw.trim(), pattern)
        } catch (_: DateTimeParseException) {
            null
        }
    } ?: return raw
    val minutes = Duration.between(parsed, LocalDateTime.now()).toMinutes()
    return when {
        minutes < 1 -> "À l'instant"
        minutes < 60 -> "Il y a $minutes min"
        minutes < 60 * 24 -> "Il y a ${minutes / 60}h"
        minutes < 60 * 48 -> "Hier"
        else -> parsed.format(SHORT_DATE)
    }
}
