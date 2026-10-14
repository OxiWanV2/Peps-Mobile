package be.lorenzogallo.pepsmobile.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import be.lorenzogallo.pepsmobile.PepsApp
import be.lorenzogallo.pepsmobile.data.model.Lesson
import be.lorenzogallo.pepsmobile.data.model.mondayOfWeek
import be.lorenzogallo.pepsmobile.data.model.toUserMessage
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val ids = manager.getAppWidgetIds(ComponentName(applicationContext, PepsWidgetProvider::class.java))
        if (ids.isEmpty()) return Result.success()

        val container = (applicationContext as PepsApp).container
        val content = try {
            buildContent(container.repository)
        } catch (e: Exception) {
            WidgetContent.error(e.toUserMessage())
        }
        val views = WidgetRenderer.render(applicationContext, content)
        ids.forEach { manager.updateAppWidget(it, views) }
        return Result.success()
    }

    private suspend fun buildContent(repository: PepsRepository): WidgetContent {
        if (!repository.hasStoredCredentials()) return WidgetContent.loggedOut()

        val today = LocalDate.now()
        val now = LocalTime.now()
        val thisWeek = repository.fetchAgenda(today.mondayOfWeek())
        val nextWeek = repository.fetchAgenda(today.mondayOfWeek().plusWeeks(1))
        val all = (thisWeek + nextWeek).sortedWith(compareBy({ it.date }, { it.start }))

        val remainingToday = all.filter { it.date == today && parseTime(it.end)?.isAfter(now) == true }
        val (dayLabel, lessons) = if (remainingToday.isNotEmpty()) {
            "Aujourd'hui" to remainingToday
        } else {
            val nextDay = all.firstOrNull { it.date.isAfter(today) }?.date
            if (nextDay == null) "Aucun cours à venir" to emptyList()
            else {
                val label = if (nextDay == today.plusDays(1)) "Demain" else nextDay.format(DAY).replaceFirstChar { it.uppercase() }
                label to all.filter { it.date == nextDay }
            }
        }

        val nextEvaluation = all.firstOrNull { it.hasEvaluation && !it.date.isBefore(today) }
        val footer = nextEvaluation?.let { "Prochaine éval. : ${it.date.format(SHORT_DAY)} · ${it.title}" }
            ?: "Aucune évaluation annoncée sur 2 semaines"

        return WidgetContent(
            title = "$dayLabel · ${today.format(SHORT_DAY)}",
            lines = lessons.take(MAX_LINES).map { formatLesson(it) },
            footer = footer,
            moreCount = (lessons.size - MAX_LINES).coerceAtLeast(0),
        )
    }

    private fun formatLesson(lesson: Lesson): String {
        val room = lesson.room.takeIf { it.isNotEmpty() }?.let { " · ${it.replace("|", "/")}" }.orEmpty()
        val badge = when {
            lesson.hasEvaluation -> " · Évaluation"
            lesson.hasHomework -> " · Devoir"
            else -> ""
        }
        return "${lesson.start}  ${lesson.title}$room$badge"
    }

    private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()

    private companion object {
        const val MAX_LINES = 4
        val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
        val SHORT_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)
    }
}
