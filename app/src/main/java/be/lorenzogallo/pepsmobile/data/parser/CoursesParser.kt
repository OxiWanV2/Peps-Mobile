package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.Course
import org.jsoup.Jsoup

object CoursesParser {
    private val COURSE_ID = Regex("coursID=(\\d+)")

    fun parse(html: String): List<Course> {
        val doc = Jsoup.parse(html)
        return doc.select("div.cardCours").mapNotNull { card ->
            val href = card.selectFirst("a[href*=coursID]")?.attr("href") ?: return@mapNotNull null
            val id = COURSE_ID.find(href)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null

            val header = card.selectFirst(".card-header")
            val style = header?.attr("style").orEmpty()
            val background = HtmlUtils.parseHexColor(
                HtmlUtils.styleProperty(style, "background-color"),
                HtmlUtils.DEFAULT_COURSE_COLOR,
            )
            val foreground = HtmlUtils.parseHexColor(HtmlUtils.styleProperty(style, "color"), HtmlUtils.BLACK)

            val span = header?.selectFirst("span")
            val tooltip = span?.attr("data-bs-title").orEmpty()
            val teachers = if (tooltip.isNotBlank()) {
                HtmlUtils.splitOnBr(tooltip)
            } else {
                listOfNotNull(span?.text()?.trim()?.takeIf { it.isNotEmpty() })
            }

            val name = card.selectFirst("h6.card-title a")?.text()?.trim()
                ?: card.selectFirst(".card-title")?.text()?.trim()
                ?: return@mapNotNull null

            Course(id = id, name = name, teachers = teachers, color = background, textColor = foreground)
        }.distinctBy { it.id }
    }
}
