package be.lorenzogallo.pepsmobile.data.model

enum class Tone { NEUTRAL, SUCCESS, WARNING, DANGER, INFO }

data class Score(
    val display: String,
    val percent: Int?,
    val tone: Tone,
)

data class Kpi(
    val label: String,
    val value: String,
    val tone: Tone,
)

data class ScoreRow(
    val name: String,
    val detail: String?,
    val score: Score?,
    val tone: Tone,
    val courseId: Int? = null,
)

data class Evaluation(
    val date: String,
    val label: String,
    val tags: List<String>,
    val score: Score?,
    val tone: Tone,
    val rows: List<ScoreRow>,
    val comment: String?,
) {
    val hasDetails: Boolean get() = rows.isNotEmpty() || !comment.isNullOrBlank()
}

data class GenericTable(
    val headers: List<String>,
    val rows: List<List<String>>,
)

data class Panel(
    val title: String,
    val subtitle: String?,
    val kpis: List<Kpi> = emptyList(),
    val evaluations: List<Evaluation> = emptyList(),
    val scoreRows: List<ScoreRow> = emptyList(),
    val tables: List<GenericTable> = emptyList(),
    val notes: List<String> = emptyList(),
    val text: String? = null,
) {
    val isEmpty: Boolean
        get() = kpis.isEmpty() && evaluations.isEmpty() && scoreRows.isEmpty() && tables.isEmpty() && text.isNullOrBlank()
}

data class PeriodTab(
    val label: String,
    val path: String,
    val active: Boolean,
)

data class PepsPage(
    val title: String?,
    val hubTabs: List<PeriodTab>,
    val periods: List<PeriodTab>,
    val panels: List<Panel>,
) {
    val isEmpty: Boolean get() = panels.all { it.isEmpty }
}

enum class CourseTab(val pageCours: String, val label: String) {
    EVALUATIONS("evaluations", "Évaluations"),
    STATISTICS("statistiques", "Statistiques"),
}

data class AgendaOverview(
    val thisWeek: List<Lesson>,
    val upcoming: List<Lesson>,
    val weeksAhead: Int,
) {
    val weekEvaluations: List<Lesson> get() = thisWeek.filter { it.hasEvaluation }
    val upcomingEvaluations: List<Lesson> get() = upcoming.filter { it.hasEvaluation }
    fun homework(from: java.time.LocalDate): List<Lesson> =
        (thisWeek + upcoming).filter { it.hasHomework && !it.date.isBefore(from) }.sortedWith(compareBy({ it.date }, { it.start }))
}
