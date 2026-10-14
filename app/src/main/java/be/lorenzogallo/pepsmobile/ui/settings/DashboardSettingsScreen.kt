package be.lorenzogallo.pepsmobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import be.lorenzogallo.pepsmobile.data.prefs.DashboardWidget
import be.lorenzogallo.pepsmobile.data.prefs.DashboardWidgetConfig
import be.lorenzogallo.pepsmobile.ui.common.LocalAppContainer

@Composable
fun DashboardSettingsScreen(onBack: () -> Unit) {
    val prefs = LocalAppContainer.current.prefs
    val config by prefs.dashboardWidgets.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Personnaliser l'accueil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { prefs.setDashboardWidgets(DashboardWidget.entries.map { DashboardWidgetConfig(it, true) }) }) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Réinitialiser")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "Activez les widgets à afficher et ordonnez-les avec les flèches. Les widgets « système » sont produits par l'application, les autres par PepSchool.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(config, key = { it.widget.id }) { item ->
                val index = config.indexOf(item)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.widget.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (item.widget.system) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                                        Text(
                                            text = "système",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.widget.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = item.enabled, onCheckedChange = { prefs.toggleDashboardWidget(item.widget, it) })
                        Column {
                            IconButton(onClick = { prefs.moveDashboardWidget(item.widget, -1) }, enabled = index > 0) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Monter")
                            }
                            IconButton(onClick = { prefs.moveDashboardWidget(item.widget, 1) }, enabled = index < config.lastIndex) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Descendre")
                            }
                        }
                    }
                }
            }
        }
    }
}
