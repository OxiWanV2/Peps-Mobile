package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.Evaluation
import be.lorenzogallo.pepsmobile.data.model.GenericTable
import be.lorenzogallo.pepsmobile.data.model.Kpi
import be.lorenzogallo.pepsmobile.data.model.Panel
import be.lorenzogallo.pepsmobile.data.model.PepsPage
import be.lorenzogallo.pepsmobile.data.model.PeriodTab
import be.lorenzogallo.pepsmobile.data.model.Score
import be.lorenzogallo.pepsmobile.data.model.ScoreRow
import be.lorenzogallo.pepsmobile.data.model.Tone
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import kotlin.math.roundToInt

object GradeScale {
    const val WARNING_FROM = 50
    const val SUCCESS_FROM = 65

    fun toneFor(percent: Int): Tone = when {
        percent < WARNING_FROM -> Tone.DANGER
        percent < SUCCESS_FROM -> Tone.WARNING
        else -> Tone.SUCCESS
    }
}

object PanelParser {
    private val TONE = Regex("--(danger|success|warning|info)\\b")
    private val WIDTH = Regex("width\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*%")
    private val FRACTION = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*/\\s*([0-9]+(?:[.,][0-9]+)?)\\s*$")
    private val PERCENT = Regex("^\\s*([0-9]+(?:[.,][0-9]+)?)\\s*%\\s*$")
    private val COURSE_ID = Regex("coursID=(\\d+)")
    private val PLACEHOLDERS = setOf("–", "-", "—", "")

    fun parse(html: String): PepsPage {
        val doc = Jsoup.parse(html)
        val main: Element = doc.selectFirst("body > section") ?: doc.body()

        val title = doc.selectFirst(".hub-bar .breadcrumb .ps-select__value")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val hubTabs = main.select(".hub-tabs a.ps-tab").mapNotNull { a ->
            val label = a.text().trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val path = normalizePath(a.attr("href")) ?: return@mapNotNull null
            PeriodTab(label = label, path = path, active = a.hasClass("is-active"))
        }
        val periods = main.select("ul.nav-tabs a.nav-link").mapNotNull { a ->
            val label = a.text().trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val path = normalizePath(a.attr("href")) ?: return@mapNotNull null
            PeriodTab(label = label, path = path, active = a.hasClass("active"))
        }

        val sections = main.select("section.ps-panel").toList()
        val innermost = sections.filter { s -> sections.none { other -> other !== s && other.parents().contains(s) } }
        val panels = if (innermost.isNotEmpty()) innermost.map { parsePanel(it) } else fallbackPanels(main)

        return PepsPage(
            title = title,
            hubTabs = hubTabs,
            periods = periods,
            panels = panels.filterNot { it.isEmpty },
        )
    }

    fun normalizePath(href: String): String? {
        val h = href.trim()
        if (h.isEmpty() || h.contains("://") || h.startsWith("//")) return null
        return when {
            h.startsWith("/") -> h
            h.startsWith("?") -> "/$h"
            else -> "/$h"
        }
    }

    private fun parsePanel(section: Element): Panel {
        val title = section.selectFirst(".ps-panel__title")?.text()?.trim().orEmpty().ifEmpty { "Résultats" }
        val subtitle = section.selectFirst(".ps-panel__subtitle")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val body = section.selectFirst(".ps-panel__body") ?: section

        val kpis = body.select(".ps-kpi").map { kpi ->
            val valueEl = kpi.selectFirst(".ps-kpi__v")
            val label = kpi.selectFirst(".ps-kpi__k")?.text()?.trim().orEmpty()
            val value = valueEl?.text()?.trim().orEmpty()
            val siteTone = toneOf(valueEl?.className().orEmpty())
            val isGrade = siteTone != Tone.NEUTRAL || label.contains("moyenne", ignoreCase = true) || label.contains("note", ignoreCase = true)
            val percent = if (isGrade) percentOf(value.replace(" ", "")) else null
            Kpi(label = label, value = value, tone = percent?.let { GradeScale.toneFor(it) } ?: siteTone)
        }

        val evaluations = body.select(".ps-eval").map { parseEvaluation(it) }

        val scoreRows = mutableListOf<ScoreRow>()
        val tables = mutableListOf<GenericTable>()
        body.select("table").filter { table -> table.parents().none { it.hasClass("ps-eval") } }.forEach { table ->
            val rows = tableRows(table)
            if (rows.any { cells -> cells.any { isScoreCell(it) } }) {
                rows.forEach { cells -> scoreRowOf(cells)?.let { scoreRows += it } }
            } else {
                genericTable(table)?.let { tables += it }
            }
        }

        val notes = body.select("p.ps-muted").map { HtmlUtils.textWithLineBreaks(it) }.filter { it.isNotBlank() }

        val text = if (kpis.isEmpty() && evaluations.isEmpty() && scoreRows.isEmpty() && tables.isEmpty()) {
            val copy = body.clone()
            copy.select("script, style, noscript, .spinner-border, .visually-hidden, p.ps-muted, [id\$=Loading]").remove()
            HtmlUtils.textWithLineBreaks(copy).takeIf { it.isNotBlank() }
        } else null

        return Panel(title, subtitle, kpis, evaluations, scoreRows, tables, notes, text)
    }

    private fun parseEvaluation(eval: Element): Evaluation {
        val tone = toneOf(eval.className())
        val date = eval.selectFirst(".ps-eval__date")?.text()?.trim().orEmpty()
        val labelEl = eval.selectFirst(".ps-eval__label")
        val label = labelEl?.selectFirst(".fw-semibold")?.text()?.trim()
            ?: labelEl?.text()?.trim().orEmpty()
        val tags = labelEl?.select(".badge")?.map { it.text().trim() }?.filter { it.isNotEmpty() }.orEmpty()
        val score = eval.selectFirst(".ps-eval__note")?.let { parseScore(it) }

        val detail = eval.selectFirst(".ps-eval__detail")
        val rows = detail?.select("table")?.flatMap { table -> tableRows(table).mapNotNull { scoreRowOf(it) } }.orEmpty()
        val comment = detail?.let { d ->
            val copy = d.clone()
            copy.select("table").remove()
            HtmlUtils.textWithLineBreaks(copy).takeIf { it.isNotBlank() }
        }
        return Evaluation(date, label, tags, score, tone, rows, comment)
    }

    private fun tableRows(table: Element): List<List<Element>> {
        val bodyRows = table.select("tbody tr").toList().ifEmpty { table.select("tr").toList() }
        return bodyRows
            .filter { tr -> tr.parents().firstOrNull { it.tagName() == "table" } === table }
            .map { tr -> tr.select("> td, > th").toList() }
            .filter { cells -> cells.isNotEmpty() && cells.any { it.text().isNotBlank() } && !cells.all { it.tagName() == "th" } }
    }

    private fun scoreRowOf(cells: List<Element>): ScoreRow? {
        if (cells.isEmpty()) return null
        val scoreIndex = cells.indexOfLast { isScoreCell(it) || it.text().trim() in PLACEHOLDERS && it.selectFirst(".ps-muted") != null }
        val nameIndex = cells.indices.firstOrNull { it != scoreIndex && cells[it].text().isNotBlank() } ?: return null
        val nameCell = cells[nameIndex]
        val name = (nameCell.selectFirst(".fw-semibold")?.text() ?: nameCell.text()).trim()
        if (name.isEmpty()) return null

        val details = mutableListOf<String>()
        nameCell.select(".ps-muted").forEach { details += it.text().trim() }
        cells.forEachIndexed { index, cell ->
            if (index == nameIndex || index == scoreIndex) return@forEachIndexed
            val spans = cell.select("span").map { it.text().trim() }.filter { it.isNotEmpty() }
            if (spans.isNotEmpty()) details += spans else cell.text().trim().takeIf { it.isNotEmpty() }?.let { details += it }
        }
        val detail = details.filter { it.isNotEmpty() && it != name }.distinct().joinToString(", ").takeIf { it.isNotEmpty() }

        val score = if (scoreIndex >= 0) parseScore(cells[scoreIndex]) else null
        val row = nameCell.parent()
        val rowTone = toneOf(row?.className().orEmpty()).takeIf { it != Tone.NEUTRAL } ?: score?.tone ?: Tone.NEUTRAL
        val courseId = row?.selectFirst("a[href*=coursID]")?.attr("href")?.let { COURSE_ID.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        return ScoreRow(name = name, detail = detail, score = score, tone = rowTone, courseId = courseId)
    }

    private fun genericTable(table: Element): GenericTable? {
        val headers = table.select("thead th, thead td").map { it.text().trim() }
            .ifEmpty { table.selectFirst("tr")?.select("th")?.map { it.text().trim() } ?: emptyList() }
        val rows = tableRows(table).map { cells -> cells.map { it.text().trim() } }
        if (rows.isEmpty()) return null
        return GenericTable(headers, rows)
    }

    private fun isScoreCell(cell: Element?): Boolean {
        if (cell == null) return false
        return cell.selectFirst("[class*=ps-res-note--], [class*=ps-res-bar]") != null
    }

    fun parseScore(el: Element): Score? {
        val noteEl = el.selectFirst("[class*=ps-res-note--]") ?: el.selectFirst(".fw-semibold")
        val display = (noteEl?.text() ?: el.text()).trim().replace(Regex("\\s+"), "")
            .ifEmpty { return null }
        val siteTone = toneOf(noteEl?.className() ?: el.className())
        val barWidth = el.selectFirst("[class*=ps-res-bar--]")?.attr("style")?.let { WIDTH.find(it)?.groupValues?.get(1)?.toDoubleOrNull() }
        val percent = (barWidth?.roundToInt() ?: percentOf(display))?.coerceIn(0, 100)
        return Score(display = display, percent = percent, tone = percent?.let { GradeScale.toneFor(it) } ?: siteTone)
    }

    fun percentOf(display: String): Int? {
        PERCENT.find(display)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()?.let { return it.roundToInt() }
        val m = FRACTION.find(display) ?: return null
        val a = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val b = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        if (b <= 0) return null
        return (a / b * 100).roundToInt()
    }

    fun toneOf(className: String): Tone = when (TONE.find(className)?.groupValues?.get(1)) {
        "danger" -> Tone.DANGER
        "success" -> Tone.SUCCESS
        "warning" -> Tone.WARNING
        "info" -> Tone.INFO
        else -> Tone.NEUTRAL
    }

    private fun fallbackPanels(main: Element): List<Panel> {
        val panels = mutableListOf<Panel>()
        main.select("table").forEach { table ->
            val title = table.selectFirst("caption")?.text()?.trim()?.takeIf { it.isNotEmpty() }
                ?: previousHeading(table) ?: "Résultats"
            val rows = tableRows(table)
            if (rows.any { cells -> cells.any { isScoreCell(it) } }) {
                panels += Panel(title, null, scoreRows = rows.mapNotNull { scoreRowOf(it) })
            } else {
                genericTable(table)?.let { panels += Panel(title, null, tables = listOf(it)) }
            }
        }
        main.select(".card").filter { it.select("table").isEmpty() && it.select(".card").none { inner -> inner !== it } }
            .forEach { card ->
                val title = card.selectFirst(".card-header, .card-title, h1, h2, h3, h4, h5, h6")?.text()?.trim().orEmpty()
                val bodyEl = card.selectFirst(".card-body") ?: card
                val text = HtmlUtils.textWithLineBreaks(bodyEl).removePrefix(title).trim()
                if (title.isNotEmpty() || text.isNotEmpty()) panels += Panel(title.ifEmpty { "Informations" }, null, text = text.ifEmpty { null })
            }
        return panels
    }

    private fun previousHeading(el: Element): String? {
        var current: Element? = el
        while (current != null && current.tagName() != "body") {
            var sibling = current.previousElementSibling()
            while (sibling != null) {
                val heading = if (sibling.tagName() in HEADINGS) sibling else sibling.selectFirst("h1, h2, h3, h4, h5, h6, .card-header")
                heading?.text()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
                sibling = sibling.previousElementSibling()
            }
            current = current.parent()
        }
        return null
    }

    private val HEADINGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
}
