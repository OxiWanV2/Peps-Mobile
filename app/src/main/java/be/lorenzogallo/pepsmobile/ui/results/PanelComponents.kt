package be.lorenzogallo.pepsmobile.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import be.lorenzogallo.pepsmobile.data.model.Evaluation
import be.lorenzogallo.pepsmobile.data.model.GenericTable
import be.lorenzogallo.pepsmobile.data.model.Kpi
import be.lorenzogallo.pepsmobile.data.model.Panel
import be.lorenzogallo.pepsmobile.data.model.PepsPage
import be.lorenzogallo.pepsmobile.data.model.PeriodTab
import be.lorenzogallo.pepsmobile.data.model.Score
import be.lorenzogallo.pepsmobile.data.model.ScoreRow
import be.lorenzogallo.pepsmobile.data.model.Tone

@Composable
fun Tone.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Tone.DANGER -> MaterialTheme.colorScheme.error
        Tone.SUCCESS -> if (dark) Color(0xFF7BD88F) else Color(0xFF2E7D32)
        Tone.WARNING -> if (dark) Color(0xFFFFC46B) else Color(0xFFB26A00)
        Tone.INFO -> MaterialTheme.colorScheme.primary
        Tone.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
}

@Composable
fun Tone.containerColor(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Tone.DANGER -> MaterialTheme.colorScheme.errorContainer
        Tone.SUCCESS -> if (dark) Color(0xFF1F3A26) else Color(0xFFDDF4E0)
        Tone.WARNING -> if (dark) Color(0xFF4A3410) else Color(0xFFFFEBC8)
        Tone.INFO -> MaterialTheme.colorScheme.primaryContainer
        Tone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
fun ScoreBar(percent: Int?, tone: Tone, modifier: Modifier = Modifier) {
    val color = tone.color()
    val fraction = (percent ?: 0).coerceIn(0, 100) / 100f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
    ) {
        if (percent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
fun ScoreText(score: Score, large: Boolean = false) {
    val color = if (score.tone == Tone.NEUTRAL) MaterialTheme.colorScheme.onSurface else score.tone.color()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val slash = score.display.indexOf('/')
    val text = buildAnnotatedString {
        if (slash > 0) {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(score.display.substring(0, slash)) }
            withStyle(SpanStyle(color = muted, fontWeight = FontWeight.Normal)) { append(score.display.substring(slash)) }
        } else {
            val isPlaceholder = score.display == "–" || score.display == "-"
            withStyle(
                SpanStyle(
                    color = if (isPlaceholder) muted else color,
                    fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Bold,
                ),
            ) { append(score.display) }
        }
    }
    Text(
        text = text,
        style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.End,
        maxLines = 1,
    )
}

@Composable
private fun ScoreColumn(score: Score?, large: Boolean = false) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(92.dp)) {
        if (score == null) {
            Text(
                text = "En attente",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ScoreText(score, large)
            if (score.percent != null) {
                Spacer(Modifier.height(4.dp))
                ScoreBar(score.percent, score.tone)
            }
        }
    }
}

@Composable
fun KpiGrid(kpis: List<Kpi>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 2,
    ) {
        kpis.forEach { kpi ->
            Surface(
                color = kpi.tone.containerColor(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = kpi.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = kpi.value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (kpi.tone == Tone.NEUTRAL) MaterialTheme.colorScheme.onSurface else kpi.tone.color(),
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreRowItem(row: ScoreRow, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(if (row.tone == Tone.NEUTRAL) MaterialTheme.colorScheme.outlineVariant else row.tone.color()),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            row.detail?.takeIf { it != row.name }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        ScoreColumn(row.score)
        if (onClick != null) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Ouvrir le cours",
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EvaluationItem(evaluation: Evaluation) {
    var expanded by rememberSaveable(evaluation.date, evaluation.label) { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = evaluation.hasDetails) { expanded = !expanded }
            .padding(vertical = 10.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (evaluation.tone == Tone.NEUTRAL) MaterialTheme.colorScheme.primary else evaluation.tone.color()),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(evaluation.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (evaluation.date.isNotEmpty()) {
                        Text(
                            text = evaluation.date,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    evaluation.tags.forEach { tag ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            ScoreColumn(evaluation.score, large = true)
            if (evaluation.hasDetails) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Détails",
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(22.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, top = 6.dp)) {
                evaluation.rows.forEach { row -> ScoreRowItem(row) }
                evaluation.comment?.let { comment ->
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun GenericTableView(table: GenericTable) {
    val columns = maxOf(table.headers.size, table.rows.maxOfOrNull { it.size } ?: 0)
    if (columns in 1..4) {
        if (table.headers.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(columns) { index ->
                    Text(
                        text = table.headers.getOrNull(index).orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(if (index == 0) 2f else 1f),
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        }
        table.rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(columns) { index ->
                    Text(
                        text = row.getOrNull(index).orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(if (index == 0) 2f else 1f),
                    )
                }
            }
            if (rowIndex < table.rows.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    } else {
        table.rows.forEachIndexed { rowIndex, row ->
            Column {
                row.forEachIndexed { index, cell ->
                    if (cell.isEmpty()) return@forEachIndexed
                    val header = table.headers.getOrNull(index)
                    Text(
                        text = if (header.isNullOrEmpty()) cell else "$header : $cell",
                        style = if (index == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
            if (rowIndex < table.rows.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun PanelCard(panel: Panel, onOpenCourse: ((courseId: Int, name: String) -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(panel.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            panel.subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (panel.kpis.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                KpiGrid(panel.kpis)
            }
            if (panel.evaluations.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                panel.evaluations.forEachIndexed { index, evaluation ->
                    EvaluationItem(evaluation)
                    if (index < panel.evaluations.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (panel.scoreRows.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                panel.scoreRows.forEachIndexed { index, row ->
                    val courseId = row.courseId
                    ScoreRowItem(
                        row = row,
                        onClick = if (courseId != null && onOpenCourse != null) ({ onOpenCourse(courseId, row.name) }) else null,
                    )
                    if (index < panel.scoreRows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            panel.tables.forEach { table ->
                Spacer(Modifier.height(8.dp))
                GenericTableView(table)
            }
            panel.text?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            panel.notes.forEach { note ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun PeriodChips(periods: List<PeriodTab>, selectedPath: String?, onSelect: (PeriodTab) -> Unit) {
    if (periods.size < 2) return
    val activePath = selectedPath ?: periods.firstOrNull { it.active }?.path
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(periods, key = { it.path }) { period ->
            FilterChip(
                selected = period.path == activePath,
                onClick = { onSelect(period) },
                label = { Text(period.label) },
            )
        }
    }
}

@Composable
fun PepsPageContent(
    page: PepsPage,
    onOpenWeb: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onOpenCourse: ((courseId: Int, name: String) -> Unit)? = null,
) {
    if (page.isEmpty) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Aucun résultat pour cette période.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (onOpenWeb != null) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = onOpenWeb) { Text("Ouvrir la version web") }
            }
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(page.panels) { panel -> PanelCard(panel, onOpenCourse) }
    }
}
