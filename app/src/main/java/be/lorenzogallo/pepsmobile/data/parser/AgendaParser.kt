package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.Lesson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate

object AgendaParser {
    fun parse(body: String): List<Lesson> {
        val root = parseAjaxObject(body)
        val entries: List<JsonObject> = when (val liste = root["listeHeureCours"]) {
            is JsonObject -> liste.values.filterIsInstance<JsonObject>()
            is JsonArray -> liste.filterIsInstance<JsonObject>()
            else -> emptyList()
        }
        return entries.mapNotNull { o ->
            val date = runCatching { LocalDate.parse(o.str("date")) }.getOrNull() ?: return@mapNotNull null
            val badges = o["badges"] as? JsonObject
            Lesson(
                id = o.long("id") ?: o.long("heureCours_id") ?: 0L,
                courseId = o.int("cours_id") ?: 0,
                title = HtmlUtils.unescape(o.str("intitule")),
                start = o.str("heureStart"),
                end = o.str("heureEnd"),
                date = date,
                room = o.str("local").trim(),
                color = HtmlUtils.parseHexColor(o.str("color"), HtmlUtils.DEFAULT_COURSE_COLOR),
                lesson = HtmlUtils.htmlToText(o.str("lecon")),
                description = HtmlUtils.htmlToText(o.str("description")),
                hasEvaluation = boolish(badges?.get("evaluation")),
                hasHomework = boolish(badges?.get("devoir")),
            )
        }.sortedWith(compareBy({ it.date }, { it.start }, { it.end }, { it.title }))
    }
}
